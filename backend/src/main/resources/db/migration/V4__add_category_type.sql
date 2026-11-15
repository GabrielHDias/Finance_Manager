ALTER TABLE categories ADD COLUMN type VARCHAR(20);

UPDATE categories
SET type = 'EXPENSE'
WHERE EXISTS (SELECT 1 FROM monthly_budgets b WHERE b.category_id = categories.id)
   OR EXISTS (SELECT 1 FROM transactions t WHERE t.category_id = categories.id AND t.type = 'EXPENSE')
   OR NOT EXISTS (SELECT 1 FROM transactions t WHERE t.category_id = categories.id);

UPDATE categories
SET type = 'INCOME'
WHERE type IS NULL;

-- Categorias que ficaram como despesa e ainda têm receitas ganham uma categoria
-- de receita separada, para o histórico continuar consistente.
DO $$
DECLARE
    rec RECORD;
    new_name TEXT;
    new_id BIGINT;
    suffix INT;
BEGIN
    FOR rec IN
        SELECT DISTINCT c.id, c.user_id, c.name
        FROM categories c
        JOIN transactions t ON t.category_id = c.id AND t.type = 'INCOME'
        WHERE c.type = 'EXPENSE'
    LOOP
        suffix := 2;
        new_name := left(rec.name, 90) || ' (receita)';
        WHILE EXISTS (SELECT 1 FROM categories WHERE user_id = rec.user_id AND name = new_name) LOOP
            new_name := left(rec.name, 100 - char_length(' (receita ' || suffix || ')'))
                || ' (receita ' || suffix || ')';
            suffix := suffix + 1;
        END LOOP;
        INSERT INTO categories (name, user_id, type)
        VALUES (new_name, rec.user_id, 'INCOME')
        RETURNING id INTO new_id;
        UPDATE transactions
        SET category_id = new_id
        WHERE category_id = rec.id AND type = 'INCOME';
    END LOOP;
END $$;

ALTER TABLE categories ALTER COLUMN type SET NOT NULL;
ALTER TABLE categories
    ADD CONSTRAINT chk_categories_type CHECK (type IN ('INCOME', 'EXPENSE'));
