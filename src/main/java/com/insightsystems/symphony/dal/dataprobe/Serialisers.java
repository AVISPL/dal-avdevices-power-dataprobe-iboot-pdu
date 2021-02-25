package com.insightsystems.symphony.dal.dataprobe;

public class Serialisers {
    static class AuthRequestObject{
        private String username,password;

        AuthRequestObject(String _username,String _password){
            username = _username;
            password = _password;
        }

        public String getUsername() {return username;}
        public String getPassword() {return password;}
        public void setUsername(String username) {this.username = username;}
        public void setPassword(String password) {this.password = password;}
    }

    static class ControlObject{
        private String token,control,command;
        private String[] outlets, sequence,group;

        ControlObject(String _token,String _control,String _command,String[] _outlets,String[] _sequence, String[] _group){
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
        public String[] getSequence() {return sequence;}
        public String[] getGroup() {return group;}
        public void setToken(String token) {this.token = token;}
        public void setControl(String control) {this.control = control;}
        public void setCommand(String command) {this.command = command;}
        public void setOutlets(String[] outlets) {this.outlets = outlets;}
        public void setSequence(String[] sequence) {this.sequence = sequence;}
        public void setGroup(String[] group) {this.group = group;}
    }

    static class StateRequestObject{
        private String token;
        private String[] outlets, groups;

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
