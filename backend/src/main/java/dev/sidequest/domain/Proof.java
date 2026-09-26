package dev.sidequest.domain;

/**
 * Dovada atașată unui quest completat. Ambele câmpuri sunt opționale.
 *
 * @param text      descriere scurtă, deja curățată (null dacă lipsește)
 * @param imagePath numele fișierului din folderul de upload-uri (null dacă nu există poză)
 */
public record Proof(String text, String imagePath) {

    public static final int MAX_TEXT_LENGTH = 500;

    public static final Proof NONE = new Proof(null, null);

    public boolean isEmpty() {
        return text == null && imagePath == null;
    }
}
