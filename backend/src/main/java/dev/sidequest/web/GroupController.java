package dev.sidequest.web;

import dev.sidequest.group.GroupService;
import dev.sidequest.web.Dtos.CreateGroupRequest;
import dev.sidequest.web.Dtos.GroupDto;
import dev.sidequest.web.Dtos.JoinGroupRequest;
import dev.sidequest.web.Dtos.LeaderboardEntryDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    /** Creează grupul; răspunsul conține {@code inviteCode}, de trimis prietenilor. */
    @PostMapping("/groups")
    public ResponseEntity<GroupDto> create(@Valid @RequestBody CreateGroupRequest request) {
        GroupDto group = GroupDto.from(groupService.create(request.name(), request.creatorUserId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(group);
    }

    /** 404 dacă nu există grup cu acel cod, 409 dacă utilizatorul e deja membru. */
    @PostMapping("/groups/join")
    public GroupDto join(@Valid @RequestBody JoinGroupRequest request) {
        return GroupDto.from(groupService.join(request.userId(), request.inviteCode()));
    }

    @GetMapping("/users/{id}/groups")
    public List<GroupDto> groupsOf(@PathVariable Long id) {
        return GroupDto.from(groupService.groupsOf(id));
    }

    @GetMapping("/groups/{id}/leaderboard")
    public List<LeaderboardEntryDto> leaderboard(@PathVariable Long id) {
        return LeaderboardEntryDto.from(groupService.leaderboard(id));
    }
}
