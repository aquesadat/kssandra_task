package com.kssandra.ksd_task.provider;

import java.util.List;

public interface CryptoDataProvider {

	public abstract List<CryptoData> getData();

	public abstract String getCode();

}
