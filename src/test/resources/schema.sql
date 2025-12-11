-- ===========================
-- Table: User
-- ===========================
CREATE TABLE `user` (
                        `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                        `point` BIGINT NOT NULL DEFAULT 0,
                        `created_at` DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                        `updated_at` DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===========================
-- Table: Product
-- ===========================
CREATE TABLE `product` (
                           `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                           `product_name` VARCHAR(255),
                           `description` TEXT,
                           `price` BIGINT,
                           `original_stock_quantity` INT,
                           `stock_quantity` INT,
                           `view_count` INT NOT NULL DEFAULT 0,
                           `created_at` DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                           `updated_at` DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===========================
-- Table: Coupon
-- ===========================
CREATE TABLE `coupon` (
                          `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                          `name` VARCHAR(255),
                          `discount_type` VARCHAR(50),
                          `discount_value` BIGINT,
                          `total_quantity` INT,
                          `issued_quantity` INT,
                          `valid_from` DATETIME,
                          `valid_until` DATETIME,
                          `created_at` DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                          `updated_at` DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===========================
-- Table: UserCoupon
-- ===========================
CREATE TABLE `user_coupon` (
                               `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                               `user_id` BIGINT NOT NULL,
                               `coupon_id` BIGINT NOT NULL,
                               `order_id` BIGINT,
                               `status` VARCHAR(50) NOT NULL DEFAULT 'ISSUED',
                               `created_at` DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                               `updated_at` DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                               `version` BIGINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===========================
-- Table: CartItem
-- ===========================
CREATE TABLE `cart_item` (
                             `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                             `user_id` BIGINT NOT NULL,
                             `product_id` BIGINT NOT NULL,
                             `product_name` VARCHAR(255),
                             `price` BIGINT,
                             `quantity` INT,
                             `created_at` DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                             `updated_at` DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===========================
-- Table: Order
-- ===========================
CREATE TABLE `order_table` (
                               `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                               `user_id` BIGINT NOT NULL,
                               `user_coupon_id` BIGINT,
                               `total_amount` BIGINT,
                               `discount_amount` BIGINT,
                               `status` VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                               `orderer_name` VARCHAR(255),
                               `delivery_address` VARCHAR(255),
                               `created_at` DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                               `updated_at` DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===========================
-- Table: OrderItem
-- ===========================
CREATE TABLE `order_item` (
                              `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                              `order_id` BIGINT NOT NULL,
                              `product_id` BIGINT NOT NULL,
                              `quantity` INT,
                              `product_name` VARCHAR(255),
                              `price` BIGINT,
                              `created_at` DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                              `updated_at` DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===========================
-- Table: PointHistory
-- ===========================
CREATE TABLE `point_history` (
                                 `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 `user_id` BIGINT NOT NULL,
                                 `order_id` BIGINT,
                                 `transaction_type` VARCHAR(50),
                                 `amount` BIGINT,
                                 `balance_after` BIGINT,
                                 `created_at` DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                                 `updated_at` DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;