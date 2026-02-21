package com.healthtracker.goal;

import com.healthtracker.goal.dto.GoalRequest;
import com.healthtracker.goal.dto.GoalResponse;
import com.healthtracker.security.AuthUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    public List<GoalResponse> list(
            @AuthenticationPrincipal AuthUser user, @RequestParam(defaultValue = "false") boolean includeArchived) {
        return goalService.list(user.id(), includeArchived);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoalResponse create(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody GoalRequest request) {
        return goalService.create(user.id(), request);
    }

    @PutMapping("/{id}")
    public GoalResponse update(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long id, @Valid @RequestBody GoalRequest request) {
        return goalService.update(user.id(), id, request);
    }

    @PostMapping("/{id}/archive")
    public GoalResponse archive(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return goalService.setArchived(user.id(), id, true);
    }

    @PostMapping("/{id}/restore")
    public GoalResponse restore(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        return goalService.setArchived(user.id(), id, false);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
        goalService.delete(user.id(), id);
    }
}
