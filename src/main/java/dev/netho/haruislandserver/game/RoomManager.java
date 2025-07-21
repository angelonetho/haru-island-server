package dev.netho.haruislandserver.game;

import dev.netho.haruislandserver.entity.Player;
import dev.netho.haruislandserver.entity.Room;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class RoomManager {

    private final List<Room> rooms;

    public RoomManager() {
        this.rooms = new ArrayList<>();

        createRoom("Haru Island");

    }

    public void addPlayerToRoom(Player player, Room room) {
        room.getPlayers().add(player);
    }

    public void removePlayerFromRoom(Player player, Room room) {
        room.getPlayers().remove(player);
    }

    public Room createRoom(String name) {
        Room room = new Room(name);
        rooms.add(room);
        return room;
    }

    public Room getRoom(UUID uuid) {
        for (Room room : rooms) {
            if (room.getUuid().equals(uuid)) {
                return room;
            }
        }
        return null;
    }

    public Room getRoom(String name) {
        for (Room room : rooms) {
            if (room.getName().equals(name)) {
                return room;
            }
        }
        return null;
    }
}