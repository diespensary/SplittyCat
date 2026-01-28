-- events
CREATE INDEX idx_events_owner_user ON events(owner_user_id);

-- participants
CREATE INDEX idx_participants_created_by_user ON participants(created_by_user_id);
CREATE INDEX idx_participants_linked_user ON participants(linked_user_id);

-- expenses
CREATE INDEX idx_expenses_event ON expenses(event_id);
CREATE INDEX idx_expenses_owner_user ON expenses(owner_user_id);
CREATE INDEX idx_expenses_payer_participant ON expenses(payer_participant_id);

-- participant_shares
CREATE INDEX idx_participant_shares_participant ON participant_shares(participant_id);



