package dev.netho.haruislandserver.entity;

import java.util.UUID;

public class Player {
    private final UUID uuid;
    private final String name;
    float[] position = new float[2];
    float[] destination = new float[2];

    public Player(String name, float x, float y) {
        this.uuid = UUID.randomUUID();
        this.name = name;
        this.position[0] = x;
        this.position[1] = y;
        this.destination[0] = x;
        this.destination[1] = y;
    }

    public Player(UUID uuid, String name, float positionX, float positionY, float destinationX, float destinationY) {
        this.uuid = uuid;
        this.name = name;
        this.position[0] = positionX;
        this.position[1] = positionY;
        this.destination[0] = destinationX;
        this.destination[1] = destinationY;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public float getX() {
        return position[0];
    }

    public float getY() {
        return position[1];
    }

    public float getDestinationX() {
        return position[0];
    }

    public float getDestinationY() {
        return position[1];
    }

    public void setDestination(float x, float y) {
        this.destination[0] = x;
        this.destination[1] = y;
    }

    public void setPosition(float x, float y) {
        this.position[0] = x;
        this.position[1] = y;
    }
}
