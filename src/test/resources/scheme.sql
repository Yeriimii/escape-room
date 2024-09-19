CREATE TABLE `admin` (
                         `admin_id` int PRIMARY KEY AUTO_INCREMENT,
                         `name` varchar(100) NOT NULL,
                         `phone_number` varchar(13),
                         `role` varchar(100),
                         `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
                         `created_by` int,
                         `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
                         `updated_by` int
);

CREATE TABLE `coupon` (
                          `code` varchar(8) PRIMARY KEY,
                          `is_used` tinyint(1) DEFAULT 0,
                          `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
                          `created_by` int,
                          `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
                          `updated_by` int
);

CREATE TABLE `office` (
                          `office_id` int PRIMARY KEY AUTO_INCREMENT,
                          `name` varchar(100) NOT NULL,
                          `welcome_message` text,
                          `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
                          `created_by` int,
                          `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
                          `updated_by` int
);

CREATE TABLE `account` (
                           `office_id` int NOT NULL,
                           `bank_name` varchar(100) NOT NULL,
                           `account_number` varchar(100) NOT NULL
);

CREATE TABLE `theme` (
                         `theme_id` int PRIMARY KEY AUTO_INCREMENT,
                         `office_id` int NOT NULL,
                         `name` varchar(100) NOT NULL,
                         `price` int NOT NULL,
                         `open_time` time NOT NULL,
                         `discount` int DEFAULT 0,
                         `capacity` tinyint(1) DEFAULT 2,
                         `is_available` tinyint(1) NOT NULL DEFAULT 0,
                         `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
                         `created_by` int,
                         `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
                         `updated_by` int
);

CREATE TABLE `theme_management` (
                                    `management_id` int PRIMARY KEY AUTO_INCREMENT,
                                    `theme_id` int NOT NULL,
                                    `operating_time` datetime NOT NULL,
                                    `is_reserved` tinyint(1) NOT NULL DEFAULT 0,
                                    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
                                    `created_by` int,
                                    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
                                    `updated_by` int
);

CREATE TABLE `reservation` (
                               `reservation_id` bigint PRIMARY KEY COMMENT 'snowflake id',
                               `theme_id` int NOT NULL,
                               `customer_name` varchar(100) NOT NULL,
                               `phone_number` varchar(13) NOT NULL,
                               `head_count` tinyint(1) NOT NULL,
                               `reserved_time` datetime NOT NULL,
                               `coupon_code` varchar(8),
                               `is_paid` tinyint(1) NOT NULL DEFAULT 0,
                               `is_notified` tinyint(1) NOT NULL DEFAULT 0,
                               `is_attended` tinyint(1) NOT NULL DEFAULT 0,
                               `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
                               `updated_at` datetime DEFAULT CURRENT_TIMESTAMP,
                               `updated_by` int
);

ALTER TABLE `reservation` ADD FOREIGN KEY (`theme_id`) REFERENCES `theme` (`theme_id`);

ALTER TABLE `theme` ADD FOREIGN KEY (`office_id`) REFERENCES `office` (`office_id`);

ALTER TABLE `account` ADD FOREIGN KEY (`office_id`) REFERENCES `office` (`office_id`);

ALTER TABLE `theme_management` ADD FOREIGN KEY (`theme_id`) REFERENCES `theme` (`theme_id`);

ALTER TABLE `reservation` ADD FOREIGN KEY (`coupon_code`) REFERENCES `coupon` (`code`);

ALTER TABLE `coupon` ADD FOREIGN KEY (`created_by`) REFERENCES `admin` (`admin_id`);
