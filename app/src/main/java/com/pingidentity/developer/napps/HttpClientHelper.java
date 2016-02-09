package com.pingidentity.developer.napps;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpClientHelper {

    public static HttpClientResponse doPost(String postUrl, String postBody) {

        Log.d(Constants.LOG_TAG, "Creating connection to: " + postUrl);
        HttpClientResponse responseObject = new HttpClientResponse();

        //TODO: DEV ONLY! Remove before deploying in production
        trustAllHosts();

        try {
            URL url = new URL(postUrl);

            URLConnection urlConnection = url.openConnection();
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection)urlConnection;

            //TODO: DEV ONLY! Remove before deploying in production
            httpsURLConnection.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            httpsURLConnection.setRequestMethod("POST");
            httpsURLConnection.setDoInput(true);
            httpsURLConnection.setDoOutput(true);
            httpsURLConnection.setUseCaches(false);
            httpsURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpsURLConnection.connect();

            Log.d(Constants.LOG_TAG, "POSTing data: " + postBody);

            DataOutputStream postData = new DataOutputStream(httpsURLConnection.getOutputStream());
            postData.writeBytes(postBody);
            postData.flush();
            postData.close();

            int responseCode = httpsURLConnection.getResponseCode();
            BufferedReader responseReader;

            Log.d(Constants.LOG_TAG, "Got response code: " + responseCode);

            if(httpsURLConnection.getErrorStream() != null) {
                responseReader = new BufferedReader(new InputStreamReader(httpsURLConnection.getErrorStream(), "UTF-8"));
            } else {
                responseReader = new BufferedReader(new InputStreamReader(httpsURLConnection.getInputStream(), "UTF-8"));
            }

            String responseLine = null;
            String responseData = "";

            while ((responseLine = responseReader.readLine()) != null) {
                responseData += responseLine;
            }

            responseObject.responseCode = HttpsURLConnection.HTTP_OK;
            responseObject.responseData = responseData;

            httpsURLConnection.disconnect();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException protoException) {
            protoException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        return responseObject;
    }

    public static HttpClientResponse doGet(String getUrl, String authorizationHeader) {

        Log.d(Constants.LOG_TAG, "Creating GET request to: " + getUrl);
        HttpClientResponse responseObject = new HttpClientResponse();

        //TODO: DEV ONLY! Remove before deploying in production
        trustAllHosts();

        try {
            URL url = new URL(getUrl);

            URLConnection urlConnection = url.openConnection();
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection)urlConnection;

            //TODO: DEV ONLY! Remove before deploying in production
            httpsURLConnection.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            httpsURLConnection.setRequestMethod("GET");
            httpsURLConnection.setDoOutput(true);
            httpsURLConnection.setUseCaches(false);
            httpsURLConnection.setRequestProperty("Authorization", authorizationHeader);
            httpsURLConnection.connect();

            int responseCode = httpsURLConnection.getResponseCode();
            BufferedReader responseReader;

            Log.d(Constants.LOG_TAG, "Got response code: " + responseCode);

            if(httpsURLConnection.getErrorStream() != null) {
                responseReader = new BufferedReader(new InputStreamReader(httpsURLConnection.getErrorStream(), "UTF-8"));
            } else {
                responseReader = new BufferedReader(new InputStreamReader(httpsURLConnection.getInputStream(), "UTF-8"));
            }

            String responseLine = null;
            String responseData = "";

            while ((responseLine = responseReader.readLine()) != null) {
                responseData += responseLine;
            }

            responseObject.responseCode = HttpsURLConnection.HTTP_OK;
            responseObject.responseData = responseData;

            httpsURLConnection.disconnect();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException protoException) {
            protoException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        return responseObject;
    }

    //TODO: DEV ONLY! Remove before deploying in production
    private static void trustAllHosts() {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException { }

            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException { }

            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[] {};
            }
        } };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection
                    .setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
