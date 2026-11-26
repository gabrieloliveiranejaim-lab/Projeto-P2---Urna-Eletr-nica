-- Projeto 2: Urna eletrônica (SQLite)
-- Rode PRAGMA foreign_keys = ON em TODA conexão aberta pelo Java,
-- porque o SQLite deixa as chaves estrangeiras desligadas por padrão.
PRAGMA foreign_keys = ON;

-- Eleitor (Pessoa + Eleitor). O ja_votou fica aqui (RN2.1).
CREATE TABLE IF NOT EXISTS eleitor (
    id        INTEGER PRIMARY KEY AUTOINCREMENT,
    nome      TEXT    NOT NULL,
    cpf       TEXT    NOT NULL UNIQUE,
    titulo    TEXT    NOT NULL UNIQUE,
    ja_votou  INTEGER NOT NULL DEFAULT 0 CHECK (ja_votou IN (0, 1))
);

-- Todo candidato também é eleitor: a chave primária é a própria chave do eleitor.
CREATE TABLE IF NOT EXISTS candidato (
    eleitor_id INTEGER PRIMARY KEY REFERENCES eleitor(id),
    numero     INTEGER NOT NULL UNIQUE,
    cargo      TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS urna (
    id      INTEGER PRIMARY KEY AUTOINCREMENT,
    secao   TEXT    NOT NULL,
    estado  TEXT    NOT NULL DEFAULT 'ABERTA' CHECK (estado IN ('ABERTA', 'ENCERRADA'))
);

-- Quais candidatos concorrem em cada urna (agregação).
CREATE TABLE IF NOT EXISTS urna_candidato (
    urna_id      INTEGER NOT NULL REFERENCES urna(id),
    candidato_id INTEGER NOT NULL REFERENCES candidato(eleitor_id),
    PRIMARY KEY (urna_id, candidato_id)
);

-- Voto: NÃO tem nenhuma coluna que diga quem votou (RN2.2).
-- Não acrescente data/hora aqui, para não dar pista de quem votou.
CREATE TABLE IF NOT EXISTS voto (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    urna_id      INTEGER NOT NULL REFERENCES urna(id),
    tipo         TEXT    NOT NULL CHECK (tipo IN ('VALIDO', 'BRANCO', 'NULO')),
    candidato_id INTEGER,
    -- VALIDO exige candidato; BRANCO e NULO não têm candidato.
    CHECK (
        (tipo = 'VALIDO' AND candidato_id IS NOT NULL) OR
        (tipo <> 'VALIDO' AND candidato_id IS NULL)
    ),
    -- O candidato precisa estar ligado à urna (RN2.6).
    -- Com candidato_id vazio (branco/nulo), esta regra não é aplicada.
    FOREIGN KEY (urna_id, candidato_id) REFERENCES urna_candidato(urna_id, candidato_id)
);

-- Exemplos para a apuração (RF2.5 e RF2.6), com ? = id da urna:
--
-- Votos de cada candidato:
--   SELECT candidato_id, COUNT(*) FROM voto
--   WHERE urna_id = ? AND tipo = 'VALIDO' GROUP BY candidato_id;
--
-- Brancos, nulos e total:
--   SELECT tipo, COUNT(*) FROM voto WHERE urna_id = ? GROUP BY tipo;
--   SELECT COUNT(*) FROM voto WHERE urna_id = ?;
