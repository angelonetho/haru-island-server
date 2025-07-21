package dev.netho.haruislandserver.packet;

import java.util.UUID;

public class ChatMessagePacket extends Packet{
    private final UUID playerUuid;
    private final String message;

    public ChatMessagePacket(UUID playerUuid, String message) {
        super(PacketType.CHAT_MESSAGE);

        this.playerUuid = playerUuid;
        this.message = message;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getMessage() {
        return message;
    }
}
