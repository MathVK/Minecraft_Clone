package fr.math.minecraft.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.math.minecraft.client.GameConfiguration;
import fr.math.minecraft.client.world.Coordinates;
import fr.math.minecraft.server.payload.InputPayload;
import fr.math.minecraft.server.world.ServerChunk;
import fr.math.minecraft.server.world.ServerChunkComparator;
import fr.math.minecraft.shared.network.PlayerInputData;
import org.joml.Vector3f;
import org.joml.Vector3i;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Base64;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

public class Client {

    private final String name;
    private final String uuid;
    private final InetAddress address;
    private final int port;
    private Vector3f position;
    private Vector3f front;
    private float yaw;
    private float bodyYaw;
    private float pitch;
    private float speed;
    private BufferedImage skin;
    private boolean movingLeft, movingRight, movingForward, movingBackward;
    private boolean flying, sneaking;
    private boolean active;
    private final Vector3i inputVector;
    private final Set<Coordinates> receivedChunks;
    private final PriorityQueue<ServerChunk> nearChunks;
    private Vector3f lastChunkPosition;

    public Client(String uuid, String name, InetAddress address, int port) {
        this.address = address;
        this.port = port;
        this.uuid = uuid;
        this.name = name;
        this.front = new Vector3f(0.0f, 0.0f, 0.0f);
        this.position = new Vector3f(0.0f, 0.0f, 0.0f);
        this.lastChunkPosition = new Vector3f(0, 0, 0);
        this.inputVector = new Vector3i(0, 0, 0);
        this.receivedChunks = new HashSet<>();
        this.nearChunks = new PriorityQueue<>(new ServerChunkComparator(this));
        this.yaw = 0.0f;
        this.pitch = 0.0f;
        this.speed = 0.1f;
        this.skin = null;
        this.movingLeft = false;
        this.movingRight = false;
        this.movingBackward = false;
        this.movingForward = false;
        this.flying = false;
        this.sneaking = false;
        this.active = false;
    }

    public String getName() {
        return name;
    }

    public void handleInputs(JsonNode packetData) {
        boolean movingLeft = packetData.get("left").asBoolean();
        boolean movingRight = packetData.get("right").asBoolean();
        boolean movingForward = packetData.get("forward").asBoolean();
        boolean movingBackward = packetData.get("backward").asBoolean();
        boolean flying = packetData.get("flying").asBoolean();
        boolean sneaking = packetData.get("sneaking").asBoolean();

        float yaw = packetData.get("yaw").floatValue();
        float bodyYaw = packetData.get("bodyYaw").floatValue();
        float pitch = packetData.get("pitch").floatValue();

        int inputX = packetData.get("inputX").intValue();
        int inputY = packetData.get("inputY").intValue();
        int inputZ = packetData.get("inputZ").intValue();

        this.yaw = yaw;
        this.bodyYaw = bodyYaw;
        this.pitch = pitch;

        this.movingLeft = movingLeft;
        this.movingRight = movingRight;
        this.movingForward = movingForward;
        this.movingBackward = movingBackward;

        this.inputVector.x = inputX;
        this.inputVector.y = inputY;
        this.inputVector.z = inputZ;

        this.flying = flying;
        this.sneaking = sneaking;
    }

    public void updatePosition(InputPayload payload) {

        for (PlayerInputData inputData : payload.getInputsData()) {
            float yaw = inputData.getYaw();
            float pitch = inputData.getPitch();

            this.yaw = yaw;
            this.pitch = pitch;

            float speed = this.speed * 10.0f * (1.0f / GameConfiguration.TICK_PER_SECONDS);

            front.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
            front.y = (float) Math.sin(Math.toRadians(0.0f));
            front.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));

            front.normalize();
            Vector3f right = new Vector3f(front).cross(new Vector3f(0, 1, 0)).normalize();

            if (inputData.isMovingForward()) {
                position.add(new Vector3f(front).mul(speed));
            }

            if (inputData.isMovingBackward()) {
                position.sub(new Vector3f(front).mul(speed));
            }

            if (inputData.isMovingLeft()) {
                position.sub(new Vector3f(right).mul(speed));
            }

            if (inputData.isMovingRight()) {
                position.add(new Vector3f(right).mul(speed));
            }

            if (inputData.isFlying()) {
                position.add(new Vector3f(0.0f, .5f, 0.0f));
            }

            if (inputData.isSneaking()) {
                position.sub(new Vector3f(0.0f, .5f, 0.0f));
            }
        }
        // System.out.println("Tick " + payload.getTick() + " InputVector: " + payload.getInputVector() + " Calculated position : " + position);
    }

    public Vector3f getPosition() {
        return position;
    }

    public ObjectNode toJSONWithSkin() {
        ObjectNode node = this.toJSON();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(skin, "png", baos);
            node.put("skin", Base64.getEncoder().encodeToString(baos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return node;
    }

    public ObjectNode toJSON() {
        ObjectNode node = new ObjectMapper().createObjectNode();
        node.put("name", this.name);
        node.put("uuid", this.uuid);
        node.put("x", this.position.x);
        node.put("y", this.position.y);
        node.put("z", this.position.z);
        node.put("yaw", this.yaw);
        node.put("pitch", this.pitch);
        node.put("movingLeft", this.movingLeft);
        node.put("movingRight", this.movingRight);
        node.put("movingForward", this.movingForward);
        node.put("movingBackward", this.movingBackward);
        node.put("bodyYaw", this.bodyYaw);

        return node;
    }

    public BufferedImage getSkin() {
        return skin;
    }

    public float getBodyYaw() {
        return bodyYaw;
    }

    public int getPort() {
        return port;
    }

    public InetAddress getAddress() {
        return address;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setSkin(BufferedImage skin) {
        this.skin = skin;
    }

    public String getUuid() {
        return uuid;
    }

    public Set<Coordinates> getReceivedChunks() {
        return receivedChunks;
    }

    public PriorityQueue<ServerChunk> getNearChunks() {
        return nearChunks;
    }

    public Vector3f getLastChunkPosition() {
        return lastChunkPosition;
    }

    public void setLastChunkPosition(Vector3f lastChunkPosition) {
        this.lastChunkPosition = lastChunkPosition;
    }

    public Vector3i getInputVector() {
        return inputVector;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }
}
