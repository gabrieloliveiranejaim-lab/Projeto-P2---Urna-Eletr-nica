package modelo;

public class CandidatoInvalidoException extends Exception {

    private static final long serialVersionUID = 1L;

    public CandidatoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
