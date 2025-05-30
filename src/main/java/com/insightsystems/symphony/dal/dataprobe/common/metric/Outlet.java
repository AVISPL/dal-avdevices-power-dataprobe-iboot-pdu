/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.insightsystems.symphony.dal.dataprobe.common.metric;

/**
 * Enum representing different types of Outlet settings
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 11/26/2024
 * @since 1.0.0
 */
public enum Outlet {
	NAME("Name", "Outlets#","outletNames"),
	OUTLET_CONTROL("Control","Outlets#", "outletControl"),
	STATUS("Status","Outlets#", "outlet"),
	CYCLE("Cycle","Outlets#", "cycle"),
	;

	private final String propertyName;
	private final String group;
	private final String value;

	/**
	 * Constructor for Outlet.
	 *
	 * @param defaultName The name of the Outlet property.
	 * @param group The group of the Outlet property.
	 * @param value The corresponding value in the Outlet response.
	 */
	Outlet(String defaultName, String group, String value) {
		this.propertyName = defaultName;
		this.group = group;
		this.value = value;
	}

	/**
	 * Retrieves {@link #propertyName}
	 *
	 * @return value of {@link #propertyName}
	 */
	public String getPropertyName() {
		return propertyName;
	}

	/**
	 * Retrieves {@link #group}
	 *
	 * @return value of {@link #group}
	 */
	public String getGroup() {
		return group;
	}

	/**
	 * Retrieves {@link #value}
	 *
	 * @return value of {@link #value}
	 */
	public String getValue() {
		return value;
	}

}
