ALTER TABLE app_users
  ADD COLUMN password_reset_code_hash varchar(255),
  ADD COLUMN password_reset_code_expires_at timestamptz;
