-- COSC 3P91 Lab 6 - Persistent Coffee Management System
-- Schema: COFFEES table with relational integrity rules

CREATE TABLE IF NOT EXISTS COFFEES (
    COF_ID   INT          NOT NULL,
    COF_NAME VARCHAR(32)  NOT NULL UNIQUE,
    SUP_NAME VARCHAR(40)  NOT NULL,
    PRICE    DECIMAL(10,2) NOT NULL,
    SALES    INT          NOT NULL DEFAULT 0,
    TOTAL    INT          NOT NULL DEFAULT 0,
    CONSTRAINT pk_coffees PRIMARY KEY (COF_ID),
    CONSTRAINT chk_price  CHECK (PRICE >= 0),
    CONSTRAINT chk_sales  CHECK (SALES >= 0),
    CONSTRAINT chk_total  CHECK (TOTAL >= 0)
);
