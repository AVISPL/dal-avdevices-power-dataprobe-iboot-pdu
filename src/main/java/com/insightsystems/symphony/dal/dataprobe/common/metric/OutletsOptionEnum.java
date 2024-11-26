package com.insightsystems.symphony.dal.dataprobe.common.metric;

/**
 * Enum representing different types of outlets
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 11/25/2024
 * @since 1.0.0
 */
public enum OutletsOptionEnum {
	ON("On"),
	OFF("Off"),
	CYCLE("Cycle"),
	;
	private final String name;

	OutletsOptionEnum(String name) {
		this.name = name;
	}

	/**
	 * Retrieves {@link #name}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return name;
	}

}
