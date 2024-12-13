/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */
package com.insightsystems.symphony.dal.dataprobe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;

import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.insightsystems.symphony.dal.dataprobe.Serialisers.ControlObject;
import com.insightsystems.symphony.dal.dataprobe.common.DataprobeCommand;
import com.insightsystems.symphony.dal.dataprobe.common.DataprobeConstant;
import com.insightsystems.symphony.dal.dataprobe.common.LoginInfo;
import com.insightsystems.symphony.dal.dataprobe.common.metric.DataprobeControlType;
import com.insightsystems.symphony.dal.dataprobe.common.metric.Group;
import com.insightsystems.symphony.dal.dataprobe.common.metric.Outlet;
import com.insightsystems.symphony.dal.dataprobe.common.metric.Sequence;
import javax.security.auth.login.FailedLoginException;
import org.apache.http.auth.InvalidCredentialsException;

import com.avispl.symphony.api.dal.control.Controller;
import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.error.ResourceNotReachableException;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.dal.communicator.RestCommunicator;
import com.avispl.symphony.dal.util.StringUtils;

/**
 * /*
 * An implementation of DataprobeiBootPDUCommunicator to provide communication and interaction with Dataprobe iboot PDU
 * Supported features are:
 * <p>
 * Monitoring:
 * <li>Outlet 1-N</li>
 * <li>Name</li>
 * <li>Status</li>
 * <li>Control</li>
 * <li>Cycle</li>
 *
 * <li>Group 1-N</li>
 * <li>Name</li>
 * <li>Status</li>
 * <li>Control</li>
 * <li>Cycle</li>
 *
 * <li>Sequence_name</li>
 * <li>Name</li>
 * <li>Control</li>
 * <p>
 * Controlling:
 * <li>On/Off/Cycle Outlets and Group config</li>
 * <li>Run Sequence config</li>
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 11/24/2024
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

	/**
	 * Adapter metadata properties - adapter version and build date
	 */
	private Properties adapterProperties;

	/**
	 * isEmergencyDelivery to check if control flow is trigger
	 */
	private boolean isEmergencyDelivery;

	/**
	 * A mapper for reading and writing JSON using Jackson library.
	 * ObjectMapper provides functionality for converting between Java objects and JSON.
	 * It can be used to serialize objects to JSON format, and deserialize JSON data to objects.
	 */
	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * the login info
	 */
	private LoginInfo loginInfo;

	/**
	 * List name of Groups
	 */
	private final Map<String, String> groupNames = new HashMap<>();

	/**
	 * List name of Outlets
	 */
	private final Map<String, String> outletNames = new HashMap<>();

	/**
	 * List state of Outlets
	 */
	private final Map<String, String> outletStates = new HashMap<>();

	/**
	 * List state of Group
	 */
	private final Map<String, String> groupStates = new HashMap<>();

	/**
	 * List name of analog
	 */
	private final Map<String, String> analogProperty = new HashMap<>();

	/**
	 * Configurable property for historical properties, comma separated values kept as set locally
	 */
	private Set<String> historicalProperties = new HashSet<>();

	/**
	 * Retrieves {@link #historicalProperties}
	 *
	 * @return value of {@link #historicalProperties}
	 */
	public String getHistoricalProperties() {
		return String.join(",", this.historicalProperties);
	}

	/**
	 * Sets {@link #historicalProperties} value
	 *
	 * @param historicalProperties new value of {@link #historicalProperties}
	 */
	public void setHistoricalProperties(String historicalProperties) {
		this.historicalProperties.clear();
		if (StringUtils.isNotNullOrEmpty(historicalProperties)) {
			Arrays.asList(historicalProperties.split(",")).forEach(propertyName -> {
				this.historicalProperties.add(propertyName.trim());
			});
		}
	}

	/**
	 * Configurable property for sequence properties, comma separated values kept as set locally
	 */
	private Set<String> sequenceNames = new HashSet<>();

	/**
	 * Retrieves {@link #sequenceNames}
	 *
	 * @return value of {@link #sequenceNames}
	 */
	public String getSequenceNames() {
		return String.join(",", this.sequenceNames);
	}

	/**
	 * Sets {@link #sequenceNames} value
	 *
	 * @param sequenceNames new value of {@link #sequenceNames}
	 */
	public void setSequenceNames(String sequenceNames) {
		this.sequenceNames.clear();
		if (StringUtils.isNotNullOrEmpty(sequenceNames)) {
			Arrays.asList(sequenceNames.split(",")).forEach(propertyName -> {
				this.sequenceNames.add(propertyName.trim());
			});
		}
	}

	/**
	 * Constructs a new instance of DataprobeiBootPDUCommunicator.
	 */
	public DataprobeiBootPDUCommunicator() throws IOException {
		adapterProperties = new Properties();
		adapterProperties.load(getClass().getResourceAsStream("/version.properties"));
		this.setTrustAllCertificates(true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void controlProperty(ControllableProperty controllableProperty) throws Exception {
		reentrantLock.lock();
		if (localExtendedStatistics == null) {
			return;
		}
		try {
			Map<String, String> stats = localExtendedStatistics.getStatistics();
			List<AdvancedControllableProperty> advancedControllableProperties = localExtendedStatistics.getControllableProperties();
			String controlProperty = controllableProperty.getProperty();
			String value = String.valueOf(controllableProperty.getValue());
			DataprobeControlType controlType = getByDefaultName(controlProperty);
			ControlObject controlObject = null;
			switch (controlType) {
				case OUTLET:
					controlObject = handleOutletAndGroupControl(controlProperty, value, DataprobeConstant.OUTLET_COMMAND);
					break;
				case GROUP:
					controlObject = handleOutletAndGroupControl(controlProperty, value, DataprobeConstant.GROUP_COMMAND);
					break;
				case SEQUENCE:
					controlObject = handleSequenceControl(controlProperty);
					break;
			}
			if (controlObject != null) {
				sendCommandToControlDevice(controlObject);
				updateValueForTheControllableProperty(controlProperty, value, stats, advancedControllableProperties);
			}
		} finally {
			reentrantLock.unlock();
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
	 * Authenticates the user by sending a login request and retrieves the token.
	 *
	 * @throws Exception if an error occurs during the login process.
	 * {@inheritDoc}
	 */
	@Override
	protected void authenticate() throws Exception {
		String jsonPayload = String.format(DataprobeConstant.AUTHENTICATION_PARAM, this.getLogin(), this.getPassword());
		try {
			String result = this.doPost(DataprobeCommand.API_LOGIN, jsonPayload);
			JsonNode response = objectMapper.readTree(result);
			if (response.has("success")) {
				if (response.at(DataprobeConstant.RESPONSE_SUCCESS).asBoolean()) {
					String token = response.at("/token").asText();
					if (loginInfo == null) {
						loginInfo = new LoginInfo();
					}
					loginInfo.setToken(token);
				} else {
					loginInfo = null;
					throw new InvalidCredentialsException(response.at(DataprobeConstant.RESPONSE_MESSAGE).asText());
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
	public List<Statistics> getMultipleStatistics() throws Exception {
		reentrantLock.lock();
		try {
			if (loginInfo == null) {
				loginInfo = new LoginInfo();
			}
			checkValidApiToken();
			Map<String, String> stats = new HashMap<>();
			Map<String, String> dynamicStatistics = new HashMap<>();
			ExtendedStatistics extendedStatistics = new ExtendedStatistics();
			List<AdvancedControllableProperty> advancedControllableProperties = new ArrayList<>();

			if (!isEmergencyDelivery) {
				retrieveMonitoringData();
				retrieveControllingState();

				populateOutletName(stats, advancedControllableProperties);
				populateGroupName(stats, advancedControllableProperties);
				populateSequenceStates(stats, advancedControllableProperties);
				populateAnalogData(stats, dynamicStatistics);

				extendedStatistics.setStatistics(stats);
				extendedStatistics.setDynamicStatistics(dynamicStatistics);
				extendedStatistics.setControllableProperties(advancedControllableProperties);
				localExtendedStatistics = extendedStatistics;
			}
			isEmergencyDelivery = false;
		} finally {
			reentrantLock.unlock();
		}
		return Collections.singletonList(localExtendedStatistics);
	}

	/**
	 * Populate group name
	 *
	 * @param stats to store group name
	 * @param advancedControllableProperties to handle control for group name
	 */
	private void populateGroupName(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties) {
		for (Entry<String, String> item : groupNames.entrySet()) {
			String value = item.getValue();
			createGroupStats(value, groupStates.get(value), stats, advancedControllableProperties);
		}
	}

	/**
	 * Populate outlet name
	 *
	 * @param stats to store outlet name
	 * @param advancedControllableProperties to handle control for outlet name
	 */
	private void populateOutletName(Map<String, String> stats, List<AdvancedControllableProperty> advancedControllableProperties) {
		for (Entry<String, String> item : outletNames.entrySet()) {
			String key = item.getKey();
			String value = item.getValue();
			createOutletStats(Integer.parseInt(key), value, outletStates.get(key), stats, advancedControllableProperties);
		}
	}

	/**
	 * Populate analog data
	 * @param stats store analog to display UI
	 * @param dynamicStatistics store analog to db
	 */
	private void populateAnalogData(Map<String, String> stats, Map<String, String> dynamicStatistics) {
		for (Entry<String, String> item : analogProperty.entrySet()) {
			boolean isCurrentCase = item.getKey().contains(DataprobeConstant.CURRENT);
			double currentValue = Double.parseDouble(item.getValue());
			String key = item.getKey();
			String unit = getUnitForKey(key);
			String value = isCurrentCase ? String.valueOf(currentValue * 1000) : item.getValue();
			String getCustomKey;
			if (DataprobeConstant.T0.equals(key)) {
				getCustomKey = DataprobeConstant.TEMPERATURE + unit;
			} else if (key.contains(DataprobeConstant.T1)) {
				getCustomKey = DataprobeConstant.TEMPERATURE_T1_PROBE + unit;
			} else {
				getCustomKey = key + unit;
			}
			boolean isHistorical = historicalProperties.contains(getCustomKey);
			if(currentValue < DataprobeConstant.MAXIMUM_CURRENT_VALUE){
				if (isHistorical) {
					dynamicStatistics.put(getCustomKey, value);
					continue;
				}
				stats.put(getCustomKey, value);
			}
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
		super.internalDestroy();
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

	/**
	 * Retrieves and processes device names and analog data from a remote API response.
	 */
	private void retrieveMonitoringData() {
		try {
			String jsonPayload = String.format(DataprobeConstant.RETRIEVE_NAME, this.loginInfo.getToken());
			String result = this.doPost(DataprobeCommand.RETRIEVE_INFO, jsonPayload);
			JsonNode namesResponse = objectMapper.readTree(result);

			if (!namesResponse.has("names") || !namesResponse.has("analog")) {
				throw new Exception("Unable to parse names from response. 'names' field missing in response.");
			}
			handleGetDataByResponse(namesResponse.at("/names"), "outletNames", outletNames);
			handleGetDataByResponse(namesResponse.at("/names"), "groupNames", groupNames);
			handleGroupGetDataByResponse(namesResponse.at("/analog"), analogProperty);
		} catch (Exception e) {
			throw new ResourceNotReachableException("Unable to retrieve names from response.", e);
		}
	}

	/**
	 * Retrieves and processes the group names from the given JSON node.
	 * @param namesJson The JSON node containing the "groupNames" data.param namesJson
	 * @param groupName The group name of property
	 * @param listName  The list name to store property
	 */
	private void handleGetDataByResponse(JsonNode namesJson, String groupName, Map<String, String> listName) {
		if (namesJson.has(groupName)) {
			JsonNode outletsNode = namesJson.path(groupName);
			listName.clear();
			if(outletsNode != null) {
				Iterator<Map.Entry<String, JsonNode>> fields = outletsNode.fields();
				while (fields.hasNext()) {
					Map.Entry<String, JsonNode> field = fields.next();
					listName.put(field.getKey(), field.getValue().asText());
				}
			}
		} else {
			throw new ResourceNotReachableException("Unable to parse names from response. 'groupNames' fields missing.");
		}
	}

	/**
	 * Retrieves and processes the group names from the given JSON node.
	 * @param namesJson the Json return data
	 * @param mapDataResponse the store to map data from response
	 */
	private void handleGroupGetDataByResponse(JsonNode namesJson, Map<String, String> mapDataResponse) {
		mapDataResponse.clear();
		if(namesJson != null) {
			Iterator<Map.Entry<String, JsonNode>> fields = namesJson.fields();
			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> field = fields.next();
				mapDataResponse.put(field.getKey(), field.getValue().asText());
			}
		}
	}

	/**
	 * Retrieves and processes the states of outlets and groups from a remote API response.
	 */
	private void retrieveControllingState() {
		try {
			String result = this.doPost(DataprobeCommand.RETRIEVE_INFO, createJsonRetrieveString());
			JsonNode stateResponse = objectMapper.readTree(result);
			if (!stateResponse.at(DataprobeConstant.RESPONSE_SUCCESS).asBoolean() && !stateResponse.at(DataprobeConstant.RESPONSE_MESSAGE).asText().contains("There are no Groups")) {
				throw new ResourceNotReachableException(stateResponse.at(DataprobeConstant.RESPONSE_MESSAGE).asText());
			}
			handleGroupGetDataByResponse(stateResponse.at("/outlets"), outletStates);
			handleGroupGetDataByResponse(stateResponse.at("/groups"), groupStates);
		} catch (Exception e) {
			throw new ResourceNotReachableException("Can not retrieve the information of the device", e);
		}
	}

	/**
	 * Populates the states of sequences and updates the stats and controls.
	 *
	 * @param stats to store the state of each sequence.
	 * @param controls to store control properties for each sequence.
	 */
	private void populateSequenceStates(Map<String, String> stats, List<AdvancedControllableProperty> controls) {
		if (!sequenceNames.isEmpty()) {
			for (String sequenceName : sequenceNames) {
				for (Sequence item : Sequence.values()) {
					String sequence = formatSequenceName(sequenceName) + DataprobeConstant.HASH + item.getPropertyName();
					switch (item) {
						case NAME:
							stats.put(sequence, sequenceName);
							break;
						case CONTROL:
							addAdvancedControlProperties(
									controls,
									stats,
									createButton(sequence, "Run", "Running", 0),
									DataprobeConstant.NONE
							);
							break;
//						case STOP:
//							addAdvancedControlProperties(
//									controls,
//									stats,
//									createButton(sequence, "Stop", "Stopping", 0),
//									DataprobeConstant.NONE
//							);
//							break;
						default:
							logger.debug(String.format("The Adaptor is not support this property %s", item.getPropertyName()));
							break;
					}
				}
			}
		}
	}

	/**
	 * Creates and updates the statistics and controls for a specific outlet based on its properties.
	 *
	 * @param number the outlet number used to format the property name
	 * @param name the raw name of the outlet, used to extract the formatted outlet name
	 * @param state the current state of the outlet (e.g., "On", "Off")
	 * @param stats a {@code Map<String, String>} where outlet-specific stats will be stored
	 * @param controls a {@code List<AdvancedControllableProperty>} where controls for the outlet will be added
	 */
	private void createOutletStats(int number, String name, String state, Map<String, String> stats, List<AdvancedControllableProperty> controls) {
		for (Outlet item : Outlet.values()) {
			String propertyName = formatOutletName(number) + DataprobeConstant.HASH + item.getPropertyName();
			switch (item) {
				case NAME:
					String result = name.substring(name.indexOf(" ") + 1);
					stats.put(propertyName, result);
					break;
				case OUTLET_CONTROL:
					int status = DataprobeConstant.ON.equalsIgnoreCase(state) ? 1 : 0;
					addAdvancedControlProperties(controls, stats,
							createSwitch(propertyName, status, DataprobeConstant.OFF, DataprobeConstant.ON),
							String.valueOf(status));
					break;
				case CYCLE:
					addAdvancedControlProperties(controls, stats, createButton(propertyName, DataprobeConstant.CYCLE, DataprobeConstant.CYCLING, 0), DataprobeConstant.NONE);
					break;
				default:
					stats.put(propertyName, state);
					break;
			}
		}
	}

	/**
	 * Creates and updates the statistics and controls for a specific group based on its properties.
	 *
	 * @param name the raw name of the group, used to extract the formatted group name
	 * @param state the current state of the group (e.g., "On", "Off")
	 * @param stats a {@code Map<String, String>} where group-specific stats will be stored
	 * @param controls a {@code List<AdvancedControllableProperty>} where controls for the group will be added
	 */
	private void createGroupStats(String name, String state, Map<String, String> stats, List<AdvancedControllableProperty> controls) {
		for (Group item : Group.values()) {
			String propertyName = formatGroupName(name) + DataprobeConstant.HASH + item.getPropertyName();
			switch (item) {
				case NAME:
					stats.put(propertyName, name);
					break;
				case STATUS:
					stats.put(propertyName, "?".equals(state) ? "No outlets in group" : state);
					break;
				case OUTLET_CONTROL:
					int status = DataprobeConstant.ON.equalsIgnoreCase(state) ? 1 : 0;
					addAdvancedControlProperties(controls, stats, createSwitch(propertyName, status, DataprobeConstant.OFF, DataprobeConstant.ON), String.valueOf(status));
					break;
				case CYCLE:
					addAdvancedControlProperties(controls, stats, createButton(propertyName, DataprobeConstant.CYCLE, DataprobeConstant.CYCLING, 0), DataprobeConstant.NONE);
					break;
				default:
					stats.put(propertyName, state);
					break;
			}
		}
	}

	/**
	 * Creates a JSON string representation of the state request object.
	 *
	 * @return a JSON string representing the state request, which includes the token, outlet names, and group names
	 * @throws JsonProcessingException if an error occurs during JSON serialization
	 */
	private String createJsonRetrieveString() throws JsonProcessingException {
		Serialisers.StateRequestObject stateRequestObject = new Serialisers.StateRequestObject(
				this.loginInfo.getToken(),
				outletNames.keySet().toArray(new String[0]),
				groupNames.values().toArray(new String[0])
		);
		return objectMapper.writeValueAsString(stateRequestObject);
	}

	/**
	 * Create switch is control property for metric
	 *
	 * @param name the name of property
	 * @param status initial status (0|1)
	 * @return AdvancedControllableProperty switch instance
	 */
	private AdvancedControllableProperty createSwitch(String name, int status, String labelOff, String labelOn) {
		AdvancedControllableProperty.Switch toggle = new AdvancedControllableProperty.Switch();
		toggle.setLabelOff(labelOff);
		toggle.setLabelOn(labelOn);

		AdvancedControllableProperty advancedControllableProperty = new AdvancedControllableProperty();
		advancedControllableProperty.setName(name);
		advancedControllableProperty.setValue(status);
		advancedControllableProperty.setType(toggle);
		advancedControllableProperty.setTimestamp(new Date());

		return advancedControllableProperty;
	}

	/**
	 * Create a button.
	 *
	 * @param name name of the button
	 * @param label label of the button
	 * @param labelPressed label of the button after pressing it
	 * @param gracePeriod grace period of button
	 * @return This returns the instance of {@link AdvancedControllableProperty} type Button.
	 */
	private AdvancedControllableProperty createButton(String name, String label, String labelPressed, long gracePeriod) {
		AdvancedControllableProperty.Button button = new AdvancedControllableProperty.Button();
		button.setLabel(label);
		button.setLabelPressed(labelPressed);
		button.setGracePeriod(gracePeriod);
		return new AdvancedControllableProperty(name, new Date(), button, DataprobeConstant.EMPTY);
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

			String propertyValue = StringUtils.isNotNullOrEmpty(value) && !DataprobeConstant.NONE.equals(value) ? value : DataprobeConstant.EMPTY;
			stats.put(property.getName(), propertyValue);
			advancedControllableProperties.add(property);
		}
	}

	/**
	 * Determines the unit to be appended to a given key based on specific patterns.
	 *
	 * @param key the key to analyze for unit assignment
	 * @return a string representing the unit (e.g., "(mA)", "(V)", "(C)") or an empty string if no unit matches
	 */
	private String getUnitForKey(String key) {
		if (key.contains(DataprobeConstant.CURRENT)) {
			return "(mA)";
		} else if (key.contains(DataprobeConstant.VOLTAGE)) {
			return "(V)";
		} else if (key.contains(DataprobeConstant.T0) || key.contains(DataprobeConstant.T1)) {
			return "(C)";
		}
		return "";
	}

	/**
	 * Retrieves the Sequence enum based on its default name.
	 *
	 * @param name The default name of the control type enum.
	 * @return The control type enum corresponding to the default name, or unknown if not found.
	 */
	public static DataprobeControlType getByDefaultName(String name) {
		for (DataprobeControlType dataprobeControlType : DataprobeControlType.values()) {
			if (name.toLowerCase().startsWith(dataprobeControlType.getName())) {
				return dataprobeControlType;
			}
		}
		return null;
	}

	/**
	 * Handles the sequence control base on the name of sequence provided by user
	 *
	 * @param controlProperty the control property string, containing the group information and action
	 */
	private ControlObject handleSequenceControl(String controlProperty) {
		try {
			int startIndex = controlProperty.indexOf(DataprobeConstant.UNDER_SCORE);
			int endIndex = controlProperty.indexOf(DataprobeConstant.HASH);
			String sequence = controlProperty.substring(startIndex + 1, endIndex);
			String command = "run";
//			if (controlProperty.contains(DataprobeConstant.STOP)) {
//				command = "stop";
//			}
			return new ControlObject(this.loginInfo.getToken(), "sequence", command, null, sequence, null);
		} catch (Exception e) {
			throw new ResourceNotReachableException("Can not control this sequence", e);
		}
	}

	/**
	 * Handles the group control operation based on the given control property and value.
	 *
	 * @param controlProperty the control property string, containing the group information and action
	 * @param value the control value indicating the desired state (e.g., "1" for on, "0" for off)
	 * @param groupName the control groupName indicating the group control
	 * @return a {@link ControlObject} representing the group control operation to be executed
	 */
	private ControlObject handleOutletAndGroupControl(String controlProperty, String value, String groupName) {
		int startIndex = controlProperty.indexOf(DataprobeConstant.UNDER_SCORE);
		int endIndex = controlProperty.indexOf(DataprobeConstant.HASH);
		String group = controlProperty.substring(startIndex + 1, endIndex);
		String command = "1".equals(value) ? "on" : "off";
		if (controlProperty.contains(DataprobeConstant.CYCLE)) {
			command = "cycle";
		}
		return new ControlObject(this.loginInfo.getToken(), groupName, command, null, null, group);
	}

	/**
	 * Sends a command to control a device and processes the response.
	 *
	 * @param controlObject the {@link ControlObject} that represents the command to control the device
	 * @throws ResourceNotReachableException if there is an error in communication with the device or if the device fails to process the command
	 */
	private void sendCommandToControlDevice(ControlObject controlObject) {
		try {
			String controlResponse = this.doPost(DataprobeCommand.CONTROL, objectMapper.writeValueAsString(controlObject));
			JsonNode deviceResponse = objectMapper.readTree(controlResponse);
			if (!deviceResponse.at(DataprobeConstant.RESPONSE_SUCCESS).asBoolean()
					&& !deviceResponse.at(DataprobeConstant.RESPONSE_MESSAGE).asText().contains("There are no data")) {
				throw new ResourceNotReachableException(deviceResponse.at(DataprobeConstant.RESPONSE_MESSAGE).asText());
			}
		} catch (Exception e) {
			throw new ResourceNotReachableException("Can not control device", e);
		}
	}

	/**
	 * Update the value for the control metric
	 *
	 * @param property is name of the metric
	 * @param value the value is value of properties
	 * @param extendedStatistics list statistics property
	 * @param advancedControllableProperties the advancedControllableProperties is list AdvancedControllableProperties
	 */
	private void updateValueForTheControllableProperty(String property, String value, Map<String, String> extendedStatistics, List<AdvancedControllableProperty> advancedControllableProperties) {
		for (AdvancedControllableProperty advancedControllableProperty : advancedControllableProperties) {
			if (advancedControllableProperty.getName().equals(property)) {
				extendedStatistics.put(property, value);
				advancedControllableProperty.setValue(value);
				break;
			}
		}
	}

	/**
	 * Formats the outlet name by appending a number to the outlet constant.
	 *
	 * @param number the number to be appended to the outlet constant
	 * @return the formatted outlet name as a string, e.g., "OUTLET1" if number is 1
	 */
	private String formatOutletName(int number) {
		return DataprobeConstant.OUTLET + number;
	}

	/**
	 * Formats the group name.
	 *
	 * @param groupName the number to be appended to the group constant
	 * @return the formatted group name as a string
	 */
	private String formatGroupName(String groupName) {
		return DataprobeConstant.GROUP + groupName;
	}

	/**
	 * Formats the sequence.
	 *
	 * @param sequenceName the sequence to be typed by user
	 * @return the formatted sequence as a string
	 */
	private String formatSequenceName(String sequenceName) {
		return DataprobeConstant.SEQUENCE + sequenceName;
	}
}