package com.pingidentity.developer.napps;

import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.UUID;

public class ProtocolHelper {

    public ProtocolHelper() {
    }

    public static String buildAuthorizationUrl() {

        SessionManager currentSession = SessionManager.getInstance();

        // As this is a new request, we will generate new state and pkce challenges
        String pkceChallenge = UUID.randomUUID().toString();
        String state = UUID.randomUUID().toString();

        currentSession.setPkceChallenge(pkceChallenge);
        currentSession.setState(state);

        String authorizationUrl = Constants.pf_baseUrl + Constants.pf_authz_endpoint + "?";
        authorizationUrl += "client_id=" + urlEncodeParameter(Constants.clientId);
        authorizationUrl += "&response_type=code";
        authorizationUrl += "&code_challenge=" + urlEncodeParameter(pkceChallenge);
        authorizationUrl += "&redirect_uri=" + urlEncodeParameter(Constants.redirectUri);
        authorizationUrl += "&scope=" + Constants.scope;
        authorizationUrl += "&state=" + state;
        authorizationUrl += "&acr_values=" + Constants.acr_values;

        return authorizationUrl;
    }

    public static void handleAuthorizationResponse(Context ctx, Uri callbackUri) {

        // Parse the callback

        if (callbackUri == null) return;
        SessionManager currentSession = SessionManager.getInstance();

        String error = callbackUri.getQueryParameter("error");
        if (error != null) {
            Log.d(Constants.LOG_TAG, "An Error Occurred: " + error + " - " + callbackUri.getQueryParameter("error_description"));
        }

        String code = callbackUri.getQueryParameter("code");
        String state = callbackUri.getQueryParameter("state");

        if (!state.equalsIgnoreCase(currentSession.getState())) {
            Log.d(Constants.LOG_TAG, "State values DO NOT MATCH!!!");
            return;
        }

        swapAuthorizationCodeForTokens(ctx, code);

        return;
    }

    private static void swapAuthorizationCodeForTokens(Context ctx, String code) {

        // Exchange the code for the tokens

        if (code == null) return;
        SessionManager currentSession = SessionManager.getInstance();
        Boolean idTokenValidationFailed = false;

        String tokenUrl = Constants.pf_baseUrl + Constants.pf_token_endpoint;
        String tokenRequest = "client_id=" + Constants.clientId;
        tokenRequest += "&grant_type=authorization_code";
        tokenRequest += "&code_verifier=" + currentSession.getPkceChallenge();
        tokenRequest += "&code=" + code;
        tokenRequest += "&redirect_uri=" + Constants.redirectUri;

        HttpClientResponse httpResponse = HttpClientHelper.doPost(tokenUrl, tokenRequest);

        if (httpResponse.responseCode == 200) {

            try {
                JSONObject jsonResponse = new JSONObject(httpResponse.responseData);

                if (jsonResponse.has("access_token")) {
                    currentSession.setAccessToken(jsonResponse.getString("access_token"));
                }
                if (jsonResponse.has("refresh_token")) {
                    currentSession.setRefreshToken(jsonResponse.getString("refresh_token"));
                }
                if (jsonResponse.has("id_token")) {
                    currentSession.setIdToken(jsonResponse.getString("id_token"));
                    if (!validateIdToken(jsonResponse.getString("id_token"))) {

                        Intent broadcastIntent = new Intent(Constants.ACTION_ERROR);
                        broadcastIntent.putExtra(Constants.INFO_MORE, "id_token validation failed");
                        ctx.sendBroadcast(broadcastIntent);
                        idTokenValidationFailed = true;
                    }
                }

            } catch (JSONException jException) {
                jException.printStackTrace();
            }

            if (!idTokenValidationFailed) {
                Intent broadcastIntent = new Intent(Constants.ACTION_EXCHANGED_CODE);
                broadcastIntent.putExtra(Constants.INFO_MORE, httpResponse.responseData);
                ctx.sendBroadcast(broadcastIntent);
            }

        } else {

            Intent broadcastIntent = new Intent(Constants.ACTION_ERROR);
            broadcastIntent.putExtra(Constants.INFO_MORE, httpResponse.responseData);
            ctx.sendBroadcast(broadcastIntent);
        }
    }

    public static void refreshAccessToken(Context ctx) {

        // Refresh the OAuth 2.0 access_token

        SessionManager currentSession = SessionManager.getInstance();

        String tokenUrl = Constants.pf_baseUrl + Constants.pf_token_endpoint;
        String tokenRequest = "client_id=" + Constants.clientId;
        tokenRequest += "&grant_type=refresh_token";
        tokenRequest += "&refresh_token=" + currentSession.getRefreshToken();

        HttpClientResponse httpResponse = HttpClientHelper.doPost(tokenUrl, tokenRequest);

        if (httpResponse.responseCode == 200) {

            try {
                JSONObject jsonResponse = new JSONObject(httpResponse.responseData);

                if (jsonResponse.has("access_token")) {
                    currentSession.setAccessToken(jsonResponse.getString("access_token"));
                }
                if (jsonResponse.has("refresh_token")) {
                    currentSession.setRefreshToken(jsonResponse.getString("refresh_token"));
                }
            } catch (JSONException jException) {
                jException.printStackTrace();
            }

            Intent broadcastIntent = new Intent(Constants.ACTION_REFRESHED_TOKEN);
            broadcastIntent.putExtra(Constants.INFO_MORE, httpResponse.responseData);
            ctx.sendBroadcast(broadcastIntent);
        } else {

            Intent broadcastIntent = new Intent(Constants.ACTION_ERROR);
            broadcastIntent.putExtra(Constants.INFO_MORE, httpResponse.responseData);
            ctx.sendBroadcast(broadcastIntent);
        }
    }

