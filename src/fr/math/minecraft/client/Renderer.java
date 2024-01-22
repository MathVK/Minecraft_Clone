package fr.math.minecraft.client;

import fr.math.minecraft.client.entity.Player;
import fr.math.minecraft.client.fonts.CFont;
import fr.math.minecraft.client.gui.BlockButton;
import fr.math.minecraft.client.gui.GuiText;
import fr.math.minecraft.client.gui.menus.MainMenu;
import fr.math.minecraft.client.gui.menus.Menu;
import fr.math.minecraft.client.gui.menus.MenuBackgroundType;
import fr.math.minecraft.client.manager.FontManager;
import fr.math.minecraft.client.meshs.*;
import fr.math.minecraft.client.packet.SkinRequestPacket;
import fr.math.minecraft.client.texture.CubemapTexture;
import fr.math.minecraft.client.texture.Texture;
import fr.math.minecraft.client.vertex.Vertex;
import fr.math.minecraft.client.world.Chunk;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33.*;

public class Renderer {

    private final PlayerMesh playerMesh;
    private final FontMesh fontMesh;
    private final SkyboxMesh skyboxMesh;
    private final ButtonMesh buttonMesh;
    private final Shader playerShader;
    private final Shader chunkShader;
    private final Shader fontShader;
    private final Shader imageShader;
    private final Shader nametagTextShader;
    private final Shader nametagShader;
    private final Shader skyboxShader;
    private final Texture terrainTexture;
    private final Texture defaultSkinTexture;
    private final Texture minecraftTitleTexture;
    private final Texture widgetsTexture;
    private final ImageMesh minecraftTitleMesh;
    private final FontManager fontManager;
    private final CFont font;
    private final Map<String, Texture> skinsMap;
    private final CubemapTexture panoramaTexture;
    private String emptyText;

    public Renderer() {
        this.playerMesh = new PlayerMesh();
        this.font = new CFont(GameConfiguration.FONT_FILE_PATH, GameConfiguration.FONT_SIZE);
        this.fontMesh = new FontMesh(font);
        this.skyboxMesh = new SkyboxMesh();
        for (int i = 0; i < 256; i++) {
            emptyText += " ";
        }
        this.buttonMesh = new ButtonMesh();

        int titleWidth = (int) (1002 * .5f);
        int titleHeight = (int) (197 * .5f);

        this.minecraftTitleMesh = new ImageMesh(
                titleWidth,
                titleHeight,
                (int) (GameConfiguration.WINDOW_WIDTH - 1002 * .5f) / 2,
                (int) (GameConfiguration.WINDOW_HEIGHT - 197 * .5f - 100)
        );

        this.fontManager = new FontManager();

        this.playerShader = new Shader("res/shaders/player.vert", "res/shaders/player.frag");
        this.chunkShader = new Shader("res/shaders/chunk.vert", "res/shaders/chunk.frag");
        this.fontShader = new Shader("res/shaders/font.vert", "res/shaders/font.frag");
        this.nametagShader = new Shader("res/shaders/nametag.vert", "res/shaders/nametag.frag");
        this.nametagTextShader = new Shader("res/shaders/nametag_text.vert", "res/shaders/nametag_text.frag");
        this.skyboxShader = new Shader("res/shaders/skybox.vert", "res/shaders/skybox.frag");
        this.imageShader = new Shader("res/shaders/image.vert", "res/shaders/image.frag");

        this.terrainTexture = new Texture("res/textures/terrain.png", 1);
        this.defaultSkinTexture = new Texture("res/textures/skin.png", 2);
        this.minecraftTitleTexture = new Texture("res/textures/gui/title/minecraft_title.png", 3);
        this.widgetsTexture = new Texture("res/textures/gui/widgets.png", 5);

        String[] panoramas = new String[6];
        int[] index = new int[]{ 1, 3, 5, 4, 0, 2 };

        for (int i = 0; i < index.length; i++) {
            panoramas[i] = "res/textures/gui/title/panorama_" + index[i] + ".png";
        }

        this.panoramaTexture = new CubemapTexture(panoramas, 4);
        this.skinsMap = new HashMap<>();

        this.terrainTexture.load();
        this.defaultSkinTexture.load();
        this.minecraftTitleTexture.load();
        this.panoramaTexture.load();
        this.widgetsTexture.load();
    }

    public void render(Camera camera, Player player) {

        Texture skinTexture;

        if (skinsMap.containsKey(player.getUuid())) {
            skinTexture = skinsMap.get(player.getUuid());
        } else {
            SkinRequestPacket packet = new SkinRequestPacket(player.getUuid());
            packet.send();
            BufferedImage skin = packet.getSkin();
            if (skin != null) {
                player.setSkin(skin);
                skinTexture = new Texture(player.getSkin(), 2);
                skinTexture.load();
                skinsMap.put(player.getUuid(), skinTexture);
            } else {
                skinTexture = defaultSkinTexture;
            }
        }

        playerShader.enable();
        playerShader.sendInt("uTexture", skinTexture.getSlot());

        glActiveTexture(GL_TEXTURE0 + skinTexture.getSlot());
        skinTexture.bind();

        camera.matrix(playerShader, player);

        playerMesh.draw();

        skinTexture.unbind();

        this.renderNametag(camera, player);
    }

    public void renderNametag(Camera camera, Player player) {
        this.renderNametagBar(camera, player);
        this.renderNametagText(camera, player);
    }

