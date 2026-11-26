import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) {

        List<Eleitor> eleitores = new ArrayList<>();
        List<Candidato> candidatos = new ArrayList<>();
        List<Urna> urnas = new ArrayList<>();

        candidatos.add(new Candidato("Ana Souza",    "111.111.111-11", "TE-0001", 10, "Prefeito"));
        candidatos.add(new Candidato("Bruno Lima",   "222.222.222-22", "TE-0002", 20, "Prefeito"));
        candidatos.add(new Candidato("Carla Mendes", "333.333.333-33", "TE-0003", 30, "Prefeito"));

        eleitores.addAll(candidatos);
        eleitores.add(new Eleitor("Diego Alves",    "444.444.444-44", "TE-0004"));
        eleitores.add(new Eleitor("Elisa Rocha",    "555.555.555-55", "TE-0005"));
        eleitores.add(new Eleitor("Fabio Nunes",    "666.666.666-66", "TE-0006"));
        eleitores.add(new Eleitor("Gabriela Pires", "777.777.777-77", "TE-0007"));
        eleitores.add(new Eleitor("Hugo Teixeira",  "888.888.888-88", "TE-0008"));
        eleitores.add(new Eleitor("Iara Campos",    "999.999.999-99", "TE-0009"));

        Urna urna = new Urna(1, "0042", candidatos);
        urnas.add(urna);

        titulo("CADASTROS");
        System.out.println("Eleitores cadastrados: " + eleitores.size()
                + " (" + candidatos.size() + " deles são candidatos)");
        System.out.println("Candidatos na urna " + urna.getId()
                + " (seção " + urna.getSecao() + "):");
        for (Candidato c : urna.getCandidatos()) {
            System.out.println("  - " + c);
        }

        titulo("VOTAÇÃO");

        votarPorNumero(urna, eleitores, "TE-0004", 10);
        votarPorNumero(urna, eleitores, "555.555.555-55", 20);
        votarPorNumero(urna, eleitores, "TE-0006", 99);
        votarEmBranco(urna, eleitores, "TE-0007");
        votarPorNumero(urna, eleitores, "TE-0009", 10);
        votarPorNumero(urna, eleitores, "TE-0001", 10);
        votarPorNumero(urna, eleitores, "TE-0002", 20);
        votarPorNumero(urna, eleitores, "TE-0003", 30);

        titulo("TESTES DAS REGRAS");

        System.out.println("-> Diego tenta votar de novo (RN2.1):");
        int antes = urna.getTotalVotos();
        votarPorNumero(urna, eleitores, "TE-0004", 20);
        System.out.println("   Total de votos antes: " + antes
                + " | depois: " + urna.getTotalVotos() + " (não mudou)");

        System.out.println("-> Eleitor não cadastrado tenta votar (RN2.6):");
        votarPorNumero(urna, eleitores, "TE-9999", 10);

        System.out.println("-> Hugo tenta votar em candidato que não concorre na urna:");
        Candidato forasteiro = new Candidato("Zeca Lopes", "000.000.000-00", "TE-0099", 40, "Prefeito");
        try {
            Eleitor hugo = identificar(eleitores, "TE-0008");
            hugo.verificarSePodeVotar();
            urna.votar(forasteiro);
            hugo.registrarVoto();
        } catch (EleitorNaoEncontradoException | EleitorJaVotouException
                 | UrnaEncerradaException | CandidatoInvalidoException e) {
            System.out.println("   [ERRO] " + e.getMessage());
        }
        System.out.println("   Hugo já votou? " + buscarOuNull(eleitores, "TE-0008").isJaVotou()
                + " (continua podendo votar)");

        System.out.println("-> Tentar contar os votos com a urna ainda aberta:");
        try {
            urna.contabilizarVotos();
        } catch (IllegalStateException e) {
            System.out.println("   [ERRO] " + e.getMessage());
        }

        titulo("ENCERRAMENTO");
        urna.encerrar();
        System.out.println("Urna " + urna.getId() + " encerrada.");

        System.out.println("-> Hugo tenta votar depois do encerramento (RN2.3):");
        votarPorNumero(urna, eleitores, "TE-0008", 30);
        System.out.println("   Hugo já votou? " + buscarOuNull(eleitores, "TE-0008").isJaVotou());

        titulo("APURAÇÃO - URNA " + urna.getId() + " / SEÇÃO " + urna.getSecao());

        Map<Candidato, Integer> resultado = urna.contabilizarVotos();
        int somaCandidatos = 0;
        for (Map.Entry<Candidato, Integer> linha : resultado.entrySet()) {
            System.out.printf("%-38s %3d%n", linha.getKey(), linha.getValue());
            somaCandidatos += linha.getValue();
        }
        int brancos = urna.contarBrancos();
        int nulos = urna.contarNulos();
        System.out.printf("%-38s %3d%n", "Brancos", brancos);
        System.out.printf("%-38s %3d%n", "Nulos", nulos);
        System.out.printf("%-38s %3d%n", "TOTAL GERAL", urna.getTotalVotos());

        int quemVotou = 0;
        for (Eleitor e : eleitores) {
            if (e.isJaVotou()) {
                quemVotou++;
            }
        }
        System.out.println();
        System.out.println("Conferência: " + somaCandidatos + " + " + brancos + " + " + nulos
                + " = " + (somaCandidatos + brancos + nulos)
                + " | eleitores que votaram: " + quemVotou
                + (somaCandidatos + brancos + nulos == quemVotou ? "  [OK]" : "  [DIVERGENTE]"));
    }

  
    private static void votarPorNumero(Urna urna, List<Eleitor> eleitores,
                                       String tituloOuCpf, int numero) {
        try {
            Eleitor eleitor = identificar(eleitores, tituloOuCpf);
            eleitor.verificarSePodeVotar();
            TipoVoto tipo = urna.votar(numero);
            eleitor.registrarVoto();
            System.out.println("  " + eleitor.getNome() + ": voto registrado.");
            if (tipo == TipoVoto.NULO) {
                System.out.println("    [AVISO] Número " + numero
                        + " não existe. O voto foi contado como NULO.");
            }
        } catch (EleitorNaoEncontradoException | EleitorJaVotouException
                 | UrnaEncerradaException e) {
            System.out.println("   [ERRO] " + e.getMessage());
        }
    }

    private static void votarEmBranco(Urna urna, List<Eleitor> eleitores, String tituloOuCpf) {
        try {
            Eleitor eleitor = identificar(eleitores, tituloOuCpf);
            eleitor.verificarSePodeVotar();
            urna.votarBranco();
            eleitor.registrarVoto();
            System.out.println("  " + eleitor.getNome() + ": voto registrado.");
        } catch (EleitorNaoEncontradoException | EleitorJaVotouException
                 | UrnaEncerradaException e) {
            System.out.println("   [ERRO] " + e.getMessage());
        }
    }

    private static Eleitor identificar(List<Eleitor> eleitores, String tituloOuCpf)
            throws EleitorNaoEncontradoException {
        for (Eleitor e : eleitores) {
            if (e.getTituloEleitor().equals(tituloOuCpf) || e.getCpf().equals(tituloOuCpf)) {
                return e;
            }
        }
        throw new EleitorNaoEncontradoException(
                "Título ou CPF '" + tituloOuCpf + "' não cadastrado.");
    }

    private static Eleitor buscarOuNull(List<Eleitor> eleitores, String tituloOuCpf) {
        try {
            return identificar(eleitores, tituloOuCpf);
        } catch (EleitorNaoEncontradoException e) {
            return null;
        }
    }

    private static void titulo(String texto) {
        System.out.println();
        System.out.println("=== " + texto + " ===");
    }
}

