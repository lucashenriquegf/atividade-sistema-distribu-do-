# Calculadora TCP em Java

Servidor monolítico: comunicação TCP, validação e cálculo estão no mesmo programa.
`Cliente.java` é um programa de console independente, como exige a atividade.
Ambos pertencem ao mesmo projeto e usam a mesma imagem Docker, executada em dois
contêineres. Todo cálculo ocorre no servidor.

## Arquivos

| Arquivo | Responsabilidade |
| --- | --- |
| `src/Servidor.java` | Aceitar conexões, validar operações e devolver resultados. |
| `src/Cliente.java` | Ler o console, enviar operações e exibir respostas. |
| `Dockerfile` | Compilar com Java 17 e montar a imagem de execução. |
| `compose.yaml` | Configurar servidor e cliente na mesma rede Docker. |

## Executar com Docker

Tenha Docker instalado e em execução, com Docker Compose v2. No Windows, use
Docker Desktop no modo de contêineres Linux. Não é preciso instalar Java no host.

1. Extraia o ZIP e abra um terminal dentro da pasta `calculadora-monolito`.
2. Inicie o servidor:

   ```bash
   docker compose up --build servidor
   ```

   Aguarde a mensagem `[Servidor] Aguardando conexões na porta 5000`. O primeiro
   build precisa de acesso à internet para baixar as imagens Java.

3. Abra outro terminal na mesma pasta e inicie o cliente:

   ```bash
   docker compose run --rm cliente
   ```

4. Digite uma operação por linha, mantendo espaços entre os três elementos:

   ```text
   10 + 5
   10 - 5
   10 * 5
   10 / 5
   0,1 + 0,2
   10 / 0
   SAIR
   ```

   As respostas serão `Resultado: 15`, `Resultado: 5`, `Resultado: 50`,
   `Resultado: 2`, `Resultado: 0.3`, um erro de divisão por zero e a confirmação
   de encerramento. Depois de um erro de operação, a conexão continua aberta.

5. Para demonstrar clientes simultâneos, execute o comando do cliente em mais
   um terminal. Deixe os dois clientes conectados e faça operações em ambos.
6. Capture uma tela mostrando os terminais do servidor e do cliente, para a
   entrega da atividade.
7. Para encerrar tudo, saia dos clientes com `SAIR`, pressione `Ctrl+C` no
   terminal do servidor e execute:

   ```bash
   docker compose down
   ```

## Protocolo

TCP na porta fixa **5000**, com texto em **UTF-8**. Cada mensagem termina com
**`\n`**. O cliente envia uma linha e aguarda uma resposta antes de enviar a próxima.

| Mensagem do cliente | Resposta do servidor |
| --- | --- |
| `10 + 5\n` | `Resultado: 15\n` |
| `10 / 0\n` | `Erro: divisão por zero não é permitida.\n` |
| `SAIR\n` | `Sessão encerrada.\n`, seguida do fechamento da conexão |

São aceitos `+`, `-`, `*` e `/`, números negativos e decimais com ponto ou vírgula.
Cada número pode ter até 64 caracteres; notação científica, `NaN` e `Infinity`
não são aceitos. A saída usa ponto decimal. Soma, subtração e multiplicação são
exatas em decimal; divisão usa até 16 algarismos significativos, com o
arredondamento `HALF_EVEN` de `MathContext.DECIMAL64`.

O servidor atende até oito sessões simultâneas com `ExecutorService`; as demais
aguardam na fila. Uma sessão sem novos dados por dois minutos é encerrada.
O cliente limita a cinco segundos a conexão e a espera pela resposta. Os fluxos
e sockets são fechados com `try-with-resources`.

`servidor` é o endereço do servidor dentro da rede Compose. A publicação
`127.0.0.1:5000:5000` também permite acesso pelo próprio host usando `localhost`.

## Executar diretamente com Java 17 ou superior

Com um JDK instalado, na pasta do projeto:

```bash
javac --release 17 -encoding UTF-8 -d out src/Servidor.java src/Cliente.java
java -cp out Servidor
```

Em outro terminal:

```bash
java -cp out Cliente localhost
```

## Validação

O código Java foi compilado e a comunicação TCP foi testada localmente:
quatro operações, decimais, divisão por zero, entradas inválidas, operações
repetidas na mesma conexão, encerramento e clientes simultâneos.
Docker não estava disponível no ambiente de preparação; a imagem e os
contêineres precisam ser executados na sua máquina.

Referências de execução: [Docker Compose run](https://docs.docker.com/reference/cli/docker/compose/run/)
e [perfis do Compose](https://docs.docker.com/compose/how-tos/profiles/).
