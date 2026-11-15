-- Campus Resource Exchange - database schema (MySQL 8)
CREATE DATABASE IF NOT EXISTS campus_exchange;
USE campus_exchange;

-- Students who can lend and borrow resources
CREATE TABLE IF NOT EXISTS users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    name    VARCHAR(80)  NOT NULL,
    email   VARCHAR(100) NOT NULL UNIQUE
);

-- Items that a student (the owner) offers to lend
CREATE TABLE IF NOT EXISTS resources (
    resource_id INT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(100) NOT NULL,
    category    VARCHAR(40)  NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'Available',
    owner_id    INT          NOT NULL,
    CONSTRAINT fk_resources_owner FOREIGN KEY (owner_id) REFERENCES users (user_id)
);

-- One row per successful borrow (the borrow history)
CREATE TABLE IF NOT EXISTS requests (
    request_id   INT PRIMARY KEY AUTO_INCREMENT,
    resource_id  INT  NOT NULL,
    borrower_id  INT  NOT NULL,
    request_date DATE NOT NULL,
    CONSTRAINT fk_requests_resource FOREIGN KEY (resource_id) REFERENCES resources (resource_id),
    CONSTRAINT fk_requests_borrower FOREIGN KEY (borrower_id) REFERENCES users (user_id)
);
