package fr.math.minecraft.client.network;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.math.minecraft.client.Game;
import fr.math.minecraft.client.MinecraftClient;
import fr.math.minecraft.client.entity.player.Player;
import fr.math.minecraft.client.events.*;
import fr.math.minecraft.client.events.listeners.EventListener;
import fr.math.minecraft.client.events.listeners.PacketEventListener;
import fr.math.minecraft.client.events.listeners.PacketListener;
import fr.math.minecraft.client.events.listeners.PlayerListener;
import fr.math.minecraft.client.network.payload.StatePayload;
import fr.math.minecraft.logger.LogType;
import fr.math.minecraft.logger.LoggerUtility;
import fr.math.minecraft.shared.world.Material;
import org.apache.log4j.Logger;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.io.IOException;
import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;

public class PacketReceiver extends Thread {

    private final Game game;
    private final ArrayList<PacketEventListener> packetListeners;
    private final ArrayList<EventListener> eventListeners;
    private final static Logger logger = LoggerUtility.getClientLogger(PacketReceiver.class, LogType.TXT);
    private static PacketReceiver instance = null;
    private boolean receiving;
    private int ping;
    private double lastPingTime;

    private PacketReceiver() {
        this.setName("PacketReceiver");
        this.game = Game.getInstance();
        this.packetListeners = new ArrayList<>();
        this.eventListeners = new ArrayList<>();
        this.receiving = false;
        this.lastPingTime = System.currentTimeMillis();

        this.addEventListener(new PacketListener());
        this.addEventListener(new PlayerListener(game));
    }

    @Override
    public void run() {
        long lastTime = System.currentTimeMillis();
        long lastUpdateTime = System.currentTimeMillis();
        long timer = 0;
        while (!glfwWindowShouldClose(game.getWindow())) {

            long currentTime = System.currentTimeMillis();
            long deltaTime = currentTime - lastTime;

            lastTime = currentTime;

            timer += deltaTime;

            if (currentTime - lastUpdateTime >= 1000) {
                game.getPlayer().setPing(ping);
                lastUpdateTime = currentTime;
            }

            this.handlePacket();
        }
    }

    private void handlePacket() {
        MinecraftClient client = Game.getInstance().getClient();
        ObjectMapper mapper = new ObjectMapper();

        try {
            String response = client.receive();

            if (response == null) return;

            JsonNode responseData = mapper.readTree(response);
            String packetType = responseData.get("type").asText();

            switch (packetType) {
                case "PLAYER_JOIN":
                    this.notifyEvent(new PlayerJoinEvent(responseData));
                    break;
                case "PLAYERS_LIST":
                    int tick;
                    if (responseData.get("tick") == null) {
                        tick = 0;
                    } else  {
                        tick = responseData.get("tick").asInt();
                    }
                    this.notifyEvent(new PlayerListPacketEvent(tick, (ArrayNode) responseData.get("players")));
                    break;
                case "STATE_PAYLOAD":
                    game.getPacketPool().submit(() -> {
                        StatePayload statePayload = new StatePayload(responseData);
                        this.notifyEvent(new ServerStateEvent(statePayload));
                    });
                    break;
                case "SKIN_PACKET":
                    String base64Skin = responseData.get("skin").asText();
                    String playerUuid = responseData.get("uuid").asText();
                    this.notifyEvent(new SkinPacketEvent(base64Skin, playerUuid));
                    break;
                case "PLAYER_STATE":
                    game.getPacketPool().submit(() -> {
                        Player player = game.getPlayers().get(responseData.get("uuid").asText());
                        if (player == null) {
                            return;
                        }
                        StatePayload payload = new StatePayload(responseData);
                        this.notifyEvent(new PlayerStateEvent(player, payload));
                    });
                    break;
                case "PING_REPLY":
                    long receivedTime = responseData.get("receivedTime").longValue();
                    long sentTime = responseData.get("sentTime").longValue();
                    long currentTime = System.currentTimeMillis();

                    this.ping = (int) (currentTime - sentTime);
                    break;
                case "PLAYER_BREAK_EVENT":
                    System.out.println(responseData);
                    ArrayNode blocksData = (ArrayNode) responseData.get("aimedBlocks");
                    Player player = game.getPlayers().get(responseData.get("uuid").asText());
                    for (int i = 0; i < blocksData.size(); i++) {
                        JsonNode node = blocksData.get(i);
                        Vector3i blockPosition = new Vector3i(node.get("x").asInt(), node.get("y").asInt(), node.get("z").asInt());
                        this.notifyEvent(new BlockBreakEvent(player, blockPosition));
                    }
                    break;
                case "DROPPED_ITEM_STATE":
                    String uuid = responseData.get("uuid").asText();
                    float itemX = responseData.get("x").floatValue();
                    float itemY = responseData.get("y").floatValue();
                    float itemZ = responseData.get("z").floatValue();
                    byte materialID = (byte) responseData.get("materialID").asInt();
                    Material material = Material.getMaterialById(materialID);

                    this.notifyEvent(new DroppedItemEvent(game.getWorld(), uuid, material, new Vector3f(itemX, itemY, itemZ)));
                    break;
                default:
                    logger.warn("Le packet " + packetType + " est inconnu et a été ignoré.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void notifyEvent(PlayerStateEvent event) {
        for (PacketEventListener listener : packetListeners) {
            listener.onPlayerState(event);
        }
    }

    private void notifyEvent(DroppedItemEvent event) {
        for (PacketEventListener listener : packetListeners) {
            listener.onDroppedItemState(event);
        }
    }

    private void notifyEvent(PlayerJoinEvent event) {
        for (EventListener listener : eventListeners) {
            listener.onPlayerJoin(event);
        }
    }

    private void notifyEvent(ServerStateEvent event) {
        for (PacketEventListener listener : packetListeners) {
            listener.onServerState(event);
        }
    }

    private void notifyEvent(ChunkPacketEvent event) {
        for (PacketEventListener listener : packetListeners) {
            listener.onChunkPacket(event);
        }
    }

    private void notifyEvent(PlayerListPacketEvent event) {
        for (PacketEventListener listener : packetListeners) {
            listener.onPlayerListPacket(event);
        }
    }

    private void notifyEvent(SkinPacketEvent event) {
        for (PacketEventListener listener : packetListeners) {
            listener.onSkinPacket(event);
        }
    }

    private void notifyEvent(BlockBreakEvent event) {
        for (EventListener listener : eventListeners) {
            listener.onBlockBreak(event);
        }
    }

    public void addEventListener(EventListener listener) {
        eventListeners.add(listener);
    }

    public void addEventListener(PacketEventListener listener) {
        packetListeners.add(listener);
    }

    public static PacketReceiver getInstance() {
        if (instance == null) {
            instance = new PacketReceiver();
        }
        return instance;
    }

    public boolean isReceiving() {
        return receiving;
    }

    public void setReceiving(boolean receiving) {
        this.receiving = receiving;
    }
}
