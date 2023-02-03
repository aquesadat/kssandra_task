/**
 * 
 */
package com.kssandra.ksd_task.provider.factory;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.kssandra.ksd_common.enums.DataProviderEnum;
import com.kssandra.ksd_common.exception.DataCollectException;
import com.kssandra.ksd_task.provider.CryptoDataProvider;
import com.kssandra.ksd_task.provider.alphavantage.AlphaVantageCryptoDataProvider;

/**
 * @author aquesada
 *
 */
@SpringBootTest
class CryptoDataProviderFactoryTest {

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.provider.factory.CryptoDataProviderFactory#getDataProvider(java.lang.String)}.
	 * 
	 */
	@Test
	@DisplayName("DataProvider null")
	void testGetDataProviderExceptionProviderNull() {
		assertThrows(DataCollectException.class, () -> CryptoDataProviderFactory.getDataProvider(null),
				"Data provider is null");
	}

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.provider.factory.CryptoDataProviderFactory#getDataProvider(java.lang.String)}.
	 * 
	 */
	@Test
	@DisplayName("DataProvider invalid")
	void testGetDataProviderExceptionProviderInvalid() {
		String providerCodeKO = "YF"; // Yahoo Finances
		assertThrows(DataCollectException.class, () -> CryptoDataProviderFactory.getDataProvider(providerCodeKO),
				"Data provider not valid".concat(providerCodeKO));
	}

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.provider.factory.CryptoDataProviderFactory#getDataProvider(java.lang.String)}.
	 * 
	 * @throws DataCollectException
	 */
	@Test
	void testGetDataProviderOK() throws DataCollectException {
		String providerCodeOK = "AV"; // Alpha Vantage
		CryptoDataProvider cxDataProvider = CryptoDataProviderFactory.getDataProvider(providerCodeOK);

		assertNotNull(cxDataProvider);
		assertEquals(DataProviderEnum.AV.toString(), cxDataProvider.getType());
		assertEquals(AlphaVantageCryptoDataProvider.class, cxDataProvider.getClass());

	}

}
