package fr.math.minecraft.client.handler;

import fr.math.minecraft.client.Game;
import fr.math.minecraft.client.GameConfiguration;
import fr.math.minecraft.client.entity.Player;
import fr.math.minecraft.client.manager.WorldManager;
import fr.math.minecraft.shared.network.PlayerInputData;
import fr.math.minecraft.client.world.World;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class TickHandler extends Thread {

    public TickHandler() {
        this.setName("TickHandler");
    }

    @Override
    public void run() {

        Game game = Game.getInstance();
        WorldManager worldManager = game.getWorldManager();
        World world = game.getWorld();
        double lastTime = glfwGetTime();
        double tickTimer = 0;

        while (!glfwWindowShouldClose(game.getWindow())) {

            double currentTime = glfwGetTime();
            double deltaTime = currentTime - lastTime;

            tickTimer += deltaTime;

            lastTime = currentTime;

            while (tickTimer >= GameConfiguration.TICK_RATE) {
                tickTimer -= GameConfiguration.TICK_RATE;
            }
        }
    }
}