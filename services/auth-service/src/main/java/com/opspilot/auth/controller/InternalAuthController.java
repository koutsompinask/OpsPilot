package com.opspilot.auth.controller;

import com.opspilot.auth.dto.InternalCreateUserRequest;
import com.opspilot.auth.dto.InternalCreateUserResponse;
import com.opspilot.auth.exception.ForbiddenException;
import com.opspilot.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/auth")
public class InternalAuthController {

    private final AuthService authService;
    private final String serviceToken;

    public InternalAuthController(AuthService authService, @Value("${auth.service-token}") String serviceToken) {
        this.authService = authService;
        this.serviceToken = serviceToken;
    }

    @PostMapping("/users")
    public InternalCreateUserResponse createUser(
            @RequestHeader(name = "X-Service-Token", required = false) String providedToken,
            @Valid @RequestBody InternalCreateUserRequest request
    ) {
        if (!serviceToken.equals(providedToken)) {
            throw new ForbiddenException("Invalid service token");
        }
        return authService.createInternalUser(request);
    }
}
