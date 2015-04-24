package com.example.afton.cometradardriver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;

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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;

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
    //TODO inserting into update location
    String email;
    String routeName;           // The name of the selected route
    boolean isOnDuty;           // Is the driver on duty?
    boolean isFull;             // Is the cart full?
    int maximumCapacity;

    private Marker shuttleMarker;
    private LinkedList<Marker> pickupMarkers = new LinkedList<Marker>();

    LatLng userLocation = new LatLng(32.985700, -96.752514);        // The driver's current location, defaults to center of UTD campus

    private static final String GET_RIDERS_URL =
              "http://104.197.3.201:3000/api/getRiderLocations";
    private static final String GET_ROUTE_URL =
            "http://104.197.3.201:3000/route-waypoints";
    private static final String GET_SHUTTLE_CAPACITY_URL =
            "http://104.197.3.201:3000/api/getShuttleCapacity";
    private static final String GET_DUTY_STATUS_URL =
            "http://104.197.3.201:3000/api/getDutyStatus";
    private static final String UPDATE_DUTY_STATUS_URL =
            "http://104.197.3.201:3000/api/updateDutyStatus";
    private static final String UPDATE_ROUTE_STOPS_URL =
            "http://104.197.3.201:3000/api/updateRouteStops";
    private static final String UPDATE_LOCATION_URL =
            "http://104.197.3.201:3000/api/updateLocation";
    private static final String UPDATE_RIDER_COUNT_URL =
            "http://104.197.3.201:3000/api/updateRiderCount";

    private static boolean driverUpdatedDutyStatus = false;
//    BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Bundle bundle = this.getIntent().getExtras();
        routeName = bundle.getString("route");
        email = bundle.getString("email");

        setUpMapIfNeeded();
//        startBluetooth();

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

//    private void turnOnBT(){
//        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//        startActivityForResult(intent, 1);
//    }

    /**
     * sets up bluetooth
     */
