package dao;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import modelo.Candidato;
import modelo.Eleitor;
import modelo.TipoVoto;
import modelo.Urna;

// Toda a conversa com o banco SQLite fica nesta classe.
// O resto do programa (Main, Urna, Eleitor...) não sabe nada de SQL.
public class BancoDados {

    // ------------------------------------------------------------------
    // Comandos SQL (as tabelas do enunciado)
    // ------------------------------------------------------------------

    private static final String SQL_CRIAR_ELEITOR =
            "CREATE TABLE IF NOT EXISTS eleitor ("
            + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " nome TEXT NOT NULL,"
            + " cpf TEXT NOT NULL UNIQUE,"
            + " titulo TEXT NOT NULL UNIQUE,"
            + " ja_votou INTEGER NOT NULL DEFAULT 0 CHECK (ja_votou IN (0, 1)))";

    // Todo candidato também é eleitor: a chave é a própria chave do eleitor.
    private static final String SQL_CRIAR_CANDIDATO =
            "CREATE TABLE IF NOT EXISTS candidato ("
            + " eleitor_id INTEGER PRIMARY KEY REFERENCES eleitor(id),"
            + " numero INTEGER NOT NULL UNIQUE,"
            + " cargo TEXT NOT NULL)";

    private static final String SQL_CRIAR_URNA =
            "CREATE TABLE IF NOT EXISTS urna ("
            + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " secao TEXT NOT NULL,"
            + " estado TEXT NOT NULL DEFAULT 'ABERTA' CHECK (estado IN ('ABERTA', 'ENCERRADA')))";

    private static final String SQL_CRIAR_URNA_CANDIDATO =
            "CREATE TABLE IF NOT EXISTS urna_candidato ("
            + " urna_id INTEGER NOT NULL REFERENCES urna(id),"
            + " candidato_id INTEGER NOT NULL REFERENCES candidato(eleitor_id),"
            + " PRIMARY KEY (urna_id, candidato_id))";

    // A tabela voto NÃO tem nenhuma coluna que diga quem votou (RN2.2).
    private static final String SQL_CRIAR_VOTO =
            "CREATE TABLE IF NOT EXISTS voto ("
            + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " urna_id INTEGER NOT NULL REFERENCES urna(id),"
            + " tipo TEXT NOT NULL CHECK (tipo IN ('VALIDO', 'BRANCO', 'NULO')),"
            + " candidato_id INTEGER,"
            + " CHECK ((tipo = 'VALIDO' AND candidato_id IS NOT NULL)"
            + "     OR (tipo <> 'VALIDO' AND candidato_id IS NULL)),"
            + " FOREIGN KEY (urna_id, candidato_id)"
            + "     REFERENCES urna_candidato(urna_id, candidato_id))";

    private static final String SQL_INSERIR_ELEITOR =
            "INSERT INTO eleitor (nome, cpf, titulo, ja_votou) VALUES (?, ?, ?, ?)";

    private static final String SQL_ID_DO_ELEITOR =
            "SELECT id FROM eleitor WHERE cpf = ?";

    private static final String SQL_INSERIR_CANDIDATO =
            "INSERT INTO candidato (eleitor_id, numero, cargo) VALUES (?, ?, ?)";

    private static final String SQL_INSERIR_URNA =
            "INSERT INTO urna (id, secao, estado) VALUES (?, ?, 'ABERTA')";

    private static final String SQL_INSERIR_URNA_CANDIDATO =
            "INSERT INTO urna_candidato (urna_id, candidato_id) VALUES (?, ?)";

    private static final String SQL_INSERIR_VOTO =
            "INSERT INTO voto (urna_id, tipo, candidato_id) VALUES (?, ?, ?)";

    // O "AND ja_votou = 0" faz o próprio banco recusar um segundo voto do mesmo eleitor.
    private static final String SQL_MARCAR_JA_VOTOU =
            "UPDATE eleitor SET ja_votou = 1 WHERE cpf = ? AND ja_votou = 0";

    private static final String SQL_ENCERRAR_URNA =
            "UPDATE urna SET estado = 'ENCERRADA' WHERE id = ?";

    private static final String SQL_LER_ELEITORES =
            "SELECT e.id, e.nome, e.cpf, e.titulo, e.ja_votou, c.numero, c.cargo"
            + " FROM eleitor e LEFT JOIN candidato c ON c.eleitor_id = e.id"
            + " ORDER BY e.id";

