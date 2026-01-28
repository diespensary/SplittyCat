-- EVENTS -> USERS
ALTER TABLE events
    ADD CONSTRAINT fk_events_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES users(id)
            ON DELETE RESTRICT;

-- PARTICIPANTS -> EVENTS
ALTER TABLE participants
    ADD CONSTRAINT fk_participants_event
        FOREIGN KEY (event_id) REFERENCES events(id)
            ON DELETE CASCADE;

-- PARTICIPANTS -> USERS
ALTER TABLE participants
    ADD CONSTRAINT fk_participants_linked_user
        FOREIGN KEY (linked_user_id) REFERENCES users(id)
            ON DELETE RESTRICT;

ALTER TABLE participants
    ADD CONSTRAINT fk_participants_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES users(id)
            ON DELETE SET NULL;

-- EXPENSES -> EVENTS
ALTER TABLE expenses
    ADD CONSTRAINT fk_expenses_event
        FOREIGN KEY (event_id) REFERENCES events(id)
            ON DELETE CASCADE;

-- EXPENSES -> USERS
ALTER TABLE expenses
    ADD CONSTRAINT fk_expenses_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES users(id)
            ON DELETE RESTRICT;

-- EXPENSES -> CURRENCIES
ALTER TABLE expenses
    ADD CONSTRAINT fk_expenses_currency
        FOREIGN KEY (currency_id) REFERENCES currencies(id)
            ON DELETE RESTRICT;

-- EXPENSES -> PARTICIPANTS
-- DEFERRABLE нужно для стабильного DELETE event с каскадами
ALTER TABLE expenses
    ADD CONSTRAINT fk_expenses_payer_participant
        FOREIGN KEY (payer_participant_id) REFERENCES participants(id)
            ON DELETE RESTRICT
            DEFERRABLE INITIALLY DEFERRED;

-- PARTICIPANTS_SHARES -> EXPENSES
ALTER TABLE participant_shares
    ADD CONSTRAINT fk_shares_expense
        FOREIGN KEY (expense_id) REFERENCES expenses(id)
            ON DELETE CASCADE;

-- PARTICIPANTS_SHARES -> PARTICIPANTS
-- DEFERRABLE по той же причине: чтобы DELETE event проходил независимо от порядка каскадов
ALTER TABLE participant_shares
    ADD CONSTRAINT fk_shares_participant
        FOREIGN KEY (participant_id) REFERENCES participants(id)
            ON DELETE RESTRICT
            DEFERRABLE INITIALLY DEFERRED;

