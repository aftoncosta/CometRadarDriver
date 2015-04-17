package com.example.afton.cometradardriver;

import android.graphics.Color;
import android.os.AsyncTask;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class GetRoute extends AsyncTask<Void, Void, String> {

    String jsonString = "";
    ArrayList<LatLng> poly = new ArrayList<LatLng>();
    MapsActivity ma;
    LatLng[] waypoints;

    public GetRoute(MapsActivity mAct, LatLng[] waypoints) {
        super();
        ma = mAct;
        this.waypoints = waypoints;
    }

    @Override
    protected String doInBackground(Void... arg0) {
        URL url = null;

        try {
            url = getURL();

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

    private URL getURL(){
        try {

//            double originLat = 32.9856748;
//            double originLong = -96.75524339999998;
//            double destinationLat = 32.9855582;
//            double destinationLong = -96.7499986;
//            double[] waypointsLat = {32.9837381, 32.9837774, 32.9855606, 32.9856448};
//            double[] waypointsLong = {-96.7544246, -96.75588640000001, -96.75002030000002, -96.74969579999998};

            double originLat = waypoints[0].latitude;
            double originLong = waypoints[0].longitude;
            double destinationLat = waypoints[waypoints.length-1].latitude;
            double destinationLong = waypoints[waypoints.length-1].longitude;

            double[] waypointsLat = new double[waypoints.length-2];
            double[] waypointsLong =  new double[waypoints.length-2];
            for(int i = 1; i < waypoints.length-1; i++){
                waypointsLat[i-1] = waypoints[i].latitude;
                waypointsLong[i-1] = waypoints[i].longitude;
            }

            String url = "https://maps.googleapis.com/maps/api/directions/json?origin="
                    + originLat + ","
                    + originLong + "&destination="
                    + destinationLat + ","
                    + destinationLong + "&waypoints=";

            for (int i = 0; i < waypointsLat.length; i++)
                url += waypointsLat[i] + "," + waypointsLong[i] + "|";

            url += "&sensor=false&key=AIzaSyB2T0ODhKgWpFWJEyBmDkaYqU0GNGm1HYE";

            return new URL(url);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result){
        JSONObject jsonObject;
        JSONObject polyArray;
        JSONArray routesArray;
        try {
            //Grabbing the Polyline points String. This does pull the correct value.
            //Parsing is correct.
            jsonObject = new JSONObject(jsonString);
            routesArray = jsonObject.getJSONArray("routes");
            JSONObject route = routesArray.getJSONObject(0);
            polyArray = route.getJSONObject("overview_polyline");
            String polyPoints = polyArray.getString("points");

            //Passing the Polyline points from the JSON file I get from the Google Directions URL into a decoder.
            poly = (ArrayList)decodePoly(polyPoints);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ma.mMap.addPolyline(new PolylineOptions()
                .addAll(poly)
                .width(8)
                .color(Color.BLUE));


        /* Uncomment this section when ready to test cart tracking
            DO NOT uncomment until then. I accidentally left the emulator open with this running
            and quickly used up my Google Directions API daily quota (2,500 calls)... so yeah.
        */
//
//        while(true){
//            ma.updatePickupAndCartLocations();
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
    }

    private List<LatLng> decodePoly(String encoded) {

        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),(((double) lng / 1E5)));

            poly.add(p);
        }

        return poly;
    }

}