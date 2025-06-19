-- Create shared_stashes table
CREATE TABLE shared_stashes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stash_hash VARCHAR(255) NOT NULL,
    repository_id BIGINT NOT NULL,
    shared_by VARCHAR(255) NOT NULL,
    shared_at TIMESTAMP NOT NULL,
    description VARCHAR(500),
    branch VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (repository_id) REFERENCES repositories(id) ON DELETE CASCADE,
    UNIQUE KEY uk_stash_hash_repo (stash_hash, repository_id)
);

-- Add index for faster queries
CREATE INDEX idx_shared_stashes_repository_id ON shared_stashes(repository_id);

