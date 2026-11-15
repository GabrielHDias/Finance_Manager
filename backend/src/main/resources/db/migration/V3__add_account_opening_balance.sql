ALTER TABLE accounts
    ADD COLUMN opening_balance NUMERIC(19, 2) NOT NULL DEFAULT 0;

ALTER TABLE accounts
    ADD CONSTRAINT chk_accounts_opening_balance_scale
        CHECK (scale(opening_balance) <= 2);
