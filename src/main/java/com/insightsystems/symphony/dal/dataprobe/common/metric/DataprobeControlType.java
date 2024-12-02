/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.insightsystems.symphony.dal.dataprobe.common.metric;

import java.util.Arrays;
import java.util.Optional;

import com.insightsystems.symphony.dal.dataprobe.common.DataprobeConstant;

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
	 * @param defaultName The name of the sequence property.
	 */
	DataprobeControlType(String defaultName) {
		this.name = defaultName;
	}

	/**
	 * Retrieves {@link #name}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves the Sequence enum based on its default name.
	 *
	 * @param name The default name of the control type enum.
	 * @return The control type enum corresponding to the default name, or unknown if not found.
	 */
	public static DataprobeControlType getByDefaultName(String name) {
		if (name.startsWith(DataprobeConstant.OUTLET)) {
			return DataprobeControlType.OUTLET;
		}

		if (name.startsWith(DataprobeConstant.GROUP)) {
			return DataprobeControlType.GROUP;
		}

		if (name.startsWith(DataprobeConstant.SEQUENCE)) {
			return DataprobeControlType.SEQUENCE;
		}
		return DataprobeControlType.UNKNOWN;
	}
}

