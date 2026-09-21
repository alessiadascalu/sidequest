package dev.sidequest.web;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Acceptă doar ID-uri IANA de regiune (ex. "Europe/Bucharest"), nu offset-uri ca "+02:00" sau "Z". */
@Documented
@Constraint(validatedBy = ValidZoneIdValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidZoneId {

    String message() default "trebuie să fie un fus orar IANA valid, ex. Europe/Bucharest";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
