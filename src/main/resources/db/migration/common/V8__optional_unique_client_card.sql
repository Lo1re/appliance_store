-- Preserve existing assignments. Duplicate non-empty numbers must be resolved explicitly.
ALTER TABLE client ALTER COLUMN card DROP NOT NULL;
UPDATE client SET card = NULLIF(TRIM(card), '');
ALTER TABLE client ADD CONSTRAINT uq_client_card UNIQUE (card);
ALTER TABLE client ADD CONSTRAINT ck_client_card_canonical CHECK (card IS NULL OR (card = TRIM(card) AND card <> ''));
