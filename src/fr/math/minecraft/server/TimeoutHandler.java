package fr.math.minecraft.server;

import fr.math.minecraft.logger.LogType;
import fr.math.minecraft.logger.LoggerUtility;
import fr.math.minecraft.server.api.MinecraftApiFacade;
import fr.math.minecraft.server.api.Server;
import fr.math.minecraft.server.websockets.ServerStatus;
import org.apache.logging.log4j.Logger;

public class TimeoutHandler extends Thread {

    private final MinecraftServer server;
    private final static double TIMEOUT_DELAY_MS = 15000;
    private final String uuid;
    private final static Logger logger = LoggerUtility.getServerLogger(TimeoutHandler.class, LogType.TXT);;

    public TimeoutHandler(MinecraftServer server, String uuid) {
        this.server = server;
        this.uuid = uuid;
    }

    @Override
    public void run() {
        boolean timeout = false;
        while (!timeout) {
            long currentTime = System.currentTimeMillis();
            synchronized (server.getLastActivities()) {
                long lastTimeSeen = server.getLastActivities().get(uuid);
                if (currentTime - lastTimeSeen > TIMEOUT_DELAY_MS) {
                    synchronized (server.getClients()) {
                        Client client = server.getClients().get(uuid);
                        if (!client.isActive()) continue;
                        String clientName = client.getName();
                        timeout = true;

                        server.getClients().remove(uuid);
                        server.getLastActivities().remove(uuid);
                        Server serverData = server.getServerData();
                        ServerStatus serverStatus = new ServerStatus(serverData.getIp(), server.getClients().size(), server.getChatMessages().size());
                        server.getWebSocketServer().broadcastStatus(serverStatus);
                        MinecraftApiFacade api = new MinecraftApiFacade();
                        api.updateServer(serverData, serverStatus);
                        logger.info("La connexion avec le client " + uuid + " (" + clientName + ") a été perdu... (déconnexion)");
                        logger.info(clientName + " a quitté la partie. (" + server.getClients().size() + "/???)");
                    }
                }
            }
        }
    }
}
