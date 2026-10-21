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
  interest_areas TEXT[],
  interest_embedding vector(1024),
  PRIMARY KEY (follower_id, followed_id),
  CONSTRAINT ck_no_self_follow CHECK (follower_id <> followed_id)
);
ALTER TABLE researcher_follows ADD COLUMN IF NOT EXISTS interest_areas TEXT[];
ALTER TABLE researcher_follows ADD COLUMN IF NOT EXISTS interest_embedding vector(1024);

CREATE TABLE IF NOT EXISTS group_follows (
  follower_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  group_id VARCHAR(255) NOT NULL REFERENCES research_group(group_id) ON DELETE CASCADE,
  PRIMARY KEY (follower_id, group_id)
);

CREATE TABLE IF NOT EXISTS post_likes (
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, post_id)
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

CREATE TABLE IF NOT EXISTS user_discovery_vectors (
  user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  embedding vector(1024) NOT NULL
);

CREATE TABLE IF NOT EXISTS user_like_vectors (
  user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  embedding vector(1024) NOT NULL
);

DELETE FROM user_like_vectors;
INSERT INTO user_like_vectors(user_id, embedding)
SELECT l.user_id, l2_normalize(avg(l2_normalize(v.embedding)))
FROM post_likes l JOIN post_vectors v ON v.post_id=l.post_id
WHERE vector_norm(v.embedding)>0
GROUP BY l.user_id
HAVING vector_norm(avg(l2_normalize(v.embedding)))>0
ON CONFLICT(user_id) DO UPDATE SET embedding=excluded.embedding;

CREATE TABLE IF NOT EXISTS indexing_jobs (
  entity_type VARCHAR(10) NOT NULL,
  entity_id VARCHAR(255) NOT NULL,
  version BIGINT NOT NULL DEFAULT 1,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  attempts INTEGER NOT NULL DEFAULT 0,
  available_at TIMESTAMP NOT NULL DEFAULT now(),
  lease_until TIMESTAMP,
  PRIMARY KEY (entity_type, entity_id)
);
