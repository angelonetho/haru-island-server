package dev.netho.haruislandserver.packet;

import java.util.UUID;

public class PlayerMovementPacket extends Packet{
    private UUID playerUuid;
    private float x;
    private float y;

    public PlayerMovementPacket() {
        super(PacketType.PLAYER_MOVEMENT);
    }

    public PlayerMovementPacket(UUID playerUuid, float x, float y) {
        super(PacketType.PLAYER_MOVEMENT);
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
