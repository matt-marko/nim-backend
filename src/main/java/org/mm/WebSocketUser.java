package org.mm;

import jakarta.websocket.*;

public class WebSocketUser {
    private Session session;
    private String username;
    private String gameCode;
    private boolean host;
    
    public Session getSession() {
        return this.session;
    }

    public String getGameCode() {
        return this.gameCode;
    }

    public String getUsername() {
        return this.username;
    }

    public boolean getHost() {
        return this.host;
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

    public void setHost(boolean host) {
        this.host = host;
    }
}
