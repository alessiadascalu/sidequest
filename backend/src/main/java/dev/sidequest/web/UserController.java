package dev.sidequest.web;

import dev.sidequest.domain.Proof;
import dev.sidequest.service.LoginResult;
import dev.sidequest.service.QuestService;
import dev.sidequest.service.UserService;
import dev.sidequest.storage.ProofImageStorage;
import dev.sidequest.web.Dtos.CompleteQuestResponse;
import dev.sidequest.web.Dtos.CreateUserRequest;
import dev.sidequest.web.Dtos.HistoryEntryDto;
import dev.sidequest.web.Dtos.LoginResponse;
import dev.sidequest.web.Dtos.ProfileResponse;
import dev.sidequest.web.Dtos.TodayQuestResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final QuestService questService;
    private final ProofImageStorage images;

    public UserController(UserService userService, QuestService questService, ProofImageStorage images) {
        this.userService = userService;
        this.questService = questService;
        this.images = images;
    }

    /** Username existent → 200 cu datele lui (login). Username nou → 201 cu contul proaspăt creat. */
    @PostMapping
    public ResponseEntity<LoginResponse> loginOrCreate(@Valid @RequestBody CreateUserRequest request) {
        LoginResult login = userService.loginOrCreate(request.username(), ZoneId.of(request.zoneId()));
        Long id = login.user().getId();
        LoginResponse body = LoginResponse.from(login, userService.profile(id), questService.history(id));
        if (!login.created()) {
            return ResponseEntity.ok(body);
        }
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}/profile").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).body(body);
    }

    @GetMapping("/{id}/quest/today")
    public TodayQuestResponse todayQuest(@PathVariable Long id) {
        return TodayQuestResponse.from(questService.getToday(id));
    }

    /**
     * Acceptă o cerere fără body (bifare simplă) sau multipart/form-data cu câmpurile opționale
     * {@code proofText} și {@code photo}.
     */
    @PostMapping("/{id}/quest/today/complete")
    public CompleteQuestResponse completeToday(@PathVariable Long id,
                                               @RequestParam(required = false) String proofText,
                                               @RequestParam(required = false) MultipartFile photo) {
        String imagePath = (photo == null || photo.isEmpty()) ? null : images.store(photo);
        try {
            return CompleteQuestResponse.from(questService.complete(id, new Proof(proofText, imagePath)));
        } catch (RuntimeException e) {
            // Completarea a eșuat (404, 409, text prea lung...): nu lăsăm poze orfane pe disc.
            images.deleteQuietly(imagePath);
            throw e;
        }
    }

    @GetMapping("/{id}/profile")
    public ProfileResponse profile(@PathVariable Long id) {
        return ProfileResponse.from(userService.profile(id));
    }

    @GetMapping("/{id}/history")
    public List<HistoryEntryDto> history(@PathVariable Long id) {
        return HistoryEntryDto.from(questService.history(id));
    }
}
