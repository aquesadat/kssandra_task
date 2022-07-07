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
import com.kssandra.ksd_common.dto.PredictionDto;
import com.kssandra.ksd_common.util.DateUtils;
import com.kssandra.ksd_core.prediction.KSDPrediction;
import com.kssandra.ksd_persistence.dao.CryptoDataDao;
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

	// In minutes. Key: time in future to predict. Value: different sample sizes to
	// take
	// as observed to make the prediction.
	private static final Map<Integer, List<Integer>> predictCfg = initSamples();

	private static Map<Integer, List<Integer>> initSamples() {
		Map<Integer, List<Integer>> aux = new TreeMap<>();
		aux.put(60, Arrays.asList(60, 120, 240));
		aux.put(360, Arrays.asList(360, 720, 1080));
		aux.put(720, Arrays.asList(720, 1080, 1440));
		aux.put(1440, Arrays.asList(1440, 2100));

		return aux;
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
				predictCfg.forEach((predictPos, sampleSizes) -> sampleSizes.forEach(sampleSize -> {
					try {
						LocalDateTime predictTime = LocalDateTime.now().plusMinutes(predictPos);
						double predictVal = KSDPrediction.getPredictedValue(
								getObservedValues(dataToAnalyze, sampleSize), DateUtils.toSeconds(predictTime));

						PredictionDto prediction = new PredictionDto(LocalDateTime.now(),
								dataToAnalyze.get(0).getCxCurrencyDto(), sampleSize, predictTime, predictVal);

						predictions.add(prediction);
					} catch (Exception ex) {
						LOG.error("Error processing sampleSize: {}, predictPos: {} and cxCurr: {}", sampleSize,
								predictPos, dataToAnalyze.get(0).getCxCurrencyDto().getCode());
						LOG.error(ex.getMessage());
					}
				}));

				predictionDao.saveAll(predictions);
			}
		}

		LOG.debug("End of calculating predictions");
	}

	/**
	 * Generates a map of observed data where the key is time (when was the
	 * observation) and the value is price (what was de value at that time)
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

}
