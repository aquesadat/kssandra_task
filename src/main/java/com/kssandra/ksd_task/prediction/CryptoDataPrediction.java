package com.kssandra.ksd_task.prediction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kssandra.ksd_common.dto.CryptoCurrencyDto;
import com.kssandra.ksd_common.dto.CryptoDataDto;
import com.kssandra.ksd_common.dto.PredictionCfgDto;
import com.kssandra.ksd_common.dto.PredictionDto;
import com.kssandra.ksd_common.util.DateUtils;
import com.kssandra.ksd_core.prediction.KSDPrediction;
import com.kssandra.ksd_persistence.dao.CryptoDataDao;
import com.kssandra.ksd_persistence.dao.PredictionCfgDao;
import com.kssandra.ksd_persistence.dao.PredictionDao;

/**
 * Class to make predictions of expected values at different times in the future
 * 
 * @author aquesada
 *
 */
@Component
public class CryptoDataPrediction {

	private static final Logger LOG = LoggerFactory.getLogger(CryptoDataPrediction.class);

	@Autowired
	private CryptoDataDao cryptoDataDao;

	@Autowired
	private PredictionDao predictionDao;

	@Autowired
	private PredictionCfgDao predictionCfgDao;

	// In minutes. Key: time in future to predict. Value: different sample sizes to
	// take as observed to make the prediction.
	private Map<Integer, List<Integer>> getPredictCfgs() {
		Map<Integer, List<Integer>> predictCfgs = new TreeMap<>();

		List<PredictionCfgDto> activeCfgs = predictionCfgDao.findAllActive();

		activeCfgs.forEach(predictCfg -> {
			if (predictCfgs.containsKey(predictCfg.getAdvance())) {
				predictCfgs.get(predictCfg.getAdvance()).add(predictCfg.getSampleSize());
			} else {
				predictCfgs.put(predictCfg.getAdvance(), new ArrayList<>(Arrays.asList(predictCfg.getSampleSize())));
			}
		});

		return predictCfgs;

	}

	/**
	 * Make predictions for each cryptocurrency at different times in the future
	 * 
	 * @param activeCxCurrs List of active cryptocurrencies
	 */
	public void predictResults(List<CryptoCurrencyDto> activeCxCurrs) {

		LOG.debug("Calculating predictions");

		List<PredictionDto> predictions = new ArrayList<>();

		for (CryptoCurrencyDto cxCurr : activeCxCurrs) {

			// List of read data from the last 48h. It´ll be used as observed values to
			// predict future values
			List<CryptoDataDto> dataToAnalyze = cryptoDataDao.getDataToAnalyze(cxCurr);

			if (dataToAnalyze != null && !dataToAnalyze.isEmpty()) {

				// Calculates a prediction for each combination of sample size (of real read
				// data) and time (future)
				getPredictCfgs().forEach((advance, sampleSizes) -> sampleSizes.forEach(sampleSize -> {
					try {
						LocalDateTime predictTime = LocalDateTime.now().plusMinutes(advance).withSecond(0).withNano(0);
						double predictVal = KSDPrediction.getPredictedValue(
								getObservedValues(dataToAnalyze, sampleSize), DateUtils.toSeconds(predictTime));

						PredictionDto prediction = new PredictionDto(LocalDateTime.now(),
								dataToAnalyze.get(0).getCxCurrencyDto(), sampleSize, predictTime, predictVal, advance);

						predictions.add(prediction);
					} catch (Exception ex) {
						LOG.error("Error processing sampleSize: {}, advance: {} and cxCurr: {}", sampleSize, advance,
								dataToAnalyze.get(0).getCxCurrencyDto().getCode(), ex);
					}

				}));

				predictionDao.saveAll(predictions);
			}
		}

		LOG.debug("End of calculating predictions");
	}

	/**
	 * Generates a map of observed data where the key is time (when was the
	 * observation) and the value is price (what was the value at that time)
	 * 
	 * @param dataToAnalyze Real read data
	 * @param sampleSize    Size of the real data sample
	 * @return
	 */
	private Map<Double, Double> getObservedValues(List<CryptoDataDto> dataToAnalyze, Integer sampleSize) {
		LocalDateTime limitSampleDate = LocalDateTime.now().minusMinutes(sampleSize);

		Function<CryptoDataDto, Double> keyMapper = d -> (double) DateUtils.toSeconds(d.getReadTime());
		Function<CryptoDataDto, Double> valueMapper = d -> (d.getHigh() + d.getLow()) / 2;

		return dataToAnalyze.stream().filter(elem -> elem.getReadTime().isAfter(limitSampleDate))
				.sorted((e1, e2) -> e2.getReadTime().compareTo(e1.getReadTime()))
				.collect(Collectors.toMap(keyMapper, valueMapper));

	}

	/**
	 * Clear old info from bbdd
	 * 
	 * @param maxStoredPrediction
	 * @param maxStoredCxData
	 */

	public void clearOld(int maxStoredCxData, int maxStoredPrediction) {
		int recordsDeleted = cryptoDataDao.deleteBefore(LocalDateTime.now().minusDays(maxStoredCxData));
		LOG.info("{} rows deleted from CryptoData table.", recordsDeleted);
		recordsDeleted = predictionDao.deleteBefore(LocalDateTime.now().minusDays(maxStoredPrediction));
		LOG.info("{} rows deleted from Predicition table.", recordsDeleted);
	}

}