class EleitorJaVotouException extends Exception {

    private static final long serialVersionUID = 1L;

    public EleitorJaVotouException(String mensagem) {
        super(mensagem);
    }
}

class UrnaEncerradaException extends Exception {

    private static final long serialVersionUID = 1L;

    public UrnaEncerradaException(String mensagem) {
        super(mensagem);
    }
}

class CandidatoInvalidoException extends Exception {

    private static final long serialVersionUID = 1L;

    public CandidatoInvalidoException(String mensagem) {
        super(mensagem);
    }
}

class EleitorNaoEncontradoException extends Exception {

    private static final long serialVersionUID = 1L;

    public EleitorNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}

class Pessoa {

    private final String nome;
    private final String cpf;

    public Pessoa(String nome, String cpf) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome é obrigatório.");
        }
        if (cpf == null || cpf.trim().isEmpty()) {
            throw new IllegalArgumentException("CPF é obrigatório.");
        }
        this.nome = nome;
        this.cpf = cpf;
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    @Override
    public String toString() {
        return nome;
    }
}

class Eleitor extends Pessoa {

    private final String tituloEleitor;
    private boolean jaVotou;

    public Eleitor(String nome, String cpf, String tituloEleitor) {
        super(nome, cpf);
        if (tituloEleitor == null || tituloEleitor.trim().isEmpty()) {
            throw new IllegalArgumentException("Título de eleitor é obrigatório.");
        }
        this.tituloEleitor = tituloEleitor;
        this.jaVotou = false;
    }

    public String getTituloEleitor() {
        return tituloEleitor;
    }

    public boolean isJaVotou() {
        return jaVotou;
    }

    public void verificarSePodeVotar() throws EleitorJaVotouException {
        if (jaVotou) {
            throw new EleitorJaVotouException(
                    "O eleitor " + getNome() + " já votou.");
        }
    }

   
    public void registrarVoto() throws EleitorJaVotouException {
        verificarSePodeVotar();
        this.jaVotou = true;
    }
}

