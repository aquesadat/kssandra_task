package com.kssandra.ksd_task.provider.alphavantage;

import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.kssandra.alphavantage_client.output.IntraDay;
import com.kssandra.alphavantage_client.output.SimpleCryptoCurrencyData;
import com.kssandra.ksd_common.dto.CryptoCurrencyDto;
import com.kssandra.ksd_common.dto.CryptoDataDto;
import com.kssandra.ksd_common.enums.DataProviderEnum;
import com.kssandra.ksd_common.exception.DataCollectException;
import com.kssandra.ksd_common.util.DateUtils;
import com.kssandra.ksd_persistence.dao.CryptoCurrencyDao;
import com.kssandra.ksd_task.provider.CryptoDataProvider;

/**
 * Data provider specification for AlphaVantage
 * 
 * @author aquesada
 *
 */
@Component
public class AlphaVantageCryptoDataProvider extends CryptoDataProvider {

	private static final Logger LOG = LoggerFactory.getLogger(AlphaVantageCryptoDataProvider.class);

	@Value("${alphavantage.connect.baseurl}")
	private String baseUrl;

	// Alphavantage service timeout
	@Value("${alphavantage.connect.timeout}")
	private Integer timeout;

	// Time interval between two consecutive data points
	@Value("${alphavantage.intraday.interval}")
	private String interval;

	// Time between batch of calls
	private static final int RQ_SLEEP = 30000;

	// Max number of concurrent calls in a batch
	private static final int MAX_RQ = 6;

	private static final String MDATA_CURRENCY_CODE = "2. Digital Currency Code";

	@Autowired
	protected CryptoCurrencyDao cryptoCurrDao;

	private static AtomicInteger nRequests = new AtomicInteger(1);

	@Override
	protected AlphaVantageIntradayClient getIntraDayClient(CryptoCurrencyDto cxCurrDto) {
		return new AlphaVantageIntradayClient(cxCurrDto.getCode(), cxCurrDto.getAvAccountDto().getApiKey(), timeout,
				baseUrl, interval);
	}

	@Override
	protected Future<?> submitThread(ExecutorService pool, CryptoCurrencyDto cxCurr) {
		stopAndGo();// Rq limits due to free api key
		return pool.submit(getIntraDayClient(cxCurr));
	}

	@Override
	protected List<CryptoDataDto> mapIntraDayRs(Object result) throws DataCollectException {

		String errMsg = null;
		if (result != null) {
			IntraDay intraResult = (IntraDay) result;
			if (intraResult.getMetaData() != null && intraResult.getMetaData().get(MDATA_CURRENCY_CODE) != null) {
				String cxCode = intraResult.getMetaData().get(MDATA_CURRENCY_CODE);
				CryptoCurrencyDto cxCurrDto = cryptoCurrDao.findByCode(cxCode);

				if (cxCurrDto != null && intraResult.getDigitalData() != null
						&& !intraResult.getDigitalData().isEmpty()) {

					try {
						return intraResult.getDigitalData().parallelStream()
								.map(elem -> parseIntraDayData(elem, cxCurrDto)).collect(Collectors.toList());
					} catch (Exception e) {
						errMsg = e.getMessage();
					}
				} else {
					errMsg = "DigitalData is null or empty";
				}
			} else {
				errMsg = "MetaData is null or empty";
			}
		} else {
			errMsg = "Result from AV is null";
		}

		throw new DataCollectException(errMsg);
	}

	/**
	 * Parse from SimpleCryptoCurrencyData (alphavantage_client) to CryptoDataDto
	 * 
	 * @param data
	 * @param cxCurrDto
	 * @return
	 */
	private static CryptoDataDto parseIntraDayData(SimpleCryptoCurrencyData data, CryptoCurrencyDto cxCurrDto) {
		CryptoDataDto dto = new CryptoDataDto();
		dto.setCxCurrencyDto(cxCurrDto);
		dto.setClose(data.getClose());
		dto.setHigh(data.getHigh());
		dto.setLow(data.getLow());
		dto.setOpen(data.getOpen());
		dto.setReadTime(data.getDateTime().atZone(ZoneId.of("GMT"))
				.withZoneSameInstant(ZoneId.of(DateUtils.DEFAULT_OFFSET)).toLocalDateTime());
		dto.setHigh(data.getHigh());
		return dto;
	}

	/**
	 * Limitations due to free api key usage
	 */
	public static synchronized void stopAndGo() {
		if (nRequests.getAndIncrement() % MAX_RQ == 0) {
			try {
				Thread.sleep(RQ_SLEEP);
				nRequests.set(1);
			} catch (InterruptedException e) {
				LOG.error(e.getMessage());
			}
		}
	}

	@Override
	protected void resetDataProvider() {
		nRequests.set(1);
	}

	@Override
	public String getType() {
		return DataProviderEnum.AV.toString();
	}

}
