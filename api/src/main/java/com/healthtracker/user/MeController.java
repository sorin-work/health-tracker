package com.healthtracker.user;

import com.healthtracker.security.AuthUser;
import com.healthtracker.user.dto.UpdateProfileRequest;
import com.healthtracker.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final UserService userService;

    public MeController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal AuthUser user) {
        return UserResponse.from(userService.get(user.id()));
    }

    @PutMapping
    public UserResponse update(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(user.id(), request);
    }
}
