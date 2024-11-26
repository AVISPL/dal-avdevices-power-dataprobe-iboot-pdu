package com.insightsystems.symphony.dal.dataprobe.common.metric;

/**
 * Enum representing different types of groups
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 11/25/2024
 * @since 1.0.0
 */
public enum GroupsOptionEnum {
	ON("On"),
	OFF("Off"),
	CYCLE("Cycle"),
	;
	private final String name;

	GroupsOptionEnum(String name) {
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