    private static final String SQL_LER_URNAS =
            "SELECT id, secao, estado FROM urna ORDER BY id";

    private static final String SQL_LER_CANDIDATOS_DA_URNA =
            "SELECT candidato_id FROM urna_candidato WHERE urna_id = ? ORDER BY candidato_id";

    private static final String SQL_LER_VOTOS_DA_URNA =
            "SELECT tipo, candidato_id FROM voto WHERE urna_id = ? ORDER BY id";

    // ------------------------------------------------------------------

    private final Connection conexao;

    // O banco é criado na primeira execução: a pasta e o arquivo são criados se não existirem,
    // e as tabelas são criadas com CREATE TABLE IF NOT EXISTS.
    public BancoDados(String arquivo) throws SQLException {
        criarPastaDoArquivo(arquivo);
        conexao = DriverManager.getConnection("jdbc:sqlite:" + arquivo);
        try (Statement st = conexao.createStatement()) {
            // O SQLite deixa as chaves estrangeiras DESLIGADAS por padrão. Ligamos aqui.
            st.execute("PRAGMA foreign_keys = ON");
            st.execute(SQL_CRIAR_ELEITOR);
            st.execute(SQL_CRIAR_CANDIDATO);
            st.execute(SQL_CRIAR_URNA);
            st.execute(SQL_CRIAR_URNA_CANDIDATO);
            st.execute(SQL_CRIAR_VOTO);
        }
    }

    private static void criarPastaDoArquivo(String arquivo) throws SQLException {
        Path pasta = Paths.get(arquivo).toAbsolutePath().getParent();
        try {
            Files.createDirectories(pasta);
        } catch (IOException e) {
            throw new SQLException("Não foi possível criar a pasta do banco: " + e.getMessage());
        }
    }

    public void fechar() {
        try {
            conexao.close();
        } catch (SQLException e) {
            // não há mais o que fazer
        }
    }

    // ------------------------------------------------------------------
    // GRAVAR
    // ------------------------------------------------------------------

    public void salvarEleitor(Eleitor eleitor) throws SQLException {
        inserirEleitor(eleitor);
    }

    // Candidato = linha em "eleitor" + linha em "candidato". As duas ou nenhuma.
    public void salvarCandidato(Candidato candidato) throws SQLException {
        conexao.setAutoCommit(false);
        try {
            int eleitorId = inserirEleitor(candidato);
            try (PreparedStatement ps = conexao.prepareStatement(SQL_INSERIR_CANDIDATO)) {
                ps.setInt(1, eleitorId);
                ps.setInt(2, candidato.getNumero());
                ps.setString(3, candidato.getCargo());
                ps.executeUpdate();
            }
            conexao.commit();
        } catch (SQLException e) {
            conexao.rollback();
            throw e;
        } finally {
            conexao.setAutoCommit(true);
        }
    }

    public void salvarUrna(Urna urna) throws SQLException {
        conexao.setAutoCommit(false);
        try {
            try (PreparedStatement ps = conexao.prepareStatement(SQL_INSERIR_URNA)) {
                ps.setInt(1, urna.getId());
                ps.setString(2, urna.getSecao());
                ps.executeUpdate();
            }
            for (Candidato c : urna.getCandidatos()) {
                try (PreparedStatement ps = conexao.prepareStatement(SQL_INSERIR_URNA_CANDIDATO)) {
                    ps.setInt(1, urna.getId());
                    ps.setInt(2, idDoEleitor(c.getCpf()));
                    ps.executeUpdate();
                }
            }
            conexao.commit();
        } catch (SQLException e) {
            conexao.rollback();
            throw e;
        } finally {
            conexao.setAutoCommit(true);
        }
    }

