package com.kssandra.ksd_task.provider.alphavantage;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kssandra.alphavantage_client.connector.AlphaVantageConnector;
import com.kssandra.alphavantage_client.output.IntraDay;

public class AlphaVantageIntradayClient extends AlphaVantageWSClient implements Callable<IntraDay> {

	private static final Logger LOG = LoggerFactory.getLogger(AlphaVantageIntradayClient.class);

	private String interval;

	public AlphaVantageIntradayClient(String cxCode, String apiKey, int timeout, String baseUrl, String interval) {
		super(cxCode, apiKey, timeout, baseUrl);
		this.interval = interval;
	}

	@Override
	public IntraDay call() throws Exception {
		String cxCode = getCryptoCode();
		LOG.debug("Calling AV for {}", cxCode);

		AlphaVantageConnector avConnector = getConnector();

		return avConnector.intraDay(cxCode, "EUR", interval);
	}

}
