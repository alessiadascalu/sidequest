package dev.sidequest.web;

import dev.sidequest.domain.User;
import dev.sidequest.service.QuestService;
import dev.sidequest.service.UserService;
import dev.sidequest.web.Dtos.CompleteQuestResponse;
import dev.sidequest.web.Dtos.CreateUserRequest;
import dev.sidequest.web.Dtos.ProfileResponse;
import dev.sidequest.web.Dtos.TodayQuestResponse;
import dev.sidequest.web.Dtos.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.ZoneId;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final QuestService questService;

    public UserController(UserService userService, QuestService questService) {
        this.userService = userService;
        this.questService = questService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.create(request.username(), ZoneId.of(request.zoneId()));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}/profile").buildAndExpand(user.getId()).toUri();
        return ResponseEntity.created(location).body(UserResponse.from(user));
    }

    @GetMapping("/{id}/quest/today")
    public TodayQuestResponse todayQuest(@PathVariable Long id) {
        return TodayQuestResponse.from(questService.getToday(id));
    }

    @PostMapping("/{id}/quest/today/complete")
    public CompleteQuestResponse completeToday(@PathVariable Long id) {
        return CompleteQuestResponse.from(questService.complete(id));
    }

    @GetMapping("/{id}/profile")
    public ProfileResponse profile(@PathVariable Long id) {
        return ProfileResponse.from(userService.profile(id));
    }
}
