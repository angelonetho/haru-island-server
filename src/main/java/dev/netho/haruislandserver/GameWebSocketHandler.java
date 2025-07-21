package dev.netho.haruislandserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.netho.haruislandserver.entity.Room;
import dev.netho.haruislandserver.entity.Player;
import dev.netho.haruislandserver.game.PlayerManager;
import dev.netho.haruislandserver.game.RoomManager;
import dev.netho.haruislandserver.packet.*;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PlayerManager playerManager= new PlayerManager();
    private final RoomManager roomManager = new RoomManager();
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String name = "Player" + session.getId().substring(0, 5);
        sessions.put(session.getId(), session);

        Player player = playerManager.createPlayer(session.getId(), name);
        System.out.println("[Server] New player: " + player.getUuid() + ".");

        var playerPacket = new PlayerPacket(player);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(playerPacket)));

        Room room = roomManager.getRoom("Haru Island");
        roomManager.addPlayerToRoom(player, room);

        var roomPacket = new RoomPacket(room);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(roomPacket)));

        var addPlayerPacket = new AddPlayerPacket(player);
        broadcast(addPlayerPacket, session);

    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Aqui você pode processar outras mensagens do client depois
        String payload = message.getPayload();
        System.out.println("[Server] Message received: " + payload);

        // Descobre o tipo de pacote
        var rootNode = objectMapper.readTree(payload);
        String packetType = rootNode.get("packetType").asText();

        if ("PLAYER_MOVEMENT".equals(packetType)) {

            UUID playerUuid = UUID.fromString(rootNode.get("playerUuid").asText());
            float x = Float.parseFloat(rootNode.get("x").asText());
            float y = Float.parseFloat(rootNode.get("y").asText());

            playerManager.setPlayerDestination(session.getId(), x, y);

            var movementPacket = new PlayerMovementPacket(playerUuid, x, y);
            broadcast(movementPacket, session);
        }

        if ("PLAYER_POSITION".equals(packetType)) {

            UUID playerUuid = UUID.fromString(rootNode.get("playerUuid").asText());
            float x = Float.parseFloat(rootNode.get("x").asText());
            float y = Float.parseFloat(rootNode.get("y").asText());

            playerManager.setPlayerPosition(session.getId(), x, y);

            var positionPacket = new PlayerPositionPacket(playerUuid, x, y);
            broadcast(positionPacket, session);
        }

        if ("CHAT_MESSAGE".equals(packetType)) {

            UUID playerUuid = UUID.fromString(rootNode.get("playerUuid").asText());
            String chatMessage = rootNode.get("message").asText();

            var chatMessagePacket = new ChatMessagePacket(playerUuid, chatMessage);
            broadcast(chatMessagePacket, session);
        }
    }

    private void broadcast(Packet packet, WebSocketSession excludeSession) throws IOException {
        for (WebSocketSession session : sessions.values()) {
            if (session.getId().equals(excludeSession.getId())) {
                continue;
            }
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(packet)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Player player = playerManager.getPlayer(session.getId());

        var removePlayerPacket = new RemovePlayerPacket(player.getUuid());
        broadcast(removePlayerPacket, session);

        sessions.remove(session.getId());
        playerManager.removePlayer(session.getId());
    }

}