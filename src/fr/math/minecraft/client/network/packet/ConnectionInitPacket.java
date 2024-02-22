package fr.math.minecraft.client.network.packet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.math.minecraft.client.Camera;
import fr.math.minecraft.client.Game;
import fr.math.minecraft.client.GameState;
import fr.math.minecraft.client.MinecraftClient;
import fr.math.minecraft.client.entity.Player;
import fr.math.minecraft.client.gui.menus.ConnectionMenu;
import fr.math.minecraft.client.gui.menus.Menu;
import fr.math.minecraft.client.network.FixedPacketSender;
import fr.math.minecraft.client.handler.TickHandler;
import fr.math.minecraft.client.manager.MenuManager;
import fr.math.minecraft.client.manager.WorldManager;
import fr.math.minecraft.client.network.PacketReceiver;
import fr.math.minecraft.client.world.loader.ChunkMeshLoader;
import fr.math.minecraft.logger.LogType;
import fr.math.minecraft.logger.LoggerUtility;
import org.apache.log4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static org.lwjgl.glfw.GLFW.*;

public class ConnectionInitPacket extends ClientPacket implements Runnable {

    private final ObjectMapper mapper;
    private final Player player;
    private final static Logger logger = LoggerUtility.getClientLogger(ConnectionInitPacket.class, LogType.TXT);;
    private String giftedId;

    public ConnectionInitPacket(Player player) {
        this.mapper = new ObjectMapper();
        this.player = player;
    }

    @Override
    public void run() {

        Game game = Game.getInstance();
        Camera camera = game.getCamera();
        MenuManager menuManager = game.getMenuManager();

        try {
            this.send();

            menuManager.close(ConnectionMenu.class);
            glfwSetInputMode(game.getWindow(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
            game.setState(GameState.PLAYING);

            player.setYaw(0.0f);
            camera.update(player);

            PacketReceiver packetReceiver = PacketReceiver.getInstance();
            packetReceiver.start();

            FixedPacketSender packetHandler = FixedPacketSender.getInstance();
            packetHandler.start();

            TickHandler tickHandler = new TickHandler();
            tickHandler.start();

            ChunkMeshLoader meshThread = new ChunkMeshLoader(game);
            meshThread.start();

        } catch (RuntimeException e) {
            e.printStackTrace();
            Menu menu = menuManager.getOpenedMenu();

            if (menu instanceof ConnectionMenu) {
                ConnectionMenu connectionMenu = (ConnectionMenu) menu;
                connectionMenu.getTitle().setText("Impossible de se connecter au serveur (timeout)");
            }
        }
    }

    @Override
    public void send() {
        MinecraftClient client = Game.getInstance().getClient();
        String message = this.toJSON();

        if (message == null)
            return;

        try {
            String data = client.sendString(message);

            JsonNode clientData = mapper.readTree(data);

            String uuid = clientData.get("uuid").asText();

            float spawnX = clientData.get("spawnX").floatValue();
            float spawnY = clientData.get("spawnY").floatValue();
            float spawnZ = clientData.get("spawnZ").floatValue();

            if (uuid.contains("USERNAME_NOT_AVAILABLE")) {
                throw new RuntimeException("Le joueur " + player.getName() + " est déjà connecté !");
            }

            if (uuid.equalsIgnoreCase("TIMEOUT_REACHED")) {
                throw new RuntimeException("Impossible d'envoyer le packet, le serveur a mis trop de temps à répondre ! (timeout)");
            }

            player.setUuid(uuid.substring(0, 36));

            player.getPosition().x = spawnX;
            player.getPosition().y = spawnY;
            player.getPosition().z = spawnZ;

            Game.getInstance().getCamera().update(player);

            logger.info("Connection initié, ID offert : " + player.getUuid());

            PlayersListPacket packet = new PlayersListPacket();
            packet.send();

            ObjectNode node = mapper.createObjectNode();
            node.put("type", "CONNECTION_INIT_ACK");
            node.put("uuid", player.getUuid());

            client.sendMessage(mapper.writeValueAsString(node));
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public String toJSON() {
        ObjectNode node = mapper.createObjectNode();
        node.put("type", "CONNECTION_INIT");
        node.put("playerName", player.getName());
        node.put("clientVersion", "1.0.0");

        BufferedImage skin = player.getSkin();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            ImageIO.write(skin, "png", baos);
            node.put("skin", Base64.getEncoder().encodeToString(baos.toByteArray()));
            baos.close();
            return mapper.writeValueAsString(node);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String getResponse() {
        return null;
    }

    public String getGiftedId() {
        return giftedId;
    }
}
