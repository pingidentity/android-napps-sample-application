package com.pingidentity.developer.napps;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.customtabs.CustomTabsCallback;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private CustomTabsClient customTabsClient;
    private CustomTabsServiceConnection customTabsServiceConnection;
    private CustomTabsSession customTabsSession;

    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //TODO: DEV ONLY! Remove before deploying in production
        //For simplicity of the demo, all actions are performed on the main thread
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Startup both the Chrome Custom Tabs service and a Broadcast receiver
        if (customTabsClient == null) { bindCustomTabsService(); }
        if (receiver == null) { registerBroadcastReceiver(); }

        // Check for Authentication callback
        Intent intent = getIntent();

        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Log.d(Constants.LOG_TAG, "Intent received: " + intent.getAction());
            Uri intentUri = intent.getData();

            if (intentUri != null) {
                Log.d(Constants.LOG_TAG, "Intent received: " + intentUri.toString());
                TextView lastAction = (TextView) findViewById(R.id.last_action);
                lastAction.setText("Received callback: " + intentUri.toString());

                if (intentUri.getScheme().equalsIgnoreCase(getString(R.string.app_redirect_scheme))) {
                    Log.d(Constants.LOG_TAG, "This was a message to our OAuth redirect_uri");
                    ProtocolHelper.handleAuthorizationResponse(this.getApplicationContext(), intentUri);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (receiver == null) { registerBroadcastReceiver(); }
    }

    @Override
    protected void onPause() {
        super.onPause();
        deregisterBroadcastReceiver();
    }

    public void onDestroy() {

        super.onDestroy();

        unbindCustomTabsService();
    }

    private void bindCustomTabsService() {

        customTabsServiceConnection = new CustomTabsServiceConnection() {

            @Override
            public void onCustomTabsServiceConnected(ComponentName componentName, CustomTabsClient customTabsClient) {
                MainActivity.this.customTabsClient = customTabsClient;
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                customTabsClient = null;
            }
        };

        String packageName = "com.android.chrome";
        CustomTabsClient.bindCustomTabsService(this, packageName, customTabsServiceConnection);
    }

    private void unbindCustomTabsService() {

        if (customTabsServiceConnection == null) return;

        unbindService(customTabsServiceConnection);
        customTabsClient = null;
    }

    private void registerBroadcastReceiver() {

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.ACTION_ERROR);
        intentFilter.addAction(Constants.ACTION_EXCHANGED_CODE);
        intentFilter.addAction(Constants.ACTION_GOT_USERINFO);
        intentFilter.addAction(Constants.ACTION_REFRESHED_TOKEN);
        intentFilter.addAction(Constants.ACTION_VALIDATED_ID_TOKEN);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                handleBroadcastAction(intent);
            }
        };

        registerReceiver(receiver, intentFilter);
    }

    private void deregisterBroadcastReceiver() {

        if (receiver != null) { unregisterReceiver(receiver); }
    }

    private void handleBroadcastAction(Intent intent) {

        String action = intent.getAction();
        String moreInfo = intent.getStringExtra(Constants.INFO_MORE);

        Log.d(Constants.LOG_TAG, "Received broadcast: " + action);

        TextView lastActionView = (TextView) findViewById(R.id.last_action);

        if (action.equals(Constants.ACTION_ERROR)) {
            lastActionView.setText(moreInfo);
        } else if (action.equals(Constants.ACTION_EXCHANGED_CODE)) {
            lastActionView.setText("Received tokens: \n" + moreInfo);
            refreshTokensInView();
        } else if (action.equals(Constants.ACTION_REFRESHED_TOKEN)) {
            lastActionView.setText("Refreshed tokens: \n" + moreInfo);
            refreshTokensInView();
        } else if (action.equals(Constants.ACTION_GOT_USERINFO)) {
            lastActionView.setText("Queried Userinfo: \n" + moreInfo);
            refreshTokensInView();
        }
    }

    private void refreshTokensInView() {

        SessionManager currentSession = SessionManager.getInstance();

        TextView textAccessToken = (TextView) findViewById(R.id.oauth2_access_token);
        if (currentSession.getAccessToken() != null || !currentSession.getSubject().isEmpty()) {
            textAccessToken.setText(currentSession.getAccessToken());
        } else {
            textAccessToken.setText("{ NO TOKEN PRESENT }");
        }

        TextView textRefreshToken = (TextView) findViewById(R.id.oauth2_refresh_token);
        if (currentSession.getRefreshToken() != null || !currentSession.getSubject().isEmpty()) {
            textRefreshToken.setText(currentSession.getRefreshToken());
        } else {
            textRefreshToken.setText("{ NO TOKEN PRESENT }");
        }

        TextView textSubject = (TextView) findViewById(R.id.oidc_subject);
        if (currentSession.getSubject() != null || !currentSession.getSubject().isEmpty()) {
            textSubject.setText(currentSession.getSubject());
        } else {
            textSubject.setText("{ NO TOKEN PRESENT }");
        }
    }

    public void prefetchContent(Uri requestUri) {

        if (customTabsClient != null) {
            customTabsClient.warmup(0);
            CustomTabsSession customTabsSession = getSession();
            customTabsSession.mayLaunchUrl(requestUri, null, null);
        }
    }

    private CustomTabsSession getSession() {

        if (customTabsClient == null) {
            customTabsSession = null;
        } else if (customTabsSession == null) {
            customTabsSession = customTabsClient.newSession(new CustomTabsCallback() {

                @Override
                public void onNavigationEvent(int navigationEvent, Bundle extras) {
                    super.onNavigationEvent(navigationEvent, extras);
                }
            });
        }
        return customTabsSession;
    }

    public void launchBrowser(View view) {

        Uri requestUri = Uri.parse(ProtocolHelper.buildAuthorizationUrl());
        Log.d(Constants.LOG_TAG, "Launching: " + requestUri.toString());

        prefetchContent(requestUri);

        CustomTabsIntent.Builder mBuilder = new CustomTabsIntent.Builder(getSession());
        CustomTabsIntent mIntent = mBuilder.build();
        mIntent.launchUrl(this, requestUri);
    }

    public void refreshToken(View view) {

        ProtocolHelper.refreshAccessToken(this.getApplicationContext());
    }

    public void getUserinfo(View view) {

        ProtocolHelper.queryUserInfoEndpoint(this.getApplicationContext());
    }
}