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
	CryptoDataProvider cryptoDataProvider;

	@MockBean
	private CryptoDataDao cryptoDataDao;

	private static final String cxCode1 = "CXT1";

	private static final String cxCode2 = "CXT2";

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.provider.CryptoDataProvider#collectIntraDayData(java.util.List)}.
	 */
	@Test
	void testCollectIntraDayData() throws DataCollectException {

		CryptoCurrencyDto cxCurr1 = new CryptoCurrencyDto(cxCode1);
		CryptoCurrencyDto cxCurr2 = new CryptoCurrencyDto(cxCode2);
		List<CryptoCurrencyDto> activeCxCurrs = new ArrayList<CryptoCurrencyDto>(Arrays.asList(cxCurr1, cxCurr2));

		Map<String, String> metaData1 = new HashMap<>(
				Maps.newHashMap(AlphaVantageCryptoDataProvider.getMdataCurrencyCode(), cxCode1));
		Map<String, String> metaData2 = new HashMap<>(
				Maps.newHashMap(AlphaVantageCryptoDataProvider.getMdataCurrencyCode(), cxCode2));
		List<SimpleCryptoCurrencyData> digitalData = new ArrayList<>(Arrays
				.asList(new SimpleCryptoCurrencyData(LocalDateTime.now(), 122.38, 123.45, 119.00, 120.54, 4562987)));
		IntraDay intraDay1 = new IntraDay(metaData1, digitalData);
		IntraDay intraDay2 = new IntraDay(metaData2, digitalData);

		doReturn(intraDay1).when(cryptoDataProvider).callService(cxCurr1);
		doReturn(intraDay2).when(cryptoDataProvider).callService(cxCurr2);

		CryptoDataDto dto1 = new CryptoDataDto();
		dto1.setCxCurrencyDto(cxCurr1);
		CryptoDataDto dto2 = new CryptoDataDto();
		dto2.setCxCurrencyDto(cxCurr2);
		List<CryptoDataDto> dataList1 = new ArrayList<CryptoDataDto>(Arrays.asList(dto1, dto1, dto1));
		List<CryptoDataDto> dataList2 = new ArrayList<CryptoDataDto>(Arrays.asList(dto2, dto2));
		doReturn(dataList1).when(cryptoDataProvider).mapIntraDayRs(intraDay1);
		doReturn(dataList2).when(cryptoDataProvider).mapIntraDayRs(intraDay2);

		Map<String, List<CryptoDataDto>> result = cryptoDataProvider.collectIntraDayData(activeCxCurrs);
		verify(cryptoDataProvider).callService(cxCurr1);
		verify(cryptoDataProvider).callService(cxCurr2);
		assertTrue(result.containsKey(cxCode1));
		assertTrue(result.containsKey(cxCode2));
		assertEquals(3, result.get(cxCode1).size());
		assertEquals(2, result.get(cxCode2).size());

		// If any active crypto currency is provided as input, the collectIntraDayData
		// returns an empty list
		assertTrue(cryptoDataProvider.collectIntraDayData(Collections.emptyList()).isEmpty());
		assertEquals(1, AlphaVantageCryptoDataProvider.getnRequests().get());

		// If a DataCollectException is thrown in mapIntraDayRs, that crypto currency
		// data won´t be inserted into the result list
		doThrow(DataCollectException.class).when(cryptoDataProvider).mapIntraDayRs(intraDay2);
		result = cryptoDataProvider.collectIntraDayData(activeCxCurrs);
		assertTrue(result.containsKey(cxCode1));
		assertFalse(result.containsKey(cxCode2));
		assertEquals(3, result.get(cxCode1).size());

	}

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.provider.CryptoDataProvider#saveDataResult(java.util.Map, java.util.List)}.
	 */
	@Test
	void testSaveDataResult() {

		CryptoCurrencyDto cxCurr1 = new CryptoCurrencyDto(cxCode1);
		CryptoCurrencyDto cxCurr2 = new CryptoCurrencyDto(cxCode2);

		Map<String, LocalDateTime> lastInserted = new HashMap<>();
		lastInserted.put(cxCode1, LocalDateTime.now().minusDays(1));
		when(cryptoDataDao.getLastInserted(anyList())).thenReturn(lastInserted);

		Map<String, List<CryptoDataDto>> dataResult = new HashMap<>();
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

		cryptoDataProvider.saveDataResult(dataResult, new ArrayList<>(Arrays.asList(cxCurr1, cxCurr2)));

		ArgumentCaptor<List<CryptoDataDto>> captor = ArgumentCaptor.forClass(List.class);
		verify(cryptoDataDao).saveAll(captor.capture());

		List<CryptoDataDto> dataToSave = captor.getValue();

		// Only 2 cyrptoDataDto will be saved for CXT1 because one of the 3 initial has
		// its readDate before lastInsertedDate
		assertEquals(2, dataToSave.stream().filter(elem -> elem.getCxCurrencyDto().getCode().equals(cxCode1)).count());

		// All 3 cryptoDataDto will be saved for CXT2 because any previous data was
		// inserted in DB (any lastInserted date exists for CXT2).
		assertEquals(3, dataToSave.stream().filter(elem -> elem.getCxCurrencyDto().getCode().equals(cxCode2)).count());

	}

}