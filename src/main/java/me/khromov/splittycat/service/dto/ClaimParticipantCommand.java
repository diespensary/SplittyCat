package me.khromov.splittycat.service.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import me.khromov.splittycat.common.util.InputNormalizer;

public record ClaimParticipantCommand(
        @NotBlank(message = "Код приглашения не может быть пустым")
        String inviteCode,

        @Positive(message = "participantId должен быть положительным")
        Long participantId,

        @Size(max = 255, message = "Имя участника не должно быть длиннее 255 символов")
        String participantName
) {

    public ClaimParticipantCommand {
        inviteCode = InputNormalizer.trim(inviteCode);
        participantName = InputNormalizer.trimToNull(participantName);
    }

    @AssertTrue(message = "Укажите либо participantId, либо participantName")
    public boolean hasParticipantSelection() {
        return participantId != null || participantName != null;
    }

    @AssertTrue(message = "Нельзя одновременно передавать participantId и participantName")
    public boolean hasSingleSelectionSource() {
        return participantId == null || participantName == null;
    }

    public boolean claimsExistingParticipant() {
        return participantId != null;
    }
}
