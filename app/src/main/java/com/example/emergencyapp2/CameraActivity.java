package com.example.emergencyapp2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

import android.content.ContentValues;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";
    private PreviewView viewFinder;
    private ImageButton switchCameraButton;
    private ImageCapture imageCapture;

    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private ProcessCameraProvider cameraProvider;

    // Start with the back camera by default
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

    // Handles the result of the permission request
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Camera permission is required to use this feature.", Toast.LENGTH_LONG).show();
                    finish(); // Close the activity if permission is denied
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        viewFinder = findViewById(R.id.viewFinder);
        ImageButton captureButton = findViewById(R.id.captureButton);
        switchCameraButton = findViewById(R.id.switchCameraButton);

        // Check for camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        captureButton.setOnClickListener(v -> takePhoto());

        switchCameraButton.setOnClickListener(v -> {
            // Toggle between front and back cameras
            if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
            } else {
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
            }
            // Re-bind the use cases to the new camera
            startCamera();
        });
    }

    private void takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        if (imageCapture == null) {
            Log.e(TAG, "ImageCapture use case is null. Cannot take photo.");
            return;
        }

        // Create time-stamped name and MediaStore entry.
        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis());

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        // Create output options object which contains file + metadata
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions
                .Builder(getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
                .build();

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputResult) {
                        // Photo saved successfully, now create and launch a share intent
                        Uri savedUri = outputResult.getSavedUri();
                        if (savedUri != null) {
                            // Create the share intent
                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            shareIntent.setType("image/jpeg");
                            shareIntent.putExtra(Intent.EXTRA_STREAM, savedUri);
                            // Grant read permission to the receiving app
                            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            // Create a chooser to show the user a list of apps to share with
                            // and start the activity
                            startActivity(Intent.createChooser(shareIntent, "Share Image Via"));
                        } else {
                            Toast.makeText(getBaseContext(), "Failed to get image URI.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                        Toast.makeText(getBaseContext(), "Photo capture failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                // Unbind all previous use cases before re-binding
                cameraProvider.unbindAll();

                // Build the Preview use case
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                // Build the ImageCapture use case (if you need to take photos)
                imageCapture = new ImageCapture.Builder().build();

                // Bind the use cases to the camera, this will follow the activity's lifecycle
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Use case binding failed", e);
                Toast.makeText(this, "Failed to start camera.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }
}