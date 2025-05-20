package com.kiddo.remotescreen.server.service.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.kiddo.remotescreen.server.entity.User;
import com.kiddo.remotescreen.server.model.TokenDto;
import com.kiddo.remotescreen.server.model.UpdatePasswordDto;
import com.kiddo.remotescreen.server.service.TokenService;
import com.kiddo.remotescreen.server.service.UserService;

@Service
public class UserServiceImpl implements UserService {
    private final DynamoDBMapper dynamoDBMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public UserServiceImpl(DynamoDBMapper dynamoDBMapper, PasswordEncoder passwordEncoder,
                           TokenService tokenService) {
        this.dynamoDBMapper = dynamoDBMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Override
    public Optional<User> findByEmail(String email) {
        var user = dynamoDBMapper.load(User.class, email);
        return Optional.ofNullable(user);
    }

    @Override
    public void updatePassword(UpdatePasswordDto updatePasswordDto) {

        // Lấy người dùng từ DynamoDB
        User existingUser = dynamoDBMapper.load(User.class, updatePasswordDto.email());
        if (existingUser == null) {
            throw new RuntimeException("User with this email does not exist");
        }

        // Kiểm tra mật khẩu mới và xác nhận mật khẩu có khớp không
        if (!updatePasswordDto.newPassword().equals(updatePasswordDto.confirmPassword())) {
            throw new RuntimeException("Password confirmation does not match");
        }

        // Mã hóa và cập nhật mật khẩu
        String encodedPassword = passwordEncoder.encode(updatePasswordDto.newPassword());
        existingUser.setPassword(encodedPassword);

        // Lưu lại vào DynamoDB
        dynamoDBMapper.save(existingUser);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            String email = userDetails.getUsername();
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            String authHeader = attr.getRequest().getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                // 5. Khóa token
                tokenService.lockToken(new TokenDto(email, token));
            }
        }
    }
}