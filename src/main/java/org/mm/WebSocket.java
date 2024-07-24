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

    final List<WebSocketUser> users = new ArrayList<>();

    final Map<String, List<WebSocketUser>> games = new HashMap<>();

    @OnOpen
    public void onOpen(
        Session session,
        @PathParam("username") String username,
        @PathParam("gameCode") String gameCode
    ) {
        List<WebSocketUser> equivalentUsers = users
            .stream()
            .filter(user -> {
                return 
                    user.getGameCode().equals(gameCode) && 
                    user.getUsername().toLowerCase().equals(username.toLowerCase());  
            }).collect(Collectors.toList());

        List<WebSocketUser> associatedGame = games.get(gameCode);
        
        if (!equivalentUsers.isEmpty()) {
            // Notify user that the host already has this name 
            broadcastToSession(MessageConstants.SAME_NAME + " " + username, session);;  
        } else if (associatedGame != null && associatedGame.size() > 1) {
            // Notify user that the game is full
            broadcastToSession(MessageConstants.GAME_FULL + " " + gameCode, session);  
        } else {
            WebSocketUser user = new WebSocketUser();
           
            user.setUsername(username);
            user.setGameCode(gameCode);
            user.setSession(session);
    
            users.add(user);
    
            // Notify the user that the connection has been opened
            broadcastToSession(MessageConstants.CONNECTION_OPENED + " " + username , session);
        } 
    }

    @OnClose
    public void onClose(
        Session session, 
        @PathParam("username") String username,
        @PathParam("gameCode") String gameCode
    ) {
        if (this.getUser(session).isPresent()) {
            this.removeUser(session);
 
            // Notify the the players that someone has left
            broadcastToGame(MessageConstants.USER_LEFT + " " + username, gameCode);
        }
    }

    @OnError
    public void onError(
        Session session, 
        @PathParam("username") String username, 
        @PathParam("gameCode") String gameCode, 
        Throwable throwable
    ) {
        if (this.getUser(session).isPresent()) {
            this.removeUser(session);

            // Notify the the players that someone has left
            broadcastToGame(MessageConstants.USER_LEFT + " " + username, gameCode);
                
            System.out.println("User left on error: " + throwable.getMessage());
        }
    }
 
    @OnMessage
    public void onMessage(
        String message,  
        @PathParam("username") String username,
        @PathParam("gameCode") String gameCode,
        Session session
    ) {
        String webSocketCode = message.split(" ")[0];

        switch (webSocketCode) {
            case MessageConstants.CREATE_GAME:
                createGame(message, username, gameCode, session);
                break;
            case MessageConstants.JOIN_GAME:
                joinGame(message, username, gameCode, session);
                break;
            case MessageConstants.START_GAME:
                startGame(message, username, gameCode, session);
                break;
            case MessageConstants.TURN:
                handleTurn(message, username, gameCode, session);
                break;
            case MessageConstants.RESTART_GAME:
                restartGame(message, username, gameCode, session);
                break;
            default:
                break;
        }
    }

    private void createGame(String message, String username, String gameCode, Session session) {
        Optional<WebSocketUser> creatingUser = this.getUser(session);

        if (creatingUser.isPresent()) {
            games.put(gameCode, List.of(creatingUser.get()));

            // Notify user that they have successfully created a game
            broadcastToSession(MessageConstants.GAME_CREATED + " " + gameCode, creatingUser.get().getSession());
        }
    }

    private void joinGame(String message, String username, String gameCode, Session session) {
        Optional<WebSocketUser> joiningUser = this.getUser(session);
        List<WebSocketUser> gameToJoin = games.get(gameCode);

        if (joiningUser.isPresent()) {
            if (gameToJoin != null) {
                List<WebSocketUser> updatedGame = List.of(gameToJoin.get(0), joiningUser.get());
                games.put(gameCode, updatedGame);

                // Notify user that they have successfully joined a game
                broadcastToSession(MessageConstants.GAME_JOINED + " " + gameCode, joiningUser.get().getSession());

                // Notify the user the name of their opponent
                broadcastToSession(MessageConstants.OPPONENT_NAME + " " + updatedGame.get(0).getUsername(), joiningUser.get().getSession());  

                // Notify host that an opponent has joined
                broadcastToSession(MessageConstants.OPPONENT_NAME + " " + joiningUser.get().getUsername(),  updatedGame.get(0).getSession()); 
            } else {
                // Notify user that the game was not found
                broadcastToSession(MessageConstants.GAME_NOT_FOUND + " " + gameCode, joiningUser.get().getSession());
            }
        }
    }

    private void startGame(String message, String username, String gameCode, Session session) {
        final String gameToStart = message.split(" ")[1];
                
        // Notify the players that the game has successfully been started
        broadcastToGame(MessageConstants.GAME_STARTED, gameToStart);
    }

    private void handleTurn(String message, String username, String gameCode, Session session) {
        List<WebSocketUser> game = games.get(gameCode);

        WebSocketUser opponent = game
            .stream()
            .filter(user -> !user.getUsername().equals(username))
            .collect(Collectors.toList())
            .get(0);

        // Notify the player of their opponent's move
        broadcastToSession(message, opponent.getSession());
    }

    private void restartGame(String message, String username, String gameCode, Session session) {
        final String gameToRestart = message.split(" ")[1];

        // Notify the players that the game has successfully been restarted
        broadcastToGame(MessageConstants.GAME_RESTARTED + " " + gameCode, gameToRestart);
    }

    private void broadcastToGame(String message, final String gameCode) {
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

    private Optional<WebSocketUser> getUser(Session session) {
        return users.stream()
            .filter(user -> user.getSession().getId().equals(session.getId()))
            .findFirst();
    }

    private void removeUser(Session session) {
        Optional<WebSocketUser> userToRemove = this.getUser(session);

        if(userToRemove.isPresent()) {
            users.removeIf(user -> {
                return user.getSession().getId().equals(userToRemove.get().getSession().getId());
            });       

            List<WebSocketUser> game = games.get(userToRemove.get().getGameCode());
            String gameCode = game.get(0).getGameCode();

            if (game != null) {
                List<WebSocketUser> newGame = 
                game.stream()
                    .filter(user -> !user.getUsername().equals(userToRemove.get().getUsername()))
                    .collect(Collectors.toList());

                if (newGame.size() != 0) {
                    games.put(gameCode, newGame);
                } else {
                    games.remove(gameCode);
                }
            }
        }
    }
}
