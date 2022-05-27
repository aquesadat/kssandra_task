package com.kssandra.ksd_task.provider.alphavantage;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.stereotype.Component;

import com.kssandra.ksd_common.enums.DataProviderEnum;
import com.kssandra.ksd_task.provider.BaseCryptoDataProvider;
import com.kssandra.ksd_task.provider.CryptoData;
import com.kssandra.ksd_task.provider.CryptoDataProvider;

@Component
public class AlphaVantageCryptoDataProvider extends BaseCryptoDataProvider implements CryptoDataProvider {

	public List<CryptoData> getData() {

		ExecutorService pool = Executors.newFixedThreadPool(getThreads());
		Vector<Future<AVDataResult>> result = new Vector();

		for (String cxCode : getCryptoCodes()) {
			Future<AVDataResult> future = pool.submit(new AlphaVantageDataCollect(cxCode));
			result.add(future);
		}

		return null;
	}

	public String getCode() {
		return DataProviderEnum.AV.toString();
	}

}
