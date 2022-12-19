/**
 * 
 */
package com.kssandra.ksd_task.prediction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.prediction.CryptoDataEval#evaluatePredictions(java.util.Map, java.util.List)}.
	 */
	@Test
	void testEvaluatePredictions() {

		String cxCode1 = "CXT1";
		CryptoCurrencyDto cxCurr1 = new CryptoCurrencyDto(cxCode1, null, null, false);
		List<CryptoCurrencyDto> activeCxCurrs = List.of(cxCurr1);

		// If the map of data is empty, any prediction will be checked
		cryptoDataEval.evaluatePredictions(new HashMap<>(), activeCxCurrs);
		verify(predictionDao, never()).findUnanalyzed(any(), any());

		List<CryptoDataDto> dataList = new ArrayList<>();
		LocalDateTime readDate = LocalDateTime.now();
		dataList.add(new CryptoDataDto(new CryptoCurrencyDto(cxCode1, null, null, false), readDate, 100, 100, 100, 75));
		dataList.add(new CryptoDataDto(new CryptoCurrencyDto(cxCode1, null, null, false), readDate.minusMinutes(15), 100, 100, 100, 75));
		dataList.add(new CryptoDataDto(new CryptoCurrencyDto(cxCode1, null, null, false), readDate.minusMinutes(30), 100, 100, 100, 75));
		dataList.add(new CryptoDataDto(new CryptoCurrencyDto(cxCode1, null, null, false), readDate.minusMinutes(45), 100, 100, 100, 75));
		dataList.add(new CryptoDataDto(new CryptoCurrencyDto(cxCode1, null, null, false), readDate.minusMinutes(60), 100, 100, 100, 75));
		Map<String, List<CryptoDataDto>> dataResult = Map.of(cxCode1, dataList);

		List<PredictionDto> unanalyzedData = new ArrayList<>();
		LocalDateTime predDate = LocalDateTime.now().plusSeconds(10);
		unanalyzedData.add(new PredictionDto(null, cxCurr1, 60, predDate.minusMinutes(15), 90, 15));
		unanalyzedData.add(new PredictionDto(null, cxCurr1, 60, predDate.minusMinutes(15), 50, 15));
		unanalyzedData.add(new PredictionDto(null, cxCurr1, 60, predDate.minusMinutes(15), 120, 15));
		unanalyzedData.add(new PredictionDto(null, cxCurr1, 90, predDate.plusMinutes(30), 90, 30));
		unanalyzedData.add(new PredictionDto(null, cxCurr1, 90, predDate.plusMinutes(30), 50, 30));
		unanalyzedData.add(new PredictionDto(null, cxCurr1, 90, predDate.plusMinutes(30), 120, 30));

		// If the list of predictions is empty, any data will be saved
		when(predictionDao.findUnanalyzed(cxCurr1, readDate)).thenReturn(Collections.emptyList());
		cryptoDataEval.evaluatePredictions(dataResult, activeCxCurrs);
		verify(predictionDao, never()).saveAll(anyList());

		// OK case
		when(predictionDao.findUnanalyzed(cxCurr1, readDate)).thenReturn(unanalyzedData);
		cryptoDataEval.evaluatePredictions(dataResult, activeCxCurrs);
		ArgumentCaptor<List<PredictionDto>> captor = ArgumentCaptor.forClass(List.class);
		verify(predictionDao).saveAll(captor.capture());
		List<PredictionDto> dataToSave = captor.getValue();
		assertEquals(3, dataToSave.size());
		assertTrue(dataToSave.stream().allMatch(e -> (e.getSampleSize() == 60 && e.getSuccess() != null)));

	}

}
