package com.pingidentity.developer.napps;

public class SessionManager {

    private String accessToken = "";
    private String refreshToken = "";
    private String idToken = "";
    private String pkceChallenge = "";
    private String state = "";
    private String subject = "";

    // Handle Singleton instance
    private static SessionManager currentSession = null;

    protected SessionManager() { };

    public static SessionManager getInstance() {
        if(currentSession == null) {
            currentSession = new SessionManager();
        }
        return currentSession;
    }

    // Getters and Setters
    public void setPkceChallenge(String value) {
        pkceChallenge = value;
    }

    public void setState(String value) {
        state = value;
    }

    public void setAccessToken(String value) {
        accessToken = value;
    }

    public void setRefreshToken(String value) {
        refreshToken = value;
    }

    public void setIdToken(String value) {
        idToken = value;
    }

    public void setSubject(String value) {
        subject = value;
    }

    public String getPkceChallenge() {
        return pkceChallenge;
    }

    public String getState() {
        return state;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public String getSubject() {
        return subject;
    }
}
