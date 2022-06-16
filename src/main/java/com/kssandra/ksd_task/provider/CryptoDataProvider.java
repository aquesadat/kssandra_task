package com.kssandra.ksd_task.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.kssandra.ksd_common.dto.CryptoCurrencyDto;
import com.kssandra.ksd_common.dto.CryptoDataDto;

@Component
public abstract class CryptoDataProvider {

	private static final Logger LOG = LoggerFactory.getLogger(CryptoDataProvider.class);

	public Map<String, List<CryptoDataDto>> getIntraDayData(List<CryptoCurrencyDto> activeCxCurrs) {

		ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		List<Future<?>> results = new ArrayList<>();

		Map<String, List<CryptoDataDto>> cryptoData = new HashMap<>();

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

	public abstract String getType();

	protected abstract List<CryptoDataDto> mapIntraDayRs(Object result);

	protected abstract Callable getIntraDayClient(CryptoCurrencyDto cxCurrency);

	protected abstract void resetClient();

}
