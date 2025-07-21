package dev.netho.haruislandserver.packet;

import java.util.UUID;

public class RemovePlayerPacket extends Packet{

    private final UUID playerUuid;

    public RemovePlayerPacket(UUID playerUuid) {
        super(PacketType.REMOVE_PLAYER);
        this.playerUuid = playerUuid;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }
}
