package dev.netho.haruislandserver.packet;

import dev.netho.haruislandserver.entity.Player;

public class PlayerPacket extends Packet{

    private final Player player;

    public PlayerPacket(Player player) {
        super(PacketType.PLAYER);

        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}
