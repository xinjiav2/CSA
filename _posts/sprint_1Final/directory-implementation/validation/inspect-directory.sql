PRAGMA table_info('directory_accounts');
PRAGMA foreign_key_list('directory_accounts');
SELECT sql FROM sqlite_master
WHERE type = 'table' AND name = 'directory_accounts';
SELECT id, name, student_id, account_type, created_at
FROM directory_accounts;
