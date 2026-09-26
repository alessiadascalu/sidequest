package dev.sidequest.service;

import dev.sidequest.domain.User;

/** @param created true dacă utilizatorul tocmai a fost creat, false dacă exista deja (login) */
public record LoginResult(User user, boolean created) {
}
