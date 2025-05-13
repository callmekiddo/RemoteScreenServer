package com.kiddo.remotescreen.server.controller;

import com.kiddo.remotescreen.server.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.kiddo.remotescreen.server.model.UpdatePasswordDto;

@RestController
@RequestMapping(value = "api/v1/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/update-password")
    public ResponseEntity<Void> updatePassword(@RequestBody @Valid UpdatePasswordDto updatePasswordDto) {
        userService.updatePassword(updatePasswordDto);
        return ResponseEntity.ok().build();
    }

}
