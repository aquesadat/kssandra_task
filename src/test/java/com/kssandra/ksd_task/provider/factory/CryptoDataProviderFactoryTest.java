/**
 * 
 */
package com.kssandra.ksd_task.provider.factory;

import static org.junit.jupiter.api.Assertions.*;

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
	 * @throws DataCollectException
	 */
	@Test
	void testGetDataProvider() throws DataCollectException {
		String providerCodeOK = "AV"; // Alpha Vantage
		String providerCodeKO = "YF"; // Yahoo Finances

		CryptoDataProvider cxDataProvider = CryptoDataProviderFactory.getDataProvider(providerCodeOK);

		assertNotNull(cxDataProvider);
		assertEquals(DataProviderEnum.AV.toString(), cxDataProvider.getType());
		assertEquals(AlphaVantageCryptoDataProvider.class, cxDataProvider.getClass());

		assertThrows(DataCollectException.class, () -> CryptoDataProviderFactory.getDataProvider(providerCodeKO),
				"Data provider not valid".concat(providerCodeKO));

		assertThrows(DataCollectException.class, () -> CryptoDataProviderFactory.getDataProvider(null),
				"Data provider is null");

	}

}
