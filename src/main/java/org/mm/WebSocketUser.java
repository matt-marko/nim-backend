package org.mm;

import jakarta.websocket.*;

public class WebSocketUser {
    private Session session;
    private String username;
    private String gameCode;
    
    public Session getSession() {
        return this.session;
    }

    public String getGameCode() {
        return this.gameCode;
    }

    public String getUsername() {
        return this.username;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void setGameCode(String gameCode) {
        this.gameCode = gameCode;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
