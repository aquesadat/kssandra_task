package com.kssandra.ksd_task.provider.factory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kssandra.ksd_common.enums.DataProviderEnum;
import com.kssandra.ksd_task.provider.CryptoDataProvider;

@Component
public class CryptoDataProviderFactory {

	private static final Logger LOG = LoggerFactory.getLogger(CryptoDataProviderFactory.class);

	@Autowired
	private List<CryptoDataProvider> providers;

	private static final Map<String, CryptoDataProvider> providersCache = new HashMap<>();

	@PostConstruct
	public void initProviders() {
		providers.forEach(pr -> providersCache.put(pr.getType(), pr));

	}

	public static CryptoDataProvider getDataProvider(String code) {

		if (providersCache.containsKey(DataProviderEnum.AV.toString())) {
			return providersCache.get(DataProviderEnum.AV.toString());
		} else {
			LOG.error("Data provider with code {} not valid.", code);
			return null;
		}
	}

}
