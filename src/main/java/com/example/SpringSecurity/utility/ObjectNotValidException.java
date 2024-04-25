package com.example.SpringSecurity.utility;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Set;

/**
 * Custom exception class for handling object validation errors.
 *
 * @ResponseStatus Indicates the HTTP response status code when this exception is thrown.
 * @AllArgsConstructor Lombok's annotation to generate a constructor with all fields for dependency injection.
 * @Data Lombok's annotation to generate getters, setters, toString, equals, and hashCode methods.
 * @Author Prince Amofah
 * @CreatedAt 30th September 2023
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
@AllArgsConstructor
@Data
public class ObjectNotValidException extends RuntimeException{
    Set<String> errorMessages;
}
