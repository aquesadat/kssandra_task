package com.kssandra.ksd_task.provider.alphavantage;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.kssandra.alphavantage_client.output.IntraDay;
import com.kssandra.alphavantage_client.output.SimpleCryptoCurrencyData;
import com.kssandra.ksd_common.dto.CryptoCurrencyDto;
import com.kssandra.ksd_common.dto.CryptoDataDto;
import com.kssandra.ksd_common.enums.DataProviderEnum;
import com.kssandra.ksd_task.provider.CryptoDataProvider;

@Component
public class AlphaVantageCryptoDataProvider extends CryptoDataProvider {

	private static final Logger LOG = LoggerFactory.getLogger(AlphaVantageCryptoDataProvider.class);

	@Value("${alphavantage.connect.baseurl}")
	private String baseUrl;

	@Value("${alphavantage.connect.timeout}")
	private Integer timeout;

	@Value("${alphavantage.intraday.interval}")
	private String interval;

	private static final int RQ_SLEEP = 30000;

	private static final int MAX_RQ = 6;

	public String getType() {
		return DataProviderEnum.AV.toString();
	}

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
	protected List<CryptoDataDto> mapIntraDayRs(Object result) {
		List<CryptoDataDto> cxDataDtos = new ArrayList<>();

		if (result != null) {
			IntraDay intraResult = (IntraDay) result;
			if (intraResult.getMetaData() != null
					&& intraResult.getMetaData().get(AVResponseParams.MDATA_CURRENCY_CODE) != null) {
				String cxCode = intraResult.getMetaData().get(AVResponseParams.MDATA_CURRENCY_CODE);
				CryptoCurrencyDto cxCurrDto = cryptoCurrDao.findByCode(cxCode);

				if (cxCurrDto != null && intraResult.getDigitalData() != null
						&& !intraResult.getDigitalData().isEmpty()) {

					cxDataDtos = intraResult.getDigitalData().parallelStream()
							.map(elem -> parseIntraDayData(elem, cxCurrDto)).collect(Collectors.toList());
				}
			}
		}

		return cxDataDtos;
	}

	private static CryptoDataDto parseIntraDayData(SimpleCryptoCurrencyData data, CryptoCurrencyDto cxCurrDto) {
		CryptoDataDto dto = new CryptoDataDto();
		dto.setCxCurrencyDto(cxCurrDto);
		dto.setClose(data.getClose());
		dto.setHigh(data.getHigh());
		dto.setLow(data.getLow());
		dto.setOpen(data.getOpen());
		dto.setReadTime(data.getDateTime().atZone(ZoneId.of("GMT")).withZoneSameInstant(ZoneId.of("Europe/Madrid"))
				.toLocalDateTime());
		dto.setHigh(data.getHigh());
		return dto;
	}

	// Rq limits due to free api key
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
	protected void resetClient() {
		nRequests.set(1);
	}

}
