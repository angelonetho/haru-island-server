package dev.netho.haruislandserver.packet;

import java.util.UUID;

public class PlayerPositionPacket extends Packet{
    private final UUID playerUuid;
    private final float x;
    private final float y;

    public PlayerPositionPacket(UUID playerUuid, float x, float y) {
        super(PacketType.PLAYER_POSITION);
        this.playerUuid = playerUuid;
        this.x = x;
        this.y = y;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
}
