package dev.netho.haruislandserver.packet;

public abstract class Packet {
    private final PacketType packetType;

    public Packet(PacketType packetType) {
        this.packetType = packetType;
    }

    public PacketType getPacketType() {
        return packetType;
    }
}