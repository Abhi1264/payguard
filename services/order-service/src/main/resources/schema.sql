CREATE SCHEMA IF NOT EXISTS ordersvc;

CREATE TABLE IF NOT EXISTS ordersvc.orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
