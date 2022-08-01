package com.kssandra.ksd_task.prediction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kssandra.ksd_common.dto.CryptoCurrencyDto;
import com.kssandra.ksd_common.dto.PredictionDto;
import com.kssandra.ksd_common.util.DateUtils;
import com.kssandra.ksd_common.dto.CryptoDataDto;
import com.kssandra.ksd_persistence.dao.PredictionDao;

/**
 * Class to evaluate predictions calculating the level of success of them once
 * real data has been collected.
 * 
 * @author aquesada
 *
 */
@Component
public class CryptoDataEval {

	private static final Logger LOG = LoggerFactory.getLogger(CryptoDataEval.class);

	@Autowired
	private PredictionDao predictionDao;

	/**
	 * For each crypto currency: Get its predictions from bbdd, calculates the
	 * success of them comparing with the real data and persists the results in
	 * bbdd.
	 * 
	 * @param dataResult    Map with cryptocurrency code as key and real data
	 *                      obtained from provided as value
	 * @param activeCxCurrs List of active cryptocurrencies
	 */
	public void evaluatePredictions(Map<String, List<CryptoDataDto>> dataResult,
			List<CryptoCurrencyDto> activeCxCurrs) {

		LOG.debug("Evaluating predictions");

		dataResult.forEach((cxCode, data) -> {

			// Gets time of the last data obtained from provider
			LocalDateTime predictTime = data.stream().max((e1, e2) -> e1.getReadTime().compareTo(e2.getReadTime()))
					.get().getReadTime();

			// Gets from DB all the unanalyzed predictions for a specific cryptocurrency and
			// before or equals to last data read from provider
			CryptoCurrencyDto cxCurrDto = activeCxCurrs.parallelStream().filter(elem -> elem.getCode().equals(cxCode))
					.findAny().get();
			List<PredictionDto> predictions = predictionDao.findUnanalyzed(cxCurrDto, predictTime);

			if (predictions != null && !predictions.isEmpty()) {
				for (PredictionDto predDto : predictions) {
					// For each unanalyzed prediction found in DB, gets the real data read from
					// provider when its read time is before to the prediction time.
					Optional<CryptoDataDto> dataRead = data.stream()
							.filter(item -> item.getReadTime().compareTo(predDto.getPredictTime()) <= 0)
							.max((e1, e2) -> e1.getReadTime().compareTo(e2.getReadTime()));

					if (dataRead.isPresent()) {
						if (DateUtils.toSeconds(predDto.getPredictTime())
								- DateUtils.toSeconds(dataRead.get().getReadTime()) < 60) { // 60s as max margin.
																							// Predictions
							// can´t be done at 00s
							predDto.setSuccess(getSuccess(dataRead.get(), predDto.getPredictVal()));
						} else {
							LOG.error("Read time: {} is too far from the predicted one: {}",
									dataRead.get().getReadTime(), predDto.getPredictTime());
						}
					} else {
						LOG.warn("Existing prediction (id: {}) is too old to check its success.", predDto.getId());
					}

				}
				predictionDao
						.saveAll(predictions.stream().filter(e -> e.getSuccess() != null).collect(Collectors.toList()));
			} else {
				LOG.error("Predictions is null or empty");
			}
		});

		LOG.debug("End of evaluating predictions");
	}

	/**
	 * Calculates level of success comparing real read value to predicted value
	 * 
	 * @param dataRead   Real value read from provider
	 * @param predictVal Expected value predicted
	 * @return Value from 0 to 100 (percentage) indicating the success of the
	 *         prediction
	 */
	private Double getSuccess(CryptoDataDto dataRead, double predictVal) {
		double low = dataRead.getLow();
		double high = dataRead.getHigh();

		Double success = null;

		if (predictVal <= high && predictVal >= low) {
			success = Double.valueOf(0);
		} else {
			if (predictVal > high) {
				success = Double.valueOf((predictVal * 100 / high) - 100);
			} else {
				success = Double.valueOf(100 - (predictVal * 100 / low));
			}
		}

		return success;
	}
}
