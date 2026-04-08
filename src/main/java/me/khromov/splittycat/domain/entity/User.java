package me.khromov.splittycat.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tg_id", nullable = false)
    private Long tgId;

    @Column(name = "username", nullable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_step", nullable = false)
    private RegistrationStep registrationStep = RegistrationStep.NONE;

    @Column(name = "onboarded", nullable = false)
    private boolean onboarded;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static User create(long tgId, String username) {
        User user = new User();
        user.tgId = tgId;
        user.username = username;
        user.registrationStep = RegistrationStep.NONE;
        user.onboarded = false;
        return user;
    }

    public void startRegistration(RegistrationStep firstStep) {
        onboarded = false;
        registrationStep = firstStep;
    }

    public void moveToRegistrationStep(RegistrationStep step) {
        registrationStep = step;
    }

    public void ensureRegistrationStep() {
        if (registrationStep == null) {
            registrationStep = RegistrationStep.NONE;
        }
    }

    public void completeRegistration() {
        onboarded = true;
        registrationStep = RegistrationStep.NONE;
    }

    public void applyUsername(String username) {
        this.username = username;
    }

    public boolean hasId(Long userId) {
        return id != null && id.equals(userId);
    }
}
