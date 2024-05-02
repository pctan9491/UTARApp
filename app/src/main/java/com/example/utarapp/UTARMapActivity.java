package com.example.utarapp;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UTARMapActivity extends AppCompatActivity {
    private MapView map = null;
    private static final String TAG = "UTARMapActivityXXX"; // TAG for Logcat
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private GeoPoint currentUserLocation = null;
    private Spinner destinationSpinner;
    private Polyline currentRouteLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_utarmap);

        destinationSpinner = findViewById(R.id.destinationSpinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter(this,
                android.R.layout.simple_spinner_item, getDestinationNames());
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(R.layout.custom_spinner_item);
        // Apply the adapter to the spinner
        destinationSpinner.setAdapter(adapter);

        destinationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String destinationName = parentView.getItemAtPosition(position).toString();
                GeoPoint destination = getDestinationGeoPoint(destinationName);
                if (currentUserLocation != null && destination != null) {
                    new FetchDirectionsTask(currentUserLocation, destination).execute();
                } else {
                    Log.d(TAG, "Current location is not available or destination is null.");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing here
            }
        });

        Log.d(TAG, "onCreate: Setting osmdroid configuration");
        Configuration.getInstance().setOsmdroidBasePath(new File(getCacheDir().getAbsolutePath(), "osmdroid"));
        Configuration.getInstance().setOsmdroidTileCache(new File(Configuration.getInstance().getOsmdroidBasePath().getAbsolutePath(), "tile"));
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map = findViewById(R.id.map);
        if (map == null) {
            Log.e(TAG, "onCreate: MapView is null!"); // Log if MapView is not found
        } else {
            Log.d(TAG, "onCreate: MapView found and setting tile source");
            map.setTileSource(TileSourceFactory.MAPNIK);

            map.setBuiltInZoomControls(true);
            map.getController().setZoom(19);
            requestPermissions();
            Log.d(TAG, "onCreate: Map is centered to UTAR's coordinates");
        }

        // Button to center map on current location
        Button btnCurrentLocation = findViewById(R.id.btnCurrentLocation);
        btnCurrentLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentUserLocation != null) {
                    map.getController().setCenter(currentUserLocation);
                }
            }
        });

        // Configure offline map tiles
        configureOfflineMapTiles();
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
        Log.d(TAG, "onResume: MapView resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
        Log.d(TAG, "onPause: MapView paused");
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
            // For simplicity, just requesting again.
            ActivityCompat.requestPermissions(UTARMapActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        } else {
            Log.i(TAG, "Requesting permission");
            // You can directly ask for the permission.
            ActivityCompat.requestPermissions(UTARMapActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                initializeLocationLayer();
            } else {
                // Permission denied.
                // You can add logic for showing your UI indicating that the permission is missing
            }
        }
    }

    private void initializeLocationLayer() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        String provider = LocationManager.GPS_PROVIDER;

        // Create a marker for the user's location
        final Marker userLocationMarker = new Marker(map);
        userLocationMarker.setIcon(getResources().getDrawable(R.drawable.ic_marker)); // Set your own marker icon here
        userLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        try {
            locationManager.requestLocationUpdates(provider, 1000, 10, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();
                    currentUserLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
                    // Update the marker's position to the new location
                    userLocationMarker.setPosition(currentUserLocation);
                    if (!map.getOverlays().contains(userLocationMarker)) {
                        map.getOverlays().add(userLocationMarker);
                    }
                    map.invalidate(); // Refresh the map

                    // Log or handle the new location as needed
                    Log.d(TAG, "Location Changed: " + lat + ", " + lon);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onProviderDisabled(String provider) {
                }
            });

            // Optionally, get the last known location and center the map on it
            Location lastKnownLocation = locationManager.getLastKnownLocation(provider);
            if (lastKnownLocation != null) {
                GeoPoint lastKnownGeoPoint = new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                map.getController().setCenter(lastKnownGeoPoint);

                // Set the marker at the last known location
                userLocationMarker.setPosition(lastKnownGeoPoint);
                if (!map.getOverlays().contains(userLocationMarker)) {
                    map.getOverlays().add(userLocationMarker);
                }
            }

        } catch (SecurityException e) {
            Log.e(TAG, "Exception: " + e.getMessage());
        }
    }


    private class FetchDirectionsTask extends AsyncTask<Void, Void, List<GeoPoint>> {
        private GeoPoint start;
        private GeoPoint destination;

        public FetchDirectionsTask(GeoPoint start, GeoPoint destination) {
            this.start = start;
            this.destination = destination;
        }

        @Override
        protected List<GeoPoint> doInBackground(Void... voids) {
            List<GeoPoint> cachedRoute = getCachedRoute(start, destination);
            if (cachedRoute != null) {
                return cachedRoute;
            }

            if (isNetworkAvailable()) {
                // Construct the URL for your routing API (this is highly dependent on the API you choose)
                String apiKey = "5b3ce3597851110001cf62481c0185a65c574b508a430f714d5cf257";
                String urlString = "https://api.openrouteservice.org/v2/directions/foot-walking?api_key=" + apiKey +
                        "&start=" + start.getLongitude() + "," + start.getLatitude() +
                        "&end=" + destination.getLongitude() + "," + destination.getLatitude() +
                        "&preference=fastest";

                HttpURLConnection urlConnection = null;
                try {
                    URL url = new URL(urlString);
                    urlConnection = (HttpURLConnection) url.openConnection();
                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                    // Read InputStream to String
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                    String responseString = sb.toString(); // This is your raw JSON response as a string

                    // Now, pass this string to your JSON parsing method
                    List<GeoPoint> routePoints = parseRoutePointsFromResponse(responseString);
                    if (routePoints != null && !routePoints.isEmpty()) {
                        cacheRoute(start, destination, routePoints);
                        return routePoints;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching directions: ", e);
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(List<GeoPoint> geoPoints) {
            if (geoPoints != null && !geoPoints.isEmpty()) {
                // Remove the previous route from the map if it exists
                if (currentRouteLine != null) {
                    map.getOverlays().remove(currentRouteLine);
                }

                // Draw the route on the map
                Polyline routeLine = new Polyline(map);
                routeLine.setPoints(geoPoints);
                routeLine.setColor(Color.RED); // Set the polyline color
                map.getOverlays().add(routeLine);
                map.invalidate(); // Refresh the map to display the new overlay

                // Keep a reference to the new route
                currentRouteLine = routeLine;

                // Display a message to the user indicating that the route is loaded from cache
                if (!isNetworkAvailable()) {
                    Toast.makeText(UTARMapActivity.this, "Route loaded from cache", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Handle the case when the route points are null (offline mode and no cached route available)
                if (!isNetworkAvailable()) {
                    // Display a message to the user indicating that no cached route is available
                    Toast.makeText(UTARMapActivity.this, "No cached route available", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Failed to fetch or parse directions.");
                }
            }
        }

        private List<GeoPoint> parseRoutePointsFromResponse(String jsonResponse) {
            // Parse your JSON response here
            List<GeoPoint> routePoints = new ArrayList<>();
            try {
                JSONObject jsonObj = new JSONObject(jsonResponse);
                JSONArray coordinates = jsonObj.getJSONArray("features")
                        .getJSONObject(0).getJSONObject("geometry").getJSONArray("coordinates");

                for (int i = 0; i < coordinates.length(); i++) {
                    JSONArray coord = coordinates.getJSONArray(i);
                    double lon = coord.getDouble(0);
                    double lat = coord.getDouble(1);
                    routePoints.add(new GeoPoint(lat, lon));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parsing JSON response: ", e);
            }
            return routePoints;
        }
    }

    private List<String> getDestinationNames() {
        List<String> destinationNames = new ArrayList<>();
        destinationNames.add("Select your destination");
        destinationNames.add("UTAR Hospital");
        destinationNames.add("Mencius Institute");
        destinationNames.add("Faculty of Information and Communication Technology (FICT) Block N");
        destinationNames.add("Faculty of Arts and Social Science (FAS)");
        destinationNames.add("Dewan Tun Dr Ling Liong Sik");
        destinationNames.add("Lecture Complex II (LDK)");
        destinationNames.add("Student Pavilion II");
        destinationNames.add("Faculty of Business and Finance (FBF)");
        destinationNames.add("Engineering Workshop");
        destinationNames.add("Lecture Complex I (IDK) Block I");
        destinationNames.add("Library");
        destinationNames.add("Block F - University Administration Block");
        destinationNames.add("Faculty Of Engineering And Green Technology (FEGT)");
        destinationNames.add("EDK, Block E");
        destinationNames.add("Faculty of Science (FSc)");
        destinationNames.add("Block C - Student Pavillion I (SP1)");
        destinationNames.add("Department of Student Affair (DSA)");
        destinationNames.add("Department of Soft Skills Competency (DSSC)");
        destinationNames.add("Department of Safety & Security (DSS)");
        destinationNames.add("Book Shop");
        destinationNames.add("Learning Complex 1, Block B");
        destinationNames.add("Department of General Services");
        destinationNames.add("Heritage Hall");
        destinationNames.add("Centre for Extension Education");
        destinationNames.add("IT Infrastructure and Support Centre");
        destinationNames.add("Department of Admissions & Credit Evaluation");
        destinationNames.add("Centre for Foundation Studies (CFS)");
// Add more destination names
        return destinationNames;
    }

    private GeoPoint getDestinationGeoPoint(String destinationName) {
        Map<String, GeoPoint> destinations = new HashMap<>();
        destinations.put("UTAR Hospital", new GeoPoint(4.335341, 101.134327));
        destinations.put("Sport Complex", new GeoPoint(4.337018, 101.133903));
        destinations.put("Mencius Institute", new GeoPoint(4.337949, 101.136789));
        destinations.put("Faculty of Information and Communication Technology (FICT) Block N", new GeoPoint(4.338836668871986, 101.13669803930291));
        destinations.put("Faculty of Arts and Social Science (FAS)", new GeoPoint(4.339291364774298, 101.13764216571643));
        destinations.put("Dewan Tun Dr Ling Liong Sik", new GeoPoint(4.340416676184043, 101.13770539976369));
        destinations.put("Lecture Complex II (LDK)", new GeoPoint(4.341788558445499, 101.14030501796375));
        destinations.put("Student Pavilion II", new GeoPoint(4.342072818332328, 101.14131943523198));
        destinations.put("Faculty of Business and Finance (FBF)", new GeoPoint(4.341127007898806, 101.14324190138562));
        destinations.put("Engineering Workshop", new GeoPoint(4.341159289922075, 101.14420757035519));
        destinations.put("Lecture Complex I (IDK) Block I", new GeoPoint(4.3407903474655525, 101.1427977858598));
        destinations.put("Library", new GeoPoint(4.339742924745467, 101.14303498171618));
        destinations.put("Block F - University Administration Block", new GeoPoint(4.341127007898806, 101.14324190138562));
        destinations.put("Faculty Of Engineering And Green Technology (FEGT)", new GeoPoint(4.338829573648899, 101.14373969036967));
        destinations.put("EDK, Block E", new GeoPoint(4.338817036764491, 101.1429287128402));
        destinations.put("Faculty of Science (FSc)", new GeoPoint(4.337969510179795, 101.14383917089944));
        destinations.put("Block C - Student Pavillion I (SP1)", new GeoPoint(4.3372731307620205, 101.14236631768887));
        destinations.put("Department of Student Affair (DSA)", new GeoPoint(4.337002638376611, 101.141969309258));
        destinations.put("Department of Soft Skills Competency (DSSC)", new GeoPoint(4.337122323516332, 101.14254464306596));
        destinations.put("Department of Safety & Security (DSS)", new GeoPoint(4.337341634786189, 101.14259962835108));
        destinations.put("Book Shop", new GeoPoint(4.336902807915645, 101.1422702869279));
        destinations.put("Learning Complex 1, Block B", new GeoPoint(4.336065614222064, 101.14114818879078));
        destinations.put("Department of General Services", new GeoPoint(4.336145921133338, 101.14144299721508));
        destinations.put("Heritage Hall", new GeoPoint(4.33545645523402, 101.14113634852548));
        destinations.put("Centre for Extension Education", new GeoPoint(4.335439382161344, 101.14093881706697));
        destinations.put("IT Infrastructure and Support Centre", new GeoPoint(4.335479139345707, 101.1414699024224));
        destinations.put("Department of Admissions & Credit Evaluation", new GeoPoint(4.335161876949061, 101.14155841665604));
        destinations.put("Centre for Foundation Studies (CFS)", new GeoPoint(4.334953549190502, 101.14118123442383));
        // Add more predefined destinations here

        return destinations.get(destinationName); // Returns null if no match is found
    }

    private void configureOfflineMapTiles() {
        File appDir = new File(getCacheDir(), "osmdroid");
        File tileDir = new File(appDir, "tiles");
        if (!tileDir.exists()) {
            tileDir.mkdirs();
        }

        // Set the osmdroid configuration to use the local tiles directory
        Configuration.getInstance().setOsmdroidBasePath(appDir);
        Configuration.getInstance().setOsmdroidTileCache(tileDir);

        // Setup tile source to use the local tiles
        map.setTileSource(new XYTileSource(
                "Mapnik",
                1, 19, 256, ".png",
                new String[]{tileDir.getAbsolutePath()},
                "© OpenStreetMap contributors"
        ));

        map.setUseDataConnection(false); // Disable internet connection for tiles
    }

    private void cacheRoute(GeoPoint start, GeoPoint end, List<GeoPoint> routePoints) {
        String routeKey = start.getLatitude() + "_" + start.getLongitude() + "_" + end.getLatitude() + "_" + end.getLongitude();
        String routeJson = new Gson().toJson(routePoints);

        SharedPreferences prefs = getSharedPreferences("RouteCachePrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(routeKey, routeJson);
        boolean success = editor.commit();

        if (success) {
            Log.d(TAG, "Route cached successfully with key: " + routeKey);
        } else {
            Log.e(TAG, "Failed to cache route with key: " + routeKey);
        }
    }

    private String generateRouteKey(GeoPoint start, GeoPoint end) {
        return start.getLatitude() + "_" + start.getLongitude() + "_" + end.getLatitude() + "_" + end.getLongitude();
    }

    private List<GeoPoint> getCachedRoute(GeoPoint start, GeoPoint end) {
        String routeKey = generateRouteKey(start, end);

        SharedPreferences prefs = getSharedPreferences("RouteCachePrefs", MODE_PRIVATE);
        String routeJson = prefs.getString(routeKey, null);

        Log.d(TAG, "Trying to get cached route for key: " + routeKey);

        if (routeJson != null && !routeJson.isEmpty()) {
            Log.d(TAG, "Cached route found for key: " + routeKey);
            return new Gson().fromJson(routeJson, new TypeToken<List<GeoPoint>>() {}.getType());
        } else {
            Log.d(TAG, "No cached route found for key: " + routeKey);
            return null;
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}