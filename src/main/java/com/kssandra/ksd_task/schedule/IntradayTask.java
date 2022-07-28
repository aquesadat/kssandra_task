package com.kssandra.ksd_task.schedule;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kssandra.ksd_common.dto.CryptoCurrencyDto;
import com.kssandra.ksd_common.dto.CryptoDataDto;
import com.kssandra.ksd_common.exception.DataCollectException;
import com.kssandra.ksd_persistence.dao.CryptoCurrencyDao;
import com.kssandra.ksd_task.prediction.CryptoDataEval;
import com.kssandra.ksd_task.prediction.CryptoDataPrediction;
import com.kssandra.ksd_task.provider.CryptoDataProvider;
import com.kssandra.ksd_task.provider.factory.CryptoDataProviderFactory;

/**
 * 
 * Scheduled task to get intraday data from a provider (like AlphaVantage),
 * analyze and save it in DB.
 * 
 * @author aquesada
 *
 */
@Component
public class IntradayTask {

	private static final Logger LOG = LoggerFactory.getLogger(IntradayTask.class);

	@Value(value = "${crypto.data.provider}")
	private String provider;

	@Autowired
	private CryptoDataPrediction cxDataPrediction;

	@Autowired
	private CryptoDataEval cxDataEval;

	@Autowired
	protected CryptoCurrencyDao cryptoCurrDao;

	/**
	 * Gets intraday data concurrently from a provider, makes predictions and
	 * finally checks success of old predictions.
	 */
	@Scheduled(cron = "${intraday.cron.expression}")
	// Scheduled(fixedDelay = Long.MAX_VALUE)
	public void scheduleTask() {

		LOG.debug("Executing scheduled task");

		List<CryptoCurrencyDto> activeCxCurrs = cryptoCurrDao.getAllActiveCxCurrencies();

		if (!activeCxCurrs.isEmpty()) {
			try {
				// Gets the configured provider to operate with
				CryptoDataProvider dataProvider = CryptoDataProviderFactory.getDataProvider(provider);

				// Gets intraday data from the provider and persists it in DB
				Map<String, List<CryptoDataDto>> dataResult = dataProvider.collectIntraDayData(activeCxCurrs);

				if (dataResult != null && !dataResult.isEmpty()) {
					// Checks success of old predictions in a new thread
					try {
						Executors.newSingleThreadExecutor()
								.execute(() -> cxDataEval.evaluatePredictions(dataResult, activeCxCurrs));
					} catch (Exception ex) {
						LOG.error("Error:", ex);
					}

					// Makes new predictions with data previously obtained from the provider
					cxDataPrediction.predictResults(activeCxCurrs);

				} else {
					LOG.error("Any data has been collected from provider");
				}
			} catch (DataCollectException e) {
				LOG.error("Error collecting data: ", e);
			}
		} else {
			LOG.warn("Any cryptocurrency configured as active");
		}

	}

}
