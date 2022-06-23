package com.kssandra.ksd_task.provider.factory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.kssandra.ksd_common.exception.DataCollectException;
import com.kssandra.ksd_task.provider.CryptoDataProvider;

/**
 * CryptoDataProviderFactory implements Factory pattern in order to allow
 * multiple providers. Each provider must be identified by a code.
 * 
 * @author aquesada
 *
 */
@Component
public class CryptoDataProviderFactory {

	@Autowired
	private List<CryptoDataProvider> providers;

	private static final Map<String, CryptoDataProvider> providersCache = new HashMap<>();

	/**
	 * Loads providersCache map with all the configured providers (and their codes).
	 */
	@PostConstruct
	public void initProviders() {
		providers.forEach(pr -> providersCache.put(pr.getType(), pr));
	}

	/**
	 * Gets the provider associated to the code specified as parameter
	 * 
	 * @param code provider code
	 * @return provider associated to the specified code
	 * @throws DataCollectException
	 */
	public static CryptoDataProvider getDataProvider(String code) throws DataCollectException {

		if (providersCache.containsKey(code)) {
			return providersCache.get(code);
		} else {
			throw new DataCollectException("Data provider not valid - ".concat(code));
		}
	}

}
