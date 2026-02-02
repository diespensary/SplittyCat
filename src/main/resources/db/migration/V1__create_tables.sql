CREATE TABLE users (
    id  bigserial PRIMARY KEY,
    tg_id bigint NOT NULL,
    username text NOT NULL,
    registration_step varchar(32) NOT NULL DEFAULT 'NONE',
    onboarded boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_tg_id UNIQUE (tg_id)
);

CREATE TABLE events (
    id bigserial PRIMARY KEY,
    title text NOT NULL,
    owner_user_id bigint NOT NULL,
    invite_code text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_events_invite_code UNIQUE (invite_code)
);

CREATE TABLE participants (
    id bigserial PRIMARY KEY,
    event_id bigint NOT NULL,
    name text NOT NULL,
    normalized_name text NOT NULL,
    linked_user_id bigint NULL,
    created_by_user_id bigint NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_participants_event_normalized_name
        UNIQUE (event_id, normalized_name),
    CONSTRAINT uq_participants_event_linked_user
        UNIQUE (event_id, linked_user_id)
);

CREATE TABLE expenses (
    id bigserial PRIMARY KEY,
    event_id bigint NOT NULL,
    title text NOT NULL,
    amount numeric(18, 2) NOT NULL CHECK (amount >= 0),
    currency_id bigint NOT NULL,
    owner_user_id bigint NOT NULL,
    payer_participant_id bigint NOT NULL,
    expense_date date NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE participant_shares (
    id bigserial PRIMARY KEY,
    expense_id bigint NOT NULL,
    participant_id bigint NOT NULL,
    amount numeric(18, 2) NOT NULL CHECK (amount >= 0),
    description varchar(500),
    participant_marked_paid_at timestamptz,
    confirmed_at timestamptz,
    CONSTRAINT uq_participant_shares_expense_participant
        UNIQUE (expense_id, participant_id)
);

CREATE TABLE currencies (
    id bigserial PRIMARY KEY,
    code varchar(3) NOT NULL,
    name text NOT NULL,
    symbol text,
    CONSTRAINT chk_currencies_code_upper CHECK (code = upper(code)),
    CONSTRAINT uq_currencies_code UNIQUE (code)
);