package com.kiddo.remotescreen.server.controller;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.kiddo.remotescreen.server.model.ForgotPasswordDto;
import com.kiddo.remotescreen.server.model.LoginResponse;
import com.kiddo.remotescreen.server.model.LoginUserDto;
import com.kiddo.remotescreen.server.model.RegisterUserDto;
import com.kiddo.remotescreen.server.service.AuthenticationService;

@RestController
@RequestMapping(value = "auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/signup")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterUserDto registerUserDto) {
        authenticationService.signup(registerUserDto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@RequestBody @Valid LoginUserDto loginUserDto) {
        LoginResponse response = authenticationService.authenticate(loginUserDto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam Integer secret,
                                              @RequestParam String email) {
        authenticationService.verifyEmail(email, secret);
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(HTML);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody @Valid ForgotPasswordDto dto) {
        authenticationService.forgotPassword(dto.email());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam Integer secret,
                                                @RequestParam String email) {
        authenticationService.resetPassword(email, secret);
        String htmlResponse = """
                <!DOCTYPE html>
                <html lang='en'>
                <head>
                    <meta charset='UTF-8'>
                    <title>Password Reset</title>
                    <style>
                        body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }
                        .container { background: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                        h2 { color: #4CAF50; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h2>Password Reset Email Sent</h2>
                        <p>Your new password has been sent to your email address.</p>
                        <p>Please check your inbox and use the new password to log in.</p>
                    </div>
                </body>
                </html>
            """;
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(htmlResponse);
    }

    private final String HTML = """
        <html>
          <head>
            <title>Email Verification</title>
            <meta charset=\"UTF-8\">
            <style>
              body {
                display: flex;
                justify-content: center;
                align-items: center;
                height: 100vh;
                margin: 0;
                font-family: Arial, sans-serif;
                background-color: #f4f4f4;
              }
              .message-box {
                text-align: center;
                background-color: #fff;
                padding: 40px;
                border-radius: 10px;
                box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
              }
              .message-box h2 {
                color: #28a745;
              }
            </style>
          </head>
          <body>
            <div class="message-box">
              <h2>🎉 Active account successfully!</h2>
              <p>Now you can log in.</p>
            </div>
          </body>
        </html>
        """;
}
