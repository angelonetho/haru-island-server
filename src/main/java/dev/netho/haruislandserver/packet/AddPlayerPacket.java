package dev.netho.haruislandserver.packet;

import dev.netho.haruislandserver.entity.Player;

public class AddPlayerPacket extends Packet{

    private final Player player;

    public AddPlayerPacket(Player player) {
        super(PacketType.ADD_PLAYER);
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}
