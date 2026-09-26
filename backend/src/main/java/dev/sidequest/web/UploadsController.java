package dev.sidequest.web;

import dev.sidequest.service.NotFoundException;
import dev.sidequest.storage.ProofImageStorage;
import dev.sidequest.storage.ProofImageStorage.StoredImage;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/** Servește pozele-dovadă salvate în folderul de upload-uri. */
@RestController
public class UploadsController {

    private final ProofImageStorage images;

    public UploadsController(ProofImageStorage images) {
        this.images = images;
    }

    @GetMapping("/uploads/{name:.+}")
    public ResponseEntity<Resource> image(@PathVariable String name) {
        StoredImage image = images.load(name)
                .orElseThrow(() -> new NotFoundException("Poza nu există"));
        // Fiecare poză primește un UUID nou, deci conținutul de la un URL nu se schimbă niciodată.
        return ResponseEntity.ok()
                .contentType(image.mediaType())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePrivate().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .body(image.resource());
    }
}
