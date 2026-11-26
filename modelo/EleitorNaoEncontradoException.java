package modelo;

public class EleitorNaoEncontradoException extends Exception {

    private static final long serialVersionUID = 1L;

    public EleitorNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
