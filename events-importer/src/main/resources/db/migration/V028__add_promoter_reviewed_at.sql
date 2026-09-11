-- When a person last reviewed a promoter row (#1336): the name matches the promoter's own
-- spelling, the kind is decided, the website is recorded or absent on purpose. NULL means nobody
-- looked, which is every row an import mints — the importer never sets this, only the admin API
-- does, through `scripts/promoter-websites.py` writing docs/promoters/REVIEWED.tsv. A timestamp
-- rather than a flag, so a row reviewed before a normalizer change can be told from one after.
--
-- Unqualified table name, deliberately: Flyway sets `search_path` from `spring.flyway.schemas`
-- before running this (ADR-004).
ALTER TABLE promoter
    ADD COLUMN reviewed_at TIMESTAMPTZ;
