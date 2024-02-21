package fr.math.minecraft.client.meshs;

import fr.math.minecraft.client.buffers.EBO;
import fr.math.minecraft.client.buffers.VAO;
import fr.math.minecraft.client.buffers.VBO;
import fr.math.minecraft.client.builder.MeshBuilder;
import fr.math.minecraft.client.world.Chunk;

import java.util.ArrayList;

import static org.lwjgl.opengl.GL33.*;

public class ChunkMesh extends Mesh {

    private boolean chunkMeshInitiated;

    public ChunkMesh(Chunk chunk) {
        ArrayList<Integer> elements = new ArrayList<>();
        vertices = new MeshBuilder().buildChunkMesh(chunk, elements);
        this.indices = elements.stream().mapToInt(Integer::valueOf).toArray();
        this.chunkMeshInitiated = false;
        //this.init();
    }

    @Override
    public void init() {
        vao = new VAO();
        vao.bind();

        vbo = new VBO(vertices);
        ebo = new EBO(indices);

        vao.linkAttrib(vbo, 0, 3, GL_FLOAT, 7 * Float.BYTES, 0);
        vao.linkAttrib(vbo, 1, 2, GL_FLOAT, 7 * Float.BYTES, 3 * Float.BYTES);
        vao.linkAttrib(vbo, 2, 1, GL_FLOAT, 7 * Float.BYTES, 5 * Float.BYTES);
        vao.linkAttrib(vbo, 3, 1, GL_FLOAT, 7 * Float.BYTES, 6 * Float.BYTES);

        vao.unbind();
        vbo.unbind();
        ebo.unbind();

        chunkMeshInitiated = true;
    }

    @Override
    public void draw() {
        vao.bind();
        glDrawElements(GL_TRIANGLES, indices.length, GL_UNSIGNED_INT, 0);
        vao.unbind();
    }

    public void delete() {
        vao.delete();
        vbo.delete();
    }

    public boolean isInitiated() {
        return chunkMeshInitiated;
    }

    public void setChunkMeshInitiated(boolean chunkMeshInitiated) {
        this.chunkMeshInitiated = chunkMeshInitiated;
    }
}
