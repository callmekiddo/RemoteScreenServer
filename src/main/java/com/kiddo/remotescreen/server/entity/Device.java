package com.kiddo.remotescreen.server.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.*;

@DynamoDBTable(tableName = "devices")
public class Device {

    @DynamoDBHashKey(attributeName = "deviceId")
    private String deviceId;

    @DynamoDBAttribute(attributeName = "machineUuid")
    private String machineUuid;

    @DynamoDBAttribute(attributeName = "devicePassword")
    private String devicePassword;

    @DynamoDBAttribute(attributeName = "deviceName")
    private String deviceName;

    @DynamoDBAttribute(attributeName = "connectedAndroid")
    private String connectedAndroid;

    @DynamoDBAttribute(attributeName = "allowRemote")
    private Boolean allowRemote = true;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDevicePassword() {
        return devicePassword;
    }

    public void setDevicePassword(String devicePassword) {
        this.devicePassword = devicePassword;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getConnectedAndroid() {
        return connectedAndroid;
    }

    public void setConnectedAndroid(String connectedAndroid) {
        this.connectedAndroid = connectedAndroid;
    }

    public Boolean getAllowRemote() {
        return allowRemote;
    }

    public void setAllowRemote(Boolean allowRemote) {
        this.allowRemote = allowRemote;
    }

    public String getMachineUuid() {
        return machineUuid;
    }

    public void setMachineUuid(String machineUuid) {
        this.machineUuid = machineUuid;
    }
}
