package com.kssandra.ksd_task.prediction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kssandra.ksd_common.dto.CryptoCurrencyDto;
import com.kssandra.ksd_common.dto.CryptoDataDto;
import com.kssandra.ksd_common.dto.PredictionDto;
import com.kssandra.ksd_common.util.DateUtils;
import com.kssandra.ksd_core.prediction.KSDPrediction;
import com.kssandra.ksd_persistence.dao.CryptoDataDao;
import com.kssandra.ksd_persistence.dao.PredictionDao;

@Component
public class CryptoDataPrediction {

	@Autowired
	private CryptoDataDao cryptoDataDao;

	@Autowired
	private PredictionDao predictionDao;

	/**
	 * En minutos. key: posición a predecir. value: distintos tamaños de muestras a
	 * usar para realizar la predicción
	 */
	private static final Map<Integer, List<Integer>> predictCfg = initSamples();

	private static Map<Integer, List<Integer>> initSamples() {
		Map<Integer, List<Integer>> aux = new TreeMap<>();
		aux.put(60, Arrays.asList(60, 120, 240));
		aux.put(360, Arrays.asList(360, 720, 1080));
		aux.put(720, Arrays.asList(720, 1080, 1440));
		aux.put(1440, Arrays.asList(1440, 2100));

		return aux;
	}

	public void predictResults(List<CryptoCurrencyDto> activeCxCurrs) {
		List<PredictionDto> predictions = null;

		for (CryptoCurrencyDto cxCurr : activeCxCurrs) {

			List<CryptoDataDto> dataToAnalyze = cryptoDataDao.getDataToAnalyze(cxCurr);

			predictions = getPrediction(dataToAnalyze);

			predictionDao.saveAll(predictions);
		}
	}

	private List<PredictionDto> getPrediction(List<CryptoDataDto> dataToAnalyze) {

		List<PredictionDto> predictions = new ArrayList<>();

		predictCfg.forEach((predictPos, sampleSizes) -> {
			System.out.println("predictPos: " + predictPos);
			sampleSizes.forEach(sampleSize -> {
				System.out.println("sampleSize: " + sampleSize);
				PredictionDto prediction = new PredictionDto();
				prediction.setCurrTime(LocalDateTime.now());
				prediction.setCxCurrencyDto(dataToAnalyze.get(0).getCxCurrencyDto());
				prediction.setSampleSize(sampleSize);

				Map<Double, Double> observedValues = getObservedValues(dataToAnalyze, sampleSize);
				// prediction.setSampleSlope(KSDPrediction.getSlope(observedValues));
				prediction.setPredictTime(LocalDateTime.now().plusMinutes(predictPos));
				prediction.setPredictVal(KSDPrediction.getPredictedValue(observedValues,
						DateUtils.toSeconds(prediction.getPredictTime())));

				observedValues.forEach((k, v) -> {
					System.out.printf("obs. key: %f%n", (double) k);
					System.out.printf("obs. value: %f%n", (double) v);
				});

				System.out.printf("pred. key: %f%n", (double) DateUtils.toSeconds(prediction.getPredictTime()));
				System.out.printf("pred. value: %f%n", (double) prediction.getPredictVal());

				predictions.add(prediction);
			});
		});

		return predictions;
	}

	private Map<Double, Double> getObservedValues(List<CryptoDataDto> dataToAnalyze, Integer sampleSize) {
		LocalDateTime limitSampleDate = LocalDateTime.now().minusMinutes(sampleSize);

		Function<CryptoDataDto, Double> keyMapper = d -> (double) DateUtils.toSeconds(d.getReadTime());
		Function<CryptoDataDto, Double> valueMapper = d -> (d.getHigh() + d.getLow()) / 2;

		return dataToAnalyze.stream().filter(elem -> elem.getReadTime().isAfter(limitSampleDate))
				.sorted((e1, e2) -> e2.getReadTime().compareTo(e1.getReadTime()))
				.collect(Collectors.toMap(keyMapper, valueMapper));

	}

	public void evaluatePredictions(Map<String, List<CryptoDataDto>> dataResult,
			List<CryptoCurrencyDto> activeCxCurrs) {

	}

}
