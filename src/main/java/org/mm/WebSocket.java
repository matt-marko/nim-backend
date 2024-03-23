package org.mm;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ServerEndpoint("/socket/{username}/{gameCode}")
@ApplicationScoped
public class WebSocket {

    final int GAME_CODE_LENGTH = 5;

    Map<String, Session> sessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(
        Session session,
        @PathParam("username") String username,
        @PathParam("gameCode") String gameCode
    ) {
        //sessions.put(username, session);

        if (gameCode.isEmpty()) {
            String generatedGameCode = generateGameCode();
            sessions.put(generatedGameCode, session);

            broadcast("CODE:" + " " + generatedGameCode);
        } else {
            Session sessionToJoin = sessions.get(gameCode);
            if (sessionToJoin != null) {

            }
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("username") String username) {
        sessions.remove(username);
        broadcast("User " + username + " left");
    }

    @OnError
    public void onError(Session session, @PathParam("username") String username, Throwable throwable) {
        sessions.remove(username);
        broadcast("User " + username + " left on error: " + throwable);
    }

    @OnMessage
    public void onMessage(
            String message,
            @PathParam("username") String username
    ) {
        broadcast(">> " + username + ": " + message);
    }

    private void broadcast(String message) {
        sessions.values().forEach(s -> {
            s.getAsyncRemote().sendObject(message, result ->  {
                if (result.getException() != null) {
                    System.out.println("Unable to send message: " + result.getException());
                }
            });
        });
    }

    private String generateGameCode() {
        final String validChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String gameCode = "";
        int randomIndex = 0;

        for (int i = 0; i < GAME_CODE_LENGTH; i++) {
            randomIndex = (int)(validChars.length() * Math.random());
            gameCode += validChars.charAt(randomIndex);
        }

        return gameCode;
    }
}
