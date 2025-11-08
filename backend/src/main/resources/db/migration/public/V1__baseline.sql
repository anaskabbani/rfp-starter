-- Public schema baseline (shared tables)
CREATE TABLE IF NOT EXISTS orgs (
  id UUID PRIMARY KEY,
  slug TEXT UNIQUE NOT NULL,
  name TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
