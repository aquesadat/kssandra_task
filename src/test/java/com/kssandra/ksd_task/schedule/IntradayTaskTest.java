/**
 * 
 */
package com.kssandra.ksd_task.schedule;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import com.kssandra.ksd_common.dto.CryptoCurrencyDto;
import com.kssandra.ksd_common.dto.CryptoDataDto;
import com.kssandra.ksd_persistence.dao.CryptoCurrencyDao;
import com.kssandra.ksd_task.prediction.CryptoDataEval;
import com.kssandra.ksd_task.prediction.CryptoDataPrediction;
import com.kssandra.ksd_task.provider.alphavantage.AlphaVantageCryptoDataProvider;

/**
 * @author aquesada
 *
 */
@SpringBootTest
class IntradayTaskTest {

	@Autowired
	IntradayTask intraDayTask;

	@MockBean
	private CryptoDataPrediction cxDataPrediction;

	@MockBean
	private CryptoDataEval cxDataEval;

	@MockBean
	protected CryptoCurrencyDao cryptoCurrDao;

	@SpyBean
	AlphaVantageCryptoDataProvider cxDataProvider;

	final String cxCode1 = "CXT1";
	final String cxCode2 = "CXT2";

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.schedule.IntradayTask#scheduleTask()}.
	 */
	@Test
	@DisplayName("No active cxs, provider not called")
	void testScheduleTaskNoActiveCxs() {

		when(cryptoCurrDao.getAllActiveCxCurrencies()).thenReturn(Collections.emptyList());

		intraDayTask.scheduleTask();

		verify(cxDataEval, never()).evaluatePredictions(anyMap(), anyList());
		verify(cxDataPrediction, never()).predictResults(anyList());
	}

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.schedule.IntradayTask#scheduleTask()}.
	 */
	@Test
	@DisplayName("No data from provider, no predictions made")
	void testScheduleTaskNoDataFromProvider() {

		List<CryptoCurrencyDto> activeCxCurrs = getActiveCxCurrs();

		when(cryptoCurrDao.getAllActiveCxCurrencies()).thenReturn(activeCxCurrs);
		doReturn(Collections.emptyMap()).when(cxDataProvider).collectIntraDayData(activeCxCurrs);

		intraDayTask.scheduleTask();

		verify(cxDataEval, never()).evaluatePredictions(anyMap(), anyList());
		verify(cxDataPrediction, never()).predictResults(anyList());
	}

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.schedule.IntradayTask#scheduleTask()}.
	 */
	@Test
	void testScheduleTaskOK() {

		List<CryptoCurrencyDto> activeCxCurrs = getActiveCxCurrs();
		Map<String, List<CryptoDataDto>> dataResult = new HashMap<>();
		dataResult.put(cxCode1, Collections.emptyList());
		dataResult.put(cxCode2, Collections.emptyList());

		when(cryptoCurrDao.getAllActiveCxCurrencies()).thenReturn(activeCxCurrs);
		doReturn(dataResult).when(cxDataProvider).collectIntraDayData(activeCxCurrs);

		intraDayTask.scheduleTask();

		verify(cxDataPrediction, atLeast(1)).predictResults(activeCxCurrs);

	}

	private List<CryptoCurrencyDto> getActiveCxCurrs() {
		CryptoCurrencyDto cxCurr1 = new CryptoCurrencyDto(cxCode1);
		CryptoCurrencyDto cxCurr2 = new CryptoCurrencyDto(cxCode2);
		return new ArrayList<CryptoCurrencyDto>(Arrays.asList(cxCurr1, cxCurr2));
	}

}
