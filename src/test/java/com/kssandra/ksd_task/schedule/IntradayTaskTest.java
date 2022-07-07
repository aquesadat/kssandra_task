/**
 * 
 */
package com.kssandra.ksd_task.schedule;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.kssandra.ksd_persistence.dao.CryptoCurrencyDao;
import com.kssandra.ksd_task.prediction.CryptoDataEval;
import com.kssandra.ksd_task.prediction.CryptoDataPrediction;

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

	/**
	 * Test method for
	 * {@link com.kssandra.ksd_task.schedule.IntradayTask#scheduleTask()}.
	 */
	@Test
	void testScheduleTask() {

		// If there aren't active crypto currencies, provider won´t be called

		// If any data is recovered from provider, any prediction will be made

		// Even if an exception occurs during evaluatePredictions call, predictResults
		// will be executed
	}

}
