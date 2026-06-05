-- ============================================================
--  Sante Diagnostics LIMS – PostgreSQL Schema
--  Run this once against your target database:
--    psql -U postgres -d sante_lims -f schema.sql
-- ============================================================

-- Enable pgcrypto for gen_random_uuid() if not already enabled
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- 1. USERS
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name        VARCHAR(150) NOT NULL,
    email            VARCHAR(255) NOT NULL UNIQUE,
    password_hash    VARCHAR(255) NOT NULL,
    role             VARCHAR(20)  NOT NULL CHECK (role IN ('SUPER_ADMIN','LAB_ATTENDANT','CUSTOMER')),
    email_verified   BOOLEAN      NOT NULL DEFAULT FALSE,
    verify_token     VARCHAR(255),                     -- email verification token
    force_pw_change  BOOLEAN      NOT NULL DEFAULT FALSE, -- TRUE for staff-created accounts
    created_by       UUID         REFERENCES users(id),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 2. PASSWORD RESET TOKENS (optional but good practice)
-- ============================================================
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    used        BOOLEAN     NOT NULL DEFAULT FALSE
);

-- ============================================================
-- 3. TEST CATALOG
-- ============================================================
CREATE TABLE IF NOT EXISTS test_types (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(150) NOT NULL UNIQUE,
    category        VARCHAR(100) NOT NULL,              -- e.g. Blood, Imaging, Biopsy
    price           NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    tat_hours       INTEGER NOT NULL CHECK (tat_hours > 0), -- Standard Turnaround Time in hours
    result_format   VARCHAR(20)  NOT NULL CHECK (result_format IN ('NUMERIC','TEXT','PDF','IMAGE')),
    description     TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_by      UUID REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 4. TEST REQUESTS
-- ============================================================
CREATE TABLE IF NOT EXISTS test_requests (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id      UUID        NOT NULL REFERENCES users(id),
    test_type_id     UUID        NOT NULL REFERENCES test_types(id),
    payment_status   VARCHAR(20) NOT NULL DEFAULT 'UNPAID' CHECK (payment_status IN ('UNPAID','PAID')),
    payment_marked_by UUID       REFERENCES users(id),  -- who marked it paid
    payment_marked_at TIMESTAMPTZ,
    status           VARCHAR(30) NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING','SAMPLE_COLLECTED','PROCESSING','VALIDATING','COMPLETED','CANCELLED')),
    result_ready_at  TIMESTAMPTZ,                       -- set when COMPLETED; drives countdown timer
    notes            TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 5. SAMPLE LIFECYCLE
-- ============================================================
CREATE TABLE IF NOT EXISTS sample_events (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id      UUID        NOT NULL REFERENCES test_requests(id) ON DELETE CASCADE,
    status          VARCHAR(30) NOT NULL
                    CHECK (status IN ('COLLECTED','RECEIVED','PROCESSING','VALIDATED','REJECTED')),
    notes           TEXT,
    recorded_by     UUID        NOT NULL REFERENCES users(id),
    recorded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 6. RESULTS
-- ============================================================
CREATE TABLE IF NOT EXISTS results (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id      UUID        NOT NULL UNIQUE REFERENCES test_requests(id) ON DELETE CASCADE,
    result_format   VARCHAR(20) NOT NULL CHECK (result_format IN ('NUMERIC','TEXT','PDF','IMAGE')),
    numeric_value   NUMERIC,                            -- used when format = NUMERIC
    text_value      TEXT,                               -- used when format = TEXT
    file_path       VARCHAR(500),                       -- absolute path on server; PDF or IMAGE
    is_validated    BOOLEAN NOT NULL DEFAULT FALSE,
    validated_by    UUID REFERENCES users(id),
    validated_at    TIMESTAMPTZ,
    notification_sent BOOLEAN NOT NULL DEFAULT FALSE,
    uploaded_by     UUID NOT NULL REFERENCES users(id),
    uploaded_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 7. AUDIT TRAIL  (immutable – no UPDATE or DELETE allowed)
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_log (
    id          BIGSERIAL   PRIMARY KEY,
    user_id     UUID        REFERENCES users(id),  -- NULL for system events
    action      VARCHAR(100) NOT NULL,             -- e.g. LOGIN, CREATE_TEST, MARK_PAID, UPLOAD_RESULT
    entity_type VARCHAR(50),                       -- e.g. test_requests, results, users
    entity_id   UUID,
    detail      TEXT,                              -- human-readable description
    ip_address  VARCHAR(45),
    logged_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Prevent any updates or deletes on audit_log
CREATE OR REPLACE RULE audit_no_update AS ON UPDATE TO audit_log DO INSTEAD NOTHING;
CREATE OR REPLACE RULE audit_no_delete AS ON DELETE TO audit_log DO INSTEAD NOTHING;

-- ============================================================
-- 8. BANK ACCOUNT DETAILS (shown to customer after order)
-- ============================================================
CREATE TABLE IF NOT EXISTS bank_details (
    id           SERIAL      PRIMARY KEY,
    bank_name    VARCHAR(100) NOT NULL,
    account_name VARCHAR(150) NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    sort_code    VARCHAR(20),
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================
-- INDEXES
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_users_email         ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role          ON users(role);
CREATE INDEX IF NOT EXISTS idx_test_requests_customer ON test_requests(customer_id);
CREATE INDEX IF NOT EXISTS idx_test_requests_status   ON test_requests(status);
CREATE INDEX IF NOT EXISTS idx_test_requests_payment  ON test_requests(payment_status);
CREATE INDEX IF NOT EXISTS idx_sample_events_request  ON sample_events(request_id);
CREATE INDEX IF NOT EXISTS idx_results_request        ON results(request_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_user         ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_entity       ON audit_log(entity_type, entity_id);

-- ============================================================
-- SEED: Default Super Admin
--   password: Admin@1234  (BCrypt hash – change on first login)
-- ============================================================
INSERT INTO users (full_name, email, password_hash, role, email_verified, force_pw_change)
VALUES (
    'Super Admin',
    'admin@santediagnostics.com',
    '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', -- bcrypt of "Admin@1234"
    'SUPER_ADMIN',
    TRUE,
    FALSE
)
ON CONFLICT (email) DO NOTHING;

-- ============================================================
-- SEED: Bank Details
-- ============================================================
INSERT INTO bank_details (bank_name, account_name, account_number, sort_code)
VALUES ('First Bank Nigeria', 'Sante Diagnostics Ltd', '3012345678', NULL)
ON CONFLICT DO NOTHING;
