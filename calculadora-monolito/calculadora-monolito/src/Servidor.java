import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Servidor {
    private static final int PORTA = 5000;
    private static final int TEMPO_INATIVO_MS = 120_000;

    public static void main(String[] args) {
        // Cada sessão ocupa uma tarefa; até oito clientes são atendidos ao mesmo tempo.
        ExecutorService clientes = Executors.newFixedThreadPool(8);

        try (ServerSocket servidor = new ServerSocket(PORTA)) {
            System.out.println("[Servidor] Aguardando conexões na porta " + PORTA);

            while (true) {
                Socket cliente = servidor.accept();
                clientes.execute(() -> atender(cliente));
            }
        } catch (IOException erro) {
            System.err.println("[Servidor] Falha de comunicação: " + erro.getMessage());
            System.exit(1);
        } finally {
            clientes.shutdown();
        }
    }

    private static void atender(Socket cliente) {
        String endereco = cliente.getRemoteSocketAddress().toString();
        System.out.println("[Servidor] Cliente conectado: " + endereco);

        // try-with-resources fecha os fluxos e o socket, inclusive quando ocorre uma exceção.
        try (Socket conexao = cliente;
             BufferedReader entrada = new BufferedReader(new InputStreamReader(
                     conexao.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter saida = new BufferedWriter(new OutputStreamWriter(
                     conexao.getOutputStream(), StandardCharsets.UTF_8))) {

            // Uma sessão sem novos dados por dois minutos libera seu atendimento.
            conexao.setSoTimeout(TEMPO_INATIVO_MS);
            String requisicao;

            // TCP não separa mensagens: cada linha completa representa uma requisição.
            while ((requisicao = entrada.readLine()) != null) {
                System.out.println("[Servidor] Recebido de " + endereco + ": " + requisicao);
                boolean encerrar = requisicao.trim().equalsIgnoreCase("SAIR");
                String resposta = encerrar ? "Sessão encerrada." : calcular(requisicao);

                saida.write(resposta);
                saida.write('\n');
                saida.flush();
                System.out.println("[Servidor] Enviado para " + endereco + ": " + resposta);

                if (encerrar) {
                    break;
                }
            }
        } catch (SocketTimeoutException erro) {
            System.err.println("[Servidor] Sessão encerrada por inatividade: " + endereco);
        } catch (IOException erro) {
            System.err.println("[Servidor] Falha com " + endereco + ": " + erro.getMessage());
        } finally {
            System.out.println("[Servidor] Cliente desconectado: " + endereco);
        }
    }

    private static String calcular(String requisicao) {
        String[] partes = requisicao.trim().split("\\s+");
        if (partes.length != 3) {
            return "Erro: use número operador número, separados por espaços. Exemplo: 10 + 5.";
        }

        try {
            // BigDecimal evita erros de representação binária, como 0.1 + 0.2 != 0.3.
            BigDecimal primeiro = lerNumero(partes[0]);
            BigDecimal segundo = lerNumero(partes[2]);

            BigDecimal resultado = switch (partes[1]) {
                case "+" -> primeiro.add(segundo);
                case "-" -> primeiro.subtract(segundo);
                case "*" -> primeiro.multiply(segundo);
                case "/" -> {
                    if (segundo.signum() == 0) {
                        throw new ArithmeticException("divisão por zero não é permitida.");
                    }
                    // Divisões não exatas são arredondadas para 16 algarismos significativos.
                    yield primeiro.divide(segundo, MathContext.DECIMAL64);
                }
                default -> throw new IllegalArgumentException("operador inválido. Use +, -, * ou /.");
            };

            return "Resultado: " + resultado.stripTrailingZeros().toPlainString();
        } catch (IllegalArgumentException | ArithmeticException erro) {
            // Erros na operação viram respostas; a sessão continua disponível para outro cálculo.
            return "Erro: " + erro.getMessage();
        }
    }

    private static BigDecimal lerNumero(String texto) {
        // Limita o tamanho e aceita apenas decimais comuns, sem expoentes ou valores especiais.
        if (texto.length() > 64 || !texto.matches("[+-]?(?:[0-9]+(?:[.,][0-9]*)?|[.,][0-9]+)")) {
            throw new IllegalArgumentException("use números decimais de até 64 caracteres.");
        }
        return new BigDecimal(texto.replace(',', '.'));
    }
}
