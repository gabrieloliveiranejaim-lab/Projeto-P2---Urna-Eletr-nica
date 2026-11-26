package modelo;

public class Eleitor extends Pessoa {

    private final String tituloEleitor;
    private boolean jaVotou;

    public Eleitor(String nome, String cpf, String tituloEleitor) {
        this(nome, cpf, tituloEleitor, false);
    }

    // Sobrecarga de construtor: usado ao LER do banco um eleitor que já votou.
    public Eleitor(String nome, String cpf, String tituloEleitor, boolean jaVotou) {
        super(nome, cpf);
        if (tituloEleitor == null || tituloEleitor.trim().isEmpty()) {
            throw new IllegalArgumentException("Título de eleitor é obrigatório.");
        }
        this.tituloEleitor = tituloEleitor;
        this.jaVotou = jaVotou;
    }

    public String getTituloEleitor() {
        return tituloEleitor;
    }

    public boolean isJaVotou() {
        return jaVotou;
    }

    // Só confere, não muda nada. Quem controla "vota no máximo uma vez" é o eleitor (RN2.1).
    public void verificarSePodeVotar() throws EleitorJaVotouException {
        if (jaVotou) {
            throw new EleitorJaVotouException(
                    "O eleitor " + getNome() + " já votou.");
        }
    }

    // Única forma de mudar jaVotou depois de criado o eleitor (encapsulamento).
    public void registrarVoto() throws EleitorJaVotouException {
        verificarSePodeVotar();
        this.jaVotou = true;
    }
}
