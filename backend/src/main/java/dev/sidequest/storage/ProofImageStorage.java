package dev.sidequest.storage;

import dev.sidequest.service.InvalidInputException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Salvează pozele-dovadă ca fișiere locale, în folderul {@code sidequest.uploads-dir} (implicit
 * {@code uploads/}, relativ la directorul din care pornește backend-ul).
 *
 * <p>Nu avem încredere în numele sau Content-Type-ul trimis de client: formatul se deduce din
 * primii octeți ai fișierului, iar numele salvat e un UUID generat de noi. Așa nu se pot urca
 * fișiere HTML/SVG deghizate în poze și nici nu se poate ieși din folder cu "../".
 */
@Component
public class ProofImageStorage {

    private static final Logger log = LoggerFactory.getLogger(ProofImageStorage.class);

    // Doar numele generate de noi: <uuid>.<extensie cunoscută>
    private static final Pattern STORED_NAME =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}[.](jpg|png|gif|webp)");

    private final Path root;

    public ProofImageStorage(@Value("${sidequest.uploads-dir:uploads}") String uploadsDir) {
        this.root = Path.of(uploadsDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Nu pot crea folderul de upload-uri " + root, e);
        }
        log.info("Pozele-dovadă se salvează în {}", root);
    }

    /** Salvează poza și întoarce numele fișierului (de pus în baza de date). */
    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidInputException("Poza trimisă e goală.");
        }
        ImageType type;
        try (InputStream in = file.getInputStream()) {
            type = ImageType.sniff(in.readNBytes(12))
                    .orElseThrow(() -> new InvalidInputException("Acceptăm doar poze JPG, PNG, GIF sau WebP."));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        String name = UUID.randomUUID() + "." + type.extension;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, root.resolve(name));
        } catch (IOException e) {
            throw new UncheckedIOException("Nu am putut salva poza", e);
        }
        return name;
    }

    /** Pentru curățenie când completarea eșuează după ce poza a fost deja scrisă. */
    public void deleteQuietly(String name) {
        if (name == null || !STORED_NAME.matcher(name).matches()) {
            return;
        }
        try {
            Files.deleteIfExists(root.resolve(name));
        } catch (IOException e) {
            log.warn("Nu am putut șterge poza orfană {}", name, e);
        }
    }

    public Optional<StoredImage> load(String name) {
        if (name == null || !STORED_NAME.matcher(name).matches()) {
            return Optional.empty();
        }
        Path path = root.resolve(name);
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        String extension = name.substring(name.lastIndexOf('.') + 1);
        return Optional.of(new StoredImage(new PathResource(path), ImageType.fromExtension(extension).mediaType));
    }

    public record StoredImage(Resource resource, MediaType mediaType) {
    }

    enum ImageType {
        JPEG("jpg", MediaType.IMAGE_JPEG),
        PNG("png", MediaType.IMAGE_PNG),
        GIF("gif", MediaType.IMAGE_GIF),
        WEBP("webp", MediaType.parseMediaType("image/webp"));

        final String extension;
        final MediaType mediaType;

        ImageType(String extension, MediaType mediaType) {
            this.extension = extension;
            this.mediaType = mediaType;
        }

        static ImageType fromExtension(String extension) {
            return Arrays.stream(values()).filter(t -> t.extension.equals(extension)).findFirst().orElseThrow();
        }

        /** Recunoaște formatul după "magic bytes". */
        static Optional<ImageType> sniff(byte[] head) {
            if (startsWith(head, 0, 0xFF, 0xD8, 0xFF)) {
                return Optional.of(JPEG);
            }
            if (startsWith(head, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
                return Optional.of(PNG);
            }
            if (startsWith(head, 0, 'G', 'I', 'F', '8')) {
                return Optional.of(GIF);
            }
            if (startsWith(head, 0, 'R', 'I', 'F', 'F') && startsWith(head, 8, 'W', 'E', 'B', 'P')) {
                return Optional.of(WEBP);
            }
            return Optional.empty();
        }

        private static boolean startsWith(byte[] data, int offset, int... expected) {
            if (data.length < offset + expected.length) {
                return false;
            }
            for (int i = 0; i < expected.length; i++) {
                if ((data[offset + i] & 0xFF) != expected[i]) {
                    return false;
                }
            }
            return true;
        }
    }
}
