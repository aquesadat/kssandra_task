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
class H2Test {

	@Autowired
	CryptoDataProvider cxDataProvider;

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.provider.CryptoDataProvider#collectIntraDayData(java.util.List)}.
	 * 
	 * @throws DataCollectException
	 */
	@Test
	void testCollectIntraDayData() throws DataCollectException {

		List<CryptoCurrencyDto> activeCxCurrs = new ArrayList<>();
		CryptoCurrencyDto cxCurr1 = new CryptoCurrencyDto();
		cxCurr1.setCode("CXT1");
		activeCxCurrs.add(cxCurr1);
		CryptoCurrencyDto cxCurr2 = new CryptoCurrencyDto();
		cxCurr2.setCode("CXT2");
		activeCxCurrs.add(cxCurr2);

		cxDataProvider.saveDataResult(null, activeCxCurrs);
	}

}
