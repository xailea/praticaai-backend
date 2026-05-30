-- ============================================================
-- PraticaAI — Migration V1: schema iniziale
-- Flyway esegue questo script una sola volta, in ordine.
-- Convenzione: V{versione}__{descrizione}.sql
-- ============================================================

-- ── Tipi ENUM ────────────────────────────────────────────────────────────────
-- Definiamo gli enum come tipi PostgreSQL nativi.
-- Hibernate con EnumType.STRING usa VARCHAR, ma i tipi nativi
-- sono più efficienti e permettono CHECK constraint impliciti.

CREATE TYPE piano_enum AS ENUM ('FREE', 'PRO', 'FAMIGLIA');
CREATE TYPE stato_documento_enum AS ENUM ('PENDING', 'PROCESSING', 'DONE', 'ERROR');
CREATE TYPE urgenza_enum AS ENUM ('ALTA', 'MEDIA', 'BASSA');

-- ── Tabella users ─────────────────────────────────────────────────────────────
-- L'id è l'UUID di Supabase Auth, non auto-generato da noi.

CREATE TABLE users (
    id              UUID            PRIMARY KEY,
    email           VARCHAR(255)    NOT NULL UNIQUE,
    piano           piano_enum      NOT NULL DEFAULT 'FREE',
    documenti_mese  INTEGER         NOT NULL DEFAULT 0,
    reset_mese      DATE            NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users (email);

-- ── Tabella documents ─────────────────────────────────────────────────────────

CREATE TABLE documents (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    nome_file       VARCHAR(255)    NOT NULL,
    tipo_documento  VARCHAR(100),
    storage_path    TEXT            NOT NULL,
    analisi_json    JSONB,
    stato           stato_documento_enum NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_documents_user_id  ON documents (user_id);
CREATE INDEX idx_documents_stato    ON documents (stato);
-- Indice GIN per query sul JSONB (es. tipo_documento, scadenze)
CREATE INDEX idx_documents_analisi  ON documents USING GIN (analisi_json);

-- ── Tabella deadlines ─────────────────────────────────────────────────────────

CREATE TABLE deadlines (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    document_id     UUID            REFERENCES documents(id) ON DELETE SET NULL,
    descrizione     TEXT            NOT NULL,
    data_scadenza   DATE,
    urgenza         urgenza_enum    NOT NULL DEFAULT 'MEDIA',
    notificata      BOOLEAN         NOT NULL DEFAULT FALSE,
    completata      BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_deadlines_user_id          ON deadlines (user_id);
CREATE INDEX idx_deadlines_data_notificata  ON deadlines (data_scadenza, notificata)
    WHERE notificata = FALSE AND completata = FALSE;  -- partial index: solo le righe utili allo scheduler

-- ── Commenti documentativi ────────────────────────────────────────────────────
COMMENT ON TABLE  users                     IS 'Utenti registrati — id sincronizzato con Supabase Auth';
COMMENT ON COLUMN users.reset_mese          IS 'Data del prossimo reset del contatore documenti_mese';
COMMENT ON TABLE  documents                 IS 'Documenti caricati e analizzati da GPT-4o';
COMMENT ON COLUMN documents.analisi_json    IS 'Output strutturato JSON di GPT-4o Vision';
COMMENT ON COLUMN documents.storage_path    IS 'Path nel bucket Supabase Storage';
COMMENT ON TABLE  deadlines                 IS 'Scadenze estratte dai documenti, con reminder email';
