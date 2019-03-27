package com.uninorte.edu.co.tracku.networking;

import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WebServiceManager {

    public static void CallWebServiceOperation(final WebServiceManagerInterface caller,
                                               final String webServiceURL,
                                               final String resourceName,
                                               final String operation,
                                               final String methodType,
                                               final String payload,
                                               final String userState) {

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(webServiceURL + "/" + resourceName + "/" + operation);
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                    if(methodType.equals("POST")){
                        httpURLConnection.setDoOutput(true);
                        httpURLConnection.getOutputStream().write(payload.getBytes());
                    }
                    httpURLConnection.setRequestMethod(methodType);
                    int responseCode = httpURLConnection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        System.out.println("Conexion webService Exitosa");
                        InputStream in = httpURLConnection.getInputStream();
                        StringBuffer stringBuffer = new StringBuffer();
                        int charIn = 0;
                        while ((charIn = in.read()) != -1) {
                            stringBuffer.append((char) charIn);
                        }
                        System.out.println(stringBuffer.toString());
                        caller.WebServiceMessageReceived(userState, stringBuffer.toString());
                    } else {
                        System.out.println("Coneccion webservice fallida");
                    }

                } catch (Exception error) {
                    System.out.println(error);
                }
            }
        });

    }
}
