package dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import modelo.Candidato;
import modelo.Urna;

// Gera o boletim de urna: urna, data, total de cada candidato, brancos, nulos e total geral.
public class Boletim {

    private static final String LINHA = "----------------------------------------\n";

    // Monta o texto do boletim. A urna precisa estar encerrada.
    public static String gerar(Urna urna) {
        Map<Candidato, Integer> resultado = urna.contabilizarVotos();
        String data = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        StringBuilder sb = new StringBuilder();
        sb.append("BOLETIM DE URNA\n");
        sb.append("Urna: ").append(urna.getId())
          .append("    Seção: ").append(urna.getSecao()).append("\n");
        sb.append("Data: ").append(data).append("\n");
        sb.append(LINHA);
        sb.append(String.format("%-34s %5s\n", "Candidato", "Votos"));
        for (Map.Entry<Candidato, Integer> linha : resultado.entrySet()) {
            sb.append(String.format("%-34s %5d\n", linha.getKey(), linha.getValue()));
        }
        sb.append(LINHA);
        sb.append(String.format("%-34s %5d\n", "Brancos", urna.contarBrancos()));
        sb.append(String.format("%-34s %5d\n", "Nulos", urna.contarNulos()));
        sb.append(String.format("%-34s %5d\n", "TOTAL GERAL", urna.getTotalVotos()));
        return sb.toString();
    }

    // Grava o boletim em "boletins/boletim_urna_<id>.txt" (cria a pasta se preciso)
    // e devolve o caminho do arquivo.
    public static String gravar(Urna urna) throws IOException {
        Path arquivo = Paths.get("boletins", "boletim_urna_" + urna.getId() + ".txt");
        Files.createDirectories(arquivo.getParent());
        Files.write(arquivo, gerar(urna).getBytes(StandardCharsets.UTF_8));
        return arquivo.toString();
    }
}
