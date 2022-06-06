package com.kssandra.ksd_task.provider.alphavantage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kssandra.alphavantage_client.connector.AlphaVantageConnector;

public class AlphaVantageWSClient {

	private static final Logger LOG = LoggerFactory.getLogger(AlphaVantageWSClient.class);

	private String cryptoCode;

	private final String apiKey;

	private final int timeout;

	private final String baseUrl;

	public AlphaVantageWSClient(String cryptoCode, String apiKey, int timeout, String baseUrl) {
		this.cryptoCode = cryptoCode;
		this.apiKey = apiKey;
		this.timeout = timeout;
		this.baseUrl = baseUrl;
	}

	protected AlphaVantageConnector getConnector() {
		return new AlphaVantageConnector(apiKey, timeout, baseUrl);
	}

	public String getCryptoCode() {
		return cryptoCode;
	}

}
