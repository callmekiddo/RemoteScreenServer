package com.kiddo.remotescreen.server.service.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.kiddo.remotescreen.server.entity.User;
import com.kiddo.remotescreen.server.enums.AccountStatus;
import com.kiddo.remotescreen.server.model.LoginResponse;
import com.kiddo.remotescreen.server.model.LoginUserDto;
import com.kiddo.remotescreen.server.model.LogoutUserDto;
import com.kiddo.remotescreen.server.model.RegisterUserDto;
import com.kiddo.remotescreen.server.model.TokenDto;
import com.kiddo.remotescreen.server.service.AuthenticationService;
import com.kiddo.remotescreen.server.service.JwtService;
import com.kiddo.remotescreen.server.service.TokenService;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private Logger logger = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

    private final DynamoDBMapper dynamoDBMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final AmazonSimpleEmailService amazonSimpleEmailService;
    private final JwtService jwtService;
    private final TokenService tokenService;

    static final String SUBJECT = "Confirm Your Email Address";

    private String getHtmlBody(String verificationLink) {
        return String.format(
                "<!DOCTYPE html>"
                        + "<html lang='en'>"
                        + "<head>"
                        + "<meta charset='UTF-8'>"
                        + "<title>Email Confirmation</title>"
                        + "</head>"
                        + "<body>"
                        + "<h1>Confirm Your Email Address</h1>"
                        + "<p>Thank you for registering. Please click the link below to verify your email address:</p>"
                        + "<p><a href='%s' "
                        + "style='display: inline-block; padding: 10px 20px; background-color: #1a73e8; "
                        + "color: white; text-decoration: none; border-radius: 4px;'>Verify Email</a></p>"
                        + "<p>If you did not sign up for this account, you can ignore this email.</p>"
                        + "</body>"
                        + "</html>",
                verificationLink
        );
    }

    private String getHtmlBodyReset(String resetLink) {
        return String.format(
                "<!DOCTYPE html>"
                        + "<html lang='en'>"
                        + "<head>"
                        + "<meta charset='UTF-8'>"
                        + "<title>Password Reset Request</title>"
                        + "</head>"
                        + "<body style='font-family: Arial, sans-serif; line-height: 1.6;'>"
                        + "<h2>Password Reset Request</h2>"
                        + "<p>We received a request to reset the password for your account.</p>"
                        + "<p>Click the button below to reset your password:</p>"
                        + "<p><a href='%s' "
                        + "style='display: inline-block; padding: 12px 24px; background-color: #d93025; "
                        + "color: white; text-decoration: none; border-radius: 4px;'>Reset Password</a></p>"
                        + "<p>If you did not request a password reset, you can safely ignore this email.</p>"
                        + "<p>Thank you,<br>The Support Team</p>"
                        + "</body>"
                        + "</html>",
                resetLink
        );
    }

    @Value("${fromEmail}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    public AuthenticationServiceImpl(DynamoDBMapper dynamoDBMapper, PasswordEncoder passwordEncoder,
                                     AuthenticationManager authenticationManager,
                                     AmazonSimpleEmailService amazonSimpleEmailService, JwtService jwtService,
                                     TokenService tokenService) {
        this.dynamoDBMapper = dynamoDBMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.amazonSimpleEmailService = amazonSimpleEmailService;
        this.jwtService = jwtService;
        this.tokenService = tokenService;
    }

    @Override
    public void signup(RegisterUserDto input) {
        var existingUser = dynamoDBMapper.load(User.class, input.email());
        if (existingUser != null) {
            throw new RuntimeException("User with this email already exists");
        }
        var user = new User();
        Random random = new Random();
        BeanUtils.copyProperties(input, user);
        user.setPassword(passwordEncoder.encode(input.password()));
        int secret = 100000 + random.nextInt(900000);
        user.setSecret(secret);
        user.setAction(AccountStatus.INACTIVE.name());
        dynamoDBMapper.save(user);
        var urlVerify = baseUrl + "/auth/verify-email?email=" + input.email() + "&secret=" + secret;

        var htmlBody = getHtmlBody(urlVerify);
        sendEmail(user.getEmail(),
                "Click the link below to reset your password", htmlBody);
    }

    @Override
    public LoginResponse authenticate(LoginUserDto input) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.email(),
                        input.password()
                )
        );

        User user = dynamoDBMapper.load(User.class, input.email());
        if (user == null) {
            throw new UsernameNotFoundException(input.email());
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getAction())) {
            throw new RuntimeException("The account is not activated yet");
        }

        String jwtToken = jwtService.generateToken(user);
        return new LoginResponse(jwtToken, jwtService.getExpirationTime(), user.getFullName());
    }

    @Override
    public List<User> allUsers() {
        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
        return dynamoDBMapper.scan(User.class, scanExpression);
    }

    @Override
    public void verifyEmail(String email, Integer secret) {
        var user = dynamoDBMapper.load(User.class, email);
        if (user == null) {
            throw new RuntimeException("User not found with this email");
        }

        if (!secret.equals(user.getSecret())) {
            throw new RuntimeException("Invalid secret code");
        }

        Random random = new Random();
        int secretNew = 100000 + random.nextInt(900000);
        user.setSecret(secretNew);

        user.setAction(AccountStatus.ACTIVE.name());
        dynamoDBMapper.save(user);
    }

    @Override
    public void logout(LogoutUserDto logoutUserDto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
            String email = userDetails.getUsername();
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            String authHeader = attr.getRequest().getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                tokenService.lockToken(new TokenDto(email, token));
            }
        }
    }

    @Override
    public void forgotPassword(String email) {
        var user = dynamoDBMapper.load(User.class, email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        Random random = new Random();
        int secret = 100000 + random.nextInt(900000);
        user.setSecret(secret);
        dynamoDBMapper.save(user);

        String resetUrl =  baseUrl + "/auth/reset-password?email=" + email + "&secret=" + secret;
        String htmlBody = getHtmlBodyReset(resetUrl);

        sendEmail(email, "Click the link below to reset your password", htmlBody);
    }

    @Override
    public void resetPassword(String email, Integer secret) {
        var user = dynamoDBMapper.load(User.class, email);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        if (!secret.equals(user.getSecret())) {
            throw new IllegalArgumentException("Invalid secret code");
        }

        String newPassword = generateRandomPassword();
        Random random = new Random();
        user.setPassword(passwordEncoder.encode(newPassword));
        int secretNew = 100000 + random.nextInt(900000);
        user.setSecret(secretNew);
        dynamoDBMapper.save(user);

        String htmlBody = String.format(
                "<!DOCTYPE html>"
                        + "<html lang='en'>"
                        + "<head><meta charset='UTF-8'><title>Password Reset</title></head>"
                        + "<body style='font-family: Arial, sans-serif;'>"
                        + "<h2>Your password has been reset</h2>"
                        + "<p>Your new temporary password is:</p>"
                        + "<p style='font-size: 20px; font-weight: bold;'>%s</p>"
                        + "<p>Please log in and change your password as soon as possible.</p>"
                        + "</body></html>",
                newPassword
        );

        sendEmail(email, "Your password has been reset", htmlBody);
    }

    @Async
    public void sendEmail(String toEmail, String emailMessage, String link) {
        try {
            SendEmailRequest request = new SendEmailRequest()
                    .withDestination(
                            new Destination().withToAddresses(toEmail))
                    .withMessage(new Message()
                            .withBody(new Body()
                                    .withHtml(new Content()
                                            .withCharset("UTF-8").withData(link))
                                    .withText(new Content()
                                            .withCharset("UTF-8").withData(emailMessage)))
                            .withSubject(new Content()
                                    .withCharset("UTF-8").withData(SUBJECT)))
                    .withSource(fromEmail);
            amazonSimpleEmailService.sendEmail(request);
            logger.info("Email sent!");
            System.out.println("Email sent!");
        } catch (Exception ex) {
            logger.error("The email was not sent. Error message: {}", ex.getMessage());
        }
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}