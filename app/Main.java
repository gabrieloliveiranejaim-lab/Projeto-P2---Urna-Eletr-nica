package app;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import dao.BancoDados;
import dao.Boletim;
import modelo.Candidato;
import modelo.CandidatoInvalidoException;
import modelo.Eleitor;
import modelo.EleitorJaVotouException;
import modelo.EleitorNaoEncontradoException;
import modelo.TipoVoto;
import modelo.Urna;
import modelo.UrnaEncerradaException;

public class Main {

    private static final List<Eleitor> eleitores = new ArrayList<>();
    private static final List<Candidato> candidatos = new ArrayList<>();
    private static final List<Urna> urnas = new ArrayList<>();

    private static final Scanner teclado = new Scanner(System.in);
    private static BancoDados banco;

    public static void main(String[] args) {
        try {
            banco = new BancoDados("banco/urna.db");
            carregarDoBanco();
        } catch (SQLException e) {
            System.out.println("Não foi possível abrir o banco de dados: " + e.getMessage());
            System.out.println("Confira se o driver do SQLite (arquivo sqlite-jdbc .jar) "
                    + "está no classpath ao rodar o programa.");
            return;
        }

        System.out.println("Banco aberto. Carregados: " + eleitores.size() + " eleitor(es), "
                + candidatos.size() + " candidato(s) e " + urnas.size() + " urna(s).");

        int opcao = -1;
        while (opcao != 0) {
            mostrarMenu();
            opcao = lerInteiro("Opção: ");
            switch (opcao) {
                case 1:
                    cadastrarEleitor();
                    break;
                case 2:
                    cadastrarCandidato();
                    break;
                case 3:
                    cadastrarUrna();
                    break;
                case 4:
                    votar();
                    break;
                case 5:
                    encerrarUrna();
                    break;
                case 6:
                    listarDados();
                    break;
                case 0:
                    System.out.println("Até logo!");
                    break;
                default:
                    System.out.println("Opção inválida.");
            }
        }
        banco.fechar();
    }

    private static void mostrarMenu() {
        System.out.println();
        System.out.println("===== URNA ELETRÔNICA =====");
        System.out.println("1 - Cadastrar eleitor");
        System.out.println("2 - Cadastrar candidato");
        System.out.println("3 - Cadastrar urna");
        System.out.println("4 - Votar");
        System.out.println("5 - Encerrar urna e gerar boletim");
        System.out.println("6 - Ver dados cadastrados");
        System.out.println("0 - Sair");
    }

    // Lê tudo o que está no banco e coloca nas listas.
    private static void carregarDoBanco() throws SQLException {
        eleitores.clear();
        candidatos.clear();
        urnas.clear();
        banco.carregarTudo(eleitores, candidatos, urnas);
    }


    private static void erroBanco(SQLException e) {
        System.out.println("[ERRO NO BANCO] " + e.getMessage());
        try {
            carregarDoBanco();
            System.out.println("Dados recarregados do banco.");
        } catch (SQLException e2) {
            System.out.println("Não foi possível recarregar os dados: " + e2.getMessage());
        }
    }

    // ---------- cadastros ----------

    private static void cadastrarEleitor() {
        String nome = lerTexto("Nome: ");
        String cpf = lerTexto("CPF: ");
        String titulo = lerTexto("Título de eleitor: ");
        if (buscarEleitor(cpf) != null || buscarEleitor(titulo) != null) {
            System.out.println("Já existe um eleitor com esse CPF ou título.");
            return;
        }
        try {
            Eleitor eleitor = new Eleitor(nome, cpf, titulo);
            banco.salvarEleitor(eleitor);   
            eleitores.add(eleitor);
            System.out.println("Eleitor cadastrado e gravado no banco.");
        } catch (IllegalArgumentException e) {
            System.out.println("Dados inválidos: " + e.getMessage());
        } catch (SQLException e) {
            erroBanco(e);
        }
    }

