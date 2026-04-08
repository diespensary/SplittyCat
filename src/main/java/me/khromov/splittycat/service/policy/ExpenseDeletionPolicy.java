package me.khromov.splittycat.service.policy;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.common.exception.ForbiddenException;
import me.khromov.splittycat.domain.entity.Event;
import me.khromov.splittycat.domain.entity.Expense;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.service.access.EventAccessPolicy;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExpenseDeletionPolicy {

    private final EventAccessPolicy accessPolicy;

    public void validate(Event event, Expense expense, User user) {
        boolean owner = accessPolicy.isOwner(event, user);
        boolean creator = expense.isCreatedBy(user);
        boolean payerIsUser = expense.isPaidBy(user);

        if (!owner && !creator && !payerIsUser) {
            throw new ForbiddenException("Удалить трату может только её создатель, владелец события или плательщик");
        }
    }
}
