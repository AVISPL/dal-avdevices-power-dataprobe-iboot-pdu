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
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
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
import com.insightsystems.symphony.dal.dataprobe.common.LoginInfo;
import com.insightsystems.symphony.dal.dataprobe.common.metric.Group;
import com.insightsystems.symphony.dal.dataprobe.common.metric.Outlet;
import com.insightsystems.symphony.dal.dataprobe.common.metric.Sequence;
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
	 * genericStatistics represent the generic statistics
	 **/
	private GenericStatistics genericStatistics = new GenericStatistics();

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
	private final List<String> groupNames = new ArrayList<>();

	/**
	 * Configurable property for sequence properties, comma separated values kept as set locally
	 */
	private Set<String> sequenceProperties = new HashSet<>();

	/**
	 * Retrieves {@link #sequenceProperties}
	 *
	 * @return value of {@link #sequenceProperties}
	 */
	public String getSequenceProperties() {
		return String.join(",", this.sequenceProperties);
	}

	/**
	 * Sets {@link #sequenceProperties} value
	 *
	 * @param sequenceProperties new value of {@link #sequenceProperties}
	 */
	public void setSequenceProperties(String sequenceProperties) {
		this.sequenceProperties.clear();
		Arrays.asList(sequenceProperties.split(",")).forEach(propertyName -> {
			this.sequenceProperties.add(propertyName.trim());
		});
	}

	/**
	 * List name of Outlets
	 */
	private final List<String> outletNames = new ArrayList<>();

	/**
	 * Enum of control type
	 */
	enum ControlType{outlet,group,sequence,unknown};

	/**
	 * Constructs a new instance of HaivisionKrakenCommunicator.
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
		try {
			Map<String, String> stats = localExtendedStatistics.getStatistics();
			List<AdvancedControllableProperty> advancedControllableProperties = localExtendedStatistics.getControllableProperties();
			String controlProperty = controllableProperty.getProperty();
			String value = String.valueOf(controllableProperty.getValue());
			ControlType controlType = getControlType(controlProperty);
			ControlObject controlObject = null;
			switch (controlType){
				case outlet:
					controlObject = handleOutletControl(controlProperty, value);
					break;
				case group:
					controlObject = handleGroupControl(controlProperty, value);
					break;
				case sequence:
					int startIndex = controlProperty.indexOf("_") + 1;
					int endIndex = controlProperty.indexOf("#");
					String sequence = controlProperty.substring(startIndex, endIndex);
					controlObject = new ControlObject(this.loginInfo.getToken(), "sequence", "run", null, sequence, null );
					break;
			}
			if (controlObject != null){
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
	 * {@inheritDoc}
	 */
	@Override
	protected void authenticate() throws Exception {
		String jsonPayload = String.format(DataprobeConstant.AUTHENTICATION_PARAM, this.getLogin(), this.getPassword());
		try {
			String result = this.doPost(DataprobeCommand.API_LOGIN, jsonPayload);
			JsonNode response = parseJson(result);
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
				retrieveName(dynamicStatistics);
				retrieveStatesByName(stats, advancedControllableProperties);

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
		return headers;
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
					String.valueOf(ClassLayout.parseInstance(this).toPrintable().length() / 1000));

			long adapterUptime = System.currentTimeMillis() - adapterInitializationTimestamp;
			stats.put(DataprobeConstant.ADAPTER_UPTIME_MIN, String.valueOf(adapterUptime / (1000 * 60)));
			stats.put(DataprobeConstant.ADAPTER_UPTIME, normalizeUptime(adapterUptime / 1000));
		} catch (Exception e) {
			logger.error("Failed to populate metadata information ", e);
		}
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
	 *
	 * @param dynamicStatistics a {@code Map<String, String>} where processed analog key-value pairs will be stored
	 * @throws Exception if the response is invalid, missing required fields, or cannot be parsed
	 */
	private void retrieveName(Map<String, String> dynamicStatistics) throws Exception {
		String jsonPayload = String.format(DataprobeConstant.RETRIEVE_NAME, this.loginInfo.getToken());
		String result = this.doPost(DataprobeCommand.RETRIEVE_INFO, jsonPayload);
		JsonNode namesResponse = parseJson(result);

		if (!namesResponse.has("names") || !namesResponse.has("analog")) {
			throw new Exception("Unable to parse names from response. 'names' field missing in response.");
		}
		populateAnalogData(namesResponse.at("/analog"), dynamicStatistics);
		retrieveGroupNames(namesResponse.at("/names"));
		retrieveOutletNames(namesResponse.at("/names"));
	}

	/**
	 * Retrieve and populate the analog data from the given JSON node and populates the provided statistics map.
	 *
	 * @param analogJson The JSON node containing analog data.
	 * @param dynamicStatistics The map to store the processed analog data
	 */
	private void populateAnalogData(JsonNode analogJson, Map<String, String> dynamicStatistics) {
		if (analogJson != null && analogJson.isObject()) {
			analogJson.fieldNames().forEachRemaining(key -> {
				String value = analogJson.get(key).asText();
				String unit = getUnitForKey(key);
				dynamicStatistics.put(key + unit, value);
			});
		}
	}

	/**
	 * Retrieves and processes the group names from the given JSON node.
	 *
	 * @param namesJson The JSON node containing the "groupNames" data.
	 * @throws Exception If the "groupNames" field is missing in the provided JSON node.
	 */
	private void retrieveGroupNames(JsonNode namesJson) throws Exception {
		if (namesJson.has("groupNames")) {
			JsonNode groups = namesJson.at("/groupNames");
			groupNames.clear();
			for (Iterator<String> it = groups.fieldNames(); it.hasNext(); ) {
				String key = it.next();
				groupNames.add(groups.at("/" + key).asText());
			}
		} else {
			throw new Exception("Unable to parse names from response. 'groupNames' fields missing.");
		}
	}

	/**
	 * Retrieves and processes the outlet names from the given JSON node.
	 *
	 * @param namesJson The JSON node containing the "outletNames" data.
	 * @throws Exception If the "outletNames" field is missing in the provided JSON node.
	 */
	private void retrieveOutletNames(JsonNode namesJson) throws Exception {
		if (namesJson.has("outletNames")) {
			JsonNode outlets = namesJson.at("/outletNames");
			outletNames.clear();
			for (Iterator<String> it = outlets.fieldNames(); it.hasNext(); ) {
				String key = it.next();
				String name = outlets.get(key).asText();
				outletNames.add(key + " " + name);
			}
		} else {
			throw new Exception("Unable to parse names from response. 'outletNames' fields missing.");
		}
	}

	/**
	 * Retrieves and processes the states of outlets and groups from a remote API response.
	 *
	 * @param stats a {@code Map<String, String>} where the states of outlets, groups, and sequences will be stored
	 * @param controls a {@code List<AdvancedControllableProperty>} where controls for outlets, groups, and sequences will be added
	 * @throws Exception if the response contains errors, required fields are missing, or parsing fails
	 */
	private void retrieveStatesByName(Map<String, String> stats, List<AdvancedControllableProperty> controls) throws Exception {
		String result = this.doPost(DataprobeCommand.RETRIEVE_INFO, createJsonRetrieveString());
		JsonNode stateResponse = parseJson(result);
		checkForErrors(stateResponse);

		populateOutletStates(stateResponse, stats, controls);
		populateGroupStates(stateResponse, stats, controls);
		populateSequenceStates(stats, controls);
		}

	/**
	 * Populates the states of outlets from the provided JSON response and updates the stats and controls.
	 *
	 * @param stateResponse containing the API response with outlet states.
	 * @param stats to store the state of each outlet.
	 * @param controls to store control properties for each outlet.
	 * @throws Exception If the 'outlets' field is missing in the response.
	 */
	private void populateOutletStates(JsonNode stateResponse, Map<String, String> stats, List<AdvancedControllableProperty> controls) throws Exception {
		if (stateResponse.has("outlets")) {
			JsonNode outletStates = stateResponse.at(DataprobeConstant.RESPONSE_OUTLETS);
			int outletNumber = 1;
			for (Iterator<String> it = outletStates.fieldNames(); it.hasNext(); ) {
				String fieldName = it.next();
				createOutletStats(outletNumber, fieldName, outletStates.at("/" + fieldName).asText(), stats, controls);
				outletNumber++;
			}
		} else {
			throw new Exception("Unable to parse outlet states. 'outlets' field not found.");
		}
	}

	/**
	 * Populates the states of groups from the provided JSON response and updates the stats and controls.
	 *
	 * @param stateResponse containing the API response with group states.
	 * @param stats to store the state of each group.
	 * @param controls to store control properties for each group.
	 * @throws Exception If the 'groups' field is missing in the response.
	 */
	private void populateGroupStates(JsonNode stateResponse, Map<String, String> stats, List<AdvancedControllableProperty> controls) throws Exception {
		if (stateResponse.has("groups")) {
			JsonNode groupStates = stateResponse.at(DataprobeConstant.RESPONSE_GROUPS);
			for (Iterator<String> it = groupStates.fieldNames(); it.hasNext(); ) {
				String fieldName = it.next();
				createGroupStats(fieldName, groupStates.at("/" + fieldName).asText(), stats, controls);
			}
		} else {
			throw new Exception("Unable to parse group states. 'groups' field not found.");
		}
	}

	/**
	 * Populates the states of sequences and updates the stats and controls.
	 *
	 * @param stats to store the state of each sequence.
	 * @param controls to store control properties for each sequence.
	 */
	private void populateSequenceStates(Map<String, String> stats, List<AdvancedControllableProperty> controls) {
		if (!sequenceProperties.isEmpty()) {
			for (String sequenceName : sequenceProperties) {
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
									createButton(sequence, "Run", "Running", 5),
									DataprobeConstant.NONE
							);
							break;
						default:
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
					stats.put(propertyName, state);
					break;
				case CYCLE:
					addAdvancedControlProperties(controls, stats, createButton(propertyName, DataprobeConstant.CYCLE, DataprobeConstant.CYCLING, 5), DataprobeConstant.NONE);
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
					if ("?".equals(state)) {
						stats.put(propertyName, "No outlets in group");
					} else {
						stats.put(propertyName, state);
					}
					break;
				case OUTLET_CONTROL:
					int status = DataprobeConstant.ON.equalsIgnoreCase(state) ? 1 : 0;
					addAdvancedControlProperties(controls, stats, createSwitch(propertyName, status, DataprobeConstant.OFF, DataprobeConstant.ON), String.valueOf(status));
					stats.put(propertyName, state);
					break;
				case CYCLE:
					addAdvancedControlProperties(controls, stats, createButton(propertyName, DataprobeConstant.CYCLE, DataprobeConstant.CYCLING, 5), DataprobeConstant.NONE);
					break;
				default:
					stats.put(propertyName, state);
					break;
			}
		}
	}

	/**
	 * Creates a JSON string representation of the state request object.
	 * @return a JSON string representing the state request, which includes the token, outlet names, and group names
	 * @throws JsonProcessingException if an error occurs during JSON serialization
	 */
	private String createJsonRetrieveString() throws JsonProcessingException {
		Serialisers.StateRequestObject stateRequestObject = new Serialisers.StateRequestObject(
				this.loginInfo.getToken(),
				outletNames.toArray(new String[0]),
				groupNames.toArray(new String[0])
		);
		return objectMapper.writeValueAsString(stateRequestObject);
	}

	/**
	 * Parses the given JSON string and converts it into a JsonNode object.
	 *
	 * @param json The JSON string to be parsed.
	 * @return A JsonNode representing the parsed JSON structure.
	 */
	private JsonNode parseJson(String json) throws Exception {
		return objectMapper.readTree(json);
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

			String propertyValue = StringUtils.isNotNullOrEmpty(value) ? value : DataprobeConstant.EMPTY;
			stats.put(property.getName(), propertyValue);
			advancedControllableProperties.add(property);
		}
	}

	/**
	 * Checks the device response for errors and throws an exception if any error is detected.
	 * @param deviceResponse the JSON response from the device to be checked for errors
	 * @throws ResourceNotReachableException if the response indicates failure and the error message is unexpected
	 * @throws Exception if an unexpected error occurs during error checking
	 */
	private void checkForErrors(JsonNode deviceResponse) throws Exception {
		if (!deviceResponse.at(DataprobeConstant.RESPONSE_SUCCESS).asBoolean()) {
			if (!deviceResponse.at(DataprobeConstant.RESPONSE_MESSAGE).asText().contains("There are no Groups")) {
				throw new ResourceNotReachableException(deviceResponse.at(DataprobeConstant.RESPONSE_MESSAGE).asText());
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
	 * Determines the unit to be appended to a given key based on specific patterns.
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
	 * capitalize the first character of the string
	 *
	 * @param input input string
	 * @return string after fix
	 */
	private String uppercaseFirstCharacter(String input) {
		char firstChar = input.charAt(0);
		return Character.toUpperCase(firstChar) + input.substring(1);
	}

	/**
	 * Handles the group control operation based on the given control property and value.
	 * @param controlProperty the control property string, containing the group information and action
	 * @param value the control value indicating the desired state (e.g., "1" for on, "0" for off)
	 * @return a {@link ControlObject} representing the group control operation to be executed
	 */
	private ControlObject handleGroupControl(String controlProperty, String value) {
		int startIndex = controlProperty.indexOf("_") + 1;
		int endIndex = controlProperty.indexOf("#");
		String group = controlProperty.substring(startIndex, endIndex);
		boolean state = value.equals("1");

		if (controlProperty.contains(DataprobeConstant.CYCLE)) {
				return new ControlObject(this.loginInfo.getToken(), "group", "cycle", null, null, group);
		}
		return new ControlObject(this.loginInfo.getToken(),"group",state ? "on":"off",null,null, group);
	}

	/**
	 * Handles the outlet control operation based on the given control property and value.
	 * @param controlProperty the control property string, containing the outlet information and action
	 * @param value the control value indicating the desired state (e.g., "1" for on, "0" for off)
	 * @return a {@link ControlObject} representing the outlet control operation to be executed
	 */
	private ControlObject handleOutletControl(String controlProperty, String value) {
		int startIndex = controlProperty.indexOf("_") + 1;
		int endIndex = controlProperty.indexOf(" ");
		String outlet = controlProperty.substring(startIndex, endIndex);

		if (controlProperty.contains(DataprobeConstant.CYCLE)) {
			return new ControlObject(this.loginInfo.getToken(), "outlet", "cycle", new String[]{outlet}, null, null);
		}
		boolean state = value.equals("1");
		return new ControlObject(this.loginInfo.getToken(), "outlet", state ? "on" : "off", new String[]{outlet}, null, null
		);
	}

	/**
	 * Sends a command to control a device and processes the response.
	 * @param controlObject the {@link ControlObject} that represents the command to control the device
	 * @throws ResourceNotReachableException if there is an error in communication with the device or if the device fails to process the command
	 */
	private void sendCommandToControlDevice(ControlObject controlObject) {
		try{
			String controlResponse = this.doPost(DataprobeCommand.CONTROL,objectMapper.writeValueAsString(controlObject));
			JsonNode deviceResponse = parseJson(controlResponse);
			if (!deviceResponse.at(DataprobeConstant.RESPONSE_SUCCESS).asBoolean()) {
				if (!deviceResponse.at(DataprobeConstant.RESPONSE_MESSAGE).asText().contains("There are no data")) {
					throw new ResourceNotReachableException(deviceResponse.at(DataprobeConstant.RESPONSE_MESSAGE).asText());
				}
			}
		} catch (Exception e) {
			throw new ResourceNotReachableException("Can not control device %s", e);
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
	 * Determines the type of control based on the provided control property string.
	 * @param controlProperty the control property string to be evaluated
	 * @return the {@link ControlType} corresponding to the control property
	 */
	private ControlType getControlType(String controlProperty) {
		if (controlProperty.startsWith(DataprobeConstant.OUTLET))
			return ControlType.outlet;

		if (controlProperty.startsWith(DataprobeConstant.GROUP))
			return ControlType.group;

		if (controlProperty.startsWith(DataprobeConstant.SEQUENCE))
			return ControlType.sequence;

		return ControlType.unknown;
	}

	/**
	 * Formats the outlet name by appending a number to the outlet constant.
	 * @param number the number to be appended to the outlet constant
	 * @return the formatted outlet name as a string, e.g., "OUTLET1" if number is 1
	 */
	private String formatOutletName(int number) {
		return DataprobeConstant.OUTLET + number ;
	}

	/**
	 * Formats the group name.
	 * @param groupName the number to be appended to the group constant
	 * @return the formatted group name as a string
	 */
	private String formatGroupName(String groupName) {
		return DataprobeConstant.GROUP + groupName;
	}

	/**
	 * Formats the sequence.
	 * @param sequenceName the sequence to be typed by user
	 * @return the formatted sequence as a string
	 */
	private String formatSequenceName(String sequenceName) {
		return DataprobeConstant.SEQUENCE + sequenceName;
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