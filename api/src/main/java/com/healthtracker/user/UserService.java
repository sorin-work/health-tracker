package com.healthtracker.user;

import com.healthtracker.common.ApiException;
import com.healthtracker.user.dto.UpdateProfileRequest;
import com.healthtracker.user.dto.UserResponse;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public User get(Long userId) {
        return users.findById(userId).orElseThrow(() -> ApiException.notFound("User"));
    }

    @Transactional(readOnly = true)
    public ZoneId zoneOf(Long userId) {
        return get(userId).zone();
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = get(userId);
        user.setDisplayName(request.displayName().trim());
        user.setTimezone(Timezones.validate(request.timezone()));
        if (request.unitSystem() != null) {
            user.setUnitSystem(request.unitSystem());
        }
        if (request.glucoseUnit() != null) {
            user.setGlucoseUnit(request.glucoseUnit());
        }
        return UserResponse.from(user);
    }
}
