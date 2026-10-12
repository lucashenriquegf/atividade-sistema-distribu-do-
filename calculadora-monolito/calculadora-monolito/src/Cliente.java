import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class Cliente {
    private static final int PORTA = 5000;
    private static final int TEMPO_LIMITE_MS = 5_000;

    public static void main(String[] args) {
        // No Docker, o endereço é o nome do serviço; fora dele, pode ser localhost ou um IP.
        String host = args.length > 0 ? args[0] : "localhost";

        try (Socket conexao = new Socket();
             BufferedReader console = new BufferedReader(new InputStreamReader(
                     System.in, StandardCharsets.UTF_8))) {

            System.out.println("[Cliente] Conectando a " + host + ":" + PORTA);
            conexao.connect(new InetSocketAddress(host, PORTA), TEMPO_LIMITE_MS);
            // O limite vale para a espera pela resposta, não para a digitação no console.
            conexao.setSoTimeout(TEMPO_LIMITE_MS);
            System.out.println("[Cliente] Conectado. Operações: +, -, *, /. SAIR encerra a sessão.");

            try (BufferedReader entrada = new BufferedReader(new InputStreamReader(
                         conexao.getInputStream(), StandardCharsets.UTF_8));
                 BufferedWriter saida = new BufferedWriter(new OutputStreamWriter(
                         conexao.getOutputStream(), StandardCharsets.UTF_8))) {

                while (true) {
                    System.out.print("Operação (ex.: 10 + 5): ");
                    String requisicao = console.readLine();
                    // EOF no console também solicita o encerramento pelo protocolo.
                    if (requisicao == null) {
                        requisicao = "SAIR";
                    }

                    saida.write(requisicao);
                    saida.write('\n');
                    // Sem flush, a requisição pode ficar no buffer enquanto o servidor aguarda.
                    saida.flush();
                    System.out.println("[Cliente] Enviado: " + requisicao);

                    String resposta = entrada.readLine();
                    if (resposta == null) {
                        throw new IOException("o servidor encerrou a conexão sem responder.");
                    }
                    System.out.println("[Cliente] Resposta: " + resposta);

                    if (requisicao.trim().equalsIgnoreCase("SAIR")) {
                        break;
                    }
                }
            }
            System.out.println("[Cliente] Conexão encerrada.");
        } catch (ConnectException erro) {
            System.err.println("[Cliente] Conexão recusada. Verifique se o servidor está em execução.");
            System.exit(1);
        } catch (SocketTimeoutException erro) {
            System.err.println("[Cliente] Tempo limite atingido ao conectar ou aguardar uma resposta.");
            System.exit(1);
        } catch (IOException erro) {
            System.err.println("[Cliente] Falha de comunicação: " + erro.getMessage());
            System.exit(1);
        }
    }
}
