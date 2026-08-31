# Haru Island — Servidor

Servidor de um mundo 2D multiplayer: mantém todo mundo que está conectado vendo, ao mesmo tempo, os outros jogadores andarem e conversarem.

Projeto pessoal de estudo, escrito do zero para entender como um servidor de tempo real funciona por dentro — sem engine de jogo, sem broker de mensagens, sem banco de dados. É a metade servidora do Haru Island; o cliente é um app nativo para macOS em SpriteKit e SwiftUI, em [angelonetho/HaruIsland](https://github.com/angelonetho/HaruIsland). Está em desenvolvimento: roda em container no Render e conversa com o cliente, mas ainda não tem jogadores de verdade.

## O problema

Num mundo compartilhado, o estado de um jogador é notícia para todos os outros. Requisição-resposta não resolve: ninguém vai ficar perguntando "alguém andou?" trinta vezes por segundo. O servidor precisa empurrar a informação, e é para isso que existe WebSocket.

A parte difícil não é abrir a conexão — é escrever nela. Cada mensagem que chega de um jogador roda numa thread do container, e essa thread precisa escrever em todas as outras conexões abertas. Duas coisas quebram aí:

- `WebSocketSession` do Spring **não é thread-safe para escrita**. Se duas threads chamam `sendMessage` na mesma sessão, os frames se intercalam e a conexão morre com erro de escrita parcial.
- Uma conexão pode morrer no meio do próprio broadcast. Remover a sessão da lista enquanto você itera sobre ela é outra forma de quebrar tudo.

O núcleo do projeto é o método que resolve esses dois problemas ao mesmo tempo — `broadcast`, em [`GameWebSocketHandler.java`](src/main/java/dev/netho/haruislandserver/GameWebSocketHandler.java).

## Como funciona

```
   Cliente macOS                         Servidor (Spring Boot)
   (SpriteKit)                           GameWebSocketHandler
        │                                          │
        │  handshake ws://…/ws/game                │
        ├─────────────────────────────────────────►│  registra sessão + lock
        │                                          │
        ├──── NEW_CONNECTION {nickname} ──────────►│  PlayerManager.createPlayer()
        │                                          │      ConcurrentHashMap<sessionId, Player>
        │◄─── PLAYER  { você é este UUID }─────────┤
        │◄─── ROOM    { quem já está na sala }─────┤  RoomManager → sala "Haru Island"
        │                                          │
        │                                          ├──► ADD_PLAYER (broadcast aos demais)
        │                                          │
        │  ─── PLAYER_MOVEMENT / PLAYER_POSITION ─►│
        │  ─── CHAT_MESSAGE ──────────────────────►│  broadcast(packet, exceto remetente)
        │◄──────────────── mesmos pacotes ─────────┤
        │                                          │
        │  desconecta                              ├──► REMOVE_PLAYER (broadcast)
```

**`GameWebSocketHandler`** — o coração, 168 das 591 linhas do projeto. Estende `TextWebSocketHandler`, guarda `sessions` e `sessionLocks` (dois `ConcurrentHashMap` indexados pelo id da sessão) e faz o dispatch dos pacotes que chegam.

**`PlayerManager`** — mapeia id de sessão para `Player`. É a ponte entre "esta conexão TCP" e "este personagem". Um `Player` tem UUID, nome, posição e destino.

**`RoomManager`** — dono das salas e de quem está em cada uma. Hoje existe uma sala só, `"Haru Island"`, criada no construtor.

**`packet/`** — nove classes que descrevem o protocolo. Todas estendem `Packet`, que carrega o `PacketType`. É esse campo, serializado pelo Jackson como `"packetType"`, que o outro lado usa para saber o que chegou.

## Protocolo

Oito tipos, JSON puro sobre frames de texto:

| Pacote | Campos | Direção |
|---|---|---|
| `NEW_CONNECTION` | `nickname` | cliente → servidor |
| `PLAYER` | `player` | servidor → **só quem entrou** |
| `ROOM` | `room` (com a lista de jogadores) | servidor → **só quem entrou** |
| `ADD_PLAYER` | `player` | servidor → todos, menos quem entrou |
| `REMOVE_PLAYER` | `playerUuid` | servidor → todos, menos quem saiu |
| `PLAYER_MOVEMENT` | `playerUuid`, `x`, `y` | ida e volta (destino do movimento) |
| `PLAYER_POSITION` | `playerUuid`, `x`, `y` | ida e volta (posição atual) |
| `CHAT_MESSAGE` | `playerUuid`, `message` | ida e volta |

`NEW_CONNECTION` é o único sem classe correspondente — o servidor só o lê, nunca o envia, então ele é extraído direto da árvore JSON. O nickname é truncado em 16 caracteres. Todo broadcast exclui o remetente, o que significa que o cliente aplica o próprio movimento localmente em vez de esperar o eco do servidor.

## Decisões técnicas

### WebSocket puro, não STOMP

O caminho padrão em Spring seria STOMP com um broker de mensagens. O cliente, porém, é Swift usando `URLSessionWebSocketTask` — não existe cliente STOMP de primeira classe na plataforma, e adotá-lo significaria implementar o frame format do STOMP à mão do lado do cliente, para depois ainda serializar o payload dentro dele.

A escolha foi `@EnableWebSocket` com um `TextWebSocketHandler` cru e um protocolo próprio de oito pacotes. A classe abstrata `Packet` carrega o `PacketType`, e o Jackson o serializa como um campo comum do JSON. O cliente lê `packetType` e faz o dispatch. Uma camada a menos, e o que trafega é JSON que dá para ler no log sem ferramenta nenhuma.

### Broadcast com lock por sessão e coleta de mortos

Esta é a decisão que o projeto existe para exercitar:

```java
private void broadcast(Packet packet, WebSocketSession excludeSession) throws IOException {
    String message = objectMapper.writeValueAsString(packet);
    List<WebSocketSession> closedSessions = new ArrayList<>();

    for (WebSocketSession session : sessions.values()) {
        if (session.getId().equals(excludeSession.getId())) continue;
        if (!session.isOpen()) { closedSessions.add(session); continue; }

        Object lock = sessionLocks.get(session.getId());
        synchronized (lock) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (Exception e) {
                closedSessions.add(session);
            }
        }
    }

    for (WebSocketSession session : closedSessions) {
        cleanupSession(session);
    }
}
```

Quatro coisas aqui não são acidentais:

- **O lock é por destinatário, não global.** Cada sessão ganha um `Object` próprio em `sessionLocks` no momento em que conecta. Serializar as escritas numa mesma sessão é obrigatório; serializar o broadcast inteiro num único lock não é, e custaria caro — com um lock global, dois jogadores andando ao mesmo tempo esperariam um pelo outro sem necessidade. Assim, broadcasts concorrentes só disputam quando miram exatamente o mesmo socket.
- **`isOpen()` é verificado duas vezes.** Fora do lock como caminho rápido, para nem tentar adquiri-lo numa sessão já morta; dentro do lock por correção, porque a sessão pode ter fechado enquanto a thread esperava.
- **O `catch` é `Exception`, não `IOException`.** Uma falha de runtime no envio também significa que aquele socket não serve mais. Tratar só `IOException` deixaria sessões zumbis na lista.
- **A limpeza acontece depois do loop.** As sessões mortas vão para uma `List` durante a iteração e só passam por `cleanupSession` quando ela termina, que é o que remove o jogador da sala, do `PlayerManager` e dos dois mapas. `sessions.values()` é uma view fracamente consistente do `ConcurrentHashMap`, então mutar durante a iteração não lançaria exceção — mas separar as duas fases deixa o invariante óbvio para quem lê.

O método não nasceu assim. O histórico do repositório mostra a convergência, de `6bfbf98` até `eaa0d20`: primeiro tratar sessão fechada, depois extrair a limpeza, depois acumular os mortos numa lista, depois o lock por sessão.

### Identidade vem do servidor; posição vem do cliente

O UUID do jogador é gerado no construtor de `Player`, no servidor, e devolvido no pacote `PLAYER`. O cliente nunca escolhe quem ele é — se escolhesse, dois jogadores poderiam colidir de identidade e um poderia assumir a do outro.

As coordenadas seguem o caminho oposto: o servidor não simula movimento. Não existe game loop, `@Scheduled` nem tick — ele recebe a posição que o cliente reporta e repassa. O ganho é latência mínima e um servidor pequeno o bastante para caber na cabeça. O custo é confiar no cliente, e é uma dívida consciente: com jogadores reais, validar deslocamento contra tempo decorrido no servidor seria o próximo passo.

## Estado atual

A validação até aqui foi manual, conectando o cliente macOS ao servidor. A única cobertura automatizada é o smoke test `contextLoads` gerado pelo Spring Initializr — o protocolo e o broadcast ainda não têm testes.

## Stack

| | |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 3.5.3 |
| Rede | `spring-boot-starter-websocket` (WebSocket cru, sem STOMP/SockJS) |
| Serialização | Jackson |
| Build | Maven Wrapper |
| Deploy | Container Docker no Render — build multi-stage, `eclipse-temurin:21-jdk` para compilar e `21-jre` para rodar |

Duas dependências no `pom.xml`. É o projeto inteiro.

## Como rodar

```bash
./mvnw spring-boot:run
```

O servidor sobe na porta 8080 e o endpoint fica em `ws://localhost:8080/ws/game`. A build exige um JDK 21.

Dá para testar sem o cliente, com qualquer ferramenta de linha de comando para WebSocket:

```bash
websocat ws://localhost:8080/ws/game
{"packetType":"NEW_CONNECTION","nickname":"teste"}
```

O servidor responde com dois pacotes — o `PLAYER`, com o UUID atribuído, e o `ROOM`, com a lista de quem está na sala:

```json
{"packetType":"PLAYER","player":{"uuid":"…","name":"teste","x":750.0,"y":750.0,…}}
{"packetType":"ROOM","room":{"uuid":"…","name":"Haru Island","players":[…]}}
```

Abrindo uma segunda conexão, a primeira recebe o `ADD_PLAYER` correspondente.

## Autoria

Projeto individual de Angelo Andrioli Netho. O cliente macOS que consome este servidor está em [angelonetho/HaruIsland](https://github.com/angelonetho/HaruIsland).
