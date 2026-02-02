package me.khromov.splittycat.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateParticipantRequest(@NotBlank String name) {
}