//    Set<BluetoothDevice> devicesArray;
//    ArrayList<String> pairedDevices = new ArrayList<String>();
//    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
//    BroadcastReceiver receiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//
//            if(BluetoothDevice.ACTION_FOUND.equals(action)){
//                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                Log.i("broadcast receiver", "new device: " + device.getName() + "\n" + device.getAddress());
//
//                String s = "";
//                for(int i = 0; i < pairedDevices.size(); i++){
//                    if(device.getName().equals(pairedDevices.get(i))){
//                        s = "(PAIRED)";
//                        Log.i("receiver.onReceive", "found new bt device " + device.getName());
//                        break;
//                    }
//                }
//
//            }else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
//
//            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
//
//            }else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
//                if(mBluetoothAdapter.getState() == mBluetoothAdapter.STATE_OFF){
//                    turnOnBT();
//                }
//            }
//        }
//    };
//    private void startBluetooth(){
//
//        registerReceiver(receiver, filter);
//        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
//        registerReceiver(receiver, filter);
//        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//        registerReceiver(receiver, filter);
//        filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
//        registerReceiver(receiver, filter);
//
//        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (mBluetoothAdapter == null) {
//            Log.e("MapsActivity", "device does not support bluetooth");
//        }
//
//        if (!mBluetoothAdapter.isEnabled()) {
//            turnOnBT();
//        }
//
//        getPairedDevices();
//        startDiscovery();
//
//        (new AcceptThread()).run();
//
//    }
//
//    private void startDiscovery() {
//        mBluetoothAdapter.cancelDiscovery();
//        mBluetoothAdapter.startDiscovery();
//    }
//
//    private void getPairedDevices(){
//        devicesArray = mBluetoothAdapter.getBondedDevices();
//        if(devicesArray.size() > 0){
//            for(BluetoothDevice d : devicesArray){
//                Log.i("getPairedDevices", d.getName() + "\n" + d.getAddress());
//            }
//        }
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_CANCELED){
            Log.d("MapsActivity", "user canceled bluetooth");
        }
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
    public void incrementRiders(View v){
        Button riderCountButton = (Button) findViewById(R.id.riderCountButton);

        //don't let count go above maximum shuttle capacity
        int currentCapacity = Integer.parseInt(riderCountButton.getText().toString());
        if(currentCapacity >= maximumCapacity) //shuttle already full, do nothing
            return;

        currentCapacity++;
        if(currentCapacity == maximumCapacity){
            isFull = true;
        }

        riderCountButton.setText(Integer.toString(currentCapacity));
        new apiRequest().execute(String.format("%s%s%s", UPDATE_RIDER_COUNT_URL + "?rname=", Uri.encode(routeName),
                "&currentCapacity=" + currentCapacity), "upc");
        new apiRequest().execute((UPDATE_ROUTE_STOPS_URL + "?rname=" + Uri.encode(routeName) + "&lat=" + userLocation.latitude
                + "&long=" + userLocation.longitude + "&isPickup=1"), "uss");
    }
    public void decrementRiders(View v){

        Button riderCountButton = (Button) findViewById(R.id.riderCountButton);

        //don't let count go below 0
        int currentCapacity = Integer.parseInt(riderCountButton.getText().toString());
        if(currentCapacity <= 0) //shuttle already full, do nothing
            return;

        if(currentCapacity == maximumCapacity){
            isFull = false;
        }
        currentCapacity--;

        riderCountButton.setText(Integer.toString(currentCapacity));
        new apiRequest().execute(String.format("%s%s%s", UPDATE_RIDER_COUNT_URL + "?rname=", Uri.encode(routeName),
                "&currentCapacity=" + currentCapacity), "upc");
        new apiRequest().execute(String.format("%s%s%s%s", UPDATE_ROUTE_STOPS_URL + "?rname=", Uri.encode(routeName),
                "&lat=" + userLocation.latitude + "&long=" + userLocation.longitude, "&isPickup=0"), "uss");
    }
    public void toggleDutyStatus(View v){
        driverUpdatedDutyStatus = true;
        new apiRequest().execute(String.format("%s%s%s%s", UPDATE_DUTY_STATUS_URL + "?rname=", Uri.encode(routeName),
                "&email=", Uri.encode(email)), "uds");

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
                "&lat=" + userLocation.latitude + "&long=" + userLocation.longitude), "ul");                        //update shuttle location

        new apiRequest().execute(String.format("%s%s", GET_RIDERS_URL + "?rname=", Uri.encode(routeName)), "upl"); //update rider locations
        new apiRequest().execute(String.format("%s%s%s%s", GET_DUTY_STATUS_URL + "?rname=", Uri.encode(routeName),
                "&email=", Uri.encode(email)), "gds"); //update onDuty

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

        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        new apiRequest().execute(String.format("%s%s", GET_ROUTE_URL + "?route=", Uri.encode(routeName)), "gr");
        new apiRequest().execute(String.format("%s%s", GET_SHUTTLE_CAPACITY_URL + "?rname=", Uri.encode(routeName)), "gsc");

        updatePickupAndCartLocations(location);
    }
    @Override
    protected void onPause() {
        super.onPause();
//        unregisterReceiver(receiver);
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
//    boolean sameLocation = false;
    @Override
    public void onLocationChanged(Location location) {

//        //we've been at the same location for 2 ticks, update as a stop
//        if(!sameLocation && location.getLatitude() == userLocation.latitude && location.getLongitude() == userLocation.longitude) {
//            new apiRequest().execute(String.format("%s%s", UPDATE_ROUTE_STOPS_URL + "?rname=", Uri.encode(routeName),
//                    "&lat=" + userLocation.latitude + "&long=" + userLocation.longitude, "&isPickup=true"), "uss");
//            sameLocation = true;
//        }
//        sameLocation = false;

        updatePickupAndCartLocations(location);
    }
    @Override
    public void onConnectionSuspended(int i) {

    }
    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }
    @Override
    public void onStop() {
        super.onStop();
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
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

            if(method.equals("gds")){
                if(driverUpdatedDutyStatus){
                    driverUpdatedDutyStatus = false;
                    return "";
                }
            }

            try {
//                Log.i("api request", params[0]);
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
                if(method.equalsIgnoreCase("ul")){
                    isOnDuty = false;
                }
                e.printStackTrace();
            }
            return "";
        }

        @Override
        protected void onPostExecute(String result) {
//            Log.i("api request", jsonString);
//            Log.i("api request", method);
            JSONArray routesArray;
            if(jsonString.length() > 1)
                if(method.equals("ul")){
                    isOnDuty = true;
                }else if (method.equals("upl")) { //update pickup locations
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
                            if(measure(userLocation.latitude, userLocation.longitude, pickupLoc.latitude, pickupLoc.longitude) > 5){
                                pickupMarkers.add(
                                    mMap.addMarker(new MarkerOptions()
                                            .position(pickupLoc)
                                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.person))));
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }else if(method.equals("gsc")) { //get route
                    try {
                        routesArray = new JSONArray(jsonString);
                        maximumCapacity = routesArray.getJSONObject(0).getInt("max");
                    }catch (JSONException e) {
                        e.printStackTrace();
                    }
                }else if(method.equals("gr")){ //get route
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
//                }else if(method.equals("gds")){ //get the duty status on the server
//                    if(!driverUpdatedDutyStatus){
//                        try {
//                            routesArray = new JSONArray(jsonString);
//                            isOnDuty = routesArray.getJSONObject(0).getBoolean("onduty");
//                        }catch (JSONException e) {
//                            e.printStackTrace();
//                        }
//                    }
                }
        }
    }

