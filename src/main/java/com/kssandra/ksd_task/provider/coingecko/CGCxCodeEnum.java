package com.kssandra.ksd_task.provider.coingecko;

import java.util.stream.Stream;

public enum CGCxCodeEnum {

	ADA("ADA", "cardano"), BTC("BTC", "bitcoin"), DOT("DOT", "polkadot"), LTC("LTC", "litecoin"), UNI("UNI", "uniswap"),
	BNB("BNB", "binancecoin"), DOGE("DOGE", "dogecoin"), ETH("ETH", "ethereum"), SOL("SOL", "solana"),
	XRP("XRP", "ripple");

	private String code;

	private String gcCode;

	CGCxCodeEnum(String code, String gcCode) {
		this.code = code;
		this.gcCode = gcCode;
	}

	public String getCode() {
		return code;
	}

	public String getGcCode() {
		return gcCode;
	}

	public static CGCxCodeEnum fromCode(String code) {
		return Stream.of(CGCxCodeEnum.values()).filter(e -> e.getCode().equals(code)).findFirst().orElse(null);
	}

	public static CGCxCodeEnum fromGCCode(String gcCode) {
		return Stream.of(CGCxCodeEnum.values()).filter(e -> e.getGcCode().equals(gcCode)).findFirst().orElse(null);
	}

	public static String getGCCode(String code) {
		CGCxCodeEnum gcEnum = fromCode(code);
		return gcEnum != null ? gcEnum.getGcCode() : null;
	}

	public static String getCode(String gcCode) {
		CGCxCodeEnum gcEnum = fromGCCode(gcCode);
		return gcEnum != null ? gcEnum.getCode() : null;
	}
}
