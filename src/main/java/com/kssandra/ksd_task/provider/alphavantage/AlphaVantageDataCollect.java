package com.kssandra.ksd_task.provider.alphavantage;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlphaVantageDataCollect implements Callable<AVDataResult> {

	private static final Logger LOG = LoggerFactory.getLogger(AlphaVantageDataCollect.class);

	private String cryptoCode;

	public AlphaVantageDataCollect(String cryptoCode) {
		super();
		this.cryptoCode = cryptoCode;
	}

	@Override
	public AVDataResult call() throws Exception {

		LOG.debug("Getting data for {}", cryptoCode);
		return null;
	}

}
