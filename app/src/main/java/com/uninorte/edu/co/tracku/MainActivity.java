package com.uninorte.edu.co.tracku;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;

import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.uninorte.edu.co.tracku.com.uninorte.edu.co.tracku.gps.GPSManager;
import com.uninorte.edu.co.tracku.com.uninorte.edu.co.tracku.gps.GPSManagerInterface;
import com.uninorte.edu.co.tracku.database.core.TrackUDatabaseManager;
import com.uninorte.edu.co.tracku.database.entities.Ubicacion;
import com.uninorte.edu.co.tracku.database.entities.User;
import com.uninorte.edu.co.tracku.networking.WebService;
import com.uninorte.edu.co.tracku.networking.WebServiceManager;
import com.uninorte.edu.co.tracku.networking.WebServiceManagerInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        GPSManagerInterface, OnMapReadyCallback, OmsFragment.OnFragmentInteractionListener, WebServiceManagerInterface, Ruta.camposLlenos {

    Activity thisActivity = this;
    GPSManager gpsManager;
    GoogleMap googleMap;
    double latitude;
    double longitude;
    OmsFragment omsFragment;
    String user = "";
    WebService web = new WebService();
    static TrackUDatabaseManager INSTANCE;
    String ip = "10.20.45.57";
    private Handler mHandler;
    private Handler sinc ;
    String userRuta, fecha_ruta1,fecha_ruta2;
    int sw = 1;

    static TrackUDatabaseManager getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (TrackUDatabaseManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context,
                            TrackUDatabaseManager.class, "database-tracku").
                            allowMainThreadQueries().fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }


    public boolean userAuth(String userName, String password) {
        try {
            List<User> usersFound = getDatabase(this).userDao().getUserByEmail(userName);
            if (usersFound.size() > 0) {
                if (usersFound.get(0).passwordHash.equals(md5(password))) {
                    return true;
                }
            } else {
                return false;
            }
        } catch (Exception error) {
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
        }
        return false;
    }

    public boolean userUbicacion(double latitude, double longitud, String userName, Date date, int sinc,String fecha) {
        try {
            Ubicacion newUbicacion = new Ubicacion();
            newUbicacion.email = userName;
            newUbicacion.latitud = latitude;
            newUbicacion.longitud = longitud;
            newUbicacion.startTime = date;
            newUbicacion.sincronizado = sinc;
            newUbicacion.fecha = fecha;
            INSTANCE.ubicacionDao().insertUbicacion(newUbicacion);

        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    public boolean userRegistration(String userName, String password) {
        try {
            User newUser = new User();
            newUser.email = userName;
            newUser.passwordHash = md5(password);
            INSTANCE.userDao().insertUser(newUser);
        } catch (Exception error) {
            Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    public String md5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHandler = new Handler();
        sinc = new Handler();
        getDatabase(this);

        String callType = getIntent().getStringExtra("callType");
        String respuesta = "";
        if (callType.equals("userLogin")) {
            String userName = getIntent().getStringExtra("userName");
            String password = getIntent().getStringExtra("password");
            System.out.println("user login");
            if (checkConnection()) {
                respuesta = "";
                try {
                    respuesta = this.web.execute("http://" + ip + ":8080/WebServiceMovilSQLite/webresources/generic/login/" + userName + "/" + md5(password)).get();
                    if (!respuesta.equals("")) {
                        Toast.makeText(this, "User found!, redirecting...", Toast.LENGTH_LONG).show();
                        this.user = userName;
                        WebServiceManager.CallWebServiceOperation(this, "http://" + ip + ":8080/WebServiceMovilSQLite/webresources",
                                "generic",
                                "status/update",
                                "POST",
                                this.user + "," + "1",
                                "statusU");
                        checkPermissions();
                    } else {
                        Toast.makeText(this, "User not found trying locally", Toast.LENGTH_LONG).show();
                        if (!userAuth(userName, password)) {
                            Toast.makeText(this, "User not found!", Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            this.user = userName;
                            checkPermissions();
                        }
                    }
                } catch (Exception e) {
                    System.out.println("excepcion login:" +e);
                    Toast.makeText(this, "Error de coneccion", Toast.LENGTH_LONG).show();
                    finish();
                }
            } else {
                if (!userAuth(userName, password)) {
                    Toast.makeText(this, "User not found!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    this.user = userName;
                    checkPermissions();
                }
            }


        } else if (callType.equals("userRegistration")) {
            String userName = getIntent().getStringExtra("userName");
            String password = getIntent().getStringExtra("password");
            System.out.println("user registration");
            if (checkConnection()) {
                respuesta = "";
                String hash = md5(password);
                try {
                    respuesta = this.web.execute("http://" + ip + ":8080/WebServiceMovilSQLite/webresources/generic/user/register/"
                            + userName + "/" + hash).get();
                    if (!respuesta.equals("")) {
                        Toast.makeText(this, "User Registered WebService", Toast.LENGTH_SHORT).show();
                        JSONArray jsonArray = new JSONArray(respuesta);
                        JSONObject jsonObject = jsonArray.getJSONObject(0);
                        String user = jsonObject.getString("nombre");
                        String pass = jsonObject.getString("password");
                        WebServiceManager.CallWebServiceOperation(this, "http://" + ip + ":8080/WebServiceMovilSQLite/webresources",
                                "generic",
                                "status/create",
                                "POST",
                                userName,
                                "statusC");
                        if (userRegistration(user, hash)) {
                            Toast.makeText(this, "User Registered Locally", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Error while registering user Locally!", Toast.LENGTH_LONG).show();
                        }
                        finish();
                    } else {
                        Toast.makeText(this, "Error while registering user!", Toast.LENGTH_LONG).show();
                        finish();
                    }
                } catch (ExecutionException | JSONException e) {
                    finish();
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    finish();
                    e.printStackTrace();
                }
                finish();
            } else {
                Toast.makeText(this, "No hay conexion", Toast.LENGTH_LONG).show();
                finish();
            }
            /*if (!userRegistration( userName, password)) {
                Toast.makeText(this, "Error while registering user!", Toast.LENGTH_LONG).show();
                finish();
            }else{
                Toast.makeText(this, "User registered!", Toast.LENGTH_LONG).show();
                finish();
            }
            */
        } else {
            finish();
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();


        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        SupportMapFragment supportMapFragment = (SupportMapFragment)
                this.getSupportFragmentManager().findFragmentById(R.id.google_maps_control);
        supportMapFragment.getMapAsync(this);

        com.github.clans.fab.FloatingActionButton floatingActionButton1 =
                (com.github.clans.fab.FloatingActionButton)
                        findViewById(R.id.zoom_in_button);
        floatingActionButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (googleMap != null) {
                    googleMap.moveCamera(CameraUpdateFactory.zoomIn());
                }
            }
        });

        com.github.clans.fab.FloatingActionButton floatingActionButton2 =
                (com.github.clans.fab.FloatingActionButton)
                        findViewById(R.id.zoom_out_button);
        floatingActionButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (googleMap != null) {
                    googleMap.moveCamera(CameraUpdateFactory.zoomOut());
                }
            }
        });

        com.github.clans.fab.FloatingActionButton floatingActionButton3 =
                (com.github.clans.fab.FloatingActionButton)
                        findViewById(R.id.focus_button);

        floatingActionButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (googleMap != null) {
                    googleMap.moveCamera(
                            CameraUpdateFactory.newLatLng(
                                    new LatLng(latitude, longitude)));
                }
            }
        });

    }

    boolean checkConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            Fragment fragment = getFragmentManager().findFragmentByTag("omsFragment");
            Fragment fragment1 = getFragmentManager().findFragmentByTag("google_maps_control");
            if (fragment != null && fragment.isVisible()) {
                getFragmentManager().beginTransaction().remove(fragment).commit();
            } else if (fragment1 != null && fragment1.isVisible()) {
                getFragmentManager().beginTransaction().remove(fragment1).commit();
            }
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.recorrido) {
            System.out.println("setting selected");
            new Ruta(this,MainActivity.this);
        } else if (id == R.id.usuarios) {
            System.out.println("ver usuarios");
            this.sw = 1;
            startTask();
        }

        return super.onOptionsItemSelected(item);
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {

            } catch (Exception e) {
                System.out.println(e);
            } finally {
                String respuesta = "";
                WebService webUsers = new WebService();
                try {
                    respuesta = webUsers.execute("http://" + ip + ":8080/WebServiceMovilSQLite/webresources/generic/status/users").get();
                    if (!respuesta.equals("")) {
                        if (omsFragment != null) {
                            omsFragment.users.clear();
                        }
                        JSONArray jsonArray = new JSONArray(respuesta);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            String estado = jsonObject.getString("status");
                            String userName = jsonObject.getString("nombre");
                            String respuesta1 = "";
                            WebService webUsers1 = new WebService();
                            try {
                                respuesta1 = webUsers1.execute("http://" + ip + ":8080/WebServiceMovilSQLite/webresources/generic/posicion/actual/" + userName).get();
                                if (!respuesta1.equals("")) {
                                    JSONArray jsonArray1 = new JSONArray(respuesta1);
                                    JSONObject jsonObject1 = jsonArray1.getJSONObject(0);
                                    System.out.println(jsonObject1.getString("longitude"));
                                    System.out.println(jsonObject1.getString("latitud"));
                                    System.out.println(jsonObject1.getString("nombre"));
                                    userMarker us = new userMarker();
                                    us.estado = Integer.parseInt(estado);
                                    us.usuario = jsonObject1.getString("nombre");
                                    us.latitud = Double.parseDouble(jsonObject1.getString("latitud"));
                                    us.longitud = Double.parseDouble(jsonObject1.getString("longitude"));
                                    omsFragment.users.add(us);
                                }
                            } catch (Exception e) {
                                System.out.println("exception: " + e);
                                return;
                            }
                        }
                        omsFragment.usersMarker();
                    }
                } catch (Exception e) {
                    System.out.println("exception: " + e);
                }
                mHandler.postDelayed(mStatusChecker, 30000);
            }
        }
    };

    void startTask() {
        mStatusChecker.run();
    }

    void stopTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    Runnable sincTast = new Runnable() {
        @Override
        public void run() {
            try{

            }catch (Exception e){

            }finally {
                List<Ubicacion> ubicaciones = INSTANCE.ubicacionDao().notSincUsers(0, user);
                System.out.println(ubicaciones.size());
                if(checkConnection()){
                    for (int i = 0; i < ubicaciones.size() ; i++) {
                        String respuesta = "";
                        String userSinc = ubicaciones.get(i).email;
                        String userLat = Double.toString(ubicaciones.get(i).latitud);
                        String userLong = Double.toString(ubicaciones.get(i).longitud);
                        String userFech = ubicaciones.get(i).fecha;
                        WebService webSinc = new WebService();
                        try{
                            respuesta = webSinc.execute("http://" + ip + ":8080/WebServiceMovilSQLite/webresources/generic/gps/registrar/" + userSinc+"/"+userLat+
                                    "/" + userLong + "/" + userFech).get();
                            if(!respuesta.equals("")){
                                INSTANCE.ubicacionDao().deleteUbicacion(ubicaciones.get(i));
                            }
                            else{
                                System.out.println("error sincronizacion");
                                return;
                            }
                        }catch (Exception e){
                            System.out.println("Error sinc: "+e);
                            return;
                        }

                    }
                }
                sinc.postDelayed(sincTast, 30000);
            }

        }
    };

    void startSinc(){
        sincTast.run();;
    }

    void stopSinc(){
        sinc.removeCallbacks(sincTast);
    }
    @SuppressWarnings("StatementWithEmptyBody")

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.

        int id = item.getItemId();

        if (id == R.id.google_maps_fragment_opt) {
            // Handle the camera action
        } else if (id == R.id.osm_fragment_opt) {

            this.omsFragment = OmsFragment.newInstance("", "");
            android.support.v4.app.FragmentTransaction fragmentTransaction =
                    getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.google_maps_control, omsFragment);
            fragmentTransaction.commit();
            startTask();
            startSinc();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);


        return true;
    }

    public void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(
                    "We need the GPS location to track U and other permissions, please grant all the permissions...");
            builder.setTitle("Permissions granting");
            builder.setPositiveButton(R.string.accept,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(thisActivity,
                                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                                            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE,
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1227);
                        }
                    });
            AlertDialog dialog = builder.create();
            dialog.show();
            return;
        } else {
            this.gpsManager = new GPSManager(this, this);
            gpsManager.InitLocationManager();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1227) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(
                        "The permissions weren't granted, then the app will be close");
                builder.setTitle("Permissions granting");
                builder.setPositiveButton(R.string.accept,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                this.gpsManager = new GPSManager(this, this);
                gpsManager.InitLocationManager();
            }
        }
    }


    @Override
    public void LocationReceived(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        System.out.println("Latitud: " + latitude);
        System.out.println("Longitud: " + longitude);
        LocalDateTime myDateObj = LocalDateTime.now();
        System.out.println(myDateObj);
        DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm:ss");
        String formattedDay = myDateObj.format(dayFormat);
        String formattedTime = myDateObj.format(timeFormat);
        System.out.println(formattedDay);
        System.out.println(formattedTime);
        Date date = new Date();
        if (checkConnection()) {
            String posR = "";
            WebService pos = new WebService();
            try{
                posR = pos.execute("http://" + ip + ":8080/WebServiceMovilSQLite/webresources/generic/gps/registrar/" + this.user +"/"+latitude+
                        "/" + longitude + "/" + formattedDay).get();
                if(!posR.equals("")){
                    Toast.makeText(this, "Posicion registrada ws", Toast.LENGTH_SHORT).show();
                }else{
                    if (!userUbicacion(latitude, longitude, this.user, date, 0,formattedDay)) {
                        Toast.makeText(this, "Error al registrar ubicacion", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Error de coneccion registrada para sincronizacion", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            catch (Exception e){
                Toast.makeText(this, "Error al guardar webservice", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (!userUbicacion(latitude, longitude, this.user, date, 0,formattedDay)) {
                Toast.makeText(this, "Error al registrar ubicacion", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Sin conexion bicacion registrada para sincronizacion", Toast.LENGTH_SHORT).show();
            }
        }
        ((TextView) findViewById(R.id.latitude_value)).setText(latitude + "");
        ((TextView) findViewById(R.id.longitude_value)).setText(longitude + "");
        if (googleMap != null) {
            googleMap.clear();
            googleMap.
                    addMarker(new MarkerOptions().
                            position(new LatLng(latitude, longitude))
                            .title("you are here")
                    );
            googleMap.moveCamera(
                    CameraUpdateFactory.newLatLng(
                            new LatLng(latitude, longitude)));
        }
        if (omsFragment != null) {
            if(sw == 1){
                omsFragment.setCenter(latitude, longitude, this.user);
            }
        }

    }

    @Override
    public void GPSManagerException(Exception error) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //this.googleMap=googleMap;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }


    @Override
    public void WebServiceMessageReceived(final String userState, final String message) {
        thisActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show();
                if (userState.equals("posR")) {
                    if (message.equals("ubicacion registrada")) {
                        System.out.println("ubicacion registrada ws");
                    } else {

                    }
                }
            }
        });
    }

    @Override
    public void trazarRuta(String usuario, String fecha1, String fecha2) {
        userRuta = usuario;
        fecha_ruta1 = fecha1;
        fecha_ruta2 = fecha2;
        String resultado = "";
        WebService webRuta = new WebService();
        try{
            resultado = webRuta.execute("http://" + ip + ":8080/WebServiceMovilSQLite/webresources/generic/FechayHora/arrojar/" + fecha1+","+fecha2+","+userRuta).get();
            if (!resultado.equals("")){
                JSONArray jsonArray = new JSONArray(resultado);
                if (jsonArray.length() == 0){
                    Toast.makeText(this, "no hay ruta para los valores ingresados", Toast.LENGTH_SHORT).show();
                }else{
                    this.sw = 0;
                    stopTask();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        userMarker us = new userMarker();
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        us.usuario = jsonObject.getString("nombre");
                        us.fecha = jsonObject.getString("fecha");
                        us.latitud = Double.parseDouble(jsonObject.getString("latitud"));
                        us.longitud = Double.parseDouble(jsonObject.getString("longitude"));
                        omsFragment.usersRoute.add(us);
                    }
                    omsFragment.usersRoute();
                }
            }
        }
        catch (Exception e){

        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        System.out.println("cerrando main");
        WebServiceManager.CallWebServiceOperation(this, "http://" + ip + ":8080/WebServiceMovilSQLite/webresources",
                "generic",
                "status/update",
                "POST",
                this.user + "," + "0",
                "statusU");
    }

    @Override
    public void onStop(){
        System.out.println("cerrando main stop");
        WebServiceManager.CallWebServiceOperation(this, "http://" + ip + ":8080/WebServiceMovilSQLite/webresources",
                "generic",
                "status/update",
                "POST",
                this.user + "," + "0",
                "statusU");
    }
}
