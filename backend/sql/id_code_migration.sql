-- ═══════════════════════════════════════════════════════════════════
-- ID Code Migration
-- Run ONCE on any existing database to:
-- 1. Add organizer_code column if missing
-- 2. Fix users with NULL user_code  → USR0001, USR0002 …
-- 3. Fix users with old format USR-XXXX → USRXXXX
-- 4. Fix organizers with NULL organizer_code → ORG0001, ORG0002 …
-- 5. Fix organizers with old format ORG-XXXX → ORGXXXX
-- ═══════════════════════════════════════════════════════════════════

USE event_booking_db;

-- ── 1. Add organizer_code column if it doesn't exist ────────────────
ALTER TABLE organizers
    ADD COLUMN IF NOT EXISTS organizer_code VARCHAR(30) NULL;

ALTER TABLE organizers
    ADD UNIQUE INDEX IF NOT EXISTS idx_org_public_id (organizer_code);

-- ── 2. Fix NULL user_code ──────────────────────────────────────────
UPDATE users
SET user_code = CONCAT('USR', LPAD(id, 4, '0'))
WHERE user_code IS NULL OR user_code = '';

-- ── 3. Fix old USR-XXXX format → USRXXXX ──────────────────────────
UPDATE users
SET user_code = REPLACE(user_code, 'USR-', 'USR')
WHERE user_code LIKE 'USR-%';

-- ── 4. Fix NULL organizer_code ──────────────────────────────────────
UPDATE organizers
SET organizer_code = CONCAT('ORG', LPAD(id, 4, '0'))
WHERE organizer_code IS NULL OR organizer_code = '';

-- ── 5. Fix old ORG-XXXX format → ORGXXXX ──────────────────────────
UPDATE organizers
SET organizer_code = REPLACE(organizer_code, 'ORG-', 'ORG')
WHERE organizer_code LIKE 'ORG-%';

-- ── 6. Verify results ─────────────────────────────────────────────
SELECT 'Users without code:' AS check_name, COUNT(*) AS count
FROM users WHERE user_code IS NULL OR user_code = ''
UNION ALL
SELECT 'Organizers without code:', COUNT(*)
FROM organizers WHERE organizer_code IS NULL OR organizer_code = ''
UNION ALL
SELECT 'Users with old USR- format:', COUNT(*)
FROM users WHERE user_code LIKE 'USR-%'
UNION ALL
SELECT 'Organizers with old ORG- format:', COUNT(*)
FROM organizers WHERE organizer_code LIKE 'ORG-%';
