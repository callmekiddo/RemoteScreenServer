package com.kiddo.remotescreen.server.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.kiddo.remotescreen.server.model.LogoutUserDto;
import com.kiddo.remotescreen.server.service.AuthenticationService;

@RestController
@RequestMapping(value = "api/v1")
public class LogoutController {

    private final AuthenticationService authenticationService;

    public LogoutController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid LogoutUserDto logoutUserDto) {
        authenticationService.logout(logoutUserDto);
        return ResponseEntity.ok().build();
    }
}
