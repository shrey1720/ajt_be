package com.collaboration.socket;

import com.collaboration.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class SocketServerRunner {

    private static final Logger log = LoggerFactory.getLogger(SocketServerRunner.class);

    @Value("${socket.server.port:9090}")
    private int port;

    private final QuestionRepository questionRepository;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public SocketServerRunner(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startSocketServer() {
        executorService.submit(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                log.info("Socket Server started on port " + port);

                while (!Thread.currentThread().isInterrupted()) {
                    Socket clientSocket = serverSocket.accept();
                    log.info("Accepted connection from " + clientSocket.getInetAddress());
                    
                    executorService.submit(new ClientHandler(clientSocket, questionRepository));
                }
            } catch (IOException e) {
                log.error("Error in Socket Server", e);
            }
        });
    }
}
