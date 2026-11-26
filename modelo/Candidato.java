package modelo;

public class Candidato extends Eleitor {

    private final int numero;
    private final String cargo;

    public Candidato(String nome, String cpf, String tituloEleitor,
                     int numero, String cargo) {
        this(nome, cpf, tituloEleitor, numero, cargo, false);
    }

    // Sobrecarga de construtor: usado ao LER do banco.
    public Candidato(String nome, String cpf, String tituloEleitor,
                     int numero, String cargo, boolean jaVotou) {
        super(nome, cpf, tituloEleitor, jaVotou);
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
