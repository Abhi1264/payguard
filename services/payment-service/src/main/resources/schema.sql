CREATE SCHEMA IF NOT EXISTS paymentsvc;

CREATE TABLE IF NOT EXISTS paymentsvc.payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    user_id UUID NOT NULL,
    amount NUMERIC(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    provider_transaction_id VARCHAR(64),
    authorized_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS payments_open_order_idx
    ON paymentsvc.payments (order_id)
    WHERE status <> 'FAILED';
