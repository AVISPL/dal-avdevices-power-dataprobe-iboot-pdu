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
	public static final String UNDER_SCORE = "_";
	public static final String OUTLET_COMMAND = "outlet";
	public static final String GROUP_COMMAND = "group";
	public static final String TEMPERATURE = "TemperatureT1";
	public static final String TEMPERATURE_T1_PROBE = "TemperatureT2";
	public static final Double MAXIMUM_CURRENT_VALUE = 999.9;

	/* Response properties */
	public static final String RESPONSE_SUCCESS = "/success";
	public static final String RESPONSE_MESSAGE = "/message";
	public static final String RESPONSE_NAMES = "/names";
	public static final String RESPONSE_ANALOG = "/analog";
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
	public static final String STOP = "Stop";
}
