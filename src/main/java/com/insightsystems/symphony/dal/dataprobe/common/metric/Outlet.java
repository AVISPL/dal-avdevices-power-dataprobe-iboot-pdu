package com.insightsystems.symphony.dal.dataprobe.common.metric;

import java.util.Arrays;
import java.util.Optional;

public enum Outlet {
	NAME("Name", "Outlets#","outletNames"),
	OUTLET_CONTROL("OutletControl","Outlets#", "outletControl"),
	STATUS("Status","Outlets#", "outlet"),
	;

	private final String propertyName;
	private final String group;
	private final String value;

	/**
	 * Constructor for Outlet.
	 *
	 * @param defaultName The name of the Outlet property.
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

	/**
	 * Retrieves the Outlet enum based on its default name.
	 *
	 * @param name The default name of the Outlet enum.
	 * @return The Outlet enum corresponding to the default name, or null if not found.
	 */
	public static Outlet getByDefaultName(String name) {
		Optional<Outlet> property = Arrays.stream(values()).filter(item -> item.getPropertyName().equalsIgnoreCase(name)).findFirst();
		return property.orElse(null);
	}
}
