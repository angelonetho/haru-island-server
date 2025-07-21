package dev.netho.haruislandserver.packet;

import dev.netho.haruislandserver.entity.Room;

public class RoomPacket extends Packet{

    private final Room room;

    public RoomPacket(Room room) {
        super(PacketType.ROOM);

        this.room = room;
    }

    public Room getRoom() {
        return room;
    }
}
