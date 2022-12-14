package com.kssandra.ksd_task.provider.coingecko;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.kssandra.ksd_common.dto.CryptoCurrencyDto;
import com.kssandra.ksd_common.dto.CryptoDataDto;
import com.kssandra.ksd_common.enums.DataProviderEnum;
import com.kssandra.ksd_common.exception.DataCollectException;
import com.kssandra.ksd_common.logger.KSDLoggerFactory;
import com.kssandra.ksd_common.util.DateUtils;
import com.kssandra.ksd_persistence.dao.CryptoCurrencyDao;
import com.kssandra.ksd_task.provider.CryptoDataProvider;
import com.litesoftwares.coingecko.CoinGeckoApiClient;
import com.litesoftwares.coingecko.impl.CoinGeckoApiClientImpl;

/**
 * Data provider specification for CoinGecko
 * 
 * @author aquesada
 *
 */
@Component
public class CoinGeckoCryptoDataProvider extends CryptoDataProvider {

	private static final Logger LOG = KSDLoggerFactory.getLogger();

	// Time between batch of calls
	private static final int RQ_SLEEP = 1000;

	// Max number of concurrent calls in a batch
	private static final int MAX_RQ = 0;

	private static final String DEFAULT_EXCH_CURR = "eur";

	@Autowired
	protected CryptoCurrencyDao cryptoCurrDao;

	private static AtomicInteger nRequests = new AtomicInteger(1);

	@Override
	protected Map<String, Map<String, Double>> callService(CryptoCurrencyDto cxCurrDto) {
		stopAndGo();
		CoinGeckoApiClient client = new CoinGeckoApiClientImpl();
		String cxCode = CGCxCodeEnum.getGCCode(cxCurrDto.getCode());
		Map<String, Map<String, Double>> result = client.getPrice(cxCode, DEFAULT_EXCH_CURR);
		client.shutdown();
		return result;
	}

	@Override
	protected List<CryptoDataDto> mapIntraDayRs(Object result) throws DataCollectException {

		String errMsg = null;
		if (result != null) {
			Map<String, Map<String, Double>> intraResult = (Map<String, Map<String, Double>>) result;
			String cxCurr = intraResult.keySet().stream().findFirst().orElse(null);
			if (cxCurr != null) {
				String cxCode = CGCxCodeEnum.getCode(cxCurr);
				CryptoCurrencyDto cxCurrDto = cryptoCurrDao.findByCode(cxCode);
				Double price = intraResult.get(cxCurr).get("eur");
				return List.of(parseIntraDayData(price, cxCurrDto));
			} else {
				errMsg = "Crypto currency not provided";
			}
		} else {
			errMsg = "Result from GC is null";
		}

		throw new DataCollectException("Error mapping response from CoinGecko: ".concat(errMsg));
	}

	/**
	 * Get a CryptoDataDto from price and cxCurr
	 * 
	 * @param price
	 * @param cxCurrDto
	 * @return
	 */
	private static CryptoDataDto parseIntraDayData(Double price, CryptoCurrencyDto cxCurrDto) {
		CryptoDataDto dto = new CryptoDataDto();
		dto.setCxCurrencyDto(cxCurrDto);
		dto.setClose(price);
		dto.setHigh(price);
		dto.setLow(price);
		dto.setOpen(price);
		dto.setReadTime(LocalDateTime.now(ZoneId.of(DateUtils.DEFAULT_OFFSET)).withSecond(0).withNano(0));
		return dto;
	}

	/**
	 * Limitations due to free api key usage
	 */
	private static synchronized void stopAndGo() {
		try {
			LOG.debug("Waiting {} ms", RQ_SLEEP);
			Thread.sleep(RQ_SLEEP);
		} catch (InterruptedException e) {
			LOG.error("Error sleeping thread", e);
		}

	}

	@Override
	protected void prepareDataProvider() {
		nRequests.set(1);
	}

	@Override
	public String getType() {
		return DataProviderEnum.CG.toString();
	}

	public static int getRqSleep() {
		return RQ_SLEEP;
	}

	public static int getMaxRq() {
		return MAX_RQ;
	}

	public static AtomicInteger getnRequests() {
		return nRequests;
	}

}