    private static Boolean validateIdToken(String jwtIdToken) {

        // Validate the OIDC id_token

        Log.d(Constants.LOG_TAG, "Validating id_token");

        // We are using OIDC Basic Profile so dsig validation is not MTI
        // Grab the payload from the JWT - [header].[payload].[signature]
        String tokenPayload = jwtIdToken.split("\\.")[1];
        Log.d(Constants.LOG_TAG, "Payload: " + tokenPayload);
        JSONObject jsonPayload = null;

        try {
            byte[] decodedPayload = Base64.decode(tokenPayload, Base64.URL_SAFE);
            jsonPayload = new JSONObject(new String(decodedPayload, "UTF-8"));

            if (jsonPayload.has("iss")) {
                if (!jsonPayload.getString("iss").equalsIgnoreCase(Constants.issuer)) {
                    Log.d(Constants.LOG_TAG, "id_token validation failed: Issuer mismatch");
                    return false;
                }
            } else {
                Log.d(Constants.LOG_TAG, "id_token validation failed: Missing issuer (iss) claim");
                return false;
            }

            if (jsonPayload.has("aud")) {
                if (!jsonPayload.getString("aud").equalsIgnoreCase(Constants.clientId)) {
                    Log.d(Constants.LOG_TAG, "id_token validation failed: Audience mismatch");
                    return false;
                }
            } else {
                Log.d(Constants.LOG_TAG, "id_token validation failed: Missing audience (aud) claim");
                return false;
            }

            if (jsonPayload.has("exp")) {
                long currentTimeStamp = System.currentTimeMillis()/1000;
                long expiryTimeStamp = jsonPayload.getLong("exp");

                if (currentTimeStamp > expiryTimeStamp) {
                    Log.d(Constants.LOG_TAG, "id_token validation failed: Token expired");
                    return false;
                }
            } else {
                Log.d(Constants.LOG_TAG, "id_token validation failed: Missing expiry (exp) claim");
                return false;
            }

            // We have a valid id_token, set the subject
            if (jsonPayload.has("sub")) {
                SessionManager currentSession = SessionManager.getInstance();
                currentSession.setSubject(jsonPayload.getString("sub"));
                Log.d(Constants.LOG_TAG, "id_token validation complete: Token valid");
                return true;
            } else {
                Log.d(Constants.LOG_TAG, "id_token validation failed: Missing subject (sub) claim");
                return false;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException encodingException) {
            encodingException.printStackTrace();
        }

        return false;
    }

    public static void queryUserInfoEndpoint(Context ctx) {

        // Call the OIDC UserInfo endpoint

        SessionManager currentSession = SessionManager.getInstance();
        Boolean subjectVerificationFailed = false;

        String userinfoUrl = Constants.pf_baseUrl + Constants.pf_userinfo_endpoint;
        String authorizationHeader = "Bearer " + currentSession.getAccessToken();

        HttpClientResponse httpResponse = HttpClientHelper.doGet(userinfoUrl, authorizationHeader);

        if (httpResponse.responseCode == 200) {

            try {
                JSONObject jsonResponse = new JSONObject(httpResponse.responseData);

                // This will be a JSON object of the claims provided by the userinfo endpoint
                // Just return "sub" for demonstration purposes
                if (jsonResponse.has("sub")) {

                    String idSubject = currentSession.getSubject();

                    if (idSubject != null && !idSubject.isEmpty()) {
                        if (!idSubject.equalsIgnoreCase(jsonResponse.getString("sub"))) {

                            Intent broadcastIntent = new Intent(Constants.ACTION_ERROR);
                            broadcastIntent.putExtra(Constants.INFO_MORE, "Subject does not match value from id_token");
                            ctx.sendBroadcast(broadcastIntent);
                            subjectVerificationFailed = true;
                        }
                    }

                    currentSession.setSubject(jsonResponse.getString("sub"));
                }
            } catch (JSONException jException) {
                jException.printStackTrace();
            }

            if (!subjectVerificationFailed) {
                Intent broadcastIntent = new Intent(Constants.ACTION_GOT_USERINFO);
                broadcastIntent.putExtra(Constants.INFO_MORE, httpResponse.responseData);
                ctx.sendBroadcast(broadcastIntent);
            }
        } else {

            Intent broadcastIntent = new Intent(Constants.ACTION_ERROR);
            broadcastIntent.putExtra(Constants.INFO_MORE, httpResponse.responseData);
            ctx.sendBroadcast(broadcastIntent);
        }
    }

    private static String urlEncodeParameter(String param) {

        String returnString = "";

        try {
            returnString = URLEncoder.encode(param, "UTF8");
        } catch (Exception ex) {
          ex.printStackTrace();
        }

        return returnString;
    }
}
