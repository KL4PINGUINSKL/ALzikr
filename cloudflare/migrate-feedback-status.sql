-- 2026-11-28: sorting out the messages (status, a private note, when it changed).
ALTER TABLE feedback ADD COLUMN status TEXT DEFAULT 'new';
ALTER TABLE feedback ADD COLUMN note TEXT;
ALTER TABLE feedback ADD COLUMN changed TEXT;
