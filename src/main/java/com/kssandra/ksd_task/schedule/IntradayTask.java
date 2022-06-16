package com.kssandra.ksd_task.schedule;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kssandra.ksd_common.dto.CryptoCurrencyDto;
import com.kssandra.ksd_common.dto.CryptoDataDto;
import com.kssandra.ksd_persistence.dao.CryptoCurrencyDao;
import com.kssandra.ksd_persistence.dao.CryptoDataDao;
import com.kssandra.ksd_task.prediction.CryptoDataPrediction;
import com.kssandra.ksd_task.provider.CryptoDataProvider;
import com.kssandra.ksd_task.provider.factory.CryptoDataProviderFactory;

@Component
public class IntradayTask {

	private static final Logger LOG = LoggerFactory.getLogger(IntradayTask.class);

	@Value(value = "${crypto.data.provider}")
	private String provider;

	@Autowired
	private CryptoDataPrediction cxDataProcessor;

	@Autowired
	protected CryptoCurrencyDao cryptoCurrDao;

	@Autowired
	private CryptoDataDao cryptoDataDao;

	// @Scheduled(cron = "${intraday.cron.expression}")
	@Scheduled(fixedDelay = Long.MAX_VALUE)
	public void scheduleTask() {

		LOG.debug("Executing scheduled task");

		List<CryptoCurrencyDto> activeCxCurrs = cryptoCurrDao.getAllActiveCryptoCurrencies();

		CryptoDataProvider dataProvider = CryptoDataProviderFactory.getDataProvider(provider);

		Map<String, List<CryptoDataDto>> dataResult = dataProvider.getIntraDayData(activeCxCurrs);

		// Se deja el save fuera por si hay que controlar alguna excepción desde este
		// punto y guardar o no en función de esto.
		saveDataResult(dataResult, activeCxCurrs);

		cxDataProcessor.evaluatePredictions(dataResult, activeCxCurrs);

		cxDataProcessor.predictResults(activeCxCurrs);

	}

	public void saveDataResult(Map<String, List<CryptoDataDto>> dataResult, List<CryptoCurrencyDto> activeCxCurrs) {

		Map<String, LocalDateTime> lastInserted = cryptoDataDao.getLastInserted(activeCxCurrs);
		LOG.info("Saving data results");

		List<CryptoDataDto> temp = new ArrayList<>();
		dataResult.forEach((key, cxData) -> {
			LOG.info("Saving data for: {}", key);
			temp.addAll(cxData.parallelStream().filter(elem -> elem.getReadTime().isAfter(lastInserted.get(key)))
					.collect(Collectors.toList()));
		});

		cryptoDataDao.saveAll(temp);
	}

}
