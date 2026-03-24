package com.collaboration.socket;

import com.collaboration.model.Question;
import com.collaboration.repository.QuestionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

public class ClientHandler implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket clientSocket;
    private final QuestionRepository questionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClientHandler(Socket clientSocket, QuestionRepository questionRepository) {
        this.clientSocket = clientSocket;
        this.questionRepository = questionRepository;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String inputLine = in.readLine();
            if (inputLine != null && !inputLine.trim().isEmpty()) {
                log.info("Received from VS Code extension: {}", inputLine);
                
                try {
                    JsonNode rootNode = objectMapper.readTree(inputLine);
                    String type = rootNode.has("type") ? rootNode.get("type").asText() : "";
                    
                    if ("QUESTION".equalsIgnoreCase(type)) {
                        Question q = new Question();
                        q.setUserId(rootNode.has("userId") ? rootNode.get("userId").asLong() : 1L);
                        q.setTitle(rootNode.has("title") ? rootNode.get("title").asText() : "Untitled Question");
                        q.setDescription(rootNode.has("description") ? rootNode.get("description").asText() : "No description");
                        q.setCode(rootNode.has("code") ? rootNode.get("code").asText() : "");
                        if(rootNode.has("tags")) {
                            q.setTags(rootNode.get("tags").asText());
                        }

                        Question saved = questionRepository.save(q);
                        
                        String response = objectMapper.writeValueAsString(Map.of(
                            "status", "success",
                            "questionId", saved.getId()
                        ));
                        out.println(response);
                    } else {
                        String err = objectMapper.writeValueAsString(Map.of("status", "error", "message", "Unknown type"));
                        out.println(err);
                    }
                } catch (Exception e) {
                    log.error("Error processing JSON payload", e);
                    String err = objectMapper.writeValueAsString(Map.of("status", "error", "message", e.getMessage()));
                    out.println(err);
                }
            }
        } catch (Exception e) {
            log.error("Socket error", e);
        } finally {
            try {
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (Exception e) {
                log.error("Error closing socket", e);
            }
        }
    }
}
