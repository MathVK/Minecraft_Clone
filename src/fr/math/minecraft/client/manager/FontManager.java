package fr.math.minecraft.client.manager;

import fr.math.minecraft.client.buffers.VBO;
import fr.math.minecraft.client.fonts.CharInfo;
import fr.math.minecraft.client.meshs.FontMesh;
import fr.math.minecraft.logger.LogType;
import fr.math.minecraft.logger.LoggerUtility;
import org.apache.log4j.Logger;

import static org.lwjgl.opengl.GL33.*;

public class FontManager {

    private final static Logger logger = LoggerUtility.getClientLogger(FontManager.class, LogType.TXT);


    public void addCharacter(FontMesh fontMesh, float x, float y, float z, float scale, CharInfo charInfo, int rgb) {
        if (fontMesh.getSize() >= FontMesh.BATCH_SIZE - 4) {
            fontMesh.flush();
        }

        float[] vertices = fontMesh.getVertices();

        float r = (float) ((rgb >> 16) & 0xFF) / 255.0f;
        float g = (float) ((rgb >> 8) & 0xFF) / 255.0f;
        float b = (float) ((rgb >> 0) & 0xFF) / 255.0f;

        float x0 = x;
        float y0 = y;
        float x1 = x + scale * charInfo.getWidth();
        float y1 = y + scale * charInfo.getHeight();

        float ux0 = charInfo.getTextureCoords()[0].x;
        float ux1 = charInfo.getTextureCoords()[1].x;
        float uy0 = charInfo.getTextureCoords()[0].y;
        float uy1 = charInfo.getTextureCoords()[1].y;

        int index = fontMesh.getSize() * FontMesh.VERTEX_SIZE;

        vertices[index] = x1;
        vertices[index + 1] = y0;
        vertices[index + 2] = z;
        vertices[index + 3] = r;
        vertices[index + 4] = g;
        vertices[index + 5] = b;
        vertices[index + 6] = ux1;
        vertices[index + 7] = uy0;

        index += FontMesh.VERTEX_SIZE;
        vertices[index] = x1;
        vertices[index + 1] = y1;
        vertices[index + 2] = z;
        vertices[index + 3] = r;
        vertices[index + 4] = g;
        vertices[index + 5] = b;
        vertices[index + 6] = ux1;
        vertices[index + 7] = uy1;

        index += FontMesh.VERTEX_SIZE;
        vertices[index] = x0;
        vertices[index + 1] = y1;
        vertices[index + 2] = z;
        vertices[index + 3] = r;
        vertices[index + 4] = g;
        vertices[index + 5] = b;
        vertices[index + 6] = ux0;
        vertices[index + 7] = uy1;

        index += FontMesh.VERTEX_SIZE;
        vertices[index] = x0;
        vertices[index + 1] = y0;
        vertices[index + 2] = z;
        vertices[index + 3] = r;
        vertices[index + 4] = g;
        vertices[index + 5] = b;
        vertices[index + 6] = ux0;
        vertices[index + 7] = uy0;

        fontMesh.setSize(fontMesh.getSize() + 4);
    }

    public void addText(FontMesh fontMesh, String text, float x, float y, float z, float scale, int rgb) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            CharInfo charInfo = fontMesh.getFont().getCharacter(c);
            if (charInfo.getWidth() == 0) {
                logger.error("Impossible de charger le caractère " + c + ", ce caractère est inconnu.");
                continue;
            }

            this.addCharacter(fontMesh, x, y, z, scale, charInfo, rgb);

            x += charInfo.getWidth() * scale;
        }
    }

}