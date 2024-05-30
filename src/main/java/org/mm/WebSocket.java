package org.mm;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@ServerEndpoint("/socket/{username}/{gameCode}")
@ApplicationScoped
public class WebSocket {

    final int GAME_CODE_LENGTH = 5;

    final List<WebSocketUser> users = new ArrayList<>();

    final Map<String, List<WebSocketUser>> games = new HashMap<>();

    @OnOpen
    public void onOpen(
        Session session,
        @PathParam("username") String username,
        @PathParam("gameCode") String gameCode
    ) {
        WebSocketUser user = new WebSocketUser();
           
        user.setUsername(username);
        user.setGameCode(gameCode);
        user.setSession(session);

        users.add(user);

        // Notify the user that the connection has been opened
        broadcastToSession(MessageConstants.CONNECTION_OPENED + " " + username, session);

        /*if (gameCode.isEmpty()) {
            String generatedGameCode = generateGameCode();

            WebSocketUser user = new WebSocketUser();
           
            user.setGameCode(generatedGameCode);
            user.setUsername(username);
            user.setSession(session);

            users.add(user);

            broadcast("CODE: " + generatedGameCode, generatedGameCode);
        } else {
            boolean gameFound = false;
            for (WebSocketUser user : users) {
                if (user.getGameCode().equals(gameCode)) {
                    WebSocketUser joiningUser = new WebSocketUser();
           
                    joiningUser.setGameCode(user.getGameCode());
                    joiningUser.setUsername(username);
                    joiningUser.setSession(session);    

                    users.add(joiningUser);

                    // Notify user that they have successfully joined
                    broadcastToSession("GAME-JOINED: " + user.getGameCode(), session);

                    // Notify the user with the name of their opponent
                    broadcastToSession("OPPONENT-NAME: " + user.getUsername(), session);         

                    // Notify host that an opponent has joined
                    broadcastToSession("OPPONENT-NAME: " + joiningUser.getUsername(), user.getSession());

                    gameFound = true;
                    break;
                }
            }

            if (!gameFound) {
                broadcastToSession("GAME-NOT-FOUND: " + gameCode, session);

                try {
                    session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Invalid game code"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }*/
    }

    @OnClose
    public void onClose(
        Session session, 
        @PathParam("username") String username,
        @PathParam("gameCode") String gameCode
    ) {
        this.removeUser(username, gameCode);
        // TODO change this message
        broadcast("User " + username + " left", gameCode);
    }

    @OnError
    public void onError(
        Session session, 
        @PathParam("username") String username, 
        @PathParam("gameCode") String gameCode, 
        Throwable throwable
    ) {
        this.removeUser(username, gameCode);
        // TODO change this message
        broadcast("User " + username + " left on error: " + throwable, gameCode);
    }
 
    @OnMessage
    public void onMessage(
        String message,  
        @PathParam("username") String username,
        @PathParam("gameCode") String gameCode
    ) {
        // TODO remove
        System.out.println("users:" + users.size());
        System.out.println("games:" + games.size());

        if (message.contains(MessageConstants.CREATE_GAME)) { 
            Optional<WebSocketUser> creatingUser = this.getUser(username, gameCode);

            if (creatingUser.isPresent()) {
                games.put(gameCode, List.of(creatingUser.get()));

                // Notify user that they have successfully created a game
                broadcastToSession(MessageConstants.GAME_CREATED + " " + gameCode, creatingUser.get().getSession());
            }
        } else if (message.contains(MessageConstants.JOIN_GAME)) {
            Optional<WebSocketUser> joiningUser = this.getUser(username, gameCode);
            List<WebSocketUser> gameToJoin = games.get(gameCode);

            if (joiningUser.isPresent()) {
                if (gameToJoin != null) {
                    List<WebSocketUser> updatedGame = List.of(gameToJoin.get(0), joiningUser.get());

                    games.put(gameCode, updatedGame);

                    // Notify user that they have successfully joined a game
                    broadcastToSession(MessageConstants.GAME_JOINED + " " + gameCode, joiningUser.get().getSession());
                } else {
                    // Notify user that the game was not found
                    broadcastToSession(MessageConstants.GAME_NOT_FOUND + " " + gameCode, joiningUser.get().getSession());
                }
            }
        }
    }

    // TODO rename to broadcast to game?
    private void broadcast(String message, final String gameCode) {
        users.stream()
            .filter(user -> user.getGameCode().equals(gameCode))
            .forEach(user -> {
                Session s = user.getSession();
                s.getAsyncRemote().sendObject(message, result ->  {
                    if (result.getException() != null) {
                        System.out.println("Unable to send message: " + result.getException());
                    }
                }); 
            });
    }

    private void broadcastToSession(String message, final Session session) {
        session.getAsyncRemote().sendObject(message, result ->  {
            if (result.getException() != null) {
                System.out.println("Unable to send message: " + result.getException());
            }
        });
    }

    private Optional<WebSocketUser> getUser(String username, String gameCode) {
        return users.stream()
            .filter(user -> user.getUsername().equals(username))
            .filter(user -> user.getGameCode().equals(gameCode))
            .findFirst();
    }

    // TODO is this necessary?
    private void closeUser(WebSocketUser user) {
        try {
            user.getSession()
                .close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "An error occurred"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeUser(String username, String gameCode) {
        Optional<WebSocketUser> userToRemove = this.getUser(username, gameCode);

        if(userToRemove.isPresent()) {
            users.removeIf(user -> {
                return 
                    user.getGameCode().equals(userToRemove.get().getGameCode()) && 
                    user.getUsername().equals(userToRemove.get().getUsername());
            });       

            List<WebSocketUser> game = games.get(userToRemove.get().getGameCode());
            List<WebSocketUser> newGame = 
                game.stream()
                    .filter(user -> !user.getUsername().equals(userToRemove.get().getUsername()))
                    .collect(Collectors.toList());

            games.put(gameCode, newGame);
        }
    }
}
