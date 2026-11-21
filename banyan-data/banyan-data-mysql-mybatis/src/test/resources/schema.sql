DROP TABLE IF EXISTS users;
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(50) NOT NULL,
    age INT,
    email VARCHAR(100),
    create_time TIMESTAMP,
    nick_name VARCHAR(50),
    city VARCHAR(50),
    status INT
);
