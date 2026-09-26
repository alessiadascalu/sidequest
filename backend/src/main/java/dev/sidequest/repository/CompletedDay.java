package dev.sidequest.repository;

import java.time.LocalDate;

/** O zi locală în care un utilizator a completat quest-ul. */
public record CompletedDay(Long userId, LocalDate date) {
}
