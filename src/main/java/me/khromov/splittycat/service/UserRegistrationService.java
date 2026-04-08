package me.khromov.splittycat.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.domain.entity.RegistrationStep;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.service.dto.UpdateUsernameCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class UserRegistrationService {

    private static final List<RegistrationStep> REGISTRATION_FLOW =
            List.of(RegistrationStep.USERNAME_CHOICE, RegistrationStep.WAITING_USERNAME);

    private final UserAccountService userAccountService;

    @Transactional
    public void startRegistration(long tgId) {
        userAccountService.requireRegisteredUser(tgId).startRegistration(firstRegistrationStep());
    }

    @Transactional
    public void completeRegistration(long tgId) {
        userAccountService.requireRegisteredUser(tgId).completeRegistration();
    }

    @Transactional
    public void proceedToNextStep(long tgId) {
        User user = userAccountService.requireRegisteredUser(tgId);
        user.moveToRegistrationStep(resolveNextStep(user.getRegistrationStep()));
    }

    @Transactional
    public User updateUsernameAndComplete(long tgId, @Valid UpdateUsernameCommand command) {
        User user = userAccountService.requireRegisteredUser(tgId);
        user.applyUsername(command.username());
        user.completeRegistration();
        return user;
    }

    private RegistrationStep firstRegistrationStep() {
        return REGISTRATION_FLOW.get(0);
    }

    private RegistrationStep resolveNextStep(RegistrationStep currentStep) {
        int currentIndex = REGISTRATION_FLOW.indexOf(currentStep);
        if (currentIndex < 0 || currentIndex + 1 >= REGISTRATION_FLOW.size()) {
            return RegistrationStep.NONE;
        }
        return REGISTRATION_FLOW.get(currentIndex + 1);
    }
}
