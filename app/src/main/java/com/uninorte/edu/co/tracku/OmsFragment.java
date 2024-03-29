package com.uninorte.edu.co.tracku;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OmsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link OmsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class OmsFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    List<userMarker> users = new ArrayList<>();
    List<userMarker> usersRoute = new ArrayList<>();


    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    MapView map;

    public OmsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment OmsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static OmsFragment newInstance(String param1, String param2) {
        OmsFragment fragment = new OmsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        Context ctx = getContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_oms, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(map==null) {
            map = (MapView) (this.getActivity()).findViewById(R.id.oms_map);
            if (map != null) {
                map.setTileSource(TileSourceFactory.MAPNIK);
                map.onResume();
            }
        }else{
            map.onResume();
        }
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        MyLocationNewOverlay myLocationNewOverlay=
                new MyLocationNewOverlay(
                        new GpsMyLocationProvider(this.getContext()),map);
        myLocationNewOverlay.enableMyLocation();
        this.map.getOverlays().add(myLocationNewOverlay);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public void setCenter(double latitude, double longitude, String usuario){
        IMapController mapController = map.getController();
        GeoPoint newCenter = new GeoPoint(latitude, longitude);
        mapController.setCenter(newCenter);
        //mapController.animateTo(newCenter);
        if(!users.isEmpty()){
            for (int i = 0; i <users.size() ; i++) {
                if(users.get(i).usuario.equals(usuario)){
                    userMarker replace = new userMarker();
                    replace.usuario = usuario;
                    replace.estado = 1;
                    replace.latitud = latitude;
                    replace.longitud = longitude;
                    users.set(i,replace);
                }
            }
        }
        try {
            addMarker(newCenter, usuario);
        }catch (Exception e){
            System.out.println("Excepcion marcador: " +e);
        }
    }

    public void usersMarker(){
        map.getOverlays().clear();
        for (int i = 0; i < users.size(); i++) {
            try{
                GeoPoint geoPoint = new GeoPoint(users.get(i).latitud,users.get(i).longitud);
                Marker marker = new Marker(map);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                marker.setIcon(getResources().getDrawable(R.drawable.osm_ic_follow_me));
                String title = users.get(i).usuario;
                if(users.get(i).estado == 1){
                    title = title + "," + "activo";
                }
                else{
                    title = title + "," + "inactivo";
                }
                marker.setTitle(title);
                marker.setPosition(geoPoint);
                map.getOverlays().add(marker);
            }
            catch (Exception e){
                System.out.println("Excepcion marcador: " +e);
            }
        }
        map.invalidate();
    }

    public void usersRoute(){
        map.getOverlays().clear();
        for (int i = 0; i < usersRoute.size(); i++) {
            try{
                GeoPoint geoPoint = new GeoPoint(usersRoute.get(i).latitud,usersRoute.get(i).longitud);
                Marker marker = new Marker(map);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                marker.setIcon(getResources().getDrawable(R.drawable.osm_ic_follow_me));
                String title = usersRoute.get(i).usuario + "," + usersRoute.get(i).fecha;
                marker.setTitle(title);
                marker.setPosition(geoPoint);
                map.getOverlays().add(marker);
            }
            catch (Exception e){
                System.out.println("Excepcion marcador: " +e);
            }
        }
        map.invalidate();
    }

    public void addMarker(GeoPoint center, String usuario){
        Marker marker = new Marker(map);
        marker.setPosition(center);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(getResources().getDrawable(R.drawable.osm_ic_follow_me));
        marker.setTitle(usuario);
        map.getOverlays().clear();
        map.getOverlays().add(marker);
        for (int i = 0; i < users.size(); i++) {
            try{
                GeoPoint geoPoint = new GeoPoint(users.get(i).latitud,users.get(i).longitud);
                Marker marker1 = new Marker(map);
                marker1.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                marker1.setIcon(getResources().getDrawable(R.drawable.osm_ic_follow_me));
                String title = users.get(i).usuario;
                if(users.get(i).estado == 1){
                    title = title + "," + "activo";
                }
                else{
                    title = title + "," + "inactivo";
                }
                marker1.setTitle(title);
                marker1.setPosition(geoPoint);
                map.getOverlays().add(marker1);
            }
            catch (Exception e){
                System.out.println("Excepcion marcador: " +e);
            }
        }
        map.invalidate();
    }

}
