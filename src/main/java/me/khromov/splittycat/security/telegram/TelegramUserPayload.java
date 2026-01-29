package me.khromov.splittycat.security.telegram;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramUserPayload(
        long id,
        String username,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName
) {
}
