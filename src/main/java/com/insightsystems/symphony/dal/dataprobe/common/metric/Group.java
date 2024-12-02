/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.insightsystems.symphony.dal.dataprobe.common.metric;

import java.util.Arrays;
import java.util.Optional;

/**
 * Enum representing different types of Group settings
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 11/26/2024
 * @since 1.0.0
 */

public enum Group {
	NAME("Name", "Groups#","groupNames"),
	OUTLET_CONTROL("Control","Groups#", "outletControl"),
	STATUS("Status","Groups#", "groups"),
	CYCLE("Cycle","Groups#", "cycle"),
	;

	private final String propertyName;
	private final String group;
	private final String value;

	/**
	 * Constructor for Group.
	 *
	 * @param defaultName The name of the group property.
	 * @param group The group of the Outlet property.
	 * @param value The corresponding value in the group response.
	 */
	Group(String defaultName, String group, String value) {
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

	/**
	 * Retrieves the Group enum based on its default name.
	 *
	 * @param name The default name of the Group enum.
	 * @return The Group enum corresponding to the default name, or null if not found.
	 */
	public static Group getByDefaultName(String name) {
		Optional<Group> property = Arrays.stream(values()).filter(item -> item.getPropertyName().equalsIgnoreCase(name)).findFirst();
		return property.orElse(null);
	}
}

