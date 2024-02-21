package fr.math.minecraft.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.math.minecraft.client.world.Coordinates;
import fr.math.minecraft.client.world.Material;
import fr.math.minecraft.server.payload.InputPayload;
import fr.math.minecraft.server.payload.StatePayload;
import fr.math.minecraft.server.worker.ChunkSender;
import fr.math.minecraft.server.world.ServerChunk;
import fr.math.minecraft.server.world.ServerChunkComparator;
import fr.math.minecraft.server.world.ServerWorld;
import fr.math.minecraft.shared.GameConfiguration;
import fr.math.minecraft.shared.network.Hitbox;
import fr.math.minecraft.shared.network.PlayerInputData;
import org.joml.Vector3f;
import org.joml.Vector3i;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

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
    private final Vector3f velocity;
    private final Vector3f gravity;

    private final Vector3f acceleration;
    private float Vmax;
    private final Set<Coordinates> receivedChunks;
    private final PriorityQueue<ServerChunk> nearChunks;
    private final Hitbox hitbox;
    private Vector3f lastChunkPosition;
    private final Queue<InputPayload> inputQueue;
    private final StatePayload[] stateBuffer;
    private final Map<Vector3i, ChunkSender> sendedChunks;

    public Client(String uuid, String name, InetAddress address, int port) {
        this.address = address;
        this.port = port;
        this.uuid = uuid;
        this.name = name;
        this.velocity = new Vector3f();
        this.inputQueue = new LinkedList<>();
        this.gravity = new Vector3f(0, -0.025f, 0);
        this.acceleration = new Vector3f();
        this.front = new Vector3f(0.0f, 0.0f, 0.0f);
        this.position = new Vector3f(0.0f, 100.0f, 0.0f);
        this.lastChunkPosition = new Vector3f(0, 0, 0);
        this.inputVector = new Vector3i(0, 0, 0);
        this.receivedChunks = new HashSet<>();
        this.sendedChunks = new HashMap<>();
        this.nearChunks = new PriorityQueue<>(new ServerChunkComparator(this));
        this.hitbox = new Hitbox(new Vector3f(0, 0, 0), new Vector3f(0.25f, 1.0f, 0.25f));
        this.stateBuffer = new StatePayload[GameConfiguration.BUFFER_SIZE];
        this.yaw = 0.0f;
        this.pitch = 0.0f;
        this.speed = 0.01f;
        this.Vmax = 1.0f;
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

    public void handleCollisions(Vector3f velocity) {
        MinecraftServer server = MinecraftServer.getInstance();
        ServerWorld world = server.getWorld();
        for (int worldX = (int) (position.x - hitbox.getWidth()) ; worldX < position.x + hitbox.getWidth() ; worldX++) {
            for (int worldY = (int) (position.y - hitbox.getHeight()) ; worldY < position.y + hitbox.getHeight() ; worldY++) {
                for (int worldZ = (int) (position.z - hitbox.getDepth()) ; worldZ < position.z + hitbox.getDepth() ; worldZ++) {

                    byte block = world.getBlockAt(worldX, worldY, worldZ);
                    Material material = Material.getMaterialById(block);

                    if (!material.isSolid()) {
                        continue;
                    }

                    if (velocity.x > 0) {
                        position.x = worldX - hitbox.getWidth();
                    } else if(velocity.x<0) {
                        position.x = worldX + hitbox.getWidth();
                    }

                    if (velocity.y > 0) {
                        position.y = worldY - hitbox.getHeight();
                        this.velocity.y = 0;
                    } else if(velocity.y<0) {
                        position.y = worldY + hitbox.getHeight() + 1;
                        this.velocity.y=0;
                    }

                    if (velocity.z > 0) {
                        position.z = worldZ - hitbox.getDepth();
                    } else if(velocity.z < 0) {
                        position.z = worldZ + hitbox.getDepth();
                    }
                }
            }
        }
    }

    public void updatePosition(InputPayload payload) {

        for (PlayerInputData inputData : payload.getInputsData()) {
            float yaw = inputData.getYaw();
            float pitch = inputData.getPitch();

            this.yaw = yaw;
            this.pitch = pitch;

            Vector3f acceleration = new Vector3f(0,0,0);

            front.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
            front.y = (float) Math.sin(Math.toRadians(0.0f));
            front.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));

            front.normalize();
            Vector3f right = new Vector3f(front).cross(new Vector3f(0, 1, 0)).normalize();

            //velocity.add(gravity);

            if (inputData.isMovingForward()) {
                acceleration.add(front);
            }

            if (inputData.isMovingBackward()) {
                acceleration.sub(front);
            }

            if (inputData.isMovingLeft()) {
                acceleration.sub(right);
            }

            if (inputData.isMovingRight()) {
                acceleration.add(right);
            }

            if (inputData.isFlying()) {
                acceleration.add(new Vector3f(0.0f, .5f, 0.0f));
            }

            if (inputData.isSneaking()) {
                acceleration.sub(new Vector3f(0.0f, .1f, 0.0f));
            }

            velocity.add(acceleration.mul(speed));

            if (velocity.length()>Vmax) {
                velocity.normalize().mul(Vmax);
            }

            position.x += velocity.x;
            handleCollisions(new Vector3f(velocity.x, 0, 0));

            position.z += velocity.z;
            handleCollisions(new Vector3f(0, 0, velocity.z));

            position.y += velocity.y;
            handleCollisions(new Vector3f(0, velocity.y, 0));

            velocity.mul(0.95f);
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

    public Vector3f getVelocity() {
        return velocity;
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

    public Queue<InputPayload> getInputQueue() {
        return inputQueue;
    }

    public StatePayload[] getStateBuffer() {
        return stateBuffer;
    }

    public Map<Vector3i, ChunkSender> getSendedChunks() {
        return sendedChunks;
    }
}
