ALTER TABLE transactions ADD COLUMN destination_account_id BIGINT;

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_destination_account
        FOREIGN KEY (destination_account_id) REFERENCES accounts (id);

ALTER TABLE transactions ALTER COLUMN category_id DROP NOT NULL;

ALTER TABLE transactions DROP CONSTRAINT chk_transactions_type;

ALTER TABLE transactions
    ADD CONSTRAINT chk_transactions_type
        CHECK (type IN ('INCOME', 'EXPENSE', 'TRANSFER'));

ALTER TABLE transactions
    ADD CONSTRAINT chk_transactions_shape
        CHECK (
            (type IN ('INCOME', 'EXPENSE') AND category_id IS NOT NULL AND destination_account_id IS NULL)
            OR
            (type = 'TRANSFER' AND category_id IS NULL AND destination_account_id IS NOT NULL AND destination_account_id <> account_id)
        );
