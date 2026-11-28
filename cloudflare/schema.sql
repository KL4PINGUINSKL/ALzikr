-- Download counts: one row per day × version × source (web/app) × country.
CREATE TABLE IF NOT EXISTS downloads (
  day TEXT NOT NULL, version TEXT NOT NULL, source TEXT NOT NULL, country TEXT NOT NULL, n INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY (day, version, source, country)
);

-- Problems and feature requests sent from the app (POST /feedback); "who" is a daily hash, not the address.
CREATE TABLE IF NOT EXISTS feedback (
  id INTEGER PRIMARY KEY AUTOINCREMENT, at TEXT NOT NULL, day TEXT NOT NULL, kind TEXT NOT NULL, text TEXT NOT NULL,
  contact TEXT, version TEXT, device TEXT, android TEXT, country TEXT, who TEXT
);
CREATE INDEX IF NOT EXISTS feedback_day ON feedback (day, who);
