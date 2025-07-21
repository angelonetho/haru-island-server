package dev.netho.haruislandserver.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Room {
    private UUID uuid;
    private String name;
    private List<Player> players;

    public Room(UUID uuid, String name, List<Player> players) {
        this.uuid = uuid;
        this.name = name;
        this.players = players;
    }

    public Room(String name) {
        this.name = name;
        this.uuid = UUID.randomUUID();
        this.players = new ArrayList<>();
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

}
