package com.example.afton.cometradardriver;

import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity {

    protected GoogleMap mMap;   // Might be null if Google Play services APK is not available.
    String routeName;           // The name of the selected route
    boolean isOnDuty;           // Is the driver on duty?
    boolean isFull;             // Is the cart full?
    LatLng userLocation;        // The driver's location

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Bundle bundle = this.getIntent().getExtras();
        routeName = bundle.getString("route");
        new GetRoute(MapsActivity.this).execute();

        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
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

        // Initialize user location
        mMap.setMyLocationEnabled(true);
        Location location = mMap.getMyLocation();
        userLocation = new LatLng(32.985700, -96.752514); // default location is center of campus
        if (location != null) {
            userLocation = new LatLng(location.getLatitude(),
                    location.getLongitude());
        }


        //////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////// DATA TO BE SENT TO DB /////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////////////////
        //////////// TODO: store "userLocation" as the driver's location for the route "routeName" ///////////
        //////////////////////////////////////////////////////////////////////////////////////////////////////


        // Moves camera to current location
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 16));

    }

    protected void updatePickupAndCartLocations(){
        BitmapDescriptor personIcon = BitmapDescriptorFactory.fromResource(R.mipmap.person); // pickupRequest image
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

        // update user location
        Location location = mMap.getMyLocation();
        userLocation = new LatLng(32.985700, -96.752514); // default location is center of campus

        if (location != null) {
            userLocation = new LatLng(location.getLatitude(),
                    location.getLongitude());
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////// DATA TO BE SENT TO DB /////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////////////////
        //////////// TODO: store "userLocation" as the driver's location for the route "routeName" ///////////
        //////////////////////////////////////////////////////////////////////////////////////////////////////

        mMap.addMarker(new MarkerOptions()
                .position(userLocation)
                .icon(cartImage));




        //////////////////////////////////////////////////////////////////////////////////////////////////////
        ///////////////////////////////// DATA TO BE GRABBED FROM DB /////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////////////////
        // TODO: Use String variable "routeName" to get pickup request locations for driver's route from DB //
        //////////////////////////////////////////////////////////////////////////////////////////////////////

        LatLng[] pickupLocations = {new LatLng(32.9837381, -96.7544246), new LatLng(32.9837774, -96.75588640000001)};

        //////////////////////////////////////////////////////////////////////////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////////////////////////

        for (LatLng pickupLoc : pickupLocations) {
            mMap.addMarker(new MarkerOptions()
                    .position(pickupLoc)
                    .icon(personIcon));
        }
    }
}

