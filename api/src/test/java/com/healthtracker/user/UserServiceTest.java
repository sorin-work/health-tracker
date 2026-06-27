package com.healthtracker.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

import com.healthtracker.common.ApiException;
import com.healthtracker.user.dto.UpdateProfileRequest;
import com.healthtracker.user.dto.UserResponse;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository users;

    UserService service;
    User sam;

    @BeforeEach
    void setUp() {
        service = new UserService(users);
        sam = new User("sam@example.com", "hash", "Sam", "UTC", Instant.EPOCH);
        sam.setId(1L);
        lenient().when(users.findById(1L)).thenReturn(Optional.of(sam));
    }

    @Test
    void newUsersStartWithMetricUnits() {
        UserResponse me = UserResponse.from(sam);

        assertThat(me.unitSystem()).isEqualTo(UnitSystem.METRIC);
        assertThat(me.glucoseUnit()).isEqualTo(GlucoseUnit.MG_DL);
    }

    @Test
    void updatesNameZoneAndUnits() {
        UserResponse updated = service.updateProfile(1L, new UpdateProfileRequest(
                " Sam Lee ", "America/Chicago", UnitSystem.IMPERIAL, GlucoseUnit.MMOL_L));

        assertThat(updated.displayName()).isEqualTo("Sam Lee");
        assertThat(updated.timezone()).isEqualTo("America/Chicago");
        assertThat(updated.unitSystem()).isEqualTo(UnitSystem.IMPERIAL);
        assertThat(updated.glucoseUnit()).isEqualTo(GlucoseUnit.MMOL_L);
    }

    @Test
    void leavesUnitsAloneWhenTheyAreNotSent() {
        sam.setUnitSystem(UnitSystem.IMPERIAL);

        UserResponse updated = service.updateProfile(1L, new UpdateProfileRequest("Sam", "UTC", null, null));

        assertThat(updated.unitSystem()).isEqualTo(UnitSystem.IMPERIAL);
        assertThat(updated.glucoseUnit()).isEqualTo(GlucoseUnit.MG_DL);
    }

    @Test
    void rejectsAnUnknownTimeZone() {
        assertThatThrownBy(() -> service.updateProfile(1L, new UpdateProfileRequest("Sam", "Mars/Olympus", null, null)))
                .isInstanceOf(ApiException.class);
    }
}
