CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE users ADD COLUMN IF NOT EXISTS author_id VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS google_subject VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS orcid VARCHAR(19);
ALTER TABLE users ADD COLUMN IF NOT EXISTS verification_level VARCHAR(30) NOT NULL DEFAULT 'NAO_VERIFICADO';
ALTER TABLE users ADD COLUMN IF NOT EXISTS institutional_domain VARCHAR(255);
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_author_id ON users(author_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_google_subject ON users(google_subject);
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_orcid ON users(orcid);

ALTER TABLE posts ADD COLUMN IF NOT EXISTS post_type VARCHAR(20);
ALTER TABLE posts ADD COLUMN IF NOT EXISTS keywords TEXT[];

CREATE TABLE IF NOT EXISTS researcher_follows (
  follower_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  followed_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  PRIMARY KEY (follower_id, followed_id),
  CONSTRAINT ck_no_self_follow CHECK (follower_id <> followed_id)
);

CREATE TABLE IF NOT EXISTS group_follows (
  follower_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  group_id VARCHAR(255) NOT NULL REFERENCES research_group(group_id) ON DELETE CASCADE,
  PRIMARY KEY (follower_id, group_id)
);

CREATE TABLE IF NOT EXISTS user_vectors (
  user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  embedding vector(1024) NOT NULL
);
CREATE TABLE IF NOT EXISTS group_vectors (
  group_id VARCHAR(255) PRIMARY KEY REFERENCES research_group(group_id) ON DELETE CASCADE,
  embedding vector(1024) NOT NULL
);
CREATE TABLE IF NOT EXISTS post_vectors (
  post_id BIGINT PRIMARY KEY REFERENCES posts(id) ON DELETE CASCADE,
  embedding vector(1024) NOT NULL
);

-- Cache local das conferências da Conference Partner (atualizado por campo a cada 24h)
CREATE TABLE IF NOT EXISTS conferences (
  id BIGINT PRIMARY KEY,
  field VARCHAR(255) NOT NULL,
  short_name VARCHAR(255),
  full_name TEXT,
  submission_date DATE,
  conference_date DATE,
  location TEXT,
  ccf_rank VARCHAR(8),
  detail_page TEXT,
  cp_index_score DOUBLE PRECISION
);
CREATE INDEX IF NOT EXISTS idx_conferences_field_date ON conferences(field, conference_date);
CREATE TABLE IF NOT EXISTS conference_vectors (
  conference_id BIGINT PRIMARY KEY REFERENCES conferences(id) ON DELETE CASCADE,
  embedding vector(1024) NOT NULL
);
CREATE TABLE IF NOT EXISTS conference_field_refresh (
  field VARCHAR(255) PRIMARY KEY,
  refreshed_at TIMESTAMP NOT NULL
);
