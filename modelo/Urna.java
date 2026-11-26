package modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Urna {

    private final int id;
    private final String secao;
    private boolean encerrada;

    // Agregação: os candidatos existem fora da urna e chegam pelo construtor.
    private final List<Candidato> candidatos;

    // Composição: a lista de votos nasce dentro da urna.
    private final List<Voto> votos = new ArrayList<>();
    private int proximoIdVoto = 1;

    public Urna(int id, String secao, List<Candidato> candidatos) {
        this.id = id;
        this.secao = secao;
        this.encerrada = false;
        this.candidatos = new ArrayList<>(candidatos);
    }

    // Sobrecarga de construtor: usado só pelo BancoDados, ao ler uma urna já gravada.
    public Urna(int id, String secao, boolean encerrada, List<Candidato> candidatos) {
        this(id, secao, candidatos);
        this.encerrada = encerrada;
    }

    // Usado só pelo BancoDados, para recolocar na urna os votos que já estavam gravados.
    public void carregarVoto(TipoVoto tipo, Candidato candidato) {
        registrar(tipo, candidato);
    }

    // A urna NÃO recebe o eleitor: ela só guarda o voto, sem saber de quem é (RN2.2).

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

    // Só conta depois que a urna foi encerrada.
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

    // O Voto fica DENTRO da Urna (composição) e o construtor é private:
    // só a Urna consegue criar um Voto. Ninguém cria Voto por fora (RN2.4),
    // e o compilador garante isso.
    // O voto NÃO guarda quem votou (RN2.2).
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