    private static void cadastrarCandidato() {
        String nome = lerTexto("Nome: ");
        String cpf = lerTexto("CPF: ");
        String titulo = lerTexto("Título de eleitor: ");
        int numero = lerInteiro("Número do candidato: ");
        String cargo = lerTexto("Cargo: ");
        if (buscarEleitor(cpf) != null || buscarEleitor(titulo) != null) {
            System.out.println("Já existe uma pessoa com esse CPF ou título.");
            return;
        }
        if (buscarCandidato(numero) != null) {
            System.out.println("Já existe um candidato com esse número.");
            return;
        }
        try {
            Candidato candidato = new Candidato(nome, cpf, titulo, numero, cargo);
            banco.salvarCandidato(candidato);
            candidatos.add(candidato);
            eleitores.add(candidato);       // todo candidato também é eleitor
            System.out.println("Candidato cadastrado e gravado no banco.");
        } catch (IllegalArgumentException e) {
            System.out.println("Dados inválidos: " + e.getMessage());
        } catch (SQLException e) {
            erroBanco(e);
        }
    }

    private static void cadastrarUrna() {
        if (candidatos.isEmpty()) {
            System.out.println("Cadastre candidatos antes de criar uma urna.");
            return;
        }
        String secao = lerTexto("Seção (ex.: 0042): ");
        if (secao.isEmpty()) {
            System.out.println("A seção é obrigatória.");
            return;
        }

        System.out.println("Candidatos cadastrados:");
        for (Candidato c : candidatos) {
            System.out.println("  " + c);
        }
        String linha = lerTexto("Números dos candidatos desta urna (separados por espaço): ");

        List<Candidato> escolhidos = new ArrayList<>();
        for (String parte : linha.split("\\s+")) {
            try {
                Candidato c = buscarCandidato(Integer.parseInt(parte));
                if (c == null) {
                    System.out.println("Número " + parte + " não existe. Ignorado.");
                } else if (!escolhidos.contains(c)) {
                    escolhidos.add(c);
                }
            } catch (NumberFormatException e) {
                System.out.println("\"" + parte + "\" não é um número. Ignorado.");
            }
        }
        if (escolhidos.isEmpty()) {
            System.out.println("Nenhum candidato válido. Urna não criada.");
            return;
        }

        int id = proximoIdUrna();
        try {
            Urna urna = new Urna(id, secao, escolhidos);
            banco.salvarUrna(urna);
            urnas.add(urna);
            System.out.println("Urna " + id + " cadastrada com " + escolhidos.size()
                    + " candidato(s) e gravada no banco.");
        } catch (SQLException e) {
            erroBanco(e);
        }
    }

 

    private static void votar() {
        Urna urna = escolherUrna();
        if (urna == null) {
            return;
        }
        String documento = lerTexto("Título de eleitor ou CPF: ");
        try {
            Eleitor eleitor = identificar(documento);  
            eleitor.verificarSePodeVotar();             
            pedirVoto(urna, eleitor);
        } catch (EleitorNaoEncontradoException | EleitorJaVotouException
                 | UrnaEncerradaException | CandidatoInvalidoException e) {
            System.out.println("[ERRO] " + e.getMessage());   
        }
    }

    private static void pedirVoto(Urna urna, Eleitor eleitor)
            throws EleitorJaVotouException, UrnaEncerradaException, CandidatoInvalidoException {
        while (true) {
            String entrada = lerTexto("Número do candidato (ou B para branco): ");

            if (entrada.equalsIgnoreCase("B")) {
                System.out.println("Voto em BRANCO.");
                if (confirmar()) {
                    urna.votarBranco();
                    concluirVoto(urna, eleitor, TipoVoto.BRANCO, null);
                    return;
                }
                continue;   // corrige: pergunta de novo
            }

            int numero;
            try {
                numero = Integer.parseInt(entrada);
            } catch (NumberFormatException e) {
                System.out.println("Digite um número ou B.");
                continue;
            }

            Candidato candidato = buscarNaUrna(urna, numero);
            if (candidato == null) {
                System.out.println("AVISO: número inexistente. O voto será NULO.");
            } else {
                System.out.println("Candidato: " + candidato);
            }

            if (confirmar()) {
                TipoVoto tipo;
                if (candidato != null) {
                    tipo = urna.votar(candidato);
                } else {
                    tipo = urna.votar(numero);   // número inexistente vira voto nulo
                }
                concluirVoto(urna, eleitor, tipo, candidato);
                return;
            }
            // se não confirmou, volta ao começo (corrige)
        }
    }


    private static void concluirVoto(Urna urna, Eleitor eleitor, TipoVoto tipo, Candidato candidato)
            throws EleitorJaVotouException {
        eleitor.registrarVoto();
        try {
            banco.registrarVoto(urna, eleitor, tipo, candidato);
            System.out.println("Voto registrado. Obrigado!");
        } catch (SQLException e) {
            System.out.println("Não foi possível gravar o voto. Tente de novo.");
            erroBanco(e);  
        }
    }

