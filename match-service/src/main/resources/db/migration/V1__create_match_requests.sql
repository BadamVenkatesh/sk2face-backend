CREATE TABLE match_requests (
        id BIGINT AUTO_INCREMENT PRIMARY KEY,
        user_id BIGINT NOT NULL,
        input_image_url VARCHAR(500),
        match_result_1 VARCHAR(500),
        match_result_2 VARCHAR(500),
        match_result_3 VARCHAR(500),
        status VARCHAR(20) DEFAULT 'COMPLETED',
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);