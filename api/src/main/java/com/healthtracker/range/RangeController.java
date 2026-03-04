package com.healthtracker.range;

import com.healthtracker.range.dto.RangeResponse;
import com.healthtracker.range.dto.UpdateRangeRequest;
import com.healthtracker.security.AuthUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ranges")
public class RangeController {

    private final RangeService rangeService;

    public RangeController(RangeService rangeService) {
        this.rangeService = rangeService;
    }

    @GetMapping
    public List<RangeResponse> list(@AuthenticationPrincipal AuthUser user) {
        return rangeService.list(user.id());
    }

    @PutMapping("/{metric}")
    public RangeResponse update(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable RangeMetric metric,
            @Valid @RequestBody UpdateRangeRequest request) {
        return rangeService.update(user.id(), metric, request);
    }

    @DeleteMapping("/{metric}")
    public RangeResponse reset(@AuthenticationPrincipal AuthUser user, @PathVariable RangeMetric metric) {
        return rangeService.reset(user.id(), metric);
    }
}
