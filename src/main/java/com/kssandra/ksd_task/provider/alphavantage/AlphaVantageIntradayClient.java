package com.kssandra.ksd_task.provider.alphavantage;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kssandra.alphavantage_client.output.IntraDay;

/**
 * Callable implementation to allow concurrent calls to AlphaVantage WS in order
 * to get intraday data
 * 
 * @author aquesada
 *
 */
public class AlphaVantageIntradayClient extends AlphaVantageWSClient implements Callable<IntraDay> {

	private static final Logger LOG = LoggerFactory.getLogger(AlphaVantageIntradayClient.class);

	private String interval;

	/**
	 * Builds an alphavantage intraday client
	 * 
	 * @param cxCode   cryptocurrency code
	 * @param apiKey   apikey to call AV
	 * @param timeout  call timeout
	 * @param baseUrl  url of WS
	 * @param interval period of time for data result
	 */
	public AlphaVantageIntradayClient(String cxCode, String apiKey, int timeout, String baseUrl, String interval) {
		super(cxCode, apiKey, timeout, baseUrl);
		this.interval = interval;
	}

	@Override
	public IntraDay call() throws Exception {
		String cxCode = getCryptoCode();
		LOG.debug("Calling AV for {}", cxCode);
		return getConnector().intraDay(cxCode, "EUR", interval);
	}

}
