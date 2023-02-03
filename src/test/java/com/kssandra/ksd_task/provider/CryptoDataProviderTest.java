/**
 * 
 */
package com.kssandra.ksd_task.provider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import com.kssandra.alphavantage_client.output.IntraDay;
import com.kssandra.alphavantage_client.output.SimpleCryptoCurrencyData;
import com.kssandra.ksd_common.dto.CryptoCurrencyDto;
import com.kssandra.ksd_common.dto.CryptoDataDto;
import com.kssandra.ksd_common.exception.DataCollectException;
import com.kssandra.ksd_persistence.dao.CryptoDataDao;
import com.kssandra.ksd_task.provider.alphavantage.AlphaVantageCryptoDataProvider;

/**
 * @author aquesada
 *
 */
@SpringBootTest
class CryptoDataProviderTest {

	@SpyBean
	AlphaVantageCryptoDataProvider cryptoDataProvider;

	@MockBean
	private CryptoDataDao cryptoDataDao;

	private static final String cxCode1 = "CXT1";

	private static final String cxCode2 = "CXT2";

	Map<String, LocalDateTime> lastInserted;

	Map<String, List<CryptoDataDto>> dataResult;

	CryptoCurrencyDto cxCurr1, cxCurr2;

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.provider.CryptoDataProvider#saveDataResult(java.util.Map, java.util.List)}.
	 */
	@BeforeEach
	void initParams() {
		cxCurr1 = new CryptoCurrencyDto(cxCode1);
		cxCurr2 = new CryptoCurrencyDto(cxCode2);

		lastInserted = new HashMap<>();
		lastInserted.put(cxCode1, LocalDateTime.now().minusDays(1));

		dataResult = new HashMap<>();
		List<CryptoDataDto> cxData1 = new ArrayList<>();
		cxData1.add(new CryptoDataDto(cxCurr1, LocalDateTime.now().minusDays(2)));
		cxData1.add(new CryptoDataDto(cxCurr1, LocalDateTime.now().minusDays(1)));
		cxData1.add(new CryptoDataDto(cxCurr1, LocalDateTime.now()));

		List<CryptoDataDto> cxData2 = new ArrayList<>();
		cxData2.add(new CryptoDataDto(cxCurr2, LocalDateTime.now().minusDays(2)));
		cxData2.add(new CryptoDataDto(cxCurr2, LocalDateTime.now().minusDays(1)));
		cxData2.add(new CryptoDataDto(cxCurr2, LocalDateTime.now()));

		dataResult.put(cxCode1, cxData1);
		dataResult.put(cxCode2, cxData2);
	}

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.provider.CryptoDataProvider#saveDataResult(java.util.Map, java.util.List)}.
	 */
	@Test
	@DisplayName("ReadDate before lastInsertedDate")
	void testSaveDataResultReadDateBefore() {

		when(cryptoDataDao.getLastInserted(anyList())).thenReturn(lastInserted);

		cryptoDataProvider.saveDataResult(dataResult, new ArrayList<>(Arrays.asList(cxCurr1, cxCurr2)));

		ArgumentCaptor<List<CryptoDataDto>> captor = ArgumentCaptor.forClass(List.class);
		verify(cryptoDataDao).saveAll(captor.capture());
		List<CryptoDataDto> dataToSave = captor.getValue();
		assertEquals(2, dataToSave.stream().filter(elem -> elem.getCxCurrencyDto().getCode().equals(cxCode1)).count());

	}

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.provider.CryptoDataProvider#saveDataResult(java.util.Map, java.util.List)}.
	 */
	@Test
	@DisplayName("No last inserted date exists")
	void testSaveDataResultNoPreviousData() {

		when(cryptoDataDao.getLastInserted(anyList())).thenReturn(lastInserted);

		cryptoDataProvider.saveDataResult(dataResult, new ArrayList<>(Arrays.asList(cxCurr1, cxCurr2)));

		ArgumentCaptor<List<CryptoDataDto>> captor = ArgumentCaptor.forClass(List.class);
		verify(cryptoDataDao).saveAll(captor.capture());
		List<CryptoDataDto> dataToSave = captor.getValue();
		assertEquals(3, dataToSave.stream().filter(elem -> elem.getCxCurrencyDto().getCode().equals(cxCode2)).count());

	}

}
