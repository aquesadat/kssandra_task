package com.kssandra.ksd_task.provider;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kssandra.ksd_persistence.dao.CryptoCurrencyDao;

@Component
public class BaseCryptoDataProvider {

	private static final Logger LOG = LoggerFactory.getLogger(BaseCryptoDataProvider.class);

	@Autowired
	protected CryptoCurrencyDao cryptoCurrDao;

	private List<String> cryptoCodes = null;

	protected List<String> getCryptoCodes() {
		if (cryptoCodes == null) {
			cryptoCodes = cryptoCurrDao.getAllCodes();
		}
		return cryptoCodes;
	}

	protected int getThreads() {
		int nThreads = Runtime.getRuntime().availableProcessors();
		LOG.info("Threads to create: {}", nThreads);
		return nThreads;
	}

}
