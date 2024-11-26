/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */
package com.insightsystems.symphony.dal.dataprobe;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty.DropDown;
import com.avispl.symphony.api.dal.dto.monitor.GenericStatistics;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightsystems.symphony.dal.dataprobe.Serialisers.ControlObject;
import com.insightsystems.symphony.dal.dataprobe.common.DataprobeCommand;
import com.insightsystems.symphony.dal.dataprobe.common.DataprobeConstant;
import com.insightsystems.symphony.dal.dataprobe.common.EnumTypeHandler;
import com.insightsystems.symphony.dal.dataprobe.common.LoginInfo;
import com.insightsystems.symphony.dal.dataprobe.common.metric.Group;
import com.insightsystems.symphony.dal.dataprobe.common.metric.GroupsOptionEnum;
import com.insightsystems.symphony.dal.dataprobe.common.metric.Outlet;
import com.insightsystems.symphony.dal.dataprobe.common.metric.OutletsOptionEnum;
import javax.security.auth.login.FailedLoginException;
import org.apache.http.auth.InvalidCredentialsException;
import org.openjdk.jol.info.ClassLayout;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.error.ResourceNotReachableException;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.dal.communicator.RestCommunicator;
import com.avispl.symphony.dal.util.StringUtils;

/**
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 8/15/2024
 * @since 1.0.0
 */
public class DataprobeiBootPDUCommunicator extends RestCommunicator implements Monitorable, Controller {



	/**
	 * ReentrantLock to prevent telnet session is closed when adapter is retrieving statistics from the device.
	 */
	private final ReentrantLock reentrantLock = new ReentrantLock();

	/**
	 * Store previous/current ExtendedStatistics
	 */
	private ExtendedStatistics localExtendedStatistics;

	/** Adapter metadata properties - adapter version and build date */
	private Properties adapterProperties;

	/**
	 * Device adapter instantiation timestamp.
	 */
	private long adapterInitializationTimestamp;

	/**
	 * isEmergencyDelivery to check if control flow is trigger
	 */
	private boolean isEmergencyDelivery;


	/**
	 * A cache that maps route names to their corresponding values.
	 */
	private final Map<String, String> cacheValue = new HashMap<>();

	/**
	 *  genericStatistics represent the generic statistics
	 **/
	private GenericStatistics genericStatistics = new GenericStatistics();

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * the login info
	 */
	private LoginInfo loginInfo;

	/**
	 * List name of Groups
	 * */
	private final List<String> groupNames = new ArrayList<>();

	/**
	 * List name of Outlets
	 * */
	private final List<String> outletNames = new ArrayList<>();

	/**
	 * Constructs a new instance of HaivisionKrakenCommunicator.
	 */
	public DataprobeiBootPDUCommunicator() throws IOException {
		adapterProperties = new Properties();
		adapterProperties.load(getClass().getResourceAsStream("/version.properties"));
		this.setTrustAllCertificates(true);
	}

	public static JsonNode loadMockData(String filePath) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.readTree(new File(filePath));
	}

