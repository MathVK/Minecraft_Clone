package fr.math.minecraft.client.entity.mob;

import fr.math.minecraft.client.Game;
import fr.math.minecraft.client.animations.*;
import fr.math.minecraft.client.entity.Ray;
import fr.math.minecraft.client.entity.player.PlayerHand;
import fr.math.minecraft.client.events.PlayerMoveEvent;
import fr.math.minecraft.client.events.listeners.EntityUpdate;
import fr.math.minecraft.client.events.listeners.EventListener;
import fr.math.minecraft.client.manager.ChunkManager;
import fr.math.minecraft.client.meshs.NametagMesh;
import fr.math.minecraft.logger.LogType;
import fr.math.minecraft.logger.LoggerUtility;
import fr.math.minecraft.server.Utils;
import fr.math.minecraft.shared.GameConfiguration;
import fr.math.minecraft.shared.PlayerAction;
import fr.math.minecraft.shared.Sprite;
import fr.math.minecraft.shared.inventory.*;
import fr.math.minecraft.shared.network.GameMode;
import fr.math.minecraft.shared.network.Hitbox;
import fr.math.minecraft.shared.network.PlayerInputData;
import fr.math.minecraft.shared.world.*;
import org.apache.log4j.Logger;
import org.joml.Math;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Zombie {
    public final static float WIDTH = .75f;
    public final static float HEIGHT = 1.75f;
    public final static float DEPTH = WIDTH;
    private Vector3f position;
    private float yaw;
    private float lastYaw;
    private float bodyYaw;
    private float pitch;
    private float speed;
    private boolean movingLeft, movingRight, movingForward, movingBackward;
    private boolean flying, sneaking, canJump, canBreakBlock, canPlaceBlock, jumping, sprinting;
    private boolean movingMouse;
    private boolean droppingItem;
    private boolean placingBlock, breakingBlock;
    private boolean canHoldItem, canPlaceHoldedItem;
    private boolean debugKeyPressed, occlusionKeyPressed, interpolationKeyPressed, inventoryKeyPressed;
    private float lastMouseX, lastMouseY;
    private String name;
    private String uuid;
    private final ArrayList<Animation> animations;
    private final MiningAnimation miningAnimation;
    private BufferedImage skin;
    private final static Logger logger = LoggerUtility.getClientLogger(Zombie.class, LogType.TXT);
    private final List<EventListener> eventListeners;
    private final Vector3f velocity;
    private final Vector3f gravity;
    private float maxSpeed, maxFall;
    private final List<PlayerInputData> inputs;
    private final Hitbox hitbox;
    private Vector3f lastPosition;
    private String skinPath;
    private EntityUpdate lastUpdate;
    private int breakBlockCooldown, placeBlockCooldown;
    private Ray attackRay, buildRay, breakRay;
    private ArrayList<Vector3i> aimedPlacedBlocks;
    private ArrayList<Vector3i> aimedBreakedBlocks;
    private ArrayList<Vector3i> aimedBlocks;
    private final PlayerInventory inventory;
    private Inventory lastInventory;
    private final float health;
    private final float maxHealth;
    private PlayerAction action;
    private Sprite sprite;

    public Zombie(String name) {
        this.position = new Vector3f(0.0f, 100.0f, 0.0f);
        this.lastPosition = new Vector3f(0, 0, 0);
        this.gravity = new Vector3f(0, -0.0025f, 0);
        this.velocity = new Vector3f();
        this.inputs = new ArrayList<>();
        this.inventory = new PlayerInventory();
        this.hitbox = new Hitbox(new Vector3f(0, 0, 0), new Vector3f(0.25f, 1.0f, 0.25f));
        this.animations = new ArrayList<>();
        this.lastUpdate = new EntityUpdate(new Vector3f(position), yaw, pitch, bodyYaw);
        this.aimedBlocks = new ArrayList<>();
        this.eventListeners = new ArrayList<>();
        this.sprite = new Sprite();
        this.miningAnimation = new MiningAnimation();
        this.action = PlayerAction.MINING;
        this.yaw = 0.0f;
        this.lastYaw = 0.0f;
        this.bodyYaw = 0.0f;
        this.pitch = 0.0f;
        this.lastMouseX = 0.0f;
        this.lastMouseY = 0.0f;
        this.speed = GameConfiguration.DEFAULT_SPEED;
        this.maxSpeed = 0.03f;
        this.maxFall = 0.03f;
        this.health = 20.0f;
        this.maxHealth = 20.0f;
        this.name = name;
        this.uuid = null;
        this.movingLeft = false;
        this.movingRight = false;
        this.movingForward = false;
        this.movingBackward = false;
        this.droppingItem = false;
        this.debugKeyPressed = false;
        this.occlusionKeyPressed = false;
        this.interpolationKeyPressed = false;
        this.inventoryKeyPressed = false;
        this.canHoldItem = false;
        this.canPlaceHoldedItem = false;
        this.movingMouse = true;
        this.sneaking = false;
        this.sprinting = false;
        this.flying = false;
        this.canJump = false;
        this.canBreakBlock = true;
        this.canPlaceBlock = true;
        this.jumping = false;
        this.placingBlock = false;
        this.breakingBlock = false;
        this.skin = null;
        this.skinPath = "res/textures/zombie.png";
        this.attackRay = new Ray(GameConfiguration.ATTACK_REACH);
        this.buildRay = new Ray(GameConfiguration.BUILDING_REACH * 3);
        this.breakRay = new Ray(GameConfiguration.BUILDING_REACH * 3);
        this.aimedPlacedBlocks = new ArrayList<>();
        this.lastInventory = inventory;
        this.initAnimations();
    }

    private void initAnimations() {
        animations.add(new ZombieWalkAnimation(this));
    }

    public void resetMoving() {
        movingLeft = false;
        movingRight = false;
        movingForward = false;
        movingBackward = false;
        flying = false;
        sneaking = false;
        jumping = false;
        movingMouse = false;
        droppingItem = false;
    }

    public void updateAnimations() {
        for (Animation animation : animations) {
            animation.update();
        }
        // hotbar.getAnimation().update();
    }

    public void update() {
        this.updateAnimations();
        GameConfiguration gameConfiguration = GameConfiguration.getInstance();

        if (gameConfiguration.isEntityInterpolationEnabled()) {
            position.x = Math.lerp(position.x, lastUpdate.getPosition().x, 0.1f);
            position.y = Math.lerp(position.y, lastUpdate.getPosition().y, 0.1f);
            position.z = Math.lerp(position.z, lastUpdate.getPosition().z, 0.1f);
            yaw = Math.lerp(yaw, lastUpdate.getYaw(), 0.1f);
            pitch = Math.lerp(pitch, lastUpdate.getPitch(), 0.1f);
            bodyYaw = Math.lerp(bodyYaw, lastUpdate.getBodyYaw(), 0.1f);
        }
    }


    public void handleCollisions(World world, Vector3f velocity) {

        int minX = (int) Math.floor(position.x - hitbox.getWidth());
        int maxX = (int) Math.ceil(position.x + hitbox.getWidth());
        int minY = (int) Math.floor(position.y - hitbox.getHeight());
        int maxY = (int) Math.ceil(position.y + hitbox.getHeight());
        int minZ = (int) Math.floor(position.z - hitbox.getDepth());
        int maxZ = (int) Math.ceil(position.z + hitbox.getDepth());

        for (int worldX = minX; worldX < maxX; worldX++) {
            for (int worldY = minY; worldY < maxY; worldY++) {
                for (int worldZ = minZ; worldZ < maxZ; worldZ++) {

                    byte block = world.getBlockAt(worldX, worldY, worldZ);
                    Material material = Material.getMaterialById(block);

                    if (!material.isSolid()) {
                        continue;
                    }

                    if (velocity.x > 0) {
                        position.x = worldX - hitbox.getWidth();
                    } else if (velocity.x < 0) {
                        position.x = worldX + hitbox.getWidth() + 1;
                    }

                    if (velocity.y > 0) {
                        position.y = worldY - hitbox.getHeight();
                        this.velocity.y = 0;
                    } else if (velocity.y < 0) {
                        maxFall = 0.03f;
                        canJump = true;
                        position.y = worldY + hitbox.getHeight() + 1;
                        this.velocity.y = 0;
                    }

                    if (velocity.z > 0) {
                        position.z = worldZ - hitbox.getDepth();
                    } else if (velocity.z < 0) {
                        position.z = worldZ + hitbox.getDepth() + 1;
                    }
                }
            }
        }
    }

    public void updatePosition(World world) {

        Vector3f front = new Vector3f();
        front.x = (float) (Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));
        front.y = (float) Math.sin(Math.toRadians(0.0f));
        front.z = (float) (Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)));

        front.normalize();

        Vector3f right = new Vector3f(front).cross(new Vector3f(0, 1, 0)).normalize();
        Vector3f acceleration = new Vector3f(0, 0, 0);

        velocity.add(gravity);

        if (movingForward) {
            acceleration.add(front);
        }

        if (movingBackward) {
            acceleration.sub(front);
        }

        if (movingLeft) {
            acceleration.sub(right);
        }

        if (movingRight) {
            acceleration.add(right);
        }

        if (flying) {
            acceleration.add(new Vector3f(0.0f, .5f, 0.0f));
        }

        if (sneaking) {
            acceleration.sub(new Vector3f(0.0f, .5f, 0.0f));
        }

        if (jumping) {
            // handleJump();
            if (canJump) {
                maxFall = 0.5f;
                acceleration.y += 10.0f;
                canJump = false;
            }
        }

        velocity.add(acceleration.mul(speed));

        if (new Vector3f(velocity.x, 0, velocity.z).length() > maxSpeed) {
            Vector3f velocityNorm = new Vector3f(velocity.x, velocity.y, velocity.z);
            velocityNorm.normalize().mul(maxSpeed);
            velocity.x = velocityNorm.x;
            velocity.z = velocityNorm.z;
        }

        if (new Vector3f(0, velocity.y, 0).length() > maxFall) {
            Vector3f velocityNorm = new Vector3f(velocity.x, velocity.y, velocity.z);
            velocityNorm.normalize().mul(maxFall);
            velocity.y = velocityNorm.y;
        }

        position.x += velocity.x;
        handleCollisions(world, new Vector3f(velocity.x, 0, 0));

        position.z += velocity.z;
        handleCollisions(world, new Vector3f(0, 0, velocity.z));

        position.y += velocity.y;
        handleCollisions(world, new Vector3f(0, velocity.y, 0));

        velocity.mul(0.95f);

        PlayerInputData inputData = new PlayerInputData(movingLeft, movingRight, movingForward, movingBackward, flying, sneaking, jumping, yaw, pitch, sprinting, placingBlock, breakingBlock, droppingItem);
        inputs.add(inputData);
    }

    public boolean isMoving() {
        return movingLeft || movingRight || movingForward || movingBackward || sneaking || flying || jumping;
    }

    public float getSpeed() {
        return speed;
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getVelocity() {
        return velocity;
    }

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setPosition(Vector3f position) {
        this.position = position;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public void setMovingLeft(boolean movingLeft) {
        this.movingLeft = movingLeft;
    }

    public void setMovingRight(boolean movingRight) {
        this.movingRight = movingRight;
    }

    public void setMovingForward(boolean movingForward) {
        this.movingForward = movingForward;
    }

    public void setMovingBackward(boolean movingBackward) {
        this.movingBackward = movingBackward;
    }

    public ArrayList<Animation> getAnimations() {
        return animations;
    }

    public BufferedImage getSkin() {
        return skin;
    }

    public void setSkin(BufferedImage skin) {
        this.skin = skin;
    }

    public float getBodyYaw() {
        return bodyYaw;
    }

    public void setBodyYaw(float bodyYaw) {
        this.bodyYaw = bodyYaw;
    }

    public void notifyEvent(PlayerMoveEvent event) {
        for (EventListener eventListener : eventListeners) {
            eventListener.onPlayerMove(event);
        }
    }

    public void addEventListener(EventListener event) {
        eventListeners.add(event);
    }

    public void removeEventListener(EventListener event) {
        eventListeners.remove(event);
    }

    public boolean isFlying() {
        return flying;
    }

    public boolean isSneaking() {
        return sneaking;
    }

    public List<PlayerInputData> getInputs() {
        return inputs;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public Vector3f getGravity() {
        return gravity;
    }

    public Vector3f getLastPosition() {
        return lastPosition;
    }

    public void setLastPosition(Vector3f lastPosition) {
        this.lastPosition = lastPosition;
    }

    public String getSkinPath() {
        return skinPath;
    }

    public void setSkinPath(String skinPath) {
        this.skinPath = skinPath;
    }

    public EntityUpdate getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(EntityUpdate lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public boolean canJump() {
        return this.canJump;
    }

    public void setCanJump(boolean canJump) {
        this.canJump = canJump;
    }

    public float getMaxFallSpeed() {
        return maxFall;
    }

    public void setMaxFallSpeed(float maxFall) {
        this.maxFall = maxFall;
    }

    public Ray getAttackRay() {
        return attackRay;
    }

    public Ray getBuildRay() {
        return buildRay;
    }

    public Ray getBreakRay() {
        return breakRay;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public ArrayList<Vector3i> getAimedPlacedBlocks() {
        return aimedPlacedBlocks;
    }

    public void setAimedPlacedBlocks(ArrayList<Vector3i> aimedPlacedBlocks) {
        this.aimedPlacedBlocks = aimedPlacedBlocks;
    }

    public PlayerInventory getInventory() {
        return inventory;
    }

    public float getHealth() {
        return health;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public PlayerAction getAction() {
        return action;
    }

    public void setAction(PlayerAction action) {
        this.action = action;
    }

    public Sprite getSprite() {
        return sprite;
    }

    public void setSprite(Sprite sprite) {
        this.sprite = sprite;
    }

    public MiningAnimation getMiningAnimation() {
        return miningAnimation;
    }

    public boolean canPlaceHoldedItem() {
        return canPlaceHoldedItem;
    }

    public void setCanPlaceHoldedItem(boolean canPlaceHoldedItem) {
        this.canPlaceHoldedItem = canPlaceHoldedItem;
    }

    public boolean canHoldItem() {
        return canHoldItem;
    }

    public void setCanHoldItem(boolean canHoldItem) {
        this.canHoldItem = canHoldItem;
    }

    public Inventory getLastInventory() {
        return lastInventory;
    }

    public void setLastInventory(Inventory lastInventory) {
        this.lastInventory = lastInventory;
    }
}
