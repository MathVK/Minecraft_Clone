package fr.math.minecraft.server.builder;

import fr.math.minecraft.server.RandomSeed;
import fr.math.minecraft.server.world.Material;
import fr.math.minecraft.server.world.ServerChunk;
import fr.math.minecraft.server.world.biome.PlainBiome;

public class  StructureBuilder {

    public static void buildSimpleTree(ServerChunk chunk, int x, int y, int z) {

        /*-------------------Etage 7--------------------------------*/
        chunk.setBlock(x+1, y+7, z, Material.OAK_LEAVES.getId());
        chunk.setBlock(x-1, y+7, z, Material.OAK_LEAVES.getId());
        chunk.setBlock(x, y+7, z+1, Material.OAK_LEAVES.getId());
        chunk.setBlock(x, y+7, z-1, Material.OAK_LEAVES.getId());
        chunk.setBlock(x, y+7, z, Material.OAK_LEAVES.getId());

        /*-------------------Etage 6--------------------------------*/
        chunk.setBlock(x+1, y+6, z, Material.OAK_LEAVES.getId());
        chunk.setBlock(x-1, y+6, z, Material.OAK_LEAVES.getId());
        chunk.setBlock(x, y+6, z+1, Material.OAK_LEAVES.getId());
        chunk.setBlock(x, y+6, z-1, Material.OAK_LEAVES.getId());
        chunk.setBlock(x-1, y+6, z-1, Material.OAK_LEAVES.getId());

        /*-------------------Etage 5--------------------------------*/
        chunk.setBlock(x+1, y+5, z-1, Material.OAK_LEAVES.getId());
        chunk.setBlock(x+1, y+5, z-2, Material.OAK_LEAVES.getId());
        chunk.setBlock(x+1, y+5, z, Material.OAK_LEAVES.getId());
        chunk.setBlock(x+1, y+5, z+1, Material.OAK_LEAVES.getId());
        chunk.setBlock(x+1, y+5, z+2, Material.OAK_LEAVES.getId());

        chunk.setBlock(x+2, y+5, z-1, Material.OAK_LEAVES.getId());
        chunk.setBlock(x+2, y+5, z, Material.OAK_LEAVES.getId());
        chunk.setBlock(x+2, y+5, z+1, Material.OAK_LEAVES.getId());

        chunk.setBlock(x, y+5, z+2, Material.OAK_LEAVES.getId());
        chunk.setBlock(x, y+5, z+1, Material.OAK_LEAVES.getId());
        chunk.setBlock(x, y+5, z-1, Material.OAK_LEAVES.getId());
        chunk.setBlock(x, y+5, z-2, Material.OAK_LEAVES.getId());

        chunk.setBlock(x-1, y+5, z+2, Material.OAK_LEAVES.getId());
        chunk.setBlock(x-1, y+5, z+1, Material.OAK_LEAVES.getId());
        chunk.setBlock(x-1, y+5, z, Material.OAK_LEAVES.getId());
        chunk.setBlock(x-1, y+5, z-1, Material.OAK_LEAVES.getId());
        chunk.setBlock(x-1, y+5, z-2, Material.OAK_LEAVES.getId());

        chunk.setBlock(x-2, y+5, z+1, Material.OAK_LEAVES.getId());
        chunk.setBlock(x-2, y+5, z, Material.OAK_LEAVES.getId());
        chunk.setBlock(x-2, y+5, z-1, Material.OAK_LEAVES.getId());
        chunk.setBlock(x-2, y+5, z-2, Material.OAK_LEAVES.getId());

        /*-------------------Etage 4--------------------------------*/
        for (int i = -2; i <= 2 ; i++) {
            for (int j = -2 ; j <= 2 ; j++) {
                chunk.setBlock(x+i, y+4, z+j, Material.OAK_LEAVES.getId());
            }
        }

        /*-------------------Tronc--------------------------------*/
        int treeSize = 6;
        for (int i = 1; i <= treeSize; i++) {
            chunk.setBlock(x, y+i, z, Material.OAK_LOG.getId());
        }
    }

    public static void buildSimpleCactus(ServerChunk chunk, int x, int y, int z) {
        RandomSeed randomSeed = RandomSeed.getInstance();
        int cactusSize = randomSeed.nextInt(3, 5);
        float cactusSizeDrop = randomSeed.nextFloat(100);
        if(cactusSizeDrop <= 99) {
            for(int i = 1; i<=cactusSize;i++){
                chunk.setBlock(x,y+i,z,Material.CACTUS.getId());
                chunk.setBlock(x-1,y+i,z-1,Material.AIR.getId());
                chunk.setBlock(x,y+i,z-1,Material.AIR.getId());
                chunk.setBlock(x + 1,y+i,z-1,Material.AIR.getId());
                chunk.setBlock(x-1,y+i,z,Material.AIR.getId());
                chunk.setBlock(x+1,y+i,z,Material.AIR.getId());
                chunk.setBlock(x-1,y+i,z+1,Material.AIR.getId());
                chunk.setBlock(x,y+i,z+1,Material.AIR.getId());
                chunk.setBlock(x+1,y+i,z+1,Material.AIR.getId());
            }
        } else {
            for(int i = 1; i<=cactusSize;i++){
                chunk.setBlock(x,y+i,z,Material.CACTUS.getId());
                chunk.setBlock(x-1,y+i,z-1,Material.AIR.getId());
                chunk.setBlock(x,y+i,z-1,Material.AIR.getId());
                chunk.setBlock(x + 1,y+i,z-1,Material.AIR.getId());
                chunk.setBlock(x-1,y+i,z,Material.AIR.getId());
                chunk.setBlock(x+1,y+i,z,Material.AIR.getId());
                chunk.setBlock(x-1,y+i,z+1,Material.AIR.getId());
                chunk.setBlock(x,y+i,z+1,Material.AIR.getId());
                chunk.setBlock(x+1,y+i,z+1,Material.AIR.getId());
            }
            chunk.setBlock(x-1,y+1,z,Material.CACTUS.getId());
            chunk.setBlock(x+1,y+1,z,Material.CACTUS.getId());
        }
    }

    public static void buildWeed(ServerChunk chunk, int x, int y, int z) {
        RandomSeed randomSeed = RandomSeed.getInstance();
        float dropRate = randomSeed.nextFloat() * 100.0f;
        if(dropRate < 5) {
            chunk.setBlock(x, y+1, z, Material.ROSE.getId());
        } else {
            chunk.setBlock(x, y+1, z, Material.WEED.getId());
        }
    }
}
