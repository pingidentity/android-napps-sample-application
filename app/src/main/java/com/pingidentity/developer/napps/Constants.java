package com.pingidentity.developer.napps;

public class Constants {

    // Used in protocol help - configure PingFederate server
    public static String pf_baseUrl = "https://sso.pingdevelopers.com";
    public static String pf_authz_endpoint = "/as/authorization.oauth2";
    public static String pf_token_endpoint = "/as/token.oauth2";
    public static String pf_userinfo_endpoint = "/idp/userinfo.openid";

    public static String issuer = "https://sso.pingdevelopers.com";
    public static String clientId = "ac_client";

    //NOTE: this must match the scheme defined in the strings.xml
    public static String redirectUri = "com.pingidentity.developer.napps://oidc_callback";

    public static String scope = "openid profile email";
    public static String acr_values = "urn:acr:form";

    // Default logging tag
    public static String LOG_TAG = "NAPPS_DEMO";

    // Actions broadcast back to the main activity
    public static String ACTION_ERROR = "com.pingidentity.developer.napps.ACTION_ERROR";
    public static String ACTION_EXCHANGED_CODE = "com.pingidentity.developer.napps.ACTION_EXCHANGED_CODE";
    public static String ACTION_REFRESHED_TOKEN = "com.pingidentity.developer.napps.ACTION_REFRESHED_TOKEN";
    public static String ACTION_VALIDATED_ID_TOKEN = "com.pingidentity.developer.napps.ACTION_VALIDATED_ID_TOKEN";
    public static String ACTION_GOT_USERINFO = "com.pingidentity.developer.napps.ACTION_GOT_USERINFO";

    // More information passed via the broadcast intent to main activity
    public static String INFO_MORE = "com.pingidentity.developer.napps.INFO_MORE";


}
