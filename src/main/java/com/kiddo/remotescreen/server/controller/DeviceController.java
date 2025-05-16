package com.kiddo.remotescreen.server.controller;

import com.kiddo.remotescreen.server.entity.Device;
import com.kiddo.remotescreen.server.model.*;
import com.kiddo.remotescreen.server.service.DeviceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/device")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping("/register")
    public ResponseEntity<DeviceRegisterResponseDto> register(@RequestBody DeviceRegisterRequestDto request) {
        String deviceId = deviceService.register(request.password(), request.deviceName(), request.machineUuid());
        return ResponseEntity.ok(new DeviceRegisterResponseDto(deviceId));
    }

    @GetMapping("/status/{deviceId}")
    public ResponseEntity<?> getDeviceStatus(@PathVariable String deviceId) {
        Device device = deviceService.getDeviceById(deviceId);
        if (device == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Device not found");
        }

        DeviceStatusDto status = new DeviceStatusDto(
                Boolean.TRUE.equals(device.getAllowRemote()),
                device.getConnectedAndroid()
        );
        return ResponseEntity.ok(status);
    }

    @PostMapping("/connect-android")
    public ResponseEntity<?> connectAndroid(@RequestBody DeviceConnectAndroidDto dto) {
        Device device = deviceService.getDeviceById(dto.deviceId());
        if (device == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Device ID not found");
        }

        if (!device.getDevicePassword().equals(dto.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Incorrect password");
        }

        if (!Boolean.TRUE.equals(device.getAllowRemote())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Remote access disabled");
        }

        if (deviceService.isDeviceBusy(dto.deviceId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Device is already in use");
        }

        deviceService.updateConnectedAndroid(dto.deviceId(), dto.androidDeviceName());

        // ✅ Trả về cả deviceId và deviceName
        return ResponseEntity.ok(new DeviceConnectResponseDto(
                device.getDeviceId(),
                device.getDeviceName()
        ));
    }



    @PutMapping("/remote-access")
    public ResponseEntity<?> updateRemoteAccess(@RequestBody DeviceRemoteAccessDto dto) {
        boolean updated = deviceService.setAllowRemote(dto.deviceId(), dto.allowRemote());
        return updated ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body("Device not found");
    }

    @PutMapping("/update-password")
    public ResponseEntity<?> updatePassword(@RequestBody DeviceUpdatePasswordDto dto) {
        boolean updated = deviceService.updatePassword(dto.deviceId(), dto.newPassword());
        return updated ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body("Device not found");
    }


    @PostMapping("/disconnect-android")
    public ResponseEntity<?> disconnectAndroid(@RequestParam String deviceId) {
        boolean ok = deviceService.clearConnectedAndroid(deviceId);
        return ok ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body("Device not found");
    }
}
