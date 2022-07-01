/**
 * 
 */
package com.kssandra.ksd_task.provider;

import static org.hamcrest.CoreMatchers.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.kssandra.alphavantage_client.output.IntraDay;
import com.kssandra.alphavantage_client.output.SimpleCryptoCurrencyData;
import com.kssandra.ksd_common.dto.CryptoCurrencyDto;
import com.kssandra.ksd_common.dto.CryptoDataDto;
import com.kssandra.ksd_common.exception.DataCollectException;
import com.kssandra.ksd_persistence.dao.CryptoDataDao;

/**
 * @author aquesada
 *
 */
@SpringBootTest
class CryptoDataProviderTest {

	@Autowired
	CryptoDataProvider cxDataProvider;

	@MockBean
	CryptoDataProvider mockDataProvider;

	@MockBean
	private CryptoDataDao mockCxDataDao;

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.provider.CryptoDataProvider#collectIntraDayData(java.util.List)}.
	 * 
	 * @throws DataCollectException
	 */
	@Test
	void testCollectIntraDayData() throws DataCollectException {

		assertTrue(cxDataProvider.collectIntraDayData(Collections.emptyList()).isEmpty());

		List<CryptoCurrencyDto> activeCxCurrs = new ArrayList<>();
		CryptoCurrencyDto cxCurr1 = new CryptoCurrencyDto();
		cxCurr1.setCode("CXT1");
		activeCxCurrs.add(cxCurr1);
//		CryptoCurrencyDto cxCurr2 = new CryptoCurrencyDto();
//		cxCurr2.setCode("CXT2");
// 		activeCxCurrs.add(cxCurr2);

		// CryptoDataProvider mockDataProvider = mock(CryptoDataProvider.class);

		when(mockDataProvider.callService(cxCurr1)).thenReturn(new IntraDay(null, null));
		when(mockDataProvider.mapIntraDayRs(any(IntraDay.class))).thenReturn(new ArrayList<>());
		doCallRealMethod().when(mockDataProvider).collectIntraDayData(activeCxCurrs);

		Map<String, List<CryptoDataDto>> result = mockDataProvider.collectIntraDayData(activeCxCurrs);

		verify(mockDataProvider).callService(cxCurr1);
		// verify(mockDataProvider).mapIntraDayRs(any(IntraDay.class));

//		CryptoDataProvider mockDataProvider = mock(CryptoDataProvider.class);
//		when(mockDataProvider.callService(cxCurr1)).thenReturn(intraDay1);
//		when(mockDataProvider.callService(cxCurr2)).thenReturn(intraDay2);
//		doNothing().when(mockCxDataDao).saveAll(anyList());
//
//		Map<String, List<CryptoDataDto>> result = cxDataProvider.collectIntraDayData(activeCxCurrs);
//		verify(mockDataProvider).callService(cxCurr1);
//		verify(mockDataProvider).callService(cxCurr2);
//		assertTrue(result.containsKey("CXT1"));
//		assertTrue(result.containsKey("CXT2"));
//		List<CryptoDataDto> dataList1 = result.get("CXT1");
//		assertFalse(dataList1.isEmpty());
//		assertFalse(result.get("CXT2").isEmpty());
//		assertEquals(122.38, dataList1.get(0).getOpen());
//		assertEquals(123.45, dataList1.get(0).getHigh());
//		assertEquals(119.00, dataList1.get(0).getLow());
//		assertEquals(120.54, dataList1.get(0).getClose());
//		assertEquals(readTime, dataList1.get(0).getReadTime());
//		assertEquals("CXT1", dataList1.get(0).getCxCurrencyDto().getCode());
//
//		activeCxCurrs = new ArrayList<>();
//		CryptoCurrencyDto cxCurr3 = new CryptoCurrencyDto();
//		cxCurr2.setCode("CXT3");
//		activeCxCurrs.add(cxCurr3);
//		when(mockDataProvider.callService(cxCurr3)).thenReturn(null);
//		assertTrue(cxDataProvider.collectIntraDayData(activeCxCurrs).isEmpty());

		// Comprobar método save. Verify?
	}

}
