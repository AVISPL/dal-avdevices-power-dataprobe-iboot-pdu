/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.insightsystems.symphony.dal.dataprobe.common.metric;

/**
 * Enum representing different types of Control type settings
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 11/26/2024
 * @since 1.0.0
 */

public enum DataprobeControlType {
	OUTLET("outlet"),
	GROUP("group"),
	SEQUENCE("sequence"),
	UNKNOWN("unknown");

	private final String name;

	/**
	 * Constructor for Sequence.
	 *
	 * @param name The name of the sequence property.
	 */
	DataprobeControlType(String name) {
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