//    private class AcceptThread extends Thread {
//        private final BluetoothServerSocket mmServerSocket;
//
//        public AcceptThread() {
//            // Use a temporary object that is later assigned to mmServerSocket,
//            // because mmServerSocket is final
//            BluetoothServerSocket tmp = null;
//            try {
//                // MY_UUID is the app's UUID string, also used by the client code
//                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord("Comet Radar Driver", UUID.fromString("79812711-7b33-40c2-962f-9a191bcfd73b"));
//            } catch (IOException e) {
//                Log.e("Accept Thread", e.toString());
//                e.printStackTrace();
//            }
//            mmServerSocket = tmp;
//        }
//
//        public void run() {
//            BluetoothSocket socket = null;
//            // Keep listening until exception occurs or a socket is returned
//            while (true) {
//                try {
//                    Log.d("Accept Thread", "running");
//                    socket = mmServerSocket.accept();
//
//                    // If a connection was accepted
//                    if (socket != null) {
//                        Log.d("Accept Thread", "spinning off child process to manage connection");
//                        // Do work to manage the connection (in a separate thread)
//                        (new ConnectedThread(socket)).run();
//                        mmServerSocket.close();
//                        break;
//                    }
//                } catch (IOException e) {
//                    break;
//                }
//            }
//        }
//
//        /** Will cancel the listening socket, and cause the thread to finish */
//        public void cancel() {
//            Log.d("Accept Thread", "canceling");
//            try {
//                mmServerSocket.close();
//            } catch (IOException e) { }
//        }
//    }

//    private class ConnectedThread extends Thread {
//        private final BluetoothSocket mmSocket;
//        private final InputStream mmInStream;
//        private final OutputStream mmOutStream;
//
//        public ConnectedThread(BluetoothSocket socket) {
//            mmSocket = socket;
//            InputStream tmpIn = null;
//            OutputStream tmpOut = null;
//
//            // Get the input and output streams, using temp objects because
//            // member streams are final
//            try {
//                tmpIn = socket.getInputStream();
//                tmpOut = socket.getOutputStream();
//            } catch (IOException e) { }
//
//            mmInStream = tmpIn;
//            mmOutStream = tmpOut;
//            Log.d("Connected Thread", "constructor finished");
//        }
//
//        public void run() {
//            Log.d("Connected Thread", "running");
//            byte[] buffer = new byte[1024];  // buffer store for the stream
//            int bytes; // bytes returned from read()
//
//            // Keep listening to the InputStream until an exception occurs
//            while (true) {
//                try {
//                    // Read from the InputStream
//                    bytes = mmInStream.read(buffer);
//                    // Send the obtained bytes to the UI activity
//                    //TODO use info
//                    StringBuilder s = new StringBuilder();
//                    for(int i = 0; i < bytes; i++){
//                        s.append((char)buffer[i]);
//                    }
//                    Log.i("bt: Connected Thread", s.toString());
////                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
////                            .sendToTarget();
//                } catch (IOException e) {
//                    break;
//                }
//            }
//        }
//
//        /* Call this from the main activity to send data to the remote device */
//        public void write(byte[] bytes) {
//            try {
//                mmOutStream.write(bytes);
//            } catch (IOException e) { }
//        }
//
//        /* Call this from the main activity to shutdown the connection */
//        public void cancel() {
//            try {
//                mmSocket.close();
//            } catch (IOException e) { }
//        }
//    }

    public double measure(double lat1, double lon1, double lat2, double lon2){  // generally used geo measurement function
        double R = 6378.137; // Radius of earth in KM
        double dLat = (lat2 - lat1) * Math.PI / 180;
        double dLon = (lon2 - lon1) * Math.PI / 180;
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = R * c;
        return d * 1000; // meters
    }

}