    // RN2.5: gravar o voto e marcar "já votou" acontecem JUNTOS.
    // Se um dos dois falhar, o rollback desfaz o outro.
    // Repare: o voto é gravado SEM dizer quem votou (RN2.2).
    public void registrarVoto(Urna urna, Eleitor eleitor, TipoVoto tipo, Candidato candidato)
            throws SQLException {
        conexao.setAutoCommit(false);
        try {
            try (PreparedStatement ps = conexao.prepareStatement(SQL_INSERIR_VOTO)) {
                ps.setInt(1, urna.getId());
                ps.setString(2, tipo.name());
                if (candidato == null) {
                    ps.setNull(3, Types.INTEGER);
                } else {
                    ps.setInt(3, idDoEleitor(candidato.getCpf()));
                }
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conexao.prepareStatement(SQL_MARCAR_JA_VOTOU)) {
                ps.setString(1, eleitor.getCpf());
                if (ps.executeUpdate() != 1) {
                    throw new SQLException("O eleitor já consta como tendo votado.");
                }
            }
            conexao.commit();
        } catch (SQLException e) {
            conexao.rollback();
            throw e;
        } finally {
            conexao.setAutoCommit(true);
        }
    }

    public void encerrarUrna(int urnaId) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(SQL_ENCERRAR_URNA)) {
            ps.setInt(1, urnaId);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------
    // LER
    // ------------------------------------------------------------------

    // Preenche as três listas do Main com o que está gravado no banco.
    public void carregarTudo(List<Eleitor> eleitores, List<Candidato> candidatos,
                             List<Urna> urnas) throws SQLException {

        // Guarda cada candidato pelo id do banco, para montar as urnas depois.
        Map<Integer, Candidato> candidatoPorId = new HashMap<>();

        try (PreparedStatement ps = conexao.prepareStatement(SQL_LER_ELEITORES);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String nome = rs.getString("nome");
                String cpf = rs.getString("cpf");
                String titulo = rs.getString("titulo");
                boolean jaVotou = rs.getInt("ja_votou") == 1;
                int numero = rs.getInt("numero");
                boolean ehCandidato = !rs.wasNull();

                if (ehCandidato) {
                    Candidato c = new Candidato(nome, cpf, titulo, numero,
                            rs.getString("cargo"), jaVotou);
                    candidatos.add(c);
                    eleitores.add(c);
                    candidatoPorId.put(id, c);
                } else {
                    eleitores.add(new Eleitor(nome, cpf, titulo, jaVotou));
                }
            }
        }

        // Primeiro lê as urnas, depois (já fechado o resultado) lê os detalhes de cada uma.
        List<String[]> linhasUrnas = new ArrayList<>();
        try (PreparedStatement ps = conexao.prepareStatement(SQL_LER_URNAS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                linhasUrnas.add(new String[] {
                    String.valueOf(rs.getInt("id")), rs.getString("secao"), rs.getString("estado")
                });
            }
        }

        for (String[] linha : linhasUrnas) {
            int urnaId = Integer.parseInt(linha[0]);

            List<Candidato> daUrna = new ArrayList<>();
            try (PreparedStatement ps = conexao.prepareStatement(SQL_LER_CANDIDATOS_DA_URNA)) {
                ps.setInt(1, urnaId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        daUrna.add(candidatoPorId.get(rs.getInt("candidato_id")));
                    }
                }
            }

            Urna urna = new Urna(urnaId, linha[1], "ENCERRADA".equals(linha[2]), daUrna);

            try (PreparedStatement ps = conexao.prepareStatement(SQL_LER_VOTOS_DA_URNA)) {
                ps.setInt(1, urnaId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        TipoVoto tipo = TipoVoto.valueOf(rs.getString("tipo"));
                        int candidatoId = rs.getInt("candidato_id");
                        Candidato c = rs.wasNull() ? null : candidatoPorId.get(candidatoId);
                        urna.carregarVoto(tipo, c);
                    }
                }
            }
            urnas.add(urna);
        }
    }

    // ------------------------------------------------------------------
    // Ajudas internas
    // ------------------------------------------------------------------

    private int inserirEleitor(Eleitor eleitor) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(SQL_INSERIR_ELEITOR)) {
            ps.setString(1, eleitor.getNome());
            ps.setString(2, eleitor.getCpf());
            ps.setString(3, eleitor.getTituloEleitor());
            ps.setInt(4, eleitor.isJaVotou() ? 1 : 0);
            ps.executeUpdate();
        }
        return idDoEleitor(eleitor.getCpf());
    }

    private int idDoEleitor(String cpf) throws SQLException {
        try (PreparedStatement ps = conexao.prepareStatement(SQL_ID_DO_ELEITOR)) {
            ps.setString(1, cpf);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        throw new SQLException("Eleitor com CPF " + cpf + " não encontrado no banco.");
    }
}
