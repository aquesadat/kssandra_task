/**
 * 
 */
package com.kssandra.ksd_task.prediction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.kssandra.ksd_common.dto.CryptoCurrencyDto;
import com.kssandra.ksd_common.dto.CryptoDataDto;
import com.kssandra.ksd_common.dto.PredictionDto;
import com.kssandra.ksd_persistence.dao.PredictionDao;

/**
 * @author aquesada
 *
 */
@SpringBootTest
class CryptoDataEvalTest {

	@Autowired
	CryptoDataEval cryptoDataEval;

	@MockBean
	PredictionDao predictionDao;

	CryptoCurrencyDto cxCurr1;

	List<CryptoCurrencyDto> activeCxCurrs;

	final String cxCode1 = "CXT1";

	Map<String, List<CryptoDataDto>> dataResult;

	LocalDateTime readDate;

	@BeforeEach
	void initializeParams() {

		cxCurr1 = new CryptoCurrencyDto(cxCode1);
		activeCxCurrs = new ArrayList<CryptoCurrencyDto>(Arrays.asList(cxCurr1));

		List<CryptoDataDto> dataList = new ArrayList<>();
		readDate = LocalDateTime.now();
		dataList.add(new CryptoDataDto(new CryptoCurrencyDto(cxCode1), readDate, 100, 100, 100, 75));
		dataList.add(new CryptoDataDto(new CryptoCurrencyDto(cxCode1), readDate.minusMinutes(15), 100, 100, 100, 75));
		dataList.add(new CryptoDataDto(new CryptoCurrencyDto(cxCode1), readDate.minusMinutes(30), 100, 100, 100, 75));
		dataList.add(new CryptoDataDto(new CryptoCurrencyDto(cxCode1), readDate.minusMinutes(45), 100, 100, 100, 75));
		dataList.add(new CryptoDataDto(new CryptoCurrencyDto(cxCode1), readDate.minusMinutes(60), 100, 100, 100, 75));
		dataResult = new HashMap<>(Maps.newHashMap(cxCode1, dataList));
	}

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.prediction.CryptoDataEval#evaluatePredictions(java.util.Map, java.util.List)}.
	 */
	@Test
	@DisplayName("Empty crypto data map, any prediction will be checked")
	void testEvaluatePredictionsWithoutCryptoData() {
		cryptoDataEval.evaluatePredictions(new HashMap<>(), activeCxCurrs);

		verify(predictionDao, never()).findUnanalyzed(any(), any());
	}

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.prediction.CryptoDataEval#evaluatePredictions(java.util.Map, java.util.List)}.
	 */
	@Test
	@DisplayName("Empty prediction, any prediction will be saved")
	void testEvaluatePredictionsWithoutPredictions() {
		when(predictionDao.findUnanalyzed(cxCurr1, readDate)).thenReturn(Collections.emptyList());

		cryptoDataEval.evaluatePredictions(dataResult, activeCxCurrs);

		verify(predictionDao, never()).saveAll(anyList());
	}

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.prediction.CryptoDataEval#evaluatePredictions(java.util.Map, java.util.List)}.
	 */
	@Test
	@DisplayName("Cryptodata map filled, predictions will be saved")
	void testEvaluatePredictionsWithCxDataAndPredictionsSaved() {
		List<PredictionDto> unanalyzedData = getUnanalyzedData();
		when(predictionDao.findUnanalyzed(cxCurr1, readDate)).thenReturn(unanalyzedData);

		cryptoDataEval.evaluatePredictions(dataResult, activeCxCurrs);

		verify(predictionDao, times(1)).saveAll(anyList());

	}

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.prediction.CryptoDataEval#evaluatePredictions(java.util.Map, java.util.List)}.
	 */
	@Test
	@DisplayName("Cryptodata map filled, 3 predictions will be saved")
	void testEvaluatePredictionsWithCxDataAndPredictionsSavedSize() {
		List<PredictionDto> unanalyzedData = getUnanalyzedData();
		when(predictionDao.findUnanalyzed(cxCurr1, readDate)).thenReturn(unanalyzedData);

		cryptoDataEval.evaluatePredictions(dataResult, activeCxCurrs);
		ArgumentCaptor<List<PredictionDto>> captor = ArgumentCaptor.forClass(List.class);
		verify(predictionDao).saveAll(captor.capture());
		List<PredictionDto> dataToSave = captor.getValue();

		assertEquals(3, dataToSave.size());
	}

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.prediction.CryptoDataEval#evaluatePredictions(java.util.Map, java.util.List)}.
	 */
	@Test
	@DisplayName("Cryptodata map filled, predictions have size=60 and success not null")
	void testEvaluatePredictionsWithCxDataAndPredictionsSavedSampleSizeAndSuccess() {
		List<PredictionDto> unanalyzedData = getUnanalyzedData();
		when(predictionDao.findUnanalyzed(cxCurr1, readDate)).thenReturn(unanalyzedData);

		cryptoDataEval.evaluatePredictions(dataResult, activeCxCurrs);
		ArgumentCaptor<List<PredictionDto>> captor = ArgumentCaptor.forClass(List.class);
		verify(predictionDao).saveAll(captor.capture());
		List<PredictionDto> dataToSave = captor.getValue();

		assertTrue(dataToSave.stream().allMatch(e -> (e.getSampleSize() == 60 && e.getSuccess() != null)));
	}

	private List<PredictionDto> getUnanalyzedData() {
		List<PredictionDto> unanalyzedData = new ArrayList<>();
		LocalDateTime predDate = LocalDateTime.now().plusSeconds(10);
		unanalyzedData.add(new PredictionDto(null, cxCurr1, 60, predDate.minusMinutes(15), 90, 15));
		unanalyzedData.add(new PredictionDto(null, cxCurr1, 60, predDate.minusMinutes(15), 50, 15));
		unanalyzedData.add(new PredictionDto(null, cxCurr1, 60, predDate.minusMinutes(15), 120, 15));
		unanalyzedData.add(new PredictionDto(null, cxCurr1, 90, predDate.plusMinutes(30), 90, 30));
		unanalyzedData.add(new PredictionDto(null, cxCurr1, 90, predDate.plusMinutes(30), 50, 30));
		unanalyzedData.add(new PredictionDto(null, cxCurr1, 90, predDate.plusMinutes(30), 120, 30));
		return unanalyzedData;
	}

}
