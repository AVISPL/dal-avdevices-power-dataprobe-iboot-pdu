/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.insightsystems.symphony.dal.dataprobe;

/**
 * @author Harry / Symphony Dev Team<br>
 * Created on 11/22/2024
 * @since 1.0.0
 */
public class Serialisers {

    /**
     * A class representing a control object, used for sending control commands to a device.
     * This includes properties for the device token, control type, command, sequence, group, and outlets.
     */
    static class ControlObject{
        private String token,control,command, group, sequence;
        private String[] outlets;

        /**
         * Constructs a new {@link ControlObject}.
         *
         * @param _token    the authentication token for the device
         * @param _control  the type of control (e.g., "group", "outlet", "sequence")
         * @param _command  the command to be sent (e.g., "on", "off", "cycle")
         * @param _outlets  an array of outlets to be controlled
         * @param _sequence the sequence for the control (maybe null)
         * @param _group    the group of outlets (maybe null)
         */
        ControlObject(String _token,String _control,String _command,String[] _outlets,String _sequence, String _group){
            token = _token;
            control = _control;
            command = _command;
            outlets = _outlets;
            sequence = _sequence;
            group = _group;
        }
        public String getToken() {return token;}
        public String getControl() {return control;}
        public String getCommand() {return command;}
        public String[] getOutlets() {return outlets;}
        public String getSequence() {return sequence;}
        public String getGroup() {return group;}
        public void setToken(String token) {this.token = token;}
        public void setControl(String control) {this.control = control;}
        public void setCommand(String command) {this.command = command;}
        public void setOutlets(String[] outlets) {this.outlets = outlets;}
        public void setSequence(String sequence) {this.sequence = sequence;}
        public void setGroup(String group) {this.group = group;}
    }

    /**
     * A class representing a state request object, used for retrieving the state of outlets and groups.
     * This includes properties for the device token, outlets, and groups.
     */
    static class StateRequestObject{
        private String token;
        private String[] outlets, groups;

        /**
         * Constructs a new {@link StateRequestObject}.
         *
         * @param _token    the authentication token for the device
         * @param _outlets  an array of outlets to be retrieved
         * @param _groups   an array of groups to be retrieved
         */
        StateRequestObject(String _token,String[] _outlets, String[] _groups){
            token = _token;
            outlets = _outlets;
            groups = _groups;
        }
        public String getToken() {return token;}
        public String[] getGroups() {return groups;}
        public String[] getOutlets() {return outlets;}
        public void setToken(String token) {this.token = token;}
        public void setOutlets(String[] outlets) {this.outlets = outlets;}
        public void setGroups(String[] groups) {this.groups = groups;}
    }
}