    private static boolean confirmar() {
        String resposta = lerTexto("Confirma? (S = confirma, N = corrige): ");
        return resposta.equalsIgnoreCase("S");
    }


    private static void encerrarUrna() {
        Urna urna = escolherUrna();
        if (urna == null) {
            return;
        }
        try {
            if (urna.isEncerrada()) {
                System.out.println("Esta urna já estava encerrada. Mostrando o resultado de novo.");
            } else {
                banco.encerrarUrna(urna.getId());   
                urna.encerrar();
            }
            System.out.println();
            System.out.println(Boletim.gerar(urna));
            String arquivo = Boletim.gravar(urna);
            System.out.println("Boletim gravado no arquivo: " + arquivo);
        } catch (SQLException e) {
            erroBanco(e);
        } catch (IOException e) {
            System.out.println("Não foi possível gravar o boletim: " + e.getMessage());
        }
    }



    private static void listarDados() {
        System.out.println();
        System.out.println("--- Eleitores (" + eleitores.size() + ") ---");
        for (Eleitor e : eleitores) {
            String papel = (e instanceof Candidato) ? "  [candidato]" : "";
            System.out.println("  " + e.getNome() + " | título " + e.getTituloEleitor()
                    + " | já votou: " + (e.isJaVotou() ? "sim" : "não") + papel);
        }
        System.out.println("--- Candidatos (" + candidatos.size() + ") ---");
        for (Candidato c : candidatos) {
            System.out.println("  " + c);
        }
        System.out.println("--- Urnas (" + urnas.size() + ") ---");
        for (Urna u : urnas) {
            System.out.println("  Urna " + u.getId() + " (seção " + u.getSecao() + ", "
                    + (u.isEncerrada() ? "encerrada" : "aberta") + ") - "
                    + u.getCandidatos().size() + " candidato(s), "
                    + u.getTotalVotos() + " voto(s)");
        }
    }


    private static Urna escolherUrna() {
        if (urnas.isEmpty()) {
            System.out.println("Nenhuma urna cadastrada.");
            return null;
        }
        System.out.println("Urnas:");
        for (Urna u : urnas) {
            System.out.println("  Urna " + u.getId() + " (seção " + u.getSecao() + ", "
                    + (u.isEncerrada() ? "encerrada" : "aberta") + ")");
        }
        int id = lerInteiro("Id da urna: ");
        for (Urna u : urnas) {
            if (u.getId() == id) {
                return u;
            }
        }
        System.out.println("Urna não encontrada.");
        return null;
    }

    private static int proximoIdUrna() {
        int maior = 0;
        for (Urna u : urnas) {
            if (u.getId() > maior) {
                maior = u.getId();
            }
        }
        return maior + 1;
    }

    private static Eleitor buscarEleitor(String tituloOuCpf) {
        for (Eleitor e : eleitores) {
            if (e.getTituloEleitor().equals(tituloOuCpf) || e.getCpf().equals(tituloOuCpf)) {
                return e;
            }
        }
        return null;
    }

    private static Eleitor identificar(String tituloOuCpf) throws EleitorNaoEncontradoException {
        Eleitor eleitor = buscarEleitor(tituloOuCpf);
        if (eleitor == null) {
            throw new EleitorNaoEncontradoException(
                    "Título ou CPF '" + tituloOuCpf + "' não cadastrado.");
        }
        return eleitor;
    }

    private static Candidato buscarCandidato(int numero) {
        for (Candidato c : candidatos) {
            if (c.getNumero() == numero) {
                return c;
            }
        }
        return null;
    }

    private static Candidato buscarNaUrna(Urna urna, int numero) {
        for (Candidato c : urna.getCandidatos()) {
            if (c.getNumero() == numero) {
                return c;
            }
        }
        return null;
    }

    private static String lerTexto(String mensagem) {
        System.out.print(mensagem);
        return teclado.nextLine().trim();
    }

    private static int lerInteiro(String mensagem) {
        while (true) {
            try {
                return Integer.parseInt(lerTexto(mensagem));
            } catch (NumberFormatException e) {
                System.out.println("Digite um número inteiro.");
            }
        }
    }
}
