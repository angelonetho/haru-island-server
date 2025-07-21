package dev.netho.haruislandserver.game;

import dev.netho.haruislandserver.entity.Player;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {

    private final ConcurrentHashMap<String, Player> players = new ConcurrentHashMap<>();

    public Player createPlayer(String sessionId, String name) {
        Player player = new Player(name, 750, 750);
        players.put(sessionId, player);
        return player;
    }

    public void setPlayerDestination(String sessionId, float x, float y) {
        players.get(sessionId).setDestination(x, y);
    }

    public void setPlayerPosition(String sessionId, float x, float y) {
        players.get(sessionId).setPosition(x, y);
    }

    public List<Player> getAllPlayers() {
        return List.copyOf(players.values());
    }

    public Player getPlayer(String sessionId) {
        return players.get(sessionId);
    }

    public void removePlayer(String sessionId) {
        players.remove(sessionId);
    }


}