//	/**
//	 * {@inheritDoc}
//	 */
//	@Override
//	protected void authenticate() throws Exception {
//		String jsonPayload = String.format(DataprobeConstant.AUTHENTICATION_PARAM, this.getLogin(), this.getPassword());
//		JsonNode response = this.doPost(DataprobeCommand.API_LOGIN, jsonPayload, JsonNode.class);
//		if(response.has("success")){
//			if (response.at("/success").asBoolean()){
//				this.loginInfo.setToken(response.at("/token").asText());
//			} else {
//				loginInfo = null;
//				throw new InvalidCredentialsException(response.at("/message").asText());
//			}
//		}
//	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void authenticate() throws Exception {
		String jsonPayload = String.format(DataprobeConstant.AUTHENTICATION_PARAM, this.getLogin(), this.getPassword());
		try {
			String result = this.doPost(DataprobeCommand.API_LOGIN, jsonPayload);
			JsonNode response = objectMapper.readTree(result);
			if (response.has("success")) {
				if (response.at("/success").asBoolean()) {
					this.loginInfo.setToken(response.at("/token").asText());
				} else {
					loginInfo = null;
					throw new InvalidCredentialsException(response.at("/message").asText());
				}
			}
		} catch (Exception e) {
			throw new FailedLoginException("Auth error when get token api" + e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalInit() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Internal init is called.");
		}
		adapterInitializationTimestamp = System.currentTimeMillis();
		super.internalInit();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalDestroy() {
		if (logger.isDebugEnabled()) {
			logger.debug("Internal destroy is called.");
		}
		localExtendedStatistics = null;
		cacheValue.clear();
		super.internalDestroy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected HttpHeaders putExtraRequestHeaders(HttpMethod httpMethod, String uri, HttpHeaders headers) throws Exception {
		if (httpMethod == HttpMethod.GET || uri.contains("/html-endpoint")) {
			headers.set("Content-Type", "text/html; charset=UTF-8");
		} else {
			headers.set("Content-Type", "application/json");
		}
		return headers;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Statistics> getMultipleStatistics() throws Exception {
		reentrantLock.lock();
		try {
			if (loginInfo == null) {
				loginInfo = new LoginInfo();
			}
			checkValidApiToken();
			Map<String, String> stats = new HashMap<>();
			Map<String, String> dynamicStatistics = new HashMap<>();
			Map<String, String> controlStats = new HashMap<>();
			ExtendedStatistics extendedStatistics = new ExtendedStatistics();

			List<AdvancedControllableProperty> advancedControllableProperties = new ArrayList<>();

			if (!isEmergencyDelivery) {
//				getOutletStates(stats, controlStats, advancedControllableProperties);
				retrieveName();
				getOutletStates(stats,advancedControllableProperties);
				retrieveMetadata(stats, dynamicStatistics);
				stats.putAll(controlStats);
				extendedStatistics.setControllableProperties(advancedControllableProperties);

				extendedStatistics.setStatistics(stats);
				extendedStatistics.setDynamicStatistics(dynamicStatistics);
				localExtendedStatistics = extendedStatistics;
			}
			isEmergencyDelivery = false;
		} finally {
			reentrantLock.unlock();
		}
		return Collections.singletonList(localExtendedStatistics);
	}

	/**
	 * Retrieves metadata information and updates the provided statistics and dynamic map.
	 *
	 * @param stats the map where statistics will be stored
	 * @param dynamicStatistics the map where dynamic statistics will be stored
	 * @throws Exception if there is an error during the retrieval process
	 */
	private void retrieveMetadata(Map<String, String> stats, Map<String, String> dynamicStatistics) throws Exception {
		try {
			stats.put(DataprobeConstant.ADAPTER_VERSION,
					getDefaultValueForNullData(adapterProperties.getProperty("aggregator.version")));
			stats.put(DataprobeConstant.ADAPTER_BUILD_DATE,
					getDefaultValueForNullData(adapterProperties.getProperty("aggregator.build.date")));

			dynamicStatistics.put(DataprobeConstant.ADAPTER_RUNNER_SIZE,
					String.valueOf(ClassLayout.parseInstance(this).toPrintable().length()/1000));

			long adapterUptime = System.currentTimeMillis() - adapterInitializationTimestamp;
			stats.put(DataprobeConstant.ADAPTER_UPTIME_MIN, String.valueOf(adapterUptime / (1000 * 60)));
			stats.put(DataprobeConstant.ADAPTER_UPTIME, normalizeUptime(adapterUptime / 1000));
		} catch (Exception e) {
			logger.error("Failed to populate metadata information ", e);
		}
	}

	/**
	 *
	 * */
	private void getOutletStates(Map<String, String> stats, Map<String, String> controlStats, List<AdvancedControllableProperty> advancedControllableProperties) throws Exception {
		String[] option = {"On", "Off", "Cycle"};
		String[] optionName = {"Sequence01", "Sequence02"};
		String[] optionGroup = {"Run", "Stop"};

		stats.put("Outlet01#Name", "VNOC-QSYS-DSP");
		stats.put("Outlet01#Status", "On");
		addAdvancedControlProperties(advancedControllableProperties, controlStats, createDropdown("Outlet01#OutletControl", option, "On"), "On");

		stats.put("Outlet02#Name", "Spare");
		stats.put("Outlet02#Status", "On");
		addAdvancedControlProperties(advancedControllableProperties, controlStats, createDropdown("Outlet02#OutletControl", option, "On"), "On");

		stats.put("Outlet03#Name", "Spare");
		stats.put("Outlet03#Status", "On");
		addAdvancedControlProperties(advancedControllableProperties, controlStats, createDropdown("Outlet03#OutletControl", option, "On"), "On");

		stats.put("Outlet04#Name", "Spare");
		stats.put("Outlet04#Status", "On");
		addAdvancedControlProperties(advancedControllableProperties, controlStats, createDropdown("Outlet04#OutletControl", option, "On"), "On");

		stats.put("Outlet05#Name", "Spare");
		stats.put("Outlet05#Status", "On");
		addAdvancedControlProperties(advancedControllableProperties, controlStats, createDropdown("Outlet05#OutletControl", option, "On"), "On");

		stats.put("Outlet06#Name", "Spare");
		stats.put("Outlet06#Status", "On");
		addAdvancedControlProperties(advancedControllableProperties, controlStats, createDropdown("Outlet06#OutletControl", option, "On"), "On");

		stats.put("Outlet07#Name", "Spare");
		stats.put("Outlet07#Status", "On");
		addAdvancedControlProperties(advancedControllableProperties, controlStats, createDropdown("Outlet07#OutletControl", option, "On"), "On");

		stats.put("Outlet08#Name", "Spare");
		stats.put("Outlet08#Status", "On");
		addAdvancedControlProperties(advancedControllableProperties, controlStats, createDropdown("Outlet08#OutletControl", option, "On"), "On");

		stats.put("Group01#Name", "Group 1");
		stats.put("Group01#Status", "Run");
		addAdvancedControlProperties(advancedControllableProperties, controlStats, createDropdown("Group01#OutletControl", optionGroup, "Run"), "Run");

		stats.put("Group02#Name", "Group 2");
		stats.put("Group02#Status", "Run");
		addAdvancedControlProperties(advancedControllableProperties, controlStats, createDropdown("Group02#OutletControl", optionGroup, "Run"), "Run");

		stats.put("Group03#Name", "Group 3");
		stats.put("Group03#Status", "Run");
		addAdvancedControlProperties(advancedControllableProperties, controlStats, createDropdown("Group03#OutletControl", optionGroup, "Run"), "Run");

		stats.put("Group04#Name", "Group 4");
		stats.put("Group04#Status", "Run");
		addAdvancedControlProperties(advancedControllableProperties, controlStats, createDropdown("Group04#OutletControl", optionGroup, "Run"), "Run");

		addAdvancedControlProperties(advancedControllableProperties, controlStats, createDropdown("Sequence#Name", optionName, "Sequence01"), "Sequence01");
		addAdvancedControlProperties(advancedControllableProperties, controlStats, createButton("Sequence#Control", "Run", "Running"), "None");
		addAdvancedControlProperties(advancedControllableProperties, controlStats, createButton("Sequence#Control", "Stop", "Stopping"), "None");

		stats.put("Line1#Voltage(V)", "258.9");
		stats.put("Line1#Current(mA)", "0.3");

		stats.put("Line2#Voltage(V)", "999.9");
		stats.put("Line2#Current(mA)", "999.9");

		stats.put("Temperature#T1(C)", "20");
		stats.put("Temperature#T2(C)", "25");
	}



	/**
	 * Check API token validation
	 * If the token expires, we send a request to get a new token
	 */
	private void checkValidApiToken() throws Exception {
		if (StringUtils.isNullOrEmpty(this.getLogin()) || StringUtils.isNullOrEmpty(this.getPassword())) {
			throw new FailedLoginException("Username or Password field is empty. Please check device credentials");
		}
		if (this.loginInfo.isTimeout() || this.loginInfo.getToken() == null) {
			authenticate();
		}
	}

	private void retrieveName() throws Exception{
//		JsonNode namesResponse = loadMockData("src/main/java/com/insightsystems/symphony/dal/dataprobe/common/mockdata/mockdata.json");
		String jsonPayload = String.format(DataprobeConstant.RETRIEVE_NAME, this.loginInfo.getToken());
		String result = this.doPost(DataprobeCommand.RETRIEVE_INFO, jsonPayload);
		JsonNode namesResponse = objectMapper.readTree(result);
		if (namesResponse.has("names")){
			JsonNode namesJson = namesResponse.at("/names");
			if (namesJson.has("outletNames") && namesJson.has("groupNames")){
				JsonNode groups = namesJson.at("/groupNames");
				groupNames.clear();
				for (Iterator<String> it = groups.fieldNames(); it.hasNext(); ) {
					String key = it.next();
					groupNames.add(groups.at("/"+key).asText());
				}

				JsonNode outlets = namesJson.at("/outletNames");
				outletNames.clear();
				for (Iterator<String> it = outlets.fieldNames(); it.hasNext(); ) {
					String key = it.next();
					outletNames.add(outlets.at("/"+key).asText());
				}
			} else{
				throw new Exception("Unable to parse names from response. 'outletNames' or 'groupNames' fields missing.");
			}
		} else {
			throw new Exception("Unable to parse names from response. 'names' field missing in response.");
		}
	}

	private void getOutletStates(Map<String, String> stats, List<AdvancedControllableProperty> controls) throws Exception {
//		JsonNode stateResponse = loadMockData("src/main/java/com/insightsystems/symphony/dal/dataprobe/common/mockdata/mockupstatus.json");
		String result = this.doPost("/services/retrieve/", createJsonRetrieveString());
		JsonNode stateResponse = objectMapper.readTree(result);
		checkForErrors(stateResponse);
		if (stateResponse.has("outlets")){
			JsonNode outletStates = stateResponse.at("/outlets");
			int outletNumber = 1;
			for (Iterator<String> it = outletStates.fieldNames(); it.hasNext(); ) {
				String fieldName = it.next();
				createOutletStats(outletNumber,fieldName,outletStates.at("/"+fieldName).asText(),stats,controls);
				outletNumber++;
			}
		} else{
			throw new Exception("Unable to parse outlet states. 'outlets' field not found.");
		}

		if (stateResponse.has("groups")){
			JsonNode groupStates = stateResponse.at("/groups");
			for (Iterator<String> it = groupStates.fieldNames(); it.hasNext(); ) {
				String fieldName = it.next();
				createGroupStats(fieldName,groupStates.at("/"+fieldName).asText(),stats,controls);
			}
		} else{
			throw new Exception("Unable to parse outlet states. 'outlets' field not found.");
		}
	}

	private void createOutletStats(int number,String name, String state, Map<String, String> stats, List<AdvancedControllableProperty> controls) {
		String[] possibleValues = EnumTypeHandler.getEnumNames(OutletsOptionEnum.class);
		for (Outlet item : Outlet.values()){
			String propertyName = uppercaseFirstCharacter(name) + DataprobeConstant.HASH + item.getPropertyName();
			switch (item){
				case NAME:
					stats.put(propertyName, name);
					break;
				case STATUS:
					stats.put(propertyName, state);
					controls.add(createDropdown(propertyName, possibleValues, state));
					break;
				case OUTLET_CONTROL:
					if (Arrays.asList(possibleValues).contains(state)) {
						addAdvancedControlProperties(controls, stats, createDropdown(propertyName, possibleValues, state), state);
					} else {
						stats.put(propertyName, DataprobeConstant.NONE);
					}
					break;
				default:
					stats.put(propertyName, state);
					break;
			}
		}
	}

	private void createGroupStats(String name,String state,Map<String,String> stats, List<AdvancedControllableProperty> controls){
		String[] possibleValues = EnumTypeHandler.getEnumNames(GroupsOptionEnum.class);
	for (Group item : Group.values()){
		String propertyName = uppercaseFirstCharacter(name) + DataprobeConstant.HASH + item.getPropertyName();
		switch (item){
			case NAME:
				stats.put(propertyName, name);
				break;
			case STATUS:
				if ("?".equals(state)) {
					stats.put(propertyName, "No outlets in group");
				} else {
					stats.put(propertyName, state);
					controls.add(createDropdown(propertyName, possibleValues, state));
				}
					break;
			case OUTLET_CONTROL:
					stats.put(propertyName, state);
					addAdvancedControlProperties(controls, stats, createDropdown(propertyName, possibleValues, state), state);
					stats.put(uppercaseFirstCharacter(name) + DataprobeConstant.HASH + Group.STATUS.getPropertyName(), state);
				break;
			default:
				stats.put(propertyName, state);
				break;
		}
	}
	}

	/**
	 * Create dropdown advanced controllable property
	 *
	 * @param name the name of the control
	 * @param initialValue initial value of the control
	 * @return AdvancedControllableProperty dropdown instance
	 */
	private AdvancedControllableProperty createDropdown(String name, String[] values, String initialValue) {
		DropDown dropDown = new DropDown();
		dropDown.setOptions(values);
		dropDown.setLabels(values);

		return new AdvancedControllableProperty(name, new Date(), dropDown, initialValue);
	}

	private String createJsonRetrieveString() throws JsonProcessingException {
		Serialisers.StateRequestObject stateRequestObject = new Serialisers.StateRequestObject(this.loginInfo.getToken(),outletNames.toArray(new String[0]),groupNames.toArray(new String[0]));
		return objectMapper.writeValueAsString(stateRequestObject);
	}

	/**
	 * Create a button.
	 *
	 * @param name name of the button
	 * @param label label of the button
	 * @param labelPressed label of the button after pressing it
	 * @return This returns the instance of {@link AdvancedControllableProperty} type Button.
	 */
	private AdvancedControllableProperty createButton(String name, String label, String labelPressed) {
		AdvancedControllableProperty.Button button = new AdvancedControllableProperty.Button();
		button.setLabel(label);
		button.setLabelPressed(labelPressed);
		return new AdvancedControllableProperty(name, new Date(), button, "");
	}

	/**
	 * Add addAdvancedControlProperties if advancedControllableProperties different empty
	 *
	 * @param advancedControllableProperties advancedControllableProperties is the list that store all controllable properties
	 * @param stats store all statistics
	 * @param property the property is item advancedControllableProperties
	 * @throws IllegalStateException when exception occur
	 */
	private void addAdvancedControlProperties(List<AdvancedControllableProperty> advancedControllableProperties, Map<String, String> stats, AdvancedControllableProperty property, String value) {
		if (property != null) {
			advancedControllableProperties.removeIf(controllableProperty -> controllableProperty.getName().equals(property.getName()));

			String propertyValue = StringUtils.isNotNullOrEmpty(value) ? value : "";
			stats.put(property.getName(), propertyValue);

			advancedControllableProperties.add(property);
		}
	}

	private void checkForErrors(JsonNode deviceResponse) throws Exception {
		if (!deviceResponse.at("/success").asBoolean()) {
			if (!deviceResponse.at("/message").asText().contains("There are no Groups")){
				throw new Exception(deviceResponse.at("/message").asText());
			}
		}
	}

	/**
	 * check value is null or empty
	 *
	 * @param value input value
	 * @return value after checking
	 */
	private String getDefaultValueForNullData(String value) {
		return StringUtils.isNotNullOrEmpty(value) && !"null".equalsIgnoreCase(value) ? uppercaseFirstCharacter(value) : DataprobeConstant.NONE;
	}

	/**
	 * capitalize the first character of the string
	 *
	 * @param input input string
	 * @return string after fix
	 */
	private String uppercaseFirstCharacter(String input) {
		char firstChar = input.charAt(0);
		return Character.toUpperCase(firstChar) + input.substring(1);
	}

	private String formatOutletName(int number,String name) {return "Outlet" + number + ": " + name;}
	private String formatGroupName(String groupName) {return "Groups#" + groupName;}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void controlProperty(ControllableProperty controllableProperty) throws Exception {
		reentrantLock.lock();
		try {
			String controlProperty = controllableProperty.getProperty();
			System.out.println("controlProperty: " + controlProperty);
			String deviceId = controllableProperty.getDeviceId();
			String value = String.valueOf(controllableProperty.getValue());
			ControlObject controlObject = null;


			if (controlObject != null){
				handleControl(controlObject);
			}

		} finally {
			reentrantLock.unlock();
		}
	}

	private void handleControl(ControlObject controlObject){
		try {
			String controlResponse = this.doPost(DataprobeCommand.CONTROL,objectMapper.writeValueAsString(controlObject));
			checkForErrors(objectMapper.readTree(controlResponse));
		} catch (Exception e ){
			throw new ResourceNotReachableException("", e);
		}
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public void controlProperties(List<ControllableProperty> controllableProperties) throws Exception {
		if (CollectionUtils.isEmpty(controllableProperties)) {
			throw new IllegalArgumentException("ControllableProperties can not be null or empty");
		}
		for (ControllableProperty p : controllableProperties) {
			try {
				controlProperty(p);
			} catch (Exception e) {
				logger.error(String.format("Error when control property %s", p.getProperty()), e);
			}
		}
	}

	/**
	 * Uptime is received in seconds, need to normalize it and make it human-readable, like
	 * 1 day(s) 5 hour(s) 12 minute(s) 55 minute(s)
	 * Incoming parameter is may have a decimal point, so in order to safely process this - it's rounded first.
	 * We don't need to add a segment of time if it's 0.
	 *
	 * @param uptimeSeconds value in seconds
	 * @return string value of format 'x day(s) x hour(s) x minute(s) x minute(s)'
	 */
	private String normalizeUptime(long uptimeSeconds) {
		StringBuilder normalizedUptime = new StringBuilder();

		long seconds = uptimeSeconds % 60;
		long minutes = uptimeSeconds % 3600 / 60;
		long hours = uptimeSeconds % 86400 / 3600;
		long days = uptimeSeconds / 86400;

		if (days > 0) {
			normalizedUptime.append(days).append(" day(s) ");
		}
		if (hours > 0) {
			normalizedUptime.append(hours).append(" hour(s) ");
		}
		if (minutes > 0) {
			normalizedUptime.append(minutes).append(" minute(s) ");
		}
		if (seconds > 0) {
			normalizedUptime.append(seconds).append(" second(s)");
		}
		return normalizedUptime.toString().trim();
	}
}