class Candidato extends Eleitor {

    private final int numero;
    private final String cargo;

    public Candidato(String nome, String cpf, String tituloEleitor,
                     int numero, String cargo) {
        super(nome, cpf, tituloEleitor);
        if (numero <= 0) {
            throw new IllegalArgumentException("Número do candidato deve ser positivo.");
        }
        if (cargo == null || cargo.trim().isEmpty()) {
            throw new IllegalArgumentException("Cargo é obrigatório.");
        }
        this.numero = numero;
        this.cargo = cargo;
    }

    public int getNumero() {
        return numero;
    }

    public String getCargo() {
        return cargo;
    }

    @Override
    public String toString() {
        return getNome() + " (nº " + numero + ", " + cargo + ")";
    }
}

enum TipoVoto {
    VALIDO,
    BRANCO,
    NULO
}

class Urna {

    private final int id;
    private final String secao;
    private boolean encerrada;

    
    private final List<Candidato> candidatos;

    
    private final List<Voto> votos = new ArrayList<>();
    private int proximoIdVoto = 1;

    public Urna(int id, String secao, List<Candidato> candidatos) {
        this.id = id;
        this.secao = secao;
        this.encerrada = false;
        this.candidatos = new ArrayList<>(candidatos);
    }


    public TipoVoto votar(Candidato candidato)
            throws UrnaEncerradaException, CandidatoInvalidoException {
        verificarAberta();
        if (!candidatos.contains(candidato)) {
            throw new CandidatoInvalidoException(
                    "O candidato " + candidato + " não concorre nesta urna.");
        }
        registrar(TipoVoto.VALIDO, candidato);
        return TipoVoto.VALIDO;
    }

    public TipoVoto votar(int numero) throws UrnaEncerradaException {
        verificarAberta();
        Candidato candidato = buscarPorNumero(numero);
        if (candidato == null) {
            registrar(TipoVoto.NULO, null);
            return TipoVoto.NULO;
        }
        registrar(TipoVoto.VALIDO, candidato);
        return TipoVoto.VALIDO;
    }

    public void votarBranco() throws UrnaEncerradaException {
        verificarAberta();
        registrar(TipoVoto.BRANCO, null);
    }

    private void registrar(TipoVoto tipo, Candidato candidato) {
        votos.add(new Voto(proximoIdVoto++, tipo, candidato));
    }

    private void verificarAberta() throws UrnaEncerradaException {
        if (encerrada) {
            throw new UrnaEncerradaException(
                    "A urna " + id + " (seção " + secao + ") está encerrada.");
        }
    }

    private Candidato buscarPorNumero(int numero) {
        for (Candidato c : candidatos) {
            if (c.getNumero() == numero) {
                return c;
            }
        }
        return null;
    }

    public void encerrar() {
        this.encerrada = true;
    }

    public Map<Candidato, Integer> contabilizarVotos() {
        if (!encerrada) {
            throw new IllegalStateException(
                    "Encerre a urna antes de contabilizar os votos.");
        }
        Map<Candidato, Integer> resultado = new LinkedHashMap<>();
        for (Candidato c : candidatos) {
            resultado.put(c, 0);
        }
        for (Voto v : votos) {
            if (v.getTipo() == TipoVoto.VALIDO) {
                resultado.merge(v.getCandidato(), 1, Integer::sum);
            }
        }
        return resultado;
    }

    public int contarBrancos() {
        return contarTipo(TipoVoto.BRANCO);
    }

    public int contarNulos() {
        return contarTipo(TipoVoto.NULO);
    }

    private int contarTipo(TipoVoto tipo) {
        int total = 0;
        for (Voto v : votos) {
            if (v.getTipo() == tipo) {
                total++;
            }
        }
        return total;
    }

    public int getId() {
        return id;
    }

    public String getSecao() {
        return secao;
    }

    public boolean isEncerrada() {
        return encerrada;
    }

    public int getTotalVotos() {
        return votos.size();
    }

    public List<Voto> getVotos() {
        return Collections.unmodifiableList(votos);
    }

    public List<Candidato> getCandidatos() {
        return Collections.unmodifiableList(candidatos);
    }


    public static class Voto {

        private final int id;
        private final TipoVoto tipo;
        private final Candidato candidato;

        private Voto(int id, TipoVoto tipo, Candidato candidato) {
            if (tipo == TipoVoto.VALIDO && candidato == null) {
                throw new IllegalArgumentException("Voto válido precisa de candidato.");
            }
            if (tipo != TipoVoto.VALIDO && candidato != null) {
                throw new IllegalArgumentException("Voto branco ou nulo não tem candidato.");
            }
            this.id = id;
            this.tipo = tipo;
            this.candidato = candidato;
        }

        public int getId() {
            return id;
        }

        public TipoVoto getTipo() {
            return tipo;
        }

        public Candidato getCandidato() {
            return candidato;
        }
    }
}
