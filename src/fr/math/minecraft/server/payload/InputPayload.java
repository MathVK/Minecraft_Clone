package fr.math.minecraft.server.payload;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.math.minecraft.client.entity.Player;
import fr.math.minecraft.shared.network.PlayerInputData;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;

public class InputPayload {

    private final int tick;
    private final String clientUuid;
    private final List<PlayerInputData> inputsData;

    public InputPayload(JsonNode payloadData) {
        this.tick = payloadData.get("tick").asInt();
        this.inputsData = new ArrayList<>();
        this.clientUuid = payloadData.get("uuid").asText();
        ArrayNode inputsArray = (ArrayNode) payloadData.get("inputs");

        for (int i = 0; i < inputsArray.size(); i++) {
            JsonNode inputNode = inputsArray.get(i);
            boolean movingForward = inputNode.get("movingForward").asBoolean();
            boolean movingBackward = inputNode.get("movingBackward").asBoolean();
            boolean movingLeft = inputNode.get("movingLeft").asBoolean();
            boolean movingRight = inputNode.get("movingRight").asBoolean();
            boolean sneaking = inputNode.get("sneaking").asBoolean();
            boolean flying = inputNode.get("flying").asBoolean();
            float yaw = inputNode.get("yaw").floatValue();
            float pitch = inputNode.get("pitch").floatValue();
            PlayerInputData inputData = new PlayerInputData(movingLeft, movingRight, movingForward, movingBackward, flying, sneaking, yaw, pitch);

            inputsData.add(inputData);
        }
    }

    public int getTick() {
        return tick;
    }

    public String getClientUuid() {
        return clientUuid;
    }

    public List<PlayerInputData> getInputsData() {
        return inputsData;
    }
}