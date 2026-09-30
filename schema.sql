-- OPTIONAL: the app creates all of this automatically on first start.
CREATE DATABASE IF NOT EXISTS expense_manager;
USE expense_manager;

CREATE TABLE IF NOT EXISTS transactions (
  id INT AUTO_INCREMENT PRIMARY KEY,
  type ENUM('INCOME','EXPENSE') NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  category VARCHAR(50) NOT NULL,
  description VARCHAR(255),
  tx_date DATE NOT NULL,
  INDEX idx_date (tx_date)
);

CREATE TABLE IF NOT EXISTS budgets (
  id INT AUTO_INCREMENT PRIMARY KEY,
  category VARCHAR(50) NOT NULL,
  budget_month CHAR(7) NOT NULL,
  limit_amount DECIMAL(12,2) NOT NULL,
  UNIQUE KEY uq_cat_month (category, budget_month)
);
