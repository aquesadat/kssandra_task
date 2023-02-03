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
import org.junit.jupiter.api.DisplayName;
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

	final String cxCode = "CXT";

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.provider.alphavantage.AlphaVantageCryptoDataProvider#callService(com.kssandra.ksd_common.dto.CryptoCurrencyDto)}.
	 */
	@Test
	@DisplayName("Max rq exceeded, wait until call again")
	void testCallServiceSleep() {

		CryptoCurrencyDto cxCurrDto = new CryptoCurrencyDto(cxCode, "Crypto Currency Test", new AVAccountDto(), true);

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
	@DisplayName("Empty list as input")
	void testCollectIntraDayDataEmptyList() throws DataCollectException {

		assertTrue(avDataProvider.collectIntraDayData(Collections.emptyList()).isEmpty());
	}

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.provider.CryptoDataProvider#collectIntraDayData(java.util.List)}.
	 * 
	 * @throws DataCollectException
	 */
	@Test
	@DisplayName("Num Requests initialized as 1")
	void testCollectIntraDayDataNRequestInit() throws DataCollectException {

		assertEquals(1, AlphaVantageCryptoDataProvider.getnRequests().get());
	}

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.provider.CryptoDataProvider#collectIntraDayData(java.util.List)}.
	 * 
	 * @throws DataCollectException
	 */
	@Test
	@DisplayName("Exists data, invoke callService")
	void testCollectIntraDayDataCallService() throws DataCollectException {

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

	}

	@Test
	@DisplayName("Null result")
	void testMapIntraDayRsExceptionResultNull() throws DataCollectException {

		assertThrows(DataCollectException.class, () -> avDataProvider.mapIntraDayRs(null),
				"Error mapping response from AlphaVantage: Result from AV is null");
	}

	@Test
	@DisplayName("Null metadata")
	void testMapIntraDayRsExceptionMetadataNull() throws DataCollectException {

		assertThrows(DataCollectException.class, () -> avDataProvider.mapIntraDayRs(new IntraDay(null, null)),
				"Error mapping response from AlphaVantage: MetaData is null or empty");

	}

	@Test
	@DisplayName("Empty metadata")
	void testMapIntraDayRsExceptionMetadataEmpty() throws DataCollectException {

		Map<String, String> metaData = new HashMap<>();
		metaData.put("4. Market Code", "EUR");

		assertThrows(DataCollectException.class, () -> avDataProvider.mapIntraDayRs(new IntraDay(metaData, null)),
				"Error mapping response from AlphaVantage: MetaData is null or empty");
	}

	@Test
	@DisplayName("Null digitalData")
	void testMapIntraDayRsExceptionDigitalDataNull() throws DataCollectException {

		when(cryptoCurrDao.findByCode("CXT_NULL")).thenReturn(null);

		Map<String, String> metaData = new HashMap<>();
		metaData.put("2. Digital Currency Code", "CXT_NULL");

		assertThrows(DataCollectException.class, () -> avDataProvider.mapIntraDayRs(new IntraDay(metaData, null)),
				"Error mapping response from AlphaVantage: DigitalData is null or empty");
	}

	@Test
	@DisplayName("Empty digitalData")
	void testMapIntraDayRsExceptionDigitalDataEmpty() throws DataCollectException {

		when(cryptoCurrDao.findByCode("CXT")).thenReturn(new CryptoCurrencyDto());

		Map<String, String> metaData = new HashMap<>();
		metaData.put(AlphaVantageCryptoDataProvider.getMdataCurrencyCode(), "CXT");
		List<SimpleCryptoCurrencyData> digitalData1 = null;

		assertThrows(DataCollectException.class,
				() -> avDataProvider.mapIntraDayRs(new IntraDay(metaData, digitalData1)),
				"Error mapping response from AlphaVantage: DigitalData is null or empty");
	}

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.provider.alphavantage.AlphaVantageCryptoDataProvider#mapIntraDayRs(java.lang.Object)}.
	 * 
	 * @throws DataCollectException
	 */
	@Test
	void testMapIntraDayRsExistsResultOK() throws DataCollectException {
		Map<String, String> metaData = getMetaData(cxCode);
		CryptoCurrencyDto cxCurrDto = new CryptoCurrencyDto(cxCode);
		LocalDateTime readtime = LocalDateTime.now();
		List<SimpleCryptoCurrencyData> digitalData = getDigitalData(readtime);

		when(cryptoCurrDao.findByCode(cxCode)).thenReturn(cxCurrDto);

		List<CryptoDataDto> result = avDataProvider.mapIntraDayRs(new IntraDay(metaData, digitalData));
		CryptoDataDto resultData = result.get(0);

		assertNotNull(result);
		assertFalse(result.isEmpty());
		assertNotNull(resultData.getCxCurrencyDto());
		assertEquals(cxCode, resultData.getCxCurrencyDto().getCode());

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
		avDataProvider.prepareDataProvider();
		assertEquals(1, AlphaVantageCryptoDataProvider.getnRequests().get());
	}

	private List<SimpleCryptoCurrencyData> getDigitalData(LocalDateTime readtime) {
		List<SimpleCryptoCurrencyData> digitalData = new ArrayList<>();
		digitalData.add(new SimpleCryptoCurrencyData(readtime, 122.38, 123.45, 119.00, 120.54, 4562987));

		return digitalData;
	}

	Map<String, String> getMetaData(String cxCode) {
		Map<String, String> metaData = new HashMap<>();
		metaData.put(AlphaVantageCryptoDataProvider.getMdataCurrencyCode(), cxCode);

		return metaData;
	}

}
