package com.kiddo.remotescreen.server.controller;

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
        String deviceId = deviceService.register(request.password(), request.deviceName());
        return ResponseEntity.ok(new DeviceRegisterResponseDto(deviceId));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody DeviceAuthRequestDto request) {
        boolean isValid = deviceService.verify(request.deviceId(), request.password());
        return isValid ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid device credentials");
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

    @PostMapping("/connect-android")
    public ResponseEntity<?> connectAndroid(@RequestBody DeviceConnectAndroidDto dto) {
        if (!deviceService.verify(dto.deviceId(), dto.password())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
        if (!deviceService.isAllowRemote(dto.deviceId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Remote access disabled");
        }
        if (deviceService.isDeviceBusy(dto.deviceId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Device is already in use");
        }
        deviceService.updateConnectedAndroid(dto.deviceId(), dto.androidDeviceName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/disconnect-android")
    public ResponseEntity<?> disconnectAndroid(@RequestParam String deviceId) {
        boolean ok = deviceService.clearConnectedAndroid(deviceId);
        return ok ? ResponseEntity.ok().build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body("Device not found");
    }
}
