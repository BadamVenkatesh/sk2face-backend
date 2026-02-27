-- Change user_id column from BIGINT to VARCHAR(36) to support UUID identifiers
-- from the API gateway JWT X-USER-ID header
ALTER TABLE match_requests MODIFY COLUMN user_id VARCHAR(36) NOT NULL;
