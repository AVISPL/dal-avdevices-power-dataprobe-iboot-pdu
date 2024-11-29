//package com.insightsystems.symphony.dal.dataprobe;
//
//import com.avispl.symphony.api.dal.control.Controller;
//import com.avispl.symphony.api.dal.dto.control.AdvancedControllableProperty;
//import com.avispl.symphony.api.dal.dto.control.ControllableProperty;
//import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
//import com.avispl.symphony.api.dal.dto.monitor.Statistics;
//import com.avispl.symphony.api.dal.monitor.Monitorable;
//import com.avispl.symphony.dal.communicator.RestCommunicator;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.insightsystems.symphony.dal.dataprobe.Serialisers.ControlObject;
//import org.apache.http.auth.InvalidCredentialsException;
//
//import java.util.*;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//public class iBootPDU extends RestCommunicator implements Monitorable, Controller {
//    enum ControlType{outlet,group,sequence,allOutlets,unknown}
//    private final ObjectMapper objectMapper = new ObjectMapper();
//    private final List<String> groupNames = new ArrayList<>();
//    private final List<String> outletNames = new ArrayList<>();
//    private final Pattern saltPattern = Pattern.compile("jsEncryptPassword\\('([\\w\\d]+)'");
//    private String authToken;
//    private String salt;
//
//    public iBootPDU(){
//        this.setAuthenticationScheme(AuthenticationScheme.None);
//    }
//
//    @Override
//    protected void authenticate() throws Exception {
//        JsonNode authResponse = objectMapper.readTree(this.doPost("/services/auth/","{\"username\":\""+this.getLogin()+"\",\"password\":\""+ this.getPassword() +"\"}"));
//        if (authResponse.has("success")){
//            if (authResponse.at("/success").asBoolean()){
//                authToken = authResponse.at("/token").asText();
//            } else {
//                throw new InvalidCredentialsException(authResponse.at("/message").asText());
//            }
//        }
//    }
//
//    @Override
//    public List<Statistics> getMultipleStatistics() throws Exception {
//        ExtendedStatistics extStats = new ExtendedStatistics();
//        Map<String,String> stats = new LinkedHashMap<>();
//        List<AdvancedControllableProperty> controls = new ArrayList<>();
//
//        if (authToken == null || authToken.isEmpty()){ this.authenticate();}
//
//        getOutletNames();
//        getOutletStates(stats,controls);
//        getSystemInformation(stats,controls);
//
//        createDefaultControls(controls,stats);
//        extStats.setStatistics(stats);
//        extStats.setControllableProperties(controls);
//        return Collections.singletonList(extStats);
//    }
//
//    private void getSystemInformation(Map<String, String> stats, List<AdvancedControllableProperty> controls) throws Exception {
//        if (salt == null || salt.isEmpty()){
//            String indexResponse = this.doGet("/");
//            Matcher saltMatcher = saltPattern.matcher(indexResponse);
//            if (saltMatcher.find()){
//                salt = saltMatcher.group(1);
//            }
//        }
//
//        JsonNode loginResponse = objectMapper.readTree(this.doPost("/php/user-login.php",
//                Collections.singletonMap("Content-Type","application/x-www-form-urlencoded"),
//                "action=login-submit&user-name="+this.getLogin()+"&user-password=" + getEncodedPassword() + "&remember-me=false"));
//
//        if (loginResponse.at("/success").asBoolean()){
//            stats.put(formatDeviceStat("Name"),loginResponse.at("/deviceConf/deviceName").asText());
//            stats.put(formatDeviceStat("NumOutlets"),loginResponse.at("/deviceConf/deviceNumOutlets").asText());
//            stats.put(formatDeviceStat("Model"),loginResponse.at("/deviceConf/modelName").asText());
//            stats.put(formatDeviceStat("DeviceCalibrated"),loginResponse.at("/deviceConf/deviceCalibrated").asText());
//            stats.put(formatDeviceStat("TempUnit"),loginResponse.at("/deviceConf/deviceTemperatureUnit").asText());
//
//            String line1Voltage = loginResponse.at("/linecords/LV1Voltage").asText();
//            if (line1Voltage.equals("0.0000")) {
//                stats.put(formatDeviceStat("Line1Connected"),"false");
//                stats.put(formatDeviceStat("Line1Voltage"), "0.0000");
//                stats.put(formatDeviceStat("Line1Current"), "0.0000");
//            } else {
//                stats.put(formatDeviceStat("Line1Connected"),"true");
//                stats.put(formatDeviceStat("Line1Voltage"), line1Voltage);
//                stats.put(formatDeviceStat("Line1Current"), loginResponse.at("/linecords/LC1Current").asText());
//
//            }
//            String line2Voltage = loginResponse.at("/linecords/LV2Voltage").asText();
//            if (line2Voltage.equals("0.0000")) {
//                stats.put(formatDeviceStat("Line2Connected"),"false");
//                stats.put(formatDeviceStat("Line2Voltage"), "0.0000");
//                stats.put(formatDeviceStat("Line2Current"), "0.0000");
//            } else {
//                stats.put(formatDeviceStat("Line2Connected"),"true");
//                stats.put(formatDeviceStat("Line2Voltage"), line1Voltage);
//                stats.put(formatDeviceStat("Line2Current"), loginResponse.at("/linecords/LC2Current").asText());
//
//            }
//
//            String temp0 = loginResponse.at("/linecords/T0Temperature").asText();
//            String temp1 = loginResponse.at("/linecords/T1Temperature").asText();
//            if (temp0.equals("0"))
//                stats.put(formatDeviceStat("TempProbe0Connected"),"false");
//            else
//                stats.put(formatDeviceStat("TempProbe0Connected"),"true");
//
//            if (temp1.equals("0"))
//                stats.put(formatDeviceStat("TempProbe1Connected"),"false");
//            else
//                stats.put(formatDeviceStat("TempProbe1Connected"),"true");
//
//            stats.put(formatDeviceStat("TempProbe0Temp"),temp0);
//            stats.put(formatDeviceStat("TempProbe1Temp"),temp1);
//
//            JsonNode networkResponse = objectMapper.readTree(this.doPost("/php/network-addresses.php",""));
//           if (networkResponse.at("/success").asBoolean()){
//               stats.put(formatDeviceStat("networkIpMode"),networkResponse.at("/networkConf/effectiveIpMode").asText());
//               stats.put(formatDeviceStat("networkGateway"),networkResponse.at("/networkConf/effectiveGateway").asText());
//               stats.put(formatDeviceStat("networkMacAddress"),networkResponse.at("/networkConf/macAddress").asText());
//           }
//        }
//    }
//
//    private String getEncodedPassword() {
//        return Base64.getEncoder().encodeToString((salt + this.getPassword()).getBytes());
//    }
//
//    private void getOutletNames() throws Exception {
//        JsonNode namesResponse = objectMapper.readTree(this.doPost("/services/retrieve/","{\"token\":\""+authToken+"\",\"names\":[\"outlets\",\"groups\",\"sequences\"]}"));
//        if (!namesResponse.at("/success").asBoolean()){
//            if (namesResponse.at("/message").asText().contains("Invalid token") || namesResponse.at("/message").asText().contains("Token expired")){
//                this.authenticate();
//                //Retry Request.
//                namesResponse = objectMapper.readTree(this.doPost("/services/retrieve/","{\"token\":\""+authToken+"\",\"names\":[\"outlets\",\"groups\",\"sequences\"]}"));
//            }
//        }
//        checkForErrors(namesResponse);
//        if (namesResponse.has("names")){
//            JsonNode namesJson = namesResponse.at("/names");
//            if (namesJson.has("outletNames") && namesJson.has("groupNames")){
//                JsonNode groups = namesJson.at("/groupNames");
//                groupNames.clear();
//                for (Iterator<String> it = groups.fieldNames(); it.hasNext(); ) {
//                    String key = it.next();
//                    groupNames.add(groups.at("/"+key).asText());
//                }
//
//                JsonNode outlets = namesJson.at("/outletNames");
//                outletNames.clear();
//                for (Iterator<String> it = outlets.fieldNames(); it.hasNext(); ) {
//                    String key = it.next();
//                    outletNames.add(outlets.at("/"+key).asText());
//                }
//            } else{
//                throw new Exception("Unable to parse names from response. 'outletNames' or 'groupNames' fields missing.");
//            }
//        } else {
//            throw new Exception("Unable to parse names from response. 'names' field missing in response.");
//        }
//    }
//
//    private void getOutletStates(Map<String, String> stats, List<AdvancedControllableProperty> controls) throws Exception {
//        JsonNode stateResponse = objectMapper.readTree(this.doPost("/services/retrieve/", createJsonRetrieveString()));
//        checkForErrors(stateResponse);
//        if (stateResponse.has("outlets")){
//            JsonNode outletStates = stateResponse.at("/outlets");
//            int outletNumber = 1;
//            for (Iterator<String> it = outletStates.fieldNames(); it.hasNext(); ) {
//                String fieldName = it.next();
//                createOutletStats(outletNumber,fieldName,outletStates.at("/"+fieldName).asText().equals("On"),stats,controls);
//                outletNumber++;
//            }
//        } else{
//            throw new Exception("Unable to parse outlet states. 'outlets' field not found.");
//        }
//
//        if (stateResponse.has("groups")){
//            JsonNode groupStates = stateResponse.at("/groups");
//            for (Iterator<String> it = groupStates.fieldNames(); it.hasNext(); ) {
//                String fieldName = it.next();
//                createGroupStats(fieldName,groupStates.at("/"+fieldName).asText(),stats,controls);
//            }
//        } else{
//            throw new Exception("Unable to parse outlet states. 'outlets' field not found.");
//        }
//    }
//
//    private void createGroupStats(String name,String state,Map<String,String> stats, List<AdvancedControllableProperty> controls){
//        String statName = formatGroupName(name);
//        switch (state){
//            case "On":
//            case "Mixed":
//                stats.put(statName,"1");
//                controls.add(createSwitch(statName,true));
//                break;
//            case "Off":
//                stats.put(statName,"0");
//                controls.add(createSwitch(statName,false));
//                break;
//            case "?":
//                stats.put(statName,"No outlets in group");
//
//        }
//    }
//
//    private void createOutletStats(int number,String name, boolean on, Map<String, String> stats, List<AdvancedControllableProperty> controls) {
//        String statName = formatOutletName(number,name);
//        stats.put(statName,on? "1" : "0");
//        controls.add(createSwitch(statName,on));
//    }
//
//    private String createJsonRetrieveString() throws JsonProcessingException {
//        Serialisers.StateRequestObject stateRequestObject = new Serialisers.StateRequestObject(authToken,outletNames.toArray(new String[0]),groupNames.toArray(new String[0]));
//        return objectMapper.writeValueAsString(stateRequestObject);
//    }
//
//    private void createDefaultControls(List<AdvancedControllableProperty> controls,Map<String,String> stats) {
//        stats.put("_AllOn","0");
//        stats.put("_AllOff","0");
//        controls.add(createButton("_AllOn","All On","Turning On..."));
//        controls.add(createButton("_AllOff","All Off","Turning Off..."));
//    }
//
//    private AdvancedControllableProperty createSwitch(String name, boolean state){
//        AdvancedControllableProperty.Switch portSwitch = new AdvancedControllableProperty.Switch();
//        portSwitch.setLabelOn("On");
//        portSwitch.setLabelOff("Off");
//        return new AdvancedControllableProperty(name,new Date(),portSwitch,state?"1":"0");
//    }
//
//    private AdvancedControllableProperty createButton(String name,String label, String pressed){
//        AdvancedControllableProperty.Button button = new AdvancedControllableProperty.Button();
//        button.setLabelPressed(pressed);
//        button.setLabel(label);
//        button.setGracePeriod(2000L);
//        return new AdvancedControllableProperty(name,new Date(),button,"0");
//    }
//
//    @Override
//    public void controlProperty(ControllableProperty controllableProperty) throws Exception {
//        if (controllableProperty == null)
//            return;
//
//        String controlProperty = controllableProperty.getProperty();
//        boolean state = String.valueOf(controllableProperty.getValue()).equals("1");
//        ControlType controlType = getControlType(controlProperty);
//
//        ControlObject controlObject = null;
//
//        switch (controlType){
//            case outlet:
//                String outlet = ""+controlProperty.charAt(0);
//                controlObject = new ControlObject(authToken,"outlet",state ? "on":"off",new String[]{outlet},null,null);
//                break;
//            case allOutlets:
//                if (controlProperty.equals("_AllOn"))
//                    controlObject = new ControlObject(authToken,"outlet","on",new String[]{"1","2","3","4","5","6","7","8"},null,null);
//                else if (controlProperty.equals("_AllOff"))
//                    controlObject = new ControlObject(authToken,"outlet","off",new String[]{"1","2","3","4","5","6","7","8"},null,null);
//                break;
//            case group:
//                String group = controlProperty.replace("Groups#","");
//                controlObject = new ControlObject(authToken,"outlet",state ? "on":"off",null,null,new String[]{group});
//                break;
//        }
//
//        if (controlObject != null){
//            String controlResponse = this.doPost("services/control/",objectMapper.writeValueAsString(controlObject));
//            checkForErrors(objectMapper.readTree(controlResponse));
//        }
//    }
//
//    private ControlType getControlType(String controlProperty) {
//        if (controlProperty.charAt(0) > 47 && controlProperty.charAt(0) <= 57)
//            return ControlType.outlet;
//
//        if (controlProperty.charAt(0) == '_')
//            return ControlType.allOutlets;
//
//        if (controlProperty.startsWith("Groups#"))
//            return ControlType.group;
//
//        if (controlProperty.startsWith("Sequences#"))
//            return ControlType.sequence;
//
//        return ControlType.unknown;
//    }
//
//    @Override
//    public void controlProperties(List<ControllableProperty> list) throws Exception {
//        for (ControllableProperty cp : list){
//            controlProperty(cp);
//        }
//    }
//
//    private String formatOutletName(int number,String name) {return number + ": "   + name;}
//    private String formatGroupName(String groupName)        {return "Groups#"       + groupName;}
//    private String formatSequenceName(String statName)      {return "Sequences#"    + statName;}
//    private String formatDeviceStat(String statName)        {return "DeviceInfo#"   + statName;}
//
//    private void checkForErrors(JsonNode deviceResponse) throws Exception {
//        if (!deviceResponse.at("/success").asBoolean()) {
//            if (!deviceResponse.at("/message").asText().contains("There are no Groups")){
//                throw new Exception(deviceResponse.at("/message").asText());
//            }
//        }
//    }
//}
