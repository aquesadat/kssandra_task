/**
 * 
 */
package com.kssandra.ksd_task.provider.alphavantage;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.kssandra.alphavantage_client.output.IntraDay;
import com.kssandra.alphavantage_client.output.SimpleCryptoCurrencyData;
import com.kssandra.alphavantage_client.service.AlphaVantageService;
import com.kssandra.ksd_common.dto.AVAccountDto;
import com.kssandra.ksd_common.dto.CryptoCurrencyDto;
import com.kssandra.ksd_common.dto.CryptoDataDto;
import com.kssandra.ksd_common.enums.DataProviderEnum;
import com.kssandra.ksd_common.exception.DataCollectException;
import com.kssandra.ksd_common.util.DateUtils;
import com.kssandra.ksd_persistence.dao.CryptoCurrencyDao;
import com.kssandra.ksd_persistence.dao.CryptoDataDao;
import com.kssandra.ksd_task.provider.CryptoDataProvider;

/**
 * @author aquesada
 *
 */
@SpringBootTest
@ExtendWith(MockitoExtension.class)
class AlphaVantageCryptoDataProviderTest {

	@Autowired
	AlphaVantageCryptoDataProvider avDataProvider;

	@MockBean
	CryptoCurrencyDao cryptoCurrDao;

	@MockBean
	AlphaVantageService avService;

//	@MockBean
//	private CryptoDataDao mockCxDataDao;

//	@Autowired
//	private CryptoDataDao cryptoDataDao;

//	@MockBean
//	CryptoDataProvider mockDataProvider;

//	@MockBean
//	AlphaVantageCryptoDataProvider mockAVProvider;

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.provider.alphavantage.AlphaVantageCryptoDataProvider#callService(com.kssandra.ksd_common.dto.CryptoCurrencyDto)}.
	 */
	@Test
	@Disabled
	void testCallService() {

		when(avService.intraDay(anyString(), anyString(), anyString(), anyString(), anyInt(), anyString()))
				.thenReturn(new IntraDay(null, null));

		CryptoCurrencyDto cxCurrDto = new CryptoCurrencyDto();
		cxCurrDto.setCode("CXT");
		cxCurrDto.setName("Crypto Currency Test");
		AVAccountDto avAccountDto = new AVAccountDto();
		avAccountDto.setApiKey("");
		avAccountDto.setUserId("");
		cxCurrDto.setAvAccountDto(avAccountDto);

		for (int i = 1; i < AlphaVantageCryptoDataProvider.getMaxRq(); i++) {
			long initTime = System.currentTimeMillis();
			avDataProvider.callService(cxCurrDto);
			assertTrue((System.currentTimeMillis() - initTime) < AlphaVantageCryptoDataProvider.getRqSleep());
		}
		long initTime = System.currentTimeMillis();
		avDataProvider.callService(cxCurrDto);
		assertTrue((System.currentTimeMillis() - initTime) >= AlphaVantageCryptoDataProvider.getRqSleep());

		assertEquals(IntraDay.class, avDataProvider.callService(cxCurrDto).getClass());

	}

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.provider.CryptoDataProvider#collectIntraDayData(java.util.List)}.
	 * 
	 * @throws DataCollectException
	 */
	@Test
	void testCollectIntraDayData() throws DataCollectException {

		assertTrue(avDataProvider.collectIntraDayData(Collections.emptyList()).isEmpty());
		assertEquals(1, AlphaVantageCryptoDataProvider.getnRequests().get());

		List<CryptoCurrencyDto> activeCxCurrs = new ArrayList<>();
		CryptoCurrencyDto cxCurr1 = new CryptoCurrencyDto();
		cxCurr1.setCode("CXT1");
		activeCxCurrs.add(cxCurr1);
		CryptoCurrencyDto cxCurr2 = new CryptoCurrencyDto();
		cxCurr2.setCode("CXT2");
		activeCxCurrs.add(cxCurr2);

		Map<String, String> metaData1 = new HashMap<>();
		metaData1.put(AlphaVantageCryptoDataProvider.getMdataCurrencyCode(), "CXT1");
		List<SimpleCryptoCurrencyData> digitalData1 = new ArrayList<>();
		LocalDateTime readTime = LocalDateTime.now();
		digitalData1.add(new SimpleCryptoCurrencyData(readTime, 122.38, 123.45, 119.00, 120.54, 4562987));
		IntraDay intraDay1 = new IntraDay(metaData1, digitalData1);

		Map<String, String> metaData2 = new HashMap<>();
		metaData2.put(AlphaVantageCryptoDataProvider.getMdataCurrencyCode(), "CXT2");
		List<SimpleCryptoCurrencyData> digitalData2 = new ArrayList<>();
		digitalData2.add(new SimpleCryptoCurrencyData(readTime, 222.38, 223.45, 219.00, 220.54, 4562987));
		IntraDay intraDay2 = new IntraDay(metaData2, digitalData2);

		AlphaVantageCryptoDataProvider mockAVProvider = mock(AlphaVantageCryptoDataProvider.class);
		when(mockAVProvider.callService(cxCurr1)).thenReturn(intraDay1);
		when(mockAVProvider.callService(cxCurr2)).thenReturn(intraDay2);
		List<CryptoDataDto> dataList1 = new ArrayList<>();
		CryptoDataDto dto = new CryptoDataDto();
		dto.setCxCurrencyDto(cxCurr1);
		dataList1.add(dto);
		List<CryptoDataDto> dataList2 = new ArrayList<>();
		CryptoDataDto dto2 = new CryptoDataDto();
		dto2.setCxCurrencyDto(cxCurr2);
		dataList2.add(dto2);
		when(mockAVProvider.mapIntraDayRs(intraDay1)).thenReturn(dataList1);
		when(mockAVProvider.mapIntraDayRs(intraDay2)).thenReturn(dataList2);
		// doNothing().when(mockCxDataDao).saveAll(anyList());

		doCallRealMethod().when(mockAVProvider).collectIntraDayData(activeCxCurrs);

		Map<String, List<CryptoDataDto>> result = mockAVProvider.collectIntraDayData(activeCxCurrs);
		verify(mockAVProvider).callService(cxCurr1);
		verify(mockAVProvider).callService(cxCurr2);
		assertTrue(result.containsKey("CXT1"));
		assertTrue(result.containsKey("CXT2"));
		assertFalse(result.get("CXT1").isEmpty());
		assertFalse(result.get("CXT2").isEmpty());

//
//		activeCxCurrs = new ArrayList<>();
//		CryptoCurrencyDto cxCurr3 = new CryptoCurrencyDto();
//		cxCurr2.setCode("CXT3");
//		activeCxCurrs.add(cxCurr3);
//		when(mockAVProvider.callService(cxCurr3)).thenReturn(null);
//		assertTrue(avDataProvider.collectIntraDayData(activeCxCurrs).isEmpty());

	}

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.provider.alphavantage.AlphaVantageCryptoDataProvider#mapIntraDayRs(java.lang.Object)}.
	 * 
	 * @throws DataCollectException
	 */
	@Test
	void testMapIntraDayRs() throws DataCollectException {

		assertThrows(DataCollectException.class, () -> avDataProvider.mapIntraDayRs(null),
				"Error mapping response from AlphaVantage: Result from AV is null");

		assertThrows(DataCollectException.class, () -> avDataProvider.mapIntraDayRs(new IntraDay(null, null)),
				"Error mapping response from AlphaVantage: MetaData is null or empty");

		Map<String, String> metaData1 = new HashMap<>();
		metaData1.put("4. Market Code", "EUR");
		assertThrows(DataCollectException.class, () -> avDataProvider.mapIntraDayRs(new IntraDay(metaData1, null)),
				"Error mapping response from AlphaVantage: MetaData is null or empty");

		when(cryptoCurrDao.findByCode("CXT_NULL")).thenReturn(null);
		Map<String, String> metaData2 = new HashMap<>();
		metaData2.put("2. Digital Currency Code", "CXT_NULL");
		assertThrows(DataCollectException.class, () -> avDataProvider.mapIntraDayRs(new IntraDay(metaData2, null)),
				"Error mapping response from AlphaVantage: DigitalData is null or empty");

		when(cryptoCurrDao.findByCode("CXT")).thenReturn(new CryptoCurrencyDto());
		Map<String, String> metaData3 = new HashMap<>();
		metaData3.put(AlphaVantageCryptoDataProvider.getMdataCurrencyCode(), "CXT");
		List<SimpleCryptoCurrencyData> digitalData1 = null;
		assertThrows(DataCollectException.class,
				() -> avDataProvider.mapIntraDayRs(new IntraDay(metaData3, digitalData1)),
				"Error mapping response from AlphaVantage: DigitalData is null or empty");

		when(cryptoCurrDao.findByCode("CXT")).thenReturn(new CryptoCurrencyDto());
		List<SimpleCryptoCurrencyData> digitalData2 = new ArrayList<>();
		assertThrows(DataCollectException.class,
				() -> avDataProvider.mapIntraDayRs(new IntraDay(metaData3, digitalData2)),
				"Error mapping response from AlphaVantage: DigitalData is null or empty");

		String cxCode = "CXT";
		CryptoCurrencyDto cxCurrDto = new CryptoCurrencyDto();
		cxCurrDto.setCode(cxCode);
		cxCurrDto.setName("Crypto Currency Test");
		List<SimpleCryptoCurrencyData> digitalData3 = new ArrayList<>();
		LocalDateTime readtime = LocalDateTime.now();
		digitalData3.add(new SimpleCryptoCurrencyData(readtime, 122.38, 123.45, 119.00, 120.54, 4562987));
		when(cryptoCurrDao.findByCode(cxCode)).thenReturn(cxCurrDto);
		List<CryptoDataDto> result = avDataProvider.mapIntraDayRs(new IntraDay(metaData3, digitalData3));
		assertNotNull(result);
		assertFalse(result.isEmpty());
		CryptoDataDto resultData = result.get(0);
		assertNotNull(resultData.getCxCurrencyDto());
		assertEquals(cxCode, resultData.getCxCurrencyDto().getCode());
		double[] expected = { 122.38, 123.45, 119.00, 120.54 };
		double[] actual = { resultData.getOpen(), resultData.getHigh(), resultData.getLow(), resultData.getClose() };
		assertArrayEquals(expected, actual);
		assertEquals(readtime.atZone(ZoneId.of("GMT")).withZoneSameInstant(ZoneId.of(DateUtils.DEFAULT_OFFSET))
				.toLocalDateTime(), resultData.getReadTime());

		List<SimpleCryptoCurrencyData> digitalData4 = new ArrayList<>();
		digitalData4.add(new SimpleCryptoCurrencyData(null, 122.38, 123.45, 119.00, 120.54, 4562987));
		assertThrows(Exception.class, () -> avDataProvider.mapIntraDayRs(new IntraDay(metaData3, digitalData4)));

	}

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.provider.alphavantage.AlphaVantageCryptoDataProvider#getType()}.
	 */
	@Test
	void testGetType() {
		assertEquals(DataProviderEnum.AV.toString(), avDataProvider.getType());
	}

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.provider.alphavantage.AlphaVantageCryptoDataProvider#resetDataProvider()}.
	 */
	@Test
	void testResetDataProvider() {
		avDataProvider.resetDataProvider();
		assertEquals(1, AlphaVantageCryptoDataProvider.getnRequests().get());
	}

}
