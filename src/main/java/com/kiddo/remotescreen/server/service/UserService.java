package com.kiddo.remotescreen.server.service;

import java.util.Optional;
import com.kiddo.remotescreen.server.entity.User;
import com.kiddo.remotescreen.server.model.UpdatePasswordDto;

public interface UserService {

    Optional<User> findByEmail(String email);

    void updatePassword(UpdatePasswordDto updatePasswordDto);
}
