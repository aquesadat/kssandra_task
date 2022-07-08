/**
 * 
 */
package com.kssandra.ksd_task.prediction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.kssandra.ksd_common.dto.CryptoCurrencyDto;
import com.kssandra.ksd_common.dto.CryptoDataDto;
import com.kssandra.ksd_common.dto.PredictionDto;
import com.kssandra.ksd_persistence.dao.CryptoDataDao;
import com.kssandra.ksd_persistence.dao.PredictionDao;

/**
 * @author aquesada
 *
 */
@SpringBootTest
class CryptoDataPredictionTest {

	@Autowired
	CryptoDataPrediction cryptoDataPrediction;

	@MockBean
	CryptoDataDao cryptoDataDao;

	@MockBean
	PredictionDao predictionDao;

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.prediction.CryptoDataPrediction#predictResults(java.util.List)}.
	 */
	@Test
	void testPredictResultsCaseKO() {

		// If the list of active cx currencies is empty, any prediction will be saved
		cryptoDataPrediction.predictResults(new ArrayList<>());
		verify(predictionDao, never()).saveAll(Mockito.anyList());

		// If there isn´t data to analyze, any prediction will be saved
		String cxCode1 = "CXT1";
		CryptoCurrencyDto cxCurr1 = new CryptoCurrencyDto(cxCode1);
		List<CryptoCurrencyDto> activeCxCurrs = new ArrayList<CryptoCurrencyDto>(Arrays.asList(cxCurr1));
		List<CryptoDataDto> dataToAnalyze = new ArrayList<>();
		when(cryptoDataDao.getDataToAnalyze(cxCurr1)).thenReturn(dataToAnalyze);

		cryptoDataPrediction.predictResults(activeCxCurrs);
		verify(predictionDao, never()).saveAll(anyList());

		// When there´s data to analyze but an exception occurs during execution
		CryptoDataDto cxData = new CryptoDataDto();
		cxData.setCxCurrencyDto(new CryptoCurrencyDto(cxCode1));
		cxData.setReadTime(LocalDateTime.now());
		cxData.setHigh(100);
		cxData.setLow(75);
		dataToAnalyze.add(cxData);
		cryptoDataPrediction.predictResults(activeCxCurrs);
		verify(predictionDao, atLeast(1)).saveAll(anyList());

	}

	@Test
	void testPredictResultsCaseOK() {

		List<CryptoDataDto> dataToAnalyze = new ArrayList<>();
		String cxCode1 = "CXT1";
		CryptoCurrencyDto cxCurr1 = new CryptoCurrencyDto(cxCode1);
		List<CryptoCurrencyDto> activeCxCurrs = new ArrayList<CryptoCurrencyDto>(Arrays.asList(cxCurr1));

		when(cryptoDataDao.getDataToAnalyze(cxCurr1)).thenReturn(dataToAnalyze);

		loadTestData(dataToAnalyze, cxCode1);
		cryptoDataPrediction.predictResults(activeCxCurrs);
		ArgumentCaptor<List<PredictionDto>> captor = ArgumentCaptor.forClass(List.class);
		verify(predictionDao).saveAll(captor.capture());
		List<PredictionDto> dataToSave = captor.getValue();
		assertFalse(dataToSave.isEmpty());
		assertTrue(dataToSave.stream().allMatch(d -> (d.getCxCurrencyDto().getCode().equals("CXT1")
				&& d.getPredictTime() != null && d.getPredictVal() > 0 && d.getSampleSize() > 0)));
	}

	private void loadTestData(List<CryptoDataDto> dataToAnalyze, String cxCode) {

		for (int i = 0; i < 3000; i++) {
			CryptoDataDto cxData = new CryptoDataDto();
			cxData.setCxCurrencyDto(new CryptoCurrencyDto(cxCode));
			cxData.setReadTime(LocalDateTime.now().minusMinutes(i * 15));
			cxData.setHigh(100);
			cxData.setLow(75);
			dataToAnalyze.add(cxData);
		}
	}

}
