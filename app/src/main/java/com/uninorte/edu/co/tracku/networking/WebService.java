package com.uninorte.edu.co.tracku.networking;

import android.os.AsyncTask;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class WebService extends AsyncTask<String, String, String> {
    @Override
    protected String doInBackground(String... strings) {
        URL url = null;
        try {
            url = new URL(strings[0]);
        } catch (Exception e) {
            return "";
        }
        try {
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setConnectTimeout(10000);
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
                return stringBuffer.toString();
            } else {
                System.out.println("Coneccion webservice fallida");
                return  "";
            }
        } catch (Exception e) {
            return "";
        }
    }
}
