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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.kssandra.ksd_common.dto.CryptoCurrencyDto;
import com.kssandra.ksd_common.dto.CryptoDataDto;
import com.kssandra.ksd_common.dto.PredictionCfgDto;
import com.kssandra.ksd_common.dto.PredictionDto;
import com.kssandra.ksd_persistence.dao.CryptoDataDao;
import com.kssandra.ksd_persistence.dao.PredictionCfgDao;
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

	@MockBean
	PredictionCfgDao predictionCfgDao;

	final String cxCode1 = "CXT1";

	CryptoCurrencyDto cxCurr1;

	List<CryptoCurrencyDto> activeCxCurrs;

	@BeforeEach
	void initializeParams() {
		cxCurr1 = new CryptoCurrencyDto(cxCode1);
		activeCxCurrs = new ArrayList<CryptoCurrencyDto>(Arrays.asList(cxCurr1));
	}

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.prediction.CryptoDataPrediction#predictResults(java.util.List)}.
	 */
	@Test
	@DisplayName("No cxcurrencies active, any prediction will be saved")
	void testPredictResultsCaseNoActiveCxs() {
		cryptoDataPrediction.predictResults(new ArrayList<>());

		verify(predictionDao, never()).saveAll(Mockito.anyList());
	}

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.prediction.CryptoDataPrediction#predictResults(java.util.List)}.
	 */
	@Test
	@DisplayName("No data to analyze, any prediction will be saved")
	void testPredictResultsCaseNoData() {
		when(cryptoDataDao.getDataToAnalyze(cxCurr1)).thenReturn(new ArrayList<>());

		cryptoDataPrediction.predictResults(activeCxCurrs);

		verify(predictionDao, never()).saveAll(anyList());
	}

	@Test
	@DisplayName("There is data to analyze, try to save")
	void testPredictResultsCaseExceptionAnalyzing() {
		List<CryptoDataDto> dataToAnalyze = new ArrayList<>();

		CryptoDataDto cxData = new CryptoDataDto();
		cxData.setCxCurrencyDto(new CryptoCurrencyDto(cxCode1));
		cxData.setReadTime(LocalDateTime.now());
		cxData.setHigh(100);
		cxData.setLow(75);
		dataToAnalyze.add(cxData);
		when(cryptoDataDao.getDataToAnalyze(cxCurr1)).thenReturn(dataToAnalyze);
		when(predictionCfgDao.findAllActive()).thenReturn(getActivePredictionsCfg());

		cryptoDataPrediction.predictResults(activeCxCurrs);

		verify(predictionDao, atLeast(1)).saveAll(anyList());
	}

	@Test
	@DisplayName("There is data to analyze, check info to save")
	void testPredictResultsCaseOK() {

		List<CryptoDataDto> dataToAnalyze = new ArrayList<>();
		when(cryptoDataDao.getDataToAnalyze(cxCurr1)).thenReturn(dataToAnalyze);
		loadTestData(dataToAnalyze, cxCode1);
		when(predictionCfgDao.findAllActive()).thenReturn(getActivePredictionsCfg());

		cryptoDataPrediction.predictResults(activeCxCurrs);

		ArgumentCaptor<List<PredictionDto>> captor = ArgumentCaptor.forClass(List.class);
		verify(predictionDao).saveAll(captor.capture());
		List<PredictionDto> dataToSave = captor.getValue();
		assertFalse(dataToSave.isEmpty());
		assertTrue(dataToSave.stream().allMatch(d -> (d.getCxCurrencyDto().getCode().equals(cxCode1)
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

	private List<PredictionCfgDto> getActivePredictionsCfg() {
		List<PredictionCfgDto> activePredictCfgs = new ArrayList<>();
		activePredictCfgs.add(new PredictionCfgDto(15, 45));
		activePredictCfgs.add(new PredictionCfgDto(15, 60));
		activePredictCfgs.add(new PredictionCfgDto(30, 45));
		activePredictCfgs.add(new PredictionCfgDto(30, 60));
		return activePredictCfgs;
	}

}
