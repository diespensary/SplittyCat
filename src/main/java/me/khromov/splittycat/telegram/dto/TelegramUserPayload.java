package me.khromov.splittycat.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramUserPayload(
        long id,
        String username,
        @JsonProperty("first_name") String firstName,
        @JsonProperty("last_name") String lastName
) {}
