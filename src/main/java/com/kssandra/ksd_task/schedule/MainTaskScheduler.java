package com.kssandra.ksd_task.schedule;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kssandra.ksd_common.dto.CryptoDataDto;
import com.kssandra.ksd_persistence.dao.CryptoDataDao;
import com.kssandra.ksd_task.provider.CryptoDataProvider;
import com.kssandra.ksd_task.provider.factory.CryptoDataProviderFactory;

@Component
public class MainTaskScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(MainTaskScheduler.class);

	@Value(value = "${crypto.data.provider}")
	private String provider;

	// @Scheduled(cron = "${intraday.cron.expression}")
	@Scheduled(fixedDelay = Long.MAX_VALUE)
	public void scheduleTask() {

		LOG.debug("Executing scheduled task");

		CryptoDataProvider dataProvider = CryptoDataProviderFactory.getDataProvider(provider);

		Map<String, List<CryptoDataDto>> dataResult = dataProvider.getIntraDayData();

		dataProvider.saveDataResult(dataResult);

	}

}
