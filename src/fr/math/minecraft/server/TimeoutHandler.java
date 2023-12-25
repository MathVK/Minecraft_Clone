package fr.math.minecraft.server;

import fr.math.minecraft.logger.LogType;
import fr.math.minecraft.logger.LoggerUtility;
import org.apache.log4j.Logger;

import java.util.Map;

public class TimeoutHandler extends Thread {

    private final MinecraftServer server;
    private final static double TIMEOUT_DELAY_MS = 1000;

    private final String uuid;
    private final Logger logger;

    public TimeoutHandler(MinecraftServer server, String uuid) {
        this.server = server;
        this.uuid = uuid;
        this.logger = LoggerUtility.getServerLogger(TimeoutHandler.class, LogType.TXT);
    }

    @Override
    public void run() {
        boolean timeout = false;
        while (!timeout) {
            long currentTime = System.currentTimeMillis();
            synchronized (server.getLastActivities()) {
                long lastTimeSeen = server.getLastActivities().get(uuid);
                if (currentTime - lastTimeSeen > TIMEOUT_DELAY_MS) {
                    System.out.println(currentTime - lastTimeSeen);
                    synchronized (server.getClients()) {
                        String clientName = server.getClients().get(uuid).getName();
                        timeout = true;

                        server.getClients().remove(uuid);
                        server.getLastActivities().remove(uuid);
                        logger.info("La connexion avec le client " + uuid + " (" + clientName + ") a été perdu... (déconnexion)");
                        logger.info(clientName + " a quitté la partie. (" + server.getClients().size() + "/???)");
                    }
                }
            }
        }
    }
}