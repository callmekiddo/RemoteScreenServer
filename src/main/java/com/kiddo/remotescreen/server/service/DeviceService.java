package com.kiddo.remotescreen.server.service;

import com.kiddo.remotescreen.server.entity.Device;

import java.util.List;
import java.util.Optional;

public interface DeviceService {
    String register(String password, String deviceName, String machineUuid);
    boolean isValidCredential(String deviceId, String password);
    boolean isAllowRemote(String deviceId);
    boolean isDeviceBusy(String deviceId);
    boolean updateConnectedAndroid(String deviceId, String androidName);
    boolean clearConnectedAndroid(String deviceId);
    boolean updatePassword(String deviceId, String newPassword);
    boolean setAllowRemote(String deviceId, boolean allow);
    Device getDeviceById(String deviceId);
    void save(Device device);
    List<String> getAllDeviceIds();
}


