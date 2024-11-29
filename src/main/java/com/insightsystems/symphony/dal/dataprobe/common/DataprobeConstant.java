/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.insightsystems.symphony.dal.dataprobe.common;

/**
 * Enum representing various the constant.
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 11/20/2024
 * @since 1.0.0
 */
public class DataprobeConstant {
	public static final String AUTHENTICATION_PARAM = "{\"username\":\"%s\", \"password\":\"%s\"}";
	public static final String RETRIEVE_NAME = "{\"token\":\"%s\", \"names\":[\"outlets\",\"groups\",\"sequences\"], \"analog\":[\"LC1\",\"LC2\",\"LV1\",\"LV2\",\"T0\",\"T1\"]}";
	public static final String GROUP = "Group_";
	public static final String OUTLET = "Outlet_";
	public static final String SEQUENCE = "Sequence_";
	public static final String NONE = "None";
	public static final String EMPTY = "";
	public static final String HASH = "#";
	public static final String INVALID_TOKEN = "invalid token";

	/* Response properties */
	public static final String RESPONSE_SUCCESS = "/success";
	public static final String RESPONSE_MESSAGE = "/message";
	public static final String RESPONSE_OUTLETS = "/outlets";
	public static final String RESPONSE_GROUPS = "/groups";

	/* Analog properties */
	public static final String VOLTAGE = "Voltage";
	public static final String CURRENT = "Current";
	public static final String T0 = "T0";
	public static final String T1 = "T1";
	public static final String ON = "On";
	public static final String OFF = "Off";

	/* Button */
	public static final String CYCLE = "Cycle";
	public static final String CYCLING = "Cycling";

	public static final String MONITORED_DEVICES_TOTAL = "MonitoredDevicesTotal";
	public static final String MONITORING_CYCLE_DURATION = "LastMonitoringCycleDuration(s)";
	public static final String ADAPTER_VERSION = "AdapterVersion";
	public static final String ADAPTER_BUILD_DATE = "AdapterBuildDate";
	public static final String ADAPTER_UPTIME_MIN = "AdapterUptime(min)";
	public static final String ADAPTER_UPTIME = "AdapterUptime";
	public static final String ADAPTER_RUNNER_SIZE = "RunnerSize(kB)";
}
