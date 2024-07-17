/**
*  Minecraft Clone Math edition : Cybersecurity - A serious game to learn network and cybersecurity
*  Copyright (C) 2024 MeAndTheHomies (Math)
*
*  This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
*
*  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
*
*  You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package fr.math.minecraft.client.world.worker;

import fr.math.minecraft.client.Game;
import fr.math.minecraft.client.meshs.ChunkMesh;
import fr.math.minecraft.client.meshs.WaterMesh;
import fr.math.minecraft.shared.world.Chunk;
import fr.math.minecraft.shared.world.World;

public class ChunkMeshWorker implements Runnable {

    private final Chunk chunk;

    public ChunkMeshWorker(Chunk chunk) {
        this.chunk = chunk;
    }

    @Override
    public void run() {
        Game game = Game.getInstance();
        World world = game.getWorld();
        // Coordinates chunkPosition = new Coordinates(chunk.getPosition().x, chunk.getPosition().y, chunk.getPosition().z);
        ChunkMesh chunkMesh = new ChunkMesh(chunk);
        WaterMesh waterMesh = new WaterMesh(chunk);
        synchronized (world.getChunks()) {
            chunk.setMesh(chunkMesh);
            chunk.setWaterMesh(waterMesh);
        }
        chunk.setLoaded(true);
        // world.getLoadingChunks().remove(chunkPosition);
    }

    public Chunk getChunk() {
        return chunk;
    }
}
