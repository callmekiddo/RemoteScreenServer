package com.kiddo.remotescreen.server.service;

import java.util.List;

import com.kiddo.remotescreen.server.model.LoginResponse;
import com.kiddo.remotescreen.server.model.LoginUserDto;
import com.kiddo.remotescreen.server.model.RegisterUserDto;
import com.kiddo.remotescreen.server.entity.User;
import com.kiddo.remotescreen.server.model.LogoutUserDto;

public interface AuthenticationService {

    void signup(RegisterUserDto input);

    LoginResponse authenticate(LoginUserDto input);

    List<User> allUsers();

    void verifyEmail(String email, Integer secret);

    void logout(LogoutUserDto logoutUserDto);

    void forgotPassword(String email);

    void resetPassword(String email, Integer secret);
}
