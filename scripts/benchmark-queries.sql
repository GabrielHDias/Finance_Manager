-- Synthetic, transactional benchmark. Run ONLY on an explicitly disposable DB.
-- No persistent rows, indexes or schema changes survive ROLLBACK.
BEGIN;
CREATE TEMP TABLE fm_bench_accounts(id bigint PRIMARY KEY, user_id bigint);
CREATE TEMP TABLE fm_bench_transactions(id bigint PRIMARY KEY, account_id bigint, amount numeric(19,2), type text, date date);
INSERT INTO fm_bench_accounts SELECT i, 1 + (i-1)/100 FROM generate_series(1,500) i;
INSERT INTO fm_bench_transactions SELECT i,1+(i-1)%500, (i%100000)::numeric/100,
 CASE WHEN i%3=0 THEN 'INCOME' ELSE 'EXPENSE' END, DATE '2026-01-01'+(i%365)::integer FROM generate_series(1,100000) i;
ANALYZE fm_bench_accounts;
ANALYZE fm_bench_transactions;
-- Original individual balance query, called once for each account by the old UI.
EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) SELECT COALESCE(SUM(CASE WHEN type='INCOME' THEN amount ELSE -amount END),0)
 FROM fm_bench_transactions WHERE account_id=1;
-- Replacement: one grouped result for every account belonging to the user.
EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) SELECT a.id,COALESCE(SUM(CASE WHEN t.type='INCOME' THEN t.amount ELSE -t.amount END),0)
 FROM fm_bench_accounts a LEFT JOIN fm_bench_transactions t ON t.account_id=a.id WHERE a.user_id=1 GROUP BY a.id;
-- Export before and after: explicit bound is applied in SQL, not after loading all rows.
EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) SELECT t.* FROM fm_bench_transactions t JOIN fm_bench_accounts a ON a.id=t.account_id
 WHERE a.user_id=1 ORDER BY t.date DESC,t.id DESC;
EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) SELECT t.* FROM fm_bench_transactions t JOIN fm_bench_accounts a ON a.id=t.account_id
 WHERE a.user_id=1 ORDER BY t.date DESC,t.id DESC LIMIT 10001;
-- Compare equivalent work for all 100 owned accounts; excludes HTTP latency.
DO $$ DECLARE started timestamptz; ignored numeric; BEGIN
 started=clock_timestamp();
 FOR account IN 1..100 LOOP
  SELECT COALESCE(SUM(CASE WHEN type='INCOME' THEN amount ELSE -amount END),0) INTO ignored
  FROM fm_bench_transactions WHERE account_id=account;
 END LOOP;
 RAISE NOTICE 'individual_total_ms=%',EXTRACT(epoch FROM clock_timestamp()-started)*1000;
 started=clock_timestamp();
 PERFORM a.id,COALESCE(SUM(CASE WHEN t.type='INCOME' THEN t.amount ELSE -t.amount END),0)
 FROM fm_bench_accounts a LEFT JOIN fm_bench_transactions t ON t.account_id=a.id WHERE a.user_id=1 GROUP BY a.id;
 RAISE NOTICE 'batch_total_ms=%',EXTRACT(epoch FROM clock_timestamp()-started)*1000;
END $$;
ROLLBACK;
