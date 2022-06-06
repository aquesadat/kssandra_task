package com.kssandra.ksd_task.provider;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kssandra.ksd_common.dto.CryptoCurrencyDto;
import com.kssandra.ksd_common.dto.CryptoDataDto;
import com.kssandra.ksd_persistence.dao.CryptoCurrencyDao;
import com.kssandra.ksd_persistence.dao.CryptoDataDao;

@Component
public abstract class CryptoDataProvider {

	private static final Logger LOG = LoggerFactory.getLogger(CryptoDataProvider.class);

	@Autowired
	protected CryptoCurrencyDao cryptoCurrDao;

	@Autowired
	private CryptoDataDao cryptoDataDao;

	private List<CryptoCurrencyDto> cxCurrDtos = null;

	public Map<String, List<CryptoDataDto>> getIntraDayData() {

		ExecutorService pool = Executors.newFixedThreadPool(getThreads());
		List<Future<?>> results = new ArrayList<>();

		Map<String, List<CryptoDataDto>> cryptoData = new HashMap<>();

		List<CryptoCurrencyDto> activeCxCurrs = getActiveCryptoCurrencies();

		if (LOG.isInfoEnabled()) {
			LOG.info("Getting data for: {}",
					activeCxCurrs.stream().map(CryptoCurrencyDto::getCode).collect(Collectors.joining(", ")));
		}

		resetClient();

		for (CryptoCurrencyDto cxCurr : activeCxCurrs) {
			Future<?> future = submitThread(pool, cxCurr);
			LOG.debug("{}  launched", cxCurr.getCode());
			results.add(future);
		}

		pool.shutdown();

		for (Future<?> result : results) {
			try {
				List<CryptoDataDto> dataList = mapIntraDayRs(result.get());
				cryptoData.put(dataList.get(0).getCxCurrencyDto().getCode(), dataList);
				LOG.debug("Data already obtained for: {}", dataList.get(0).getCxCurrencyDto().getCode());
			} catch (InterruptedException | ExecutionException e) {
				LOG.error(e.getMessage());
			}
		}

		return cryptoData;
	}

	protected Future<?> submitThread(ExecutorService pool, CryptoCurrencyDto cxCurr) {
		return pool.submit(getIntraDayClient(cxCurr));
	}

	public void saveDataResult(Map<String, List<CryptoDataDto>> dataResult) {

		ExecutorService singleThread = Executors.newSingleThreadExecutor();

		singleThread.execute(() -> {
			Map<String, LocalDateTime> lastInserts = cryptoDataDao.getLastInserts(cxCurrDtos);
			LOG.info("Saving data results");

			List<CryptoDataDto> temp = new ArrayList<>();
			dataResult.forEach((key, cxData) -> {
				LOG.info("Saving data for: {}", key);
				temp.addAll(cxData.parallelStream().filter(elem -> elem.getReadTime().isAfter(lastInserts.get(key)))
						.collect(Collectors.toList()));
			});

			cryptoDataDao.saveAll(temp);
			System.out.println("Estoy dentro del hilo");
		});

		System.out.println("Estoy fuera del hilo");
	}

	public abstract String getType();

	protected abstract List<CryptoDataDto> mapIntraDayRs(Object result);

	protected abstract Callable getIntraDayClient(CryptoCurrencyDto cxCurrency);

	protected abstract void resetClient();

	protected List<CryptoCurrencyDto> getActiveCryptoCurrencies() {
		if (cxCurrDtos == null) {
			cxCurrDtos = cryptoCurrDao.getAllActiveCryptoCurrencies();
		}
		return cxCurrDtos;
	}

	protected int getThreads() {
		int nThreads = Runtime.getRuntime().availableProcessors();
		LOG.info("Threads to create: {}", nThreads);
		return nThreads;
	}

}
