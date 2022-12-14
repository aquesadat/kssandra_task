package com.kssandra.ksd_task.provider.alphavantage;

import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.kssandra.alphavantage_client.output.IntraDay;
import com.kssandra.alphavantage_client.output.SimpleCryptoCurrencyData;
import com.kssandra.alphavantage_client.service.AlphaVantageService;
import com.kssandra.ksd_common.dto.CryptoCurrencyDto;
import com.kssandra.ksd_common.dto.CryptoDataDto;
import com.kssandra.ksd_common.enums.DataProviderEnum;
import com.kssandra.ksd_common.exception.DataCollectException;
import com.kssandra.ksd_common.logger.KSDLoggerFactory;
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

	private static final Logger LOG = KSDLoggerFactory.getLogger();

	@Value("${alphavantage.connect.baseurl}")
	private String baseUrl;

	// Alphavantage service timeout
	@Value("${alphavantage.connect.timeout}")
	private Integer timeout;

	// Time interval between two consecutive data points
	@Value("${alphavantage.intraday.interval}")
	private String interval;

	@Value("${alphavantage.intraday.outputsize}")
	private String outputSize;

	// Time between batch of calls
	private static final int RQ_SLEEP = 40000;

	// Max number of concurrent calls in a batch
	private static final int MAX_RQ = 6;

	private static final String MDATA_CURRENCY_CODE = "2. Digital Currency Code";

	private static final String DEFAULT_EXCH_CURR = "EUR";

	@Autowired
	protected CryptoCurrencyDao cryptoCurrDao;

	@Autowired
	private AlphaVantageService avService;

	private static AtomicInteger nRequests = new AtomicInteger(1);

	@Override
	protected IntraDay callService(CryptoCurrencyDto cxCurrDto) {
		stopAndGo();// Rq limits due to free api key
		return avService.intraDay(cxCurrDto.getCode(), DEFAULT_EXCH_CURR, interval,
				cxCurrDto.getAvAccountDto().getApiKey(), timeout, baseUrl, outputSize);
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

		throw new DataCollectException("Error mapping response from AlphaVantage: ".concat(errMsg));
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
		return dto;
	}

	/**
	 * Limitations due to free api key usage
	 */
	private static synchronized void stopAndGo() {
		if (nRequests.getAndIncrement() % MAX_RQ == 0) {
			try {
				Thread.sleep(RQ_SLEEP);
				nRequests.set(1);
			} catch (InterruptedException e) {
				LOG.error("Error sleeping thread", e);
			}
		}
	}

	@Override
	protected void prepareDataProvider() {
		nRequests.set(1);
	}

	@Override
	public String getType() {
		return DataProviderEnum.AV.toString();
	}

	public static int getRqSleep() {
		return RQ_SLEEP;
	}

	public static int getMaxRq() {
		return MAX_RQ;
	}

	public static String getMdataCurrencyCode() {
		return MDATA_CURRENCY_CODE;
	}

	public static AtomicInteger getnRequests() {
		return nRequests;
	}

}
