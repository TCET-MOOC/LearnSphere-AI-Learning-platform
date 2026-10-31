-- Migration V27: Add idempotency_key to payouts table (W6-09)
ALTER TABLE public.payouts
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS uk_payouts_idempotency_key
    ON public.payouts (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