    private void renderNametagBar(Camera camera, Player player) {

        NametagMesh nametagMesh = player.getNametagMesh();

        if (nametagMesh == null)
            return;

        nametagShader.enable();

        camera.matrixNametag(nametagShader, player);

        nametagMesh.draw();
    }

    private void renderNametagText(Camera camera, Player player) {
        Texture texture = font.getTexture();
        nametagTextShader.enable();
        nametagTextShader.sendInt("uTexture", texture.getSlot());

        glActiveTexture(GL_TEXTURE0 + texture.getSlot());
        texture.bind();
        camera.matrixNametag(nametagTextShader, player);

        fontManager.addText(fontMesh, player.getName(), 0, 0, 0, 1.0f, 0xFFFFFF, true);

        // fontMesh.flush();

        texture.unbind();
    }

    public void render(Camera camera, Chunk chunk) {

        chunkShader.enable();
        chunkShader.sendInt("uTexture", terrainTexture.getSlot());

        glActiveTexture(GL_TEXTURE0 + terrainTexture.getSlot());
        terrainTexture.bind();

        camera.matrix(chunkShader, chunk);

        chunk.getChunkMesh().draw();

        terrainTexture.unbind();
    }

    public void renderText(Camera camera, String text, float x, float y, int rgb, float scale) {
        this.renderText(camera, text, x, y, -10, rgb, scale, 0.0f, new Vector3i(0 , 0, 0));
    }

    public void renderText(Camera camera, String text, float x, float y, int rgb, float scale, float rotateAngle, Vector3i normal) {
        this.renderText(camera, text, x, y, -10, rgb, scale, rotateAngle, normal);
    }
    
    public void renderText(Camera camera, String text, float x, float y, float z, int rgb, float scale) {
        this.renderText(camera, text, x, y, z, rgb, scale, 0.0f, new Vector3i(0 , 0, 0));
    }

    public void renderText(Camera camera, String text, float x, float y, float z, int rgb, float scale, float rotateAngle, Vector3i normal) {
        this.renderString(camera, emptyText, -1000, -1000, 100, rgb, scale, rotateAngle, normal);
        this.renderString(camera, text, x, y, z, rgb, scale, rotateAngle, normal);
        this.renderString(camera, text, x + 2, y - 2, z - 1, 0x555555, scale, rotateAngle, normal);
    }

    private void renderString(Camera camera, String text, float x, float y, float z, int rgb, float scale, float rotateAngle, Vector3i normal) {
        Texture texture = font.getTexture();

        fontShader.enable();
        fontShader.sendInt("uTexture", texture.getSlot());

        glActiveTexture(GL_TEXTURE0 + texture.getSlot());
        texture.bind();

        camera.matrixOrtho(fontShader, rotateAngle, x, y, z, text, fontMesh, new Vector3f(normal.x, normal.y, normal.z));
        fontManager.addText(fontMesh, text, x, y, z, scale, rgb);

        fontMesh.flush();

        texture.unbind();
    }

    public void renderSkybox(Camera camera) {

        glDepthFunc(GL_LEQUAL);

        skyboxShader.enable();
        skyboxShader.sendInt("uTexture", panoramaTexture.getSlot());

        glActiveTexture(GL_TEXTURE0 + panoramaTexture.getSlot());
        panoramaTexture.bind();

        camera.matrixSkybox(skyboxShader);
        skyboxMesh.draw();

        panoramaTexture.unbind();
        glDepthFunc(GL_LESS);

    }

    public void renderImage(Camera camera, ImageMesh imageMesh, Texture texture) {
        imageShader.enable();
        imageShader.sendInt("uTexture", texture.getSlot());

        glActiveTexture(GL_TEXTURE0 + texture.getSlot());
        texture.bind();

        camera.matrixOrtho(imageShader, 0, 0);

        imageMesh.draw();

        texture.unbind();
    }

    public void renderButton(Camera camera, String text, float x, float y) {
        imageShader.enable();
        imageShader.sendInt("uTexture", widgetsTexture.getSlot());

        glActiveTexture(GL_TEXTURE0 + widgetsTexture.getSlot());
        widgetsTexture.bind();

        camera.matrixOrtho(imageShader, x, y);

        buttonMesh.draw();

        widgetsTexture.unbind();

        int width = fontManager.getTextWidth(fontMesh,.25f, text);
        float height = fontManager.getTextHeight(fontMesh,.25f, text);

        this.renderText(camera, text, x + (ButtonMesh.BUTTON_WIDTH - width) / 2.0f, y + (ButtonMesh.BUTTON_HEIGHT - height) / 2.0f, -9, 0xFFFFFF, GameConfiguration.DEFAULT_SCALE);
    }

    public FontMesh getFontMesh() {
        return fontMesh;
    }

    public void renderMenu(Camera camera, Menu menu) {

        if (menu.getBackgroundType() == MenuBackgroundType.SKYBOX_BACKGROUND) {
            this.renderSkybox(camera);
        }

        if (menu instanceof MainMenu) {
            this.renderImage(camera, minecraftTitleMesh, minecraftTitleTexture);
        }

        for (GuiText text : menu.getTexts()) {
            this.renderText(
                    camera,
                    text.getText(),
                    text.getX(),
                    text.getY(),
                    text.getZ(),
                    text.getRgb(),
                    text.getScale(),
                    text.getRotateAngle(),
                    new Vector3i(0, 0, 1)
            );
        }

        for (BlockButton button : menu.getButtons()) {
            this.renderButton(camera, button.getText(), button.getX(), button.getY());
        }
    }
}
