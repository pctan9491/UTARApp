package com.example.utarapp;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.room.Room;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;


public class ScanAttendance extends Fragment {
    private static final int CAMERA_PERMISSION_REQUEST = 200;
    private SurfaceView cameraPreview;
    private BarcodeDetector barcodeDetector;
    private CameraSource cameraSource;
    private boolean isScanProcessed = false;
    private FirebaseFirestore db;
    private long lastScanTimestamp = -1;
    private static final long SCAN_COOLDOWN_MILLIS = 3000; // Cooldown period of 30 seconds.
    private boolean isAuthenticated = false;
    private static final int LOCATION_PERMISSION_REQUEST = 2;
    private FusedLocationProviderClient fusedLocationClient;
    private QRCodeData currQRData;
    private ConnectivityManager.NetworkCallback networkCallback;
    private ConnectivityManager connectivityManager;
    private float maxZoom = 10.0f; // Example max zoom level, adjust based on testing
    private float currentZoomRatio = 1f; // Start with 1x zoom
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CameraManager cameraManager;




    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.activity_scan_attendance, container, false);
        FirebaseApp.initializeApp(requireActivity());
        cameraPreview = view.findViewById(R.id.camera_preview);
        initializeCameraSource(); // Assume this method initializes barcodeDetector and cameraSource
        //Navigation Bar Top
        FrameLayout navigationCont = view.findViewById(R.id.navigation_container);
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(navigationCont.getId(), new AttendanceTopBar());
        fragmentTransaction.commit();


        //TODO: Location Tracking and Biometric Authentication

        cameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);

        cameraPreview.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                // Check if biometric authentication is necessary and supported, then authenticate
                BiometricManager biometricManager = BiometricManager.from(requireContext());
                if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
                    authenticateUser(); // This will eventually call startCameraSource() upon successful authentication
                } else {
                    // If no biometric authentication needed, move directly to location permission check
                    checkAndRequestLocationAndCameraPermissions();

                }
            }


            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                // Handle any changes here if necessary
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (cameraSource != null) {
                    cameraSource.stop();
                }
            }
        });

        cameraPreview.setOnTouchListener(new View.OnTouchListener() {
            private ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(getActivity(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    float scale = cameraPreview.getScaleX() * detector.getScaleFactor();
                    scale = Math.max(0.1f, Math.min(scale, 10.0f)); // Limit scale
                    cameraPreview.setScaleX(scale);
                    cameraPreview.setScaleY(scale);
                    return true;
                }
            });

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleGestureDetector.onTouchEvent(event);
                return true;
            }
        });



        //Check for internet
        connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                // Network becomes available
                getActivity().runOnUiThread(() -> {
                    if (isNetworkAvailable(getContext())) {
                        Log.d("NetworkCallback", "Network Available: Synchronizing...");
                        synchronizeLocalDatabaseWithFirestore(getContext());
                    }
                });
            }

            @Override
            public void onLost(Network network) {
                // Network is lost
                Log.d("NetworkCallback", "Network Lost");
            }
        };

        return view;
    }

    private void updateZoom(float zoomRatio) {
        if (cameraDevice == null) return; // Safety check

        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraDevice.getId());
            Rect zoomRect = calculateZoomRect(zoomRatio, characteristics);
            CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);
            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Rect calculateZoomRect(float zoomRatio, CameraCharacteristics characteristics) {
        Rect activeArraySize = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        int cropWidth = (int) (activeArraySize.width() / zoomRatio);
        int cropHeight = (int) (activeArraySize.height() / zoomRatio);
        int cropX = (activeArraySize.width() - cropWidth) / 2;
        int cropY = (activeArraySize.height() - cropHeight) / 2;

        return new Rect(cropX, cropY, cropX + cropWidth, cropY + cropHeight);
    }


    private void initializeCameraSource() {
        Context context = getContext();
        if (context == null) return;
        barcodeDetector = new BarcodeDetector.Builder(getContext())
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        cameraSource = new CameraSource.Builder(getContext(), barcodeDetector)
                .setRequestedPreviewSize(640, 480)
                .setAutoFocusEnabled(true)
                .build();

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                SparseArray<Barcode> qrCodes = detections.getDetectedItems();
                if (qrCodes.size() != 0) {
                        getActivity().runOnUiThread(() -> {
                            if (scanCoolDownEnd()) {
                                String qrCodeContent = qrCodes.valueAt(0).displayValue;
                                // Parse the QR code content into an object
                                Gson gson = new Gson();
                                Log.d("QRCodeContent", "QR Code Content: " + qrCodeContent);
                                try {
                                    QRCodeData qrData = gson.fromJson(qrCodeContent, QRCodeData.class);
                                    // Now check the status in Firestore before continuing
                                    checkClassStatus(qrData, qrCodeContent);
                                    lastScanTimestamp = System.currentTimeMillis();
                                    isScanProcessed = true;
                                } catch (JsonSyntaxException e) {
                                    Log.e("ScanAttendance", "Error parsing QR code: " + qrCodeContent, e);
                                    Toast.makeText(getActivity(), "Invalid QR code format.", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                            }
                        });
                }
            }

            private boolean scanCoolDownEnd() {
                // If lastScanTimestamp is -1, it means we've never scanned before
                if (lastScanTimestamp == -1 || (System.currentTimeMillis() - lastScanTimestamp) > SCAN_COOLDOWN_MILLIS) {
                    return true;
                } else {
                    Log.d("ScanAttendance", "Scan attempt ignored due to cooldown.");
                    return false;
                }
            }
            private void checkClassStatus(QRCodeData qrData, String qrCodeContent) {
                db = FirebaseFirestore.getInstance();
                db.collection("qr_class_info")
                        .whereEqualTo("subject", qrData.getSubject())
                        .whereEqualTo("classType", qrData.getClassType())
                        .whereEqualTo("startTime", qrData.getStartTime())
                        .whereEqualTo("venue", qrData.getVenue())
                        .whereEqualTo("date", qrData.getDate())
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (queryDocumentSnapshots.isEmpty()) {
                                Toast.makeText(getActivity(), "Error: Class information not found.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            DocumentSnapshot classInfoDoc = queryDocumentSnapshots.getDocuments().get(0);
                            String status = classInfoDoc.getString("status");
                            if ("deactivate".equals(status)) {
                                Toast.makeText(getActivity(), "Error: Class is deactivated and cannot be scanned.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            checkStudentRegistration(qrData, qrCodeContent);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getActivity(), "Error checking class status.", Toast.LENGTH_SHORT).show();
                            Log.e("ScanAttendance", "Error checking class status", e);
                        });
            }
            private void checkStudentRegistration(QRCodeData qrData, String qrCodeContent) {
                // Assuming QRCodeData has a method getStudId() to get student ID. Adjust as needed.
                SharedPreferences sharedPreferences = getActivity().getSharedPreferences("LoginPrefs", MODE_PRIVATE);
                String studId = sharedPreferences.getString("LoginId", null);

                // First, check the regular timetable collection
                db.collection("timetable")
                        .whereArrayContains("studRegister", studId)
                        .whereEqualTo("course", qrData.getSubject())
                        .whereEqualTo("venue", qrData.getVenue())
                        .whereEqualTo("startTime", qrData.getStartTime())
                        .whereEqualTo("classType", qrData.getClassType())
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (!querySnapshot.isEmpty()) {
                                // Student is registered, and class is active. Proceed with scanning.
                                getLocationAndUpdateAttendance(qrCodeContent, qrData);
                            } else {
                                // Check the replacement collection if no match found in the timetable collection
                                checkReplacementClass(qrData, qrCodeContent, studId);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getActivity(), "Error checking student registration.", Toast.LENGTH_SHORT).show();
                            Log.e("ScanAttendance", "Error checking student registration", e);
                        });
            }

            private void checkReplacementClass(QRCodeData qrData, String qrCodeContent, String studId) {
                db.collection("replacement")
                        .whereEqualTo("courseCode", qrData.getSubject())
                        .whereEqualTo("venue", qrData.getVenue())
                        .whereEqualTo("startTime", qrData.getStartTime())
                        .whereEqualTo("date", qrData.getDate()) // Assuming QRCodeData has a getDate() method
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (!querySnapshot.isEmpty()) {
                                // Found a matching replacement class
                                QueryDocumentSnapshot replacementDoc = (QueryDocumentSnapshot) querySnapshot.getDocuments().get(0);
                                String timetableId = replacementDoc.getString("timetableId");

                                // Check if the student is registered for the replacement class
                                db.collection("timetable")
                                        .document(timetableId)
                                        .get()
                                        .addOnSuccessListener(timetableDoc -> {
                                            if (timetableDoc.exists()) {
                                                List<String> studRegister = (List<String>) timetableDoc.get("studRegister");
                                                if (studRegister.contains(studId)) {
                                                    // Student is registered for the replacement class
                                                    getLocationAndUpdateAttendance(qrCodeContent, qrData);
                                                } else {
                                                    Toast.makeText(getActivity(), "Error: Student not registered for this class.", Toast.LENGTH_SHORT).show();
                                                    Log.d("ScanAttendance", "StudId: " + studId);
                                                }
                                            } else {
                                                Toast.makeText(getActivity(), "Error: Student not registered for this class.", Toast.LENGTH_SHORT).show();
                                                Log.d("ScanAttendance", "StudId: " + studId);
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(getActivity(), "Error checking student registration.", Toast.LENGTH_SHORT).show();
                                            Log.e("ScanAttendance", "Error checking student registration", e);
                                        });
                            } else {
                                // No matching replacement class found
                                Toast.makeText(getActivity(), "Error: Student not registered for this class.", Toast.LENGTH_SHORT).show();
                                Log.d("ScanAttendance", "StudId: " + studId);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getActivity(), "Error checking student registration.", Toast.LENGTH_SHORT).show();
                            Log.e("ScanAttendance", "Error checking student registration", e);
                        });
            }
            private void getLocationAndUpdateAttendance(String qrCodeContent, QRCodeData qrData) {
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

                if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // Permissions are not granted, request for permissions
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
                    return;
                }

                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(location -> {
                            if (location != null) {
                                // Use the location object here to get latitude and longitude
                                double latitude = location.getLatitude();
                                double longitude = location.getLongitude();

                                // Reverse geocoding to get actual address
                                Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                                try {
                                    List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                                    if (addresses != null && !addresses.isEmpty()) {
                                        Address address = addresses.get(0);
                                        // Format the first line of address (if available), city, and country name.
                                        String addressText = address.getAddressLine(0) + ", " + address.getLocality() + ", " + address.getCountryName();
                                        Log.d("LocationUpdate", "Address: " + addressText);
                                        Log.d("LocationUpdate", "Lat: " + latitude + ", Lon: " + longitude);
                                    } else {
                                        Log.d("LocationUpdate", "No address found");
                                    }
                                } catch (IOException e) {
                                    Log.e("LocationError", "Service not available", e);
                                }
                                // Successfully retrieved location, now proceed with scanning and record the attendance along with the location
                                proceedWithScanning(qrCodeContent, qrData, latitude, longitude);
                            } else {
                                // Handle case where location is null, could try requesting updates or show an error
                                Toast.makeText(getContext(), "Location not available", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            // Handle any error in getting location
                            Log.e("LocationError", "Failed to get location", e);
                            Toast.makeText(getContext(), "Failed to get location", Toast.LENGTH_SHORT).show();
                        });
            }

            private String getUniqueDeviceID() {
                SharedPreferences sharedPreferences = getActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
                String uniqueID = sharedPreferences.getString("UniqueDeviceID", null);
                if (uniqueID == null) {
                    uniqueID = UUID.randomUUID().toString();
                    sharedPreferences.edit().putString("UniqueDeviceID", uniqueID).apply();
                }
                return uniqueID;
            }

            private void proceedWithScanning(String qrCodeContent, QRCodeData qrData, double studLatitude, double studLongitude) {
                SharedPreferences sharedPreferences = getActivity().getSharedPreferences("LoginPrefs", MODE_PRIVATE);
                String studId = sharedPreferences.getString("LoginId", null);
                String deviceID = getUniqueDeviceID(); // Retrieve the unique device ID.
                String scanTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                // Check network availability
                if (!isNetworkAvailable(getActivity())) {
                    currQRData = qrData;
                    // Network is not available, save the record locally
                    AttendanceRecordDatabase record = new AttendanceRecordDatabase();
                    record.studentId = studId;
                    record.scanTime = scanTime;
                    record.latitude = studLatitude;
                    record.longitude = studLongitude;

                    // Since Room operations must be done on a background thread
                    new Thread(() -> {
                        AppDatabase localDb = Room.databaseBuilder(getActivity().getApplicationContext(),
                                AppDatabase.class, "attendance-database").allowMainThreadQueries().build();
                        localDb.attendanceRecordDao().insert(record);
                        getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Attendance saved locally due to no network.", Toast.LENGTH_LONG).show());
                    }).start();

                    return; // Stop further execution to avoid proceeding with Firestore update
                }

                // Create a new record for the scan.
                Map<String, Object> scanRecord = new HashMap<>();
                scanRecord.put("studId", studId);
                scanRecord.put("scanTime", scanTime);
                scanRecord.put("latitude", studLatitude);
                scanRecord.put("longitude", studLongitude);
                scanRecord.put("deviceID", getUniqueDeviceID());

                // Use the same query to find the specific class info document
                db.collection("qr_class_info")
                        .whereEqualTo("subject", qrData.getSubject())
                        .whereEqualTo("classType", qrData.getClassType())
                        .whereEqualTo("startTime", qrData.getStartTime())
                        .whereEqualTo("venue", qrData.getVenue())
                        .whereEqualTo("date", qrData.getDate())
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (querySnapshot.isEmpty()) {
                                Toast.makeText(getActivity(), "Class information not found.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                                // Assuming that there's only one document that matches the query
                                DocumentSnapshot classInfoDoc = querySnapshot.getDocuments().get(0);
                            String trackingStatus = classInfoDoc.getString("trackingStatus");
                            // Assuming 'location' is an array of Number where index 0 is latitude and index 1 is longitude
                            Map<String, Object> location = (Map<String, Object>) classInfoDoc.get("location");
                            double classLatitude = 0.0;
                            double classLongitude = 0.0;
                            if (location != null) {
                                Number lat = (Number) location.get("latitude");
                                Number lng = (Number) location.get("longitude");
                                if (lat != null && lng != null) {
                                    classLatitude = lat.doubleValue();
                                    classLongitude = lng.doubleValue();
                                }
                            }

                            Log.d("Debug", "Class location - Latitude: " + classLatitude + ", Longitude: " + classLongitude);
                            String classType = qrData.getClassType();

                            // Proceed only if tracking is active
                            if ("deactivate".equals(trackingStatus)) {
                                Log.d("QRScan", "Tracking not activated, proceeding with scan.");
                                Toast.makeText(getActivity(), "Tracking not activated, proceeding with scan.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                                String message;
                                double distance = calculateDistance(studLatitude, studLongitude, classLatitude, classLongitude);

                                // Distance threshold based on class type
                                long maxDistance = "Lecture".equals(classType) ? 600 : 200;

                                if (distance > maxDistance) {
                                    message = String.format(Locale.getDefault(), "Failed: Too far from class location to scan QR. Distance: %.2f m", distance);
                                    Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                                    Log.d("QRScan", message);
                                    return; // Stop further execution if too far
                                } else {
                                    message = String.format(Locale.getDefault(), "Success: QR scanned. Distance: %.2f m", distance);
                                    Log.d("QRScan", message);
                                }
                            List<Map<String, Object>> attendanceRecords = (List<Map<String, Object>>) classInfoDoc.get("attendanceRecords");
                            if (attendanceRecords == null) {
                                attendanceRecords = new ArrayList<>();
                            }

                            // Check if the device ID exists in the attendanceRecords
                            boolean deviceIdExists = false;
                            for (Map<String, Object> record : attendanceRecords) {
                                if (deviceID.equals(record.get("deviceID"))) {
                                    deviceIdExists = true;
                                    break;
                                }
                            }

                            if (deviceIdExists) {
                                // Device ID already exists, show an error message or take appropriate action
                                Toast.makeText(getActivity(), "Same devices detected.", Toast.LENGTH_SHORT).show();
                                return; // Stop further execution
                            }

                            // Check if the student has already scanned
                            Map<String, Object> existingRecord = null;
                            for (Map<String, Object> record : attendanceRecords) {
                                if (studId.equals(record.get("studId"))) {
                                    existingRecord = record;
                                    break;
                                }
                            }
                            if (existingRecord != null) {
                                // Update existing record's scan time
                                existingRecord.put("scanTime", scanTime);
                                existingRecord.put("latitude", studLatitude);
                                existingRecord.put("longitude", studLongitude);
                                existingRecord.put("deviceID", deviceID);
                            } else {
                                // Add new attendance record
                                Map<String, Object> newRecord = new HashMap<>();
                                newRecord.put("studId", studId);
                                newRecord.put("scanTime", scanTime);
                                newRecord.put("latitude", studLatitude);
                                newRecord.put("longitude", studLongitude);
                                newRecord.put("deviceID", deviceID);
                                attendanceRecords.add(newRecord);
                            }
                                // Update the document with the new scan record
                                db.collection("qr_class_info").document(classInfoDoc.getId())
                                        .update("attendanceRecords", attendanceRecords)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(getActivity(), "Scanned QR code: " + qrCodeContent, Toast.LENGTH_SHORT).show();
                                            // Navigate to the attendance scan status activity.
                                            Intent attendanceScanStatusIntent = new Intent(getActivity(), attendanceScanStatus.class);
                                            attendanceScanStatusIntent.putExtra("QR_CODE_DATA", qrData);
                                            startActivity(attendanceScanStatusIntent);
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(getActivity(), "Failed to record attendance.", Toast.LENGTH_SHORT).show();
                                            Log.e("ScanAttendance", "Failed to record attendance", e);
                                        });
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getActivity(), "Error finding class information.", Toast.LENGTH_SHORT).show();
                            Log.e("ScanAttendance", "Error finding class information", e);
                        });

                isScanProcessed = true;
            }
        });
    }

    private void authenticateUser() {
        Executor executor = ContextCompat.getMainExecutor(getContext());
        BiometricPrompt biometricPrompt = new BiometricPrompt(ScanAttendance.this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                    // Do not start the camera source here
                    isAuthenticated = false; // Reset or ensure authentication flag is false
                });
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Authentication succeeded!", Toast.LENGTH_SHORT).show();
                    // Proceed with any actions you need to take after successful authentication here
                    isAuthenticated = true;

                    checkAndRequestLocationAndCameraPermissions();
                });
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show();
                    isAuthenticated = false; // Reset or ensure authentication flag is false
                });
            }
        });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Authentication")
                .setSubtitle("Log in using your biometric credential")
                .setNegativeButtonText("Cancel")
                .build();

        biometricPrompt.authenticate(promptInfo);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        Log.d("Debug", "calculateDistance inputs - lat1: " + lat1 + ", lon1: " + lon1 + ", lat2: " + lat2 + ", lon2: " + lon2);
        final int R = 6371; // Radius of the earth in kilometers
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        return distance;
    }
    @Override
    public void onResume() {
        super.onResume();
        // Reset isScanProcessed to allow for new scans on return
        isScanProcessed = false;
        // Start the camera source
        if (cameraPreview.getHolder().getSurface().isValid()) {
            startCameraSource();
        }
        // Check network availability and synchronize pending records if connected
        NetworkRequest request = new NetworkRequest.Builder().build();
        connectivityManager.registerNetworkCallback(request, networkCallback);
    }
    private void synchronizeLocalDatabaseWithFirestore(Context context) {
        // Execute on a separate thread to avoid blocking the UI thread
        new Thread(() -> {
            AppDatabase localDb = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "attendance-database").allowMainThreadQueries().build();
            List<AttendanceRecordDatabase> records = localDb.attendanceRecordDao().getAll();

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            if (currQRData == null) {
                // Log an error or handle the case where currQRData is not available
                return;
            }
            Log.e("NetworkAvailableAndSyncFunction", "Network Available And Sync Function");

            // Correctly execute the Firestore query and handle its results
            db.collection("qr_class_info")
                    .whereEqualTo("subject", currQRData.getSubject())
                    .whereEqualTo("classType", currQRData.getClassType())
                    .whereEqualTo("startTime", currQRData.getStartTime())
                    .whereEqualTo("venue", currQRData.getVenue())
                    .whereEqualTo("date", currQRData.getDate())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful() || task.getResult().isEmpty()) {
                            Log.e("SyncDB", "Error querying documents: ", task.getException());
                        }

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String documentId = document.getId();
                            List<Map<String, Object>> attendanceRecords = (List<Map<String, Object>>) document.get("attendanceRecords");
                            if (attendanceRecords == null) {
                                attendanceRecords = new ArrayList<>();
                            }

                            for (AttendanceRecordDatabase localRecord : records) {
                                // Location check
                                String classType = currQRData.getClassType();
                                Map<String, Object> location = (Map<String, Object>) document.get("location");
                                double classLatitude = location != null ? ((Number) location.get("latitude")).doubleValue() : 0;
                                double classLongitude = location != null ? ((Number) location.get("longitude")).doubleValue() : 0;
                                double distance = calculateDistance(localRecord.latitude, localRecord.longitude, classLatitude, classLongitude);
                                long maxDistance = "Lecture".equals(classType) ? 600 : 200;

                                if (distance <= maxDistance) {
                                    boolean exists = false;
                                    for (Map<String, Object> attendanceRecord : attendanceRecords) {
                                        if (attendanceRecord.get("studId").equals(localRecord.studentId)) {
                                            // Update existing record
                                            attendanceRecord.put("scanTime", localRecord.scanTime);
                                            attendanceRecord.put("latitude", localRecord.latitude);
                                            attendanceRecord.put("longitude", localRecord.longitude);
                                            exists = true;
                                            break;
                                        }
                                    }
                                    if (!exists) {
                                        // Add new record
                                        Map<String, Object> newRecord = new HashMap<>();
                                        newRecord.put("studId", localRecord.studentId);
                                        newRecord.put("scanTime", localRecord.scanTime);
                                        newRecord.put("latitude", localRecord.latitude);
                                        newRecord.put("longitude", localRecord.longitude);
                                        attendanceRecords.add(newRecord);
                                    }
                                } else {
                                    // Log and skip if too far
                                    String message = String.format(Locale.getDefault(), "Failed: Too far from class location to scan QR. Distance: %.2f m", distance);
                                    getActivity().runOnUiThread(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
                                    Log.d("SyncDB", "Skipping update due to distance constraint for studId: " + localRecord.studentId);
                                    continue; // Skip this loop iteration
                                }
                            }

                            // Update Firestore document
                            db.collection("qr_class_info").document(documentId)
                                    .update("attendanceRecords", attendanceRecords)
                                    .addOnSuccessListener(aVoid -> {
                                        // Delete the records from local database after successful upload
                                        for (AttendanceRecordDatabase localRecord : records) {
                                            localDb.attendanceRecordDao().delete(localRecord);
                                        }
                                        String successMessage = "Attendance successfully recorded.";
                                        getActivity().runOnUiThread(() -> Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show());
                                    })
                                    .addOnFailureListener(e -> Log.e("SyncDB", "Error updating Firestore with local record", e));
                        }
                    });

            // Reset currQRData after processing
        }).start();
    }

    private void startCameraSource() {
        if (!isAuthenticated) {
            Toast.makeText(getContext(), "Please authenticate before scanning.", Toast.LENGTH_SHORT).show();
            return; // Do not proceed with starting the camera
        }
        isAuthenticated = false; // Reset or ensure authentication flag is false
        // Check for camera permissions
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                if (cameraSource != null) {
                    cameraSource.start(cameraPreview.getHolder());
                }
            } catch (IOException e) {
                Log.e("ScanAttendance", "Unable to start camera source.", e);
                // Handle the exception properly here
                Toast.makeText(getContext(), "Unable to start camera source.", Toast.LENGTH_SHORT).show();
            }
        } else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        }
    }
    private void checkAndRequestLocationAndCameraPermissions() {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        } else if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            // Permissions are already granted, proceed to fetch location
            isAuthenticated= true;
            startCameraSource();
        }
    }

    public boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraSource != null) {
            cameraSource.stop();
        }
        // Unregister network callback
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraSource != null) {
            cameraSource.release();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Assuming camera permission is handled first and independent of location
                // You might adjust this if camera start depends on location permission
                checkAndRequestLocationAndCameraPermissions(); // Check for location permission after camera permission is granted
            } else {
                Toast.makeText(getContext(), "Camera permission is required to use the QR scanner", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //getLocationAndUpdateAttendance(tempQRCodeContent, tempQRData); // Once location is granted, fetch location and then it should lead to starting the camera
                checkAndRequestLocationAndCameraPermissions();
            } else {
                Toast.makeText(getContext(), "Location permission is required for attendance tracking", Toast.LENGTH_SHORT).show();
                isAuthenticated = false;
                // Consider whether you want to start the camera here even if location permission is denied
                // If so, uncomment the following line
                // startCameraSource();
            }
        }
    }

}