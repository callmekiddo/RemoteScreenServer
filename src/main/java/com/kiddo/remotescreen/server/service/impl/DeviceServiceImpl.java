package com.kiddo.remotescreen.server.service.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.kiddo.remotescreen.server.entity.Device;
import com.kiddo.remotescreen.server.service.DeviceService;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import org.slf4j.Logger;
import java.util.stream.Collectors;

@Service
public class DeviceServiceImpl implements DeviceService {
    private static final Logger log = LoggerFactory.getLogger(DeviceServiceImpl.class);
    private final DynamoDBMapper dynamoDBMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public DeviceServiceImpl(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    @Override
    public String register(String password, String deviceName, String machineUuid) {
        // 1. Kiểm tra thiết bị cũ
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":uuid", new AttributeValue().withS(machineUuid));
        scanExpression.withFilterExpression("machineUuid = :uuid").withExpressionAttributeValues(eav);

        List<Device> existing = dynamoDBMapper.scan(Device.class, scanExpression);

        if (!existing.isEmpty()) {
            Device device = existing.get(0);
            log.info("🔁 Existing device found for machineUuid '{}', returning deviceId '{}'", machineUuid, device.getDeviceId());
            return device.getDeviceId();
        }

        // 2. Tạo thiết bị mới
        String deviceId;
        do {
            deviceId = String.valueOf(100_000_000 + secureRandom.nextInt(900_000_000));
        } while (dynamoDBMapper.load(Device.class, deviceId) != null);

        Device device = new Device();
        device.setDeviceId(deviceId);
        device.setDevicePassword(password);
        device.setDeviceName(deviceName);
        device.setMachineUuid(machineUuid);
        device.setAllowRemote(false);
        device.setConnectedAndroid(null);

        dynamoDBMapper.save(device);
        log.info("🆕 Registered new device '{}', machineUuid='{}'", deviceId, machineUuid);
        return deviceId;
    }

    @Override
    public boolean isValidCredential(String deviceId, String password) {
        Device device = dynamoDBMapper.load(Device.class, deviceId);
        return device != null && device.getDevicePassword().equals(password);
    }

    @Override
    public List<String> getAllDeviceIds() {
        List<Device> devices = dynamoDBMapper.scan(Device.class, new DynamoDBScanExpression());
        return devices.stream().map(Device::getDeviceId).collect(Collectors.toList());
    }

    @Override
    public Device getDeviceById(String deviceId) {
        return dynamoDBMapper.load(Device.class, deviceId);
    }

    @Override
    public boolean setAllowRemote(String deviceId, boolean allow) {
        Device device = dynamoDBMapper.load(Device.class, deviceId);
        if (device == null) return false;
        device.setAllowRemote(allow);
        dynamoDBMapper.save(device);
        return true;
    }

    @Override
    public boolean isAllowRemote(String deviceId) {
        Device device = dynamoDBMapper.load(Device.class, deviceId);
        return device != null && Boolean.TRUE.equals(device.getAllowRemote());
    }

    @Override
    public boolean updatePassword(String deviceId, String newPassword) {
        Device device = dynamoDBMapper.load(Device.class, deviceId);
        if (device == null) return false;
        device.setDevicePassword(newPassword);
        dynamoDBMapper.save(device);
        return true;
    }

    @Override
    public boolean updateConnectedAndroid(String deviceId, String androidName) {
        Device device = dynamoDBMapper.load(Device.class, deviceId);
        if (device == null || device.getConnectedAndroid() != null) return false;
        device.setConnectedAndroid(androidName);
        dynamoDBMapper.save(device);
        return true;
    }

    @Override
    public boolean clearConnectedAndroid(String deviceId) {
        Device device = dynamoDBMapper.load(Device.class, deviceId);
        if (device == null) return false;
        device.setConnectedAndroid(null);
        dynamoDBMapper.save(device);
        return true;
    }

    @Override
    public boolean isDeviceBusy(String deviceId) {
        Device device = dynamoDBMapper.load(Device.class, deviceId);
        return device != null && device.getConnectedAndroid() != null;
    }

    private String generateUniqueDeviceId() {
        String deviceId;
        do {
            deviceId = String.valueOf(100_000_000 + secureRandom.nextInt(900_000_000));
        } while (dynamoDBMapper.load(Device.class, deviceId) != null);
        return deviceId;
    }

    public void save(Device device) {
        dynamoDBMapper.save(device);
    }

}

