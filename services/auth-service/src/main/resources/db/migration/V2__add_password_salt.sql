ALTER TABLE auth.auth_users
    ADD COLUMN IF NOT EXISTS password_salt VARCHAR(64);

UPDATE auth.auth_users
SET password_salt = ''
WHERE password_salt IS NULL;

ALTER TABLE auth.auth_users
    ALTER COLUMN password_salt SET NOT NULL;
