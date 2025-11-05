package dev.netho.haruislandserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.netho.haruislandserver.entity.Player;
import dev.netho.haruislandserver.entity.Room;
import dev.netho.haruislandserver.game.PlayerManager;
import dev.netho.haruislandserver.game.RoomManager;
import dev.netho.haruislandserver.packet.*;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PlayerManager playerManager = new PlayerManager();
    private final RoomManager roomManager = new RoomManager();
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("Nova sessão");

        sessions.put(session.getId(), session);

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

        if ("NEW_CONNECTION".equals(packetType)) {

            String nickname = rootNode.get("nickname").asText();

            nickname = nickname.substring(0, Math.min(nickname.length(), 16));

            Player player = playerManager.createPlayer(session.getId(), nickname);
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
    }

    private void broadcast(Packet packet, WebSocketSession excludeSession) throws IOException {
        String message = objectMapper.writeValueAsString(packet);
        List<WebSocketSession> closedSessions = new ArrayList<>();

        for (WebSocketSession session : sessions.values()) {

            if (session.getId().equals(excludeSession.getId())) {
                continue;
            }

            if (!session.isOpen()) {
                closedSessions.add(session);
                continue;
            }

            synchronized (session) {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(message));
                    }
                } catch (Exception e) {
                    System.out.println("[Server] Error sending packet: " + e.getMessage());
                    closedSessions.add(session);
                }
            }
        }

        for (WebSocketSession session : closedSessions) {
            handleClosedSession(session);
        }
    }

    private void cleanupSession(WebSocketSession session) {
        try {
            Player player = playerManager.getPlayer(session.getId());

            if (player != null) {
                Room playerRoom = roomManager.getRoomByPlayer(player);
                if (playerRoom != null) {
                    roomManager.removePlayerFromRoom(player, playerRoom);
                }
                playerManager.removePlayer(session.getId());
            }
            sessions.remove(session.getId());
        } catch (Exception e) {
            System.out.println("[Server] Error cleaning up session: " + e.getMessage());
        }
    }

    private void handleClosedSession(WebSocketSession session) throws IOException {
        Player player = playerManager.getPlayer(session.getId());

        if (player != null) {
            var removePlayerPacket = new RemovePlayerPacket(player.getUuid());
            broadcast(removePlayerPacket, session);
        }
        cleanupSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        handleClosedSession(session);

    }

}