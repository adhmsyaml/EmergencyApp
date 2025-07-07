package com.example.emergencyapp2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson; // You might need to add this dependency: implementation 'com.google.code.gson:gson:2.10.1'

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import com.example.emergencyapp2.EmergencyContact;

import android.telephony.SmsManager;
import android.util.Log;

import com.example.emergencyapp2.BuildConfig;

import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.emergencyapp2.User;
import com.example.emergencyapp2.EmergencyContact;
import java.util.ArrayList;
import java.util.List;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, PlacesBottomSheetDialogFragment.OnPlaceTypeSelectedListener {
    private GoogleMap mMap;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private static final float DEFAULT_ZOOM = 15f;

    // For Location
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private FusedLocationProviderClient fusedLocationClient;
    private Location lastKnownLocation;
    private LocationCallback locationCallback;

    // For Google Places
    private PlacesClient placesClient;
    private List<Marker> placesMarkers = new ArrayList<>();

    // For SOS
    private Handler sosHandler = new Handler(Looper.getMainLooper());
    private Runnable sosRunnable;
    private static final long LONG_PRESS_DURATION = 3000; // 3 seconds

    private IntentIntegrator integrator;
    private FloatingActionButton recenterMapFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- Firebase Auth Check ---
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(MapsActivity.this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_maps);

        ImageButton profileButton = findViewById(R.id.profileButton);
        ImageButton cameraButton = findViewById(R.id.cameraButton);
        com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton sosButton = findViewById(R.id.sosButton);        findViewById(R.id.findNearbyButton).setOnClickListener(v -> {
            PlacesBottomSheetDialogFragment bottomSheet = new PlacesBottomSheetDialogFragment();
            bottomSheet.show(getSupportFragmentManager(), "PlacesBottomSheet");
        });

        profileButton.setOnClickListener(v -> {
            startActivity(new Intent(MapsActivity.this, ProfileActivity.class));
        });

        cameraButton.setOnClickListener(v -> {
            startActivity(new Intent(MapsActivity.this, CameraActivity.class));
        });

        sosButton.setOnClickListener(v -> {
            // TODO: Implement your SOS alert logic here
            Toast.makeText(this, "SOS Activated!", Toast.LENGTH_SHORT).show();
        });

        // --- Initialize Places SDK ---
        // **IMPORTANT**: Your API key is in Maps_api.xml
        String apiKey = BuildConfig.MAPS_API_KEY;
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }
        placesClient = Places.createClient(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        recenterMapFab = findViewById(R.id.fab_recenter_map);

        setupUIListeners();

        recenterMapFab.setOnClickListener(v -> recenterMapOnUserLocation());
    }

    @Override
    public void onPlaceTypeSelected(String placeType) {
        // Now you can call your existing findNearbyPlaces method
        findNearbyPlaces(placeType);
    }

    private void recenterMapOnUserLocation() {
        if (mMap != null && lastKnownLocation != null) {
            LatLng userLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, DEFAULT_ZOOM));
            Toast.makeText(this, "Re-centering map...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Current location not available yet.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupUIListeners() {
        // --- SOS Button ---
        // Correctly declare the variable as a FloatingActionButton
        com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton sosButton = findViewById(R.id.sosButton);        sosButton.setOnClickListener(v -> Toast.makeText(MapsActivity.this, "Press and hold for 3 seconds to send SOS.", Toast.LENGTH_SHORT).show());
        sosButton.setOnLongClickListener(v -> {
            showSosOptionsDialog();
            return true;
        });

        // --- Bottom Nav Buttons ---
        findViewById(R.id.profileButton).setOnClickListener(v -> startActivity(new Intent(MapsActivity.this, ProfileActivity.class)));
        findViewById(R.id.cameraButton).setOnClickListener(v -> startActivity(new Intent(MapsActivity.this, CameraActivity.class)));
        findViewById(R.id.qrScanButton).setOnClickListener(v -> startQrScanner());
    }

    private void startQrScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt("Scan an Emergency QR Code");
        integrator.setCameraId(0);  // Use a specific camera of the device
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(true);
        integrator.setOrientationLocked(true);
        integrator.initiateScan();
    }

    // --- ADD THIS METHOD to handle the result from the scanner ---
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_LONG).show();
            } else {
                // --- We have a result! ---
                String scannedData = result.getContents();
                Toast.makeText(this, "Scanned: " + scannedData, Toast.LENGTH_LONG).show();

                // Check if it's a URL
                if (scannedData.startsWith("http://") || scannedData.startsWith("https://")) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(scannedData));
                    startActivity(browserIntent);
                }
                // Check if it looks like a phone number (simple check)
                else if (scannedData.matches("\\d{10,13}")) { // Matches 10 to 13 digits
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                    dialIntent.setData(Uri.parse("tel:" + scannedData));
                    startActivity(dialIntent);
                } else {
                    // Otherwise, just show the text content
                    new AlertDialog.Builder(this)
                            .setTitle("Scanned Content")
                            .setMessage(scannedData)
                            .setPositiveButton("OK", null)
                            .show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());
        mMap.setOnInfoWindowClickListener(new CustomInfoWindowClickListener());

        checkLocationPermission();
    }

    class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        private final View mWindow;

        CustomInfoWindowAdapter() {
            mWindow = getLayoutInflater().inflate(R.layout.custom_info_window, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            TextView title = mWindow.findViewById(R.id.info_title);
            title.setText(marker.getTitle());

            TextView snippet = mWindow.findViewById(R.id.info_snippet);
            snippet.setText(marker.getSnippet());

            return mWindow;
        }

        @Override
        public View getInfoContents(Marker marker) {
            // This method is not used in this implementation
            return null;
        }
    }

    class CustomInfoWindowClickListener implements GoogleMap.OnInfoWindowClickListener {
        @Override
        public void onInfoWindowClick(Marker marker) {
            // Create a URI for the navigation intent
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + marker.getPosition().latitude + "," + marker.getPosition().longitude);

            // Create an Intent to launch Google Maps
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            // Verify that the Google Maps app is installed before launching
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Toast.makeText(MapsActivity.this, "Google Maps is not installed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Finds nearby places of a specific type using Google Places API (via a backend proxy for security).
     * @param placeType The type of place to search for (e.g., "hospital", "police").
     */
    private void findNearbyPlaces(String placeType) {
        if (lastKnownLocation == null) {
            Toast.makeText(this, "Cannot get current location. Please wait.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Clear previous markers
        for (Marker marker : placesMarkers) {
            marker.remove();
        }
        placesMarkers.clear();

        Toast.makeText(this, "Searching for nearby " + placeType.replace('_', ' ') + "s...", Toast.LENGTH_SHORT).show();

        String apiKey = BuildConfig.MAPS_API_KEY;
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=" + lastKnownLocation.getLatitude() + "," + lastKnownLocation.getLongitude() +
                "&radius=5000" + // 5km radius
                "&type=" + placeType +
                "&key=" + apiKey;

        // Use a background thread for network request
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                String jsonResponse = response.body().string();

                // Use Gson to parse the JSON response
                Gson gson = new Gson();
                PlacesApiResponse placesResponse = gson.fromJson(jsonResponse, PlacesApiResponse.class);

                // Update UI on the main thread
                runOnUiThread(() -> {
                    if (placesResponse != null && placesResponse.results != null && !placesResponse.results.isEmpty()) {
                        // THIS IS THE CORRECTED LINE:
                        for (PlaceResult result : placesResponse.results) {
                            LatLng placeLatLng = new LatLng(result.geometry.location.lat, result.geometry.location.lng);
                            addPlaceMarker(placeLatLng, result.name, result.vicinity);
                        }
                    } else {
                        Toast.makeText(MapsActivity.this, "No nearby " + placeType.replace("_", " ") + "s found.", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (IOException e) {
                Log.e("PlacesAPI", "Error fetching places", e);
                runOnUiThread(() -> Toast.makeText(MapsActivity.this, "Error finding places.", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void addPlaceMarker(LatLng position, String title, String snippet) {
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(position)
                .title(title)
                .snippet(snippet)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        placesMarkers.add(marker);
    }


    /**
     * The rest of the file contains location permission handling, similar to before.
     */

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
        } else {
            startLocationUpdates();
            mMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
                mMap.setMyLocationEnabled(true);
            }
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_LONG).show();
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000); // 10 seconds
        locationRequest.setFastestInterval(5000);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    lastKnownLocation = locationResult.getLastLocation();
                    LatLng latLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                    // Move camera only the first time we get a location
                    if (mMap.getCameraPosition().zoom < 10) { // Simple check to see if map is at default state
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        }
    }

    private void showSosOptionsDialog() {
        if (lastKnownLocation == null) {
            Toast.makeText(this, "Cannot get current location. Please wait.", Toast.LENGTH_SHORT).show();
            return;
        }

        // The main actions are now in this array
        final CharSequence[] options = {
                "Send WhatsApp SOS",
                "Call an Emergency Contact"
        };

        // Use MaterialAlertDialogBuilder for better styling
        new MaterialAlertDialogBuilder(this)
                .setTitle("SOS Options")
                .setItems(options, (dialog, item) -> {
                    // This logic remains the same
                    if (options[item].equals("Send WhatsApp SOS")) {
                        sendSosMessage();
                    } else if (options[item].equals("Call an Emergency Contact")) {
                        showCallDialog();
                    }
                })
                // Add a distinct "Cancel" button
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // The listener can be null or just dismiss the dialog
                    dialog.dismiss();
                })
                .show();
    }

// In MapsActivity.java

    private void showCallDialog() {
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Deserialize the document into a User object, just like in the WhatsApp method
                User user = documentSnapshot.toObject(User.class);

                // Check if the user object or the contacts list is null/empty
                if (user == null || user.getEmergencyContacts() == null || user.getEmergencyContacts().isEmpty()) {
                    Toast.makeText(this, "No emergency contacts found in your profile.", Toast.LENGTH_LONG).show();
                    return;
                }

                List<EmergencyContact> contacts = user.getEmergencyContacts();

                // Create a list of names and numbers to display in the dialog for consistency
                List<String> contactDisplayNames = new ArrayList<>();
                for (EmergencyContact contact : contacts) {
                    contactDisplayNames.add(contact.getName() + " (" + contact.getNumber() + ")");
                }

                // Use MaterialAlertDialogBuilder for the same modern dialog style
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Choose a contact to call")
                        .setItems(contactDisplayNames.toArray(new CharSequence[0]), (dialog, which) -> {
                            // Get the selected contact's number
                            String numberToCall = contacts.get(which).getNumber();

                            // Create an Intent to open the dialer with the number
                            Intent dialIntent = new Intent(Intent.ACTION_DIAL);
                            dialIntent.setData(Uri.parse("tel:" + numberToCall));
                            startActivity(dialIntent);
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .show();

            } else {
                Toast.makeText(this, "User profile not found.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to load contacts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    // --- SOS SMS Sending Logic ---
// In MapsActivity.java

    private void sendSosMessage() {
        if (lastKnownLocation == null) {
            Toast.makeText(this, "Cannot get current location for SOS message.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Construct the SOS message with the location link.
        String message = "SOS! I need help. My current location is: " +
                "http://maps.google.com/maps?q=" +
                lastKnownLocation.getLatitude() + "," +
                lastKnownLocation.getLongitude();

        // 2. Create an Intent to share plain text.
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, message);
        sendIntent.setType("text/plain");

        // 3. Specifically target WhatsApp to avoid the system's share sheet.
        sendIntent.setPackage("com.whatsapp");

        // 4. Launch the intent and handle the case where WhatsApp isn't installed.
        try {
            startActivity(sendIntent);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "WhatsApp is not installed.", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Helper classes for parsing Places API JSON response ---
    public class PlacesApiResponse {
        public List<PlaceResult> results;
    }
    public class PlaceResult {
        public String name;
        public String vicinity;
        public Geometry geometry;
    }
    public class Geometry {
        public LocationDetails location;
    }
    public class LocationDetails {
        public double lat;
        public double lng;
    }
}