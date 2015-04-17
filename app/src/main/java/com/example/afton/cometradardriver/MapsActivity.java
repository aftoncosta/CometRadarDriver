package com.example.afton.cometradardriver;

import android.content.IntentSender;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;

public class MapsActivity extends FragmentActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    public static final String TAG = MapsActivity.class.getSimpleName();

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;  //probably wont be used
    private final static int UPDATE_LOCATION_TIME_INTERVAL_MAX = 2*1000; //longest period of time to wait before updating location (in milliseconds)
    private final static int UPDATE_LOCATION_TIME_INTERVAL_MIN = 1*1000; //minimum period of time between location updates (in milliseconds)

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    protected GoogleMap mMap;   // Might be null if Google Play services APK is not available.
    String routeName;           // The name of the selected route
    boolean isOnDuty;           // Is the driver on duty?
    boolean isFull;             // Is the cart full?
    //todo set initial max capacity
    int maximumCapacity;

    private Marker shuttleMarker;
    private LinkedList<Marker> pickupMarkers = new LinkedList<Marker>();

    LatLng userLocation = new LatLng(32.985700, -96.752514);        // The driver's current location, defaults to center of UTD campus

    private static final String GET_RIDERS_URL =
              "http://104.197.3.201:3000/api/getRiderLocations";
    private static final String UPDATE_LOCATION_URL =
            "http://104.197.3.201:3000/api/updateLocation";
    private static final String UPDATE_RIDER_COUNT_URL =
            "http://104.197.3.201:3000/api/updateRiderCount";
    private static final String GET_ROUTE_URL =
            "http://104.197.3.201:3000/route-waypoints";
    private static final String GET_SHUTTLE_CAPACITY_URL =
            "http://104.197.3.201:3000/api/getShuttleCapacity";

    //TODO increment riders
    public void incrementRiders(){
        Log.i("increment riders", "entered function");

        Button riderCountButton = (Button) findViewById(R.id.riderCountButton);

        //don't let count go above maximum shuttle capacity
        int currentCapacity = Integer.parseInt(riderCountButton.getText().toString());
        if(currentCapacity >= maximumCapacity) //shuttle already full, do nothing
            return;

        currentCapacity++;
        if(currentCapacity == maximumCapacity){
            isFull = true;
        }

        riderCountButton.setText(currentCapacity);

        new apiRequest().execute(String.format("%s%s%s", UPDATE_RIDER_COUNT_URL + "?rname=", Uri.encode(routeName),
                "&currentCapacity=" + currentCapacity), "upc");
    }

    //TODO decrement riders
    public void decrementRiders(){
        Log.i("decrement riders", "entered function");

        Button riderCountButton = (Button) findViewById(R.id.riderCountButton);

        //don't let count go below 0
        int currentCapacity = Integer.parseInt(riderCountButton.getText().toString());
        if(currentCapacity <= 0) //shuttle already full, do nothing
            return;

        if(currentCapacity == maximumCapacity){
            isFull = false;
        }
        currentCapacity--;

        riderCountButton.setText(currentCapacity);

        new apiRequest().execute(String.format("%s%s%s", UPDATE_RIDER_COUNT_URL + "?rname=", Uri.encode(routeName),
                "&currentCapacity=" + currentCapacity), "upc");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Bundle bundle = this.getIntent().getExtras();
        routeName = bundle.getString("route");

        setUpMapIfNeeded();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_LOCATION_TIME_INTERVAL_MAX)
                .setFastestInterval(UPDATE_LOCATION_TIME_INTERVAL_MIN)
                .setSmallestDisplacement(0);
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     **/
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera.
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        BitmapDescriptor mapOverlay = BitmapDescriptorFactory.fromResource(R.mipmap.mapoverlay); // get an image.
        LatLngBounds bounds = new LatLngBounds(new LatLng(32.976600, -96.761700), new LatLng(32.995650, -96.739400)); // get a bounds

        isOnDuty = true;
        isFull = false;

        mMap.addGroundOverlay(new GroundOverlayOptions()
                .image(mapOverlay)
                .positionFromBounds(bounds));

        mMap.setMyLocationEnabled(true);

        // Moves camera to current location
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 16));

    }

    protected void updatePickupAndCartLocations(Location location){
        BitmapDescriptor greenCartIcon = BitmapDescriptorFactory.fromResource(R.mipmap.cart_green); // available cart image
        BitmapDescriptor redCartIcon = BitmapDescriptorFactory.fromResource(R.mipmap.cart_red); // full cart image
        BitmapDescriptor blackCartIcon = BitmapDescriptorFactory.fromResource(R.mipmap.cart); // off-duty cart image
        BitmapDescriptor cartImage;

        // choose the correct cart image (on-duty, off-duty, or full)
        if (isOnDuty) {
            cartImage = greenCartIcon;
            if (isFull) {
                cartImage = redCartIcon;
            }
        } else {
            cartImage = blackCartIcon;
        }

        if(location == null)
            return;

        new apiRequest().execute(String.format("%s%s%s",UPDATE_LOCATION_URL + "?rname=", Uri.encode(routeName),
                "&lat=" + userLocation.latitude + "&long=" + userLocation.longitude), "ul");

        new apiRequest().execute(String.format("%s%s", GET_RIDERS_URL + "?rname=", Uri.encode(routeName)), "upl");

        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        userLocation = new LatLng(currentLatitude, currentLongitude);

        if(shuttleMarker != null){
            shuttleMarker.remove();
        }

        shuttleMarker = mMap.addMarker(new MarkerOptions()
                .position(userLocation)
                .icon(cartImage));

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 16));

    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("onConnected", "CONNECTED TO PLAY SERVICES!!");

        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        new apiRequest().execute(String.format("%s%s", GET_ROUTE_URL + "?route=", Uri.encode(routeName)), "gr");
        new apiRequest().execute(String.format("%s%s", GET_SHUTTLE_CAPACITY_URL + "?route=", Uri.encode(routeName)), "gsc");

        updatePickupAndCartLocations(location);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        updatePickupAndCartLocations(location);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        Log.e("Connected?", String.valueOf(mGoogleApiClient.isConnected()));
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private class apiRequest extends AsyncTask<String, String, String> {

        String jsonString = "";
        String method = "";

        public apiRequest() {
            super();
        }

        @Override
        protected String doInBackground(String... params) {
            URL url = null;
            method = params[1];

            try {
                Log.i("api request", params[0]);
                url = new URL(params[0]);

                BufferedInputStream bis = new BufferedInputStream(url.openStream());
                byte[] buffer = new byte[1024];
                StringBuilder sb = new StringBuilder();
                int bytesRead = 0;
                while((bytesRead = bis.read(buffer)) > 0) {
                    String text = new String(buffer, 0, bytesRead);
                    sb.append(text);
                }
                bis.close();
                jsonString = sb.toString();

                return jsonString;

            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i("api request", jsonString);
            Log.i("api request", method);
            JSONArray routesArray;
            if (method.equals("upl") && jsonString.length() > 1) { //update pickup locations
                try {
                    routesArray = new JSONArray(jsonString);
                    LatLng[] pickupLocations = new LatLng[routesArray.length()];

                    for (int i = 0; i < routesArray.length(); i++) {
                        JSONObject jo = routesArray.getJSONObject(i);
                        pickupLocations[i] = new LatLng(jo.getDouble("lat"), jo.getDouble("long"));
                    }

                    //remove old rider pickup locations from the map
                    while (!pickupMarkers.isEmpty()) {
                        pickupMarkers.removeFirst().remove();
                    }

                    //add new rider pickup locations to map
                    for (LatLng pickupLoc : pickupLocations) {
                        pickupMarkers.add(
                                mMap.addMarker(new MarkerOptions()
                                        .position(pickupLoc)
                                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.person))));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }else if(method.equals("gsc") && jsonString.length() > 1) { //get route
                try {
                    routesArray = new JSONArray(jsonString);
                    maximumCapacity = routesArray.getJSONObject(0).getInt("max");
                }catch (JSONException e) {
                    e.printStackTrace();
                }
            }else if(method.equals("gr") && jsonString.length() > 1){ //get route
                try {
                    routesArray = new JSONArray(jsonString);
                    LatLng[] waypoints = new LatLng[routesArray.length()+2];

                    JSONObject origin = routesArray.getJSONObject(0);
                    waypoints[0] = new LatLng(origin.getDouble("originLat"), origin.getDouble("originLong"));
                    for(int i = 0; i < routesArray.length(); i++){
                        JSONObject waypoint = routesArray.getJSONObject(i);
                        waypoints[i+1] = new LatLng(waypoint.getDouble("wp_lat"), waypoint.getDouble("wp_long"));
                    }

                    JSONObject dest = routesArray.getJSONObject(0);
                    waypoints[waypoints.length-1] = new LatLng(dest.getDouble("destLat"), dest.getDouble("destLong"));
                    new GetRoute(MapsActivity.this, waypoints).execute();

                }catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}

