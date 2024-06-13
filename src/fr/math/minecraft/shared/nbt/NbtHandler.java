package fr.math.minecraft.shared.nbt;

import fr.math.minecraft.shared.world.Material;
import org.jnbt.*;
import org.joml.Vector3i;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NbtHandler {

    private String filePath;
    private Tag mainTag;

    private HashMap<Integer, Material> mappingStruc;

    public NbtHandler(String filePath) {
        this.filePath = filePath;
        try (FileInputStream fis = new FileInputStream(filePath)){
            NBTInputStream nbtInputStream = new NBTInputStream(fis);
            this.mainTag = nbtInputStream.readTag();
        } catch (IOException e) {
        }
        this.mappingStruc = new HashMap<>();
    }

    public CompoundTag getCompoundTag() {
        if(mainTag instanceof CompoundTag) {
            CompoundTag compound = (CompoundTag) mainTag;
            return compound;
        } else {
            System.out.println("Le ficher nbt ne contient pas de Tag Compound");
            return null;
        }
    }

    public CompoundTag getCompoundTag(Tag mainTag) {
        if(mainTag instanceof CompoundTag) {
            CompoundTag compound = (CompoundTag) mainTag;
            return compound;
        } else {
            System.out.println("Le ficher nbt ne contient pas de Tag Compound");
            return null;
        }
    }

    public Tag getNbtTagValue(CompoundTag compoundTag, String nameTag) {
        for (Map.Entry<String, Tag> entry : compoundTag.getValue().entrySet()) {
            if(entry.getKey().equals(nameTag)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public ByteArrayTag getNbtBlocksArray(CompoundTag compoundTag) {
        if(compoundTag.getValue().containsKey("Blocks")) {
            ByteArrayTag blocksArray = (ByteArrayTag) compoundTag.getValue().get("Blocks");
            return blocksArray;
        } else {
            return null;
        }
    }

    public ArrayList<Byte> getCleanNbtBlocksArray(CompoundTag compoundTag) {
        if(compoundTag.getValue().containsKey("Blocks")) {
            ByteArrayTag blocksArray = (ByteArrayTag) compoundTag.getValue().get("Blocks");
            ArrayList<Byte> blockList = new ArrayList<>();
            System.out.println(blocksArray);
            for (int i = 0; i < blocksArray.getValue().length; i++) {
                blockList.add(blocksArray.getValue()[i]);
            }
            System.out.println(blockList);
            return blockList;
        } else {
            return null;
        }
    }

    public ShortTag getNbtLength(CompoundTag compoundTag) {
        if(compoundTag.getValue().containsKey("Length")) {
            ShortTag length = (ShortTag) compoundTag.getValue().get("Length");
            System.out.println("len :" + length);
            return length;
        } else {
            return null;
        }
    }

    public ShortTag getNbtWidth(CompoundTag compoundTag) {
        if(compoundTag.getValue().containsKey("Width")) {
            ShortTag width = (ShortTag) compoundTag.getValue().get("Width");
            System.out.println("width :" + width);
            return width;
        } else {
            return null;
        }
    }

    public Vector3i getBlockPosition(int indice, short length, short width){
        Vector3i blockPosition = new Vector3i();

        int x = indice % width;
        System.out.println("x:"+x);
        int reste = (indice - x)/width;
        System.out.println("reste:"+reste);
        int z = reste % length;
        System.out.println("z:"+z);
        int y = (reste - z)/length;
        System.out.println("y:"+y +"\n");

        blockPosition.x = x;
        blockPosition.y = y;
        blockPosition.z = z;

        return blockPosition;
    }

    public void setMappingStruc(CompoundTag compoundTag) {
        if(compoundTag.getValue().containsKey("SchematicaMapping")) {
            CompoundTag mapping = (CompoundTag) compoundTag.getValue().get("SchematicaMapping");
            for (Map.Entry<String, Tag> entry : mapping.getValue().entrySet()) {
                ShortTag minecraftBloc = (ShortTag) entry.getValue();

                String blockName = minecraftBloc.getName();
                blockName = blockName.replaceAll("minecraft:", "");
                int blokcID = minecraftBloc.getValue();

                Material blockMaterial = Material.getMaterialByName(blockName);
                if(blockMaterial != null && !mappingStruc.containsKey(blockMaterial)) {
                    mappingStruc.put(blokcID, blockMaterial);
                } else if(blockMaterial == null){
                    mappingStruc.put(blokcID, Material.DEBUG);
                }

            }
        }
    }

    public HashMap<Integer, Material> getMappingStruc() {
        return mappingStruc;
    }
}