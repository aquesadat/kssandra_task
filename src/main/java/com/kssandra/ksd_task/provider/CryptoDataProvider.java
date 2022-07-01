package com.kssandra.ksd_task.provider;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kssandra.alphavantage_client.output.IntraDay;
import com.kssandra.ksd_common.dto.CryptoCurrencyDto;
import com.kssandra.ksd_common.dto.CryptoDataDto;
import com.kssandra.ksd_common.exception.DataCollectException;
import com.kssandra.ksd_persistence.dao.CryptoDataDao;

/**
 * Abstract class to collect intraday data using multiple providers.
 * 
 * @author aquesada
 *
 */
@Component
public abstract class CryptoDataProvider {

	@Autowired
	private CryptoDataDao cryptoDataDao;

	private static final Logger LOG = LoggerFactory.getLogger(CryptoDataProvider.class);

	/**
	 * Gets intraday data concurrently from the provider and persists it in DB
	 * 
	 * @param activeCxCurrs
	 * @return
	 */
	public Map<String, List<CryptoDataDto>> collectIntraDayData(List<CryptoCurrencyDto> activeCxCurrs) {

		ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		List<Future<?>> results = new ArrayList<>();
		Map<String, List<CryptoDataDto>> cryptoData = new HashMap<>();

		if (LOG.isInfoEnabled()) {
			LOG.info("Getting data from {} for: {}", this.getType(),
					activeCxCurrs.stream().map(CryptoCurrencyDto::getCode).collect(Collectors.joining(", ")));
		}

		// Preparing for a new batch of executions
		resetDataProvider();

		// Each cxcurrency is exectuted in a new thread
		activeCxCurrs.forEach(cxCurr -> results.add(pool.submit(() -> callService(cxCurr))));
		pool.shutdown();

		for (Future<?> result : results) {
			try {
				List<CryptoDataDto> dataList = mapIntraDayRs(result.get());
				cryptoData.put(dataList.get(0).getCxCurrencyDto().getCode(), dataList);
				LOG.info("Data already obtained and mapped for: {}", dataList.get(0).getCxCurrencyDto().getCode());
			} catch (InterruptedException ie) {
				LOG.error("Interrupted", ie);
				Thread.currentThread().interrupt();
			} catch (ExecutionException | DataCollectException e) {
				LOG.error(e.getMessage());
			}
		}

		if (!cryptoData.isEmpty()) {
			saveDataResult(cryptoData, activeCxCurrs);
		}

		return cryptoData;
	}

	protected abstract IntraDay callService(CryptoCurrencyDto cxCurr);

	/**
	 * Persists in DB all intraday data (from all the active cryptocurrencies) from
	 * the previous WS call response
	 * 
	 * @param dataResult    Map with cryptocurrency code as key and a list of
	 *                      intraday data as value
	 * @param activeCxCurrs List of active cryptocurrencies
	 */
	private void saveDataResult(Map<String, List<CryptoDataDto>> dataResult, List<CryptoCurrencyDto> activeCxCurrs) {

		LOG.debug("Saving data results");

		Map<String, LocalDateTime> lastInserted = cryptoDataDao.getLastInserted(activeCxCurrs);

		List<CryptoDataDto> dataTosave = new ArrayList<>();

		dataResult.forEach((cxCode, cxData) -> {
			LOG.info("Saving data for: {}", cxCode);
			if (lastInserted.get(cxCode) != null) {
				// Inserts in DB the elements with a date after the last insertion
				dataTosave.addAll(
						cxData.parallelStream().filter(elem -> elem.getReadTime().isAfter(lastInserted.get(cxCode)))
								.collect(Collectors.toList()));
			} else {
				LOG.info("New crypto currency -> Initial charge");
				dataTosave.addAll(cxData.parallelStream().collect(Collectors.toList()));
			}

		});

		cryptoDataDao.saveAll(dataTosave);
	}

	/**
	 * Returns the identifier of each provider to use it in
	 * CryptoDataProviderFactory
	 * 
	 * @return the identifier of the provider
	 */
	public abstract String getType();

	/**
	 * Maps response from alphavantage_client into CryptoDataDto
	 * 
	 * @param result provider response
	 * @return list containing all the intraday data for a specific cryptocurrency
	 * @throws DataCollectException
	 */
	protected abstract List<CryptoDataDto> mapIntraDayRs(Object result) throws DataCollectException;

	/**
	 * Prepares the specific data provider for a new batch of executions
	 */
	protected abstract void resetDataProvider();

}
