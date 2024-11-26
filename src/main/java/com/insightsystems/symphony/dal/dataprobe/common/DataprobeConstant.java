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
	public static final String RETRIEVE_NAME = "{\"token\":\"%s\", \"names\":[\"outlets\",\"groups\",\"sequences\"]}";
	public static final String GROUPS = "Groups#";
	public static final String TOKEN_EXPIRED = "Token expired";
	public static final String NONE = "None";
	public static final String NAME = "Name";
	public static final String OUTLET_CONTROL = "OutletControl";
	public static final String STATUS = "Status";
	public static final String HASH = "#";
	public static final String INVALID_TOKEN = "invalid token";

	public static final String MONITORED_DEVICES_TOTAL = "MonitoredDevicesTotal";
	public static final String MONITORING_CYCLE_DURATION = "LastMonitoringCycleDuration(s)";
	public static final String ADAPTER_VERSION = "AdapterVersion";
	public static final String ADAPTER_BUILD_DATE = "AdapterBuildDate";
	public static final String ADAPTER_UPTIME_MIN = "AdapterUptime(min)";
	public static final String ADAPTER_UPTIME = "AdapterUptime";
	public static final String ADAPTER_RUNNER_SIZE = "RunnerSize(kB)";
}
