/**
 * 
 */
package com.kssandra.ksd_task.provider.alphavantage;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.util.Maps;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * @author aquesada
 *
 */
@SpringBootTest
class AlphaVantageCryptoDataProviderTest {

	@Autowired
	AlphaVantageCryptoDataProvider avDataProvider;

	@MockBean
	CryptoCurrencyDao cryptoCurrDao;

	@MockBean
	AlphaVantageService avService;

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.provider.alphavantage.AlphaVantageCryptoDataProvider#callService(com.kssandra.ksd_common.dto.CryptoCurrencyDto)}.
	 */
	@Test
	void testCallService() {

		CryptoCurrencyDto cxCurrDto = new CryptoCurrencyDto("CXT", "Crypto Currency Test", new AVAccountDto(), true);

		// If maxRq is exceeded, application should wait some time (rqSleep) until call
		// service again
		for (int i = 1; i < AlphaVantageCryptoDataProvider.getMaxRq(); i++) {
			long initTime = System.currentTimeMillis();
			avDataProvider.callService(cxCurrDto);
			assertTrue((System.currentTimeMillis() - initTime) < AlphaVantageCryptoDataProvider.getRqSleep());
		}
		long initTime = System.currentTimeMillis();
		avDataProvider.callService(cxCurrDto);
		assertTrue((System.currentTimeMillis() - initTime) >= AlphaVantageCryptoDataProvider.getRqSleep());

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

		String cxCurr1 = "CXT1";
		CryptoCurrencyDto cxCurr = new CryptoCurrencyDto(cxCurr1);
		List<CryptoCurrencyDto> activeCxCurrs = new ArrayList<CryptoCurrencyDto>(Arrays.asList(cxCurr));

		Map<String, String> metaData = new HashMap<>(
				Maps.newHashMap(AlphaVantageCryptoDataProvider.getMdataCurrencyCode(), cxCurr1));
		List<SimpleCryptoCurrencyData> digitalData = new ArrayList<>(Arrays
				.asList(new SimpleCryptoCurrencyData(LocalDateTime.now(), 122.38, 123.45, 119.00, 120.54, 4562987)));
		IntraDay intraDay = new IntraDay(metaData, digitalData);

		AlphaVantageCryptoDataProvider avCxDataProvider = mock(AlphaVantageCryptoDataProvider.class);
		when(avCxDataProvider.callService(cxCurr)).thenReturn(intraDay);

		CryptoDataDto dto = new CryptoDataDto();
		dto.setCxCurrencyDto(cxCurr);
		List<CryptoDataDto> dataList = new ArrayList<CryptoDataDto>(Arrays.asList(dto, dto, dto));
		when(avCxDataProvider.mapIntraDayRs(intraDay)).thenReturn(dataList);

		doCallRealMethod().when(avCxDataProvider).collectIntraDayData(activeCxCurrs);

		Map<String, List<CryptoDataDto>> result = avCxDataProvider.collectIntraDayData(activeCxCurrs);
		verify(avCxDataProvider).callService(cxCurr);
		assertTrue(result.containsKey(cxCurr1));
		assertFalse(result.get(cxCurr1).isEmpty());
		assertFalse(result.containsKey("CXT2"));

	}

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.provider.alphavantage.AlphaVantageCryptoDataProvider#mapIntraDayRs(java.lang.Object)}.
	 * 
	 * @throws DataCollectException
	 */
	@Test
	void testMapIntraDayRs() throws DataCollectException {

		// Testing exceptions
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

		// Testing OK case
		String cxCode = "CXT";
		CryptoCurrencyDto cxCurrDto = new CryptoCurrencyDto(cxCode);
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

		// Generic exception will be throw if digitalData list is empty
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
