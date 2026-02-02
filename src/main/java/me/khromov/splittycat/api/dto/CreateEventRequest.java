package me.khromov.splittycat.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateEventRequest(@NotBlank String title) {
}
