-- Download counts: one row per day × version × source (web/app) × country.
CREATE TABLE IF NOT EXISTS downloads (
  day TEXT NOT NULL, version TEXT NOT NULL, source TEXT NOT NULL, country TEXT NOT NULL, n INTEGER NOT NULL DEFAULT 0,
  PRIMARY KEY (day, version, source, country)
);
