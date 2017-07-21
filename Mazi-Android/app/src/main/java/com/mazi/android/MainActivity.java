package com.mazi.android;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;

public class MainActivity extends AppCompatActivity implements parking_fragment.OnHeadlineSelectedListener,college_fragment.OnHeadlineSelectedListener,OnMapReadyCallback{



    String krpURL = "http://192.168.1.204:3000";

    //var used to saved version data from REST API
    private JSONObject version_data;


    private GoogleMap mMap;
    private DB_Helper db_helper;

    //List used to store the college id, lat, lng
    private ArrayList<ArrayList<String>>hidden_college;

    //arrays for spinner info ; element match the hidden list
    public ArrayList<String> face_college = new ArrayList<>();

    private ArrayList<Marker> markers = new ArrayList<>();

    //holds the college id,lat,lng of the current college id
    public String selected_college_id;
    public String type;

    public float lat;
    public float lng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Initializes local db
        db_helper = new DB_Helper(this, null);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        new MainActivity.GetVersionData().execute();






    }

    public void startCollegeFragment(){


        college_fragment college_fragment = new college_fragment();
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.replace(R.id.contentFragment, college_fragment);
        transaction.commit();

    }

    public void startParkingFragment(String selected_college_id){
        parking_fragment parking_fragment = new parking_fragment();
        FragmentManager fm = getSupportFragmentManager();
        Bundle bundle = new Bundle();
        bundle.putString("selected_college_id",selected_college_id);
        parking_fragment.setArguments(bundle);
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.replace(R.id.contentFragment, parking_fragment,"parking_fragment");
        transaction.addToBackStack(null);
        transaction.commit();


    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng college_selected = new LatLng(lat, lng);
        mMap.moveCamera(CameraUpdateFactory.newLatLng( college_selected));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom( college_selected,17));

    }

    @Override
    public void onSpinnerItemSelected(float lat, float lng, String college_id) {
        LatLng college = new LatLng(lat, lng);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(college));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(college,13));
        selected_college_id = college_id;

    }

    @Override
    public void setMarkers(ArrayList<MarkerOptions> marker) {
        markers.clear();
        for(int i = 0; i < marker.size(); i++){
            Marker temp = mMap.addMarker(marker.get(i));
//                Log.e(TAG, "Marker id '"
            markers.add(temp);
        }
    }

    @Override
    public void onParkingSpinnerItemSelected(float lat, float lng) {
        LatLng parking = new LatLng(lat, lng);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(parking));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(parking,17));
    }

    @Override
    public void onRequest(String t) {
        type = t;
        startParkingFragment(selected_college_id);


    }


    /*
  Async task to get the data from the checkVersion REST API
  Svaes data to the version_data var
   */
    private class GetVersionData extends AsyncTask<Object, Object, Void> {
        @Override
        protected Void doInBackground(Object... args) {
            try {
                //REST API url ; calls db method to get the largest verison
                String urlstring = krpURL+"/checkVersion?ver="+ db_helper.getAllCollegeVersion();
                Log.d("KTag",Integer.toString(db_helper.getAllCollegeVersion()));
                Log.d("KTag","GetVersionData REST API check");
                URL versionUrl = new URL(urlstring);
                HttpURLConnection myConnection = (HttpURLConnection) versionUrl.openConnection();
                myConnection.setRequestProperty("User-Agent", "android-client");
                if (myConnection.getResponseCode() == 200) {
                    InputStream responseBody = myConnection.getInputStream();
                    InputStreamReader responseBodyReader  = new InputStreamReader(responseBody, "UTF-8");
                    JsonReader jsonReader = new JsonReader(responseBodyReader);
                    String var = getStringFromInputStream(responseBody);
                    version_data = new JSONObject(var);
                    Log.d("KTag","Sucsessful http REST API");

                } else {
                    Log.d("KTag","Error");
                }

            } catch (MalformedURLException e) {
                Log.d("KTag",e.toString());
                e.printStackTrace();
            } catch (IOException e) {
                Log.d("KTag",e.toString());
                e.printStackTrace();
            } catch (JSONException e) {
                Log.d("KTag",e.toString());
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            new CheckVersion().execute();
        }
    }

    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();

    }

    /*
    Checks the version_data code, updates the local db if needed
     */
    private class CheckVersion extends AsyncTask<Object, Object, Void>{

        int code;
        @Override
        protected Void doInBackground(Object... params) {

            try {
                code = version_data.getInt("code");

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if(code == 2){
                //in case version data is not up to date
                Log.d("KTag","checkVersion : New Data");
                new PushNewData().execute();
            }
            else if(code == 1){
                //in case local db had up to date data
                Log.d("KTag","checkVersion : NO New Data");
                startCollegeFragment();

            }
        }
    }



    /*
    Async task used to push new data
    Pushes data from version_data var
     */
    private class PushNewData extends AsyncTask<Object, Object, Void> {
        @Override
        protected Void doInBackground(Object... args) {

            Log.d("KTag","PushNewData");
            //first will clear all of the data from both local db tables for colleges and parking
            db_helper.clearAllTables();

            try {
                //breaks up college data and parking data into vars
                JSONObject college_data = version_data.getJSONObject("college_data");
                JSONObject parking_data = version_data.getJSONObject("parking_data");

                //parses array of ids to get all of the college data
                JSONArray college_ids = college_data.getJSONArray("ids");

                //pushes data from college into the local db table for colleges
                for(int i = 0; i < college_ids.length(); i++){
                    int num = college_ids.getInt(i);
                    db_helper.addCollege(num, college_data.getJSONObject(Integer.toString(num)));
                }

                //parses array of ids to get all of the parking data
                JSONArray parkinglot_ids = parking_data.getJSONArray("ids");

                //pushes data from college into the local db table for parking
                for(int i = 0; i < parkinglot_ids.length(); i++){
                    int num = parkinglot_ids.getInt(i);
                    db_helper.addParkingLot(num, parking_data.getJSONObject(Integer.toString(num)));
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            startCollegeFragment();
        }
    }



    @Override
    public void onBackPressed() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.contentFragment);
        if(current instanceof parking_fragment){
            mMap.clear();
            markers.clear();
        }

        super.onBackPressed();
    }



}
