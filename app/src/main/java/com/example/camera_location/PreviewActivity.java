package com.example.camera_location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class PreviewActivity extends AppCompatActivity {

    private ActivityResultLauncher<String[]> locationPermissionRequest;
    private FusedLocationProviderClient fusedLocationClient;
    private TextView latitudeTextView, longitudeTextView;
    private String latitudeValue, longitudeValue;
    private Database.TaskDBHelper mHelper;
    private PhotoAdapter adapter;
    private RecyclerView recyclerView;
    private Bitmap bitmap;
    private String photoPath;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        ImageView resultPreview = findViewById(R.id.resultPreview);
        Button btnSave = findViewById(R.id.btnSave);
        Button btnCancel = findViewById(R.id.btnCancel);
        latitudeTextView = findViewById(R.id.latitudeTextView);
        longitudeTextView = findViewById(R.id.longitudeTextView);

        Intent intent = getIntent();
        photoPath = intent.getStringExtra("photoPath");
        bitmap = BitmapFactory.decodeFile(photoPath);
        resultPreview.setImageBitmap(bitmap);

        mHelper = new Database.TaskDBHelper(this);

        locationPermissionRequest = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    Boolean fineLocationGranted = result.getOrDefault(
                            android.Manifest.permission.ACCESS_FINE_LOCATION, false);
                    Boolean coarseLocationGranted = result.getOrDefault(
                            Manifest.permission.ACCESS_COARSE_LOCATION, false);

                    if (fineLocationGranted != null && fineLocationGranted) {
                        Log.d("Main", "Fine Location Granted");
                        getLastLocation();
                    } else if (coarseLocationGranted != null && coarseLocationGranted) {
                        Log.d("Main", "Coarse Location Granted");
                        getLastLocation();
                    } else {
                        Log.d("Main", "Location Permission not Allowed");
                    }
                }
        );

        launchLocationPermission();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnSave.setOnClickListener(v -> savePhoto());

        btnCancel.setOnClickListener(v -> cancelPhoto());
    }

    private void launchLocationPermission() {
        locationPermissionRequest.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            launchLocationPermission();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        latitudeValue = String.valueOf(location.getLatitude());
                        latitudeTextView.setText(latitudeValue);
                        longitudeValue = String.valueOf(location.getLongitude());
                        longitudeTextView.setText(longitudeValue);
                    }
                });
    }

    private void savePhoto() {
        File dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        File albumDir = new File(dcimDir, "Camera-Location");

        if (!albumDir.exists()) {
            albumDir.mkdirs();
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String photoFilePath = albumDir.getAbsolutePath() + "/" + timestamp + ".jpg";
        File photoFile = new File(photoFilePath);

        try (FileOutputStream out = new FileOutputStream(photoFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out);
            Toast.makeText(this, "Photo has been saved successfully", Toast.LENGTH_SHORT).show();

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, timestamp);
            values.put(MediaStore.Images.Media.DISPLAY_NAME, timestamp + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
            values.put(MediaStore.Images.Media.DATA, photoFile.getAbsolutePath());
            getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            MediaScannerConnection.scanFile(this,
                    new String[]{photoFile.getAbsolutePath()},
                    null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            SQLiteDatabase db = mHelper.getWritableDatabase();
                            ContentValues values = new ContentValues();
                            values.put(Database.TaskContract.TaskEntry.COL_IMAGE_PATH, path);
                            values.put(Database.TaskContract.TaskEntry.COL_LATITUDE, latitudeValue);
                            values.put(Database.TaskContract.TaskEntry.COL_LONGITUDE, longitudeValue);
                            db.insertWithOnConflict(Database.TaskContract.TaskEntry.TABLE,
                                    null,
                                    values,
                                    SQLiteDatabase.CONFLICT_REPLACE);
                            PhotoAdapter.updateUI(this, adapter, recyclerView);
                            db.close();
                        }
                    });


        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        finish();
    }

    private void cancelPhoto() {
        File file = new File(photoPath);
        if (file.exists()) {
            file.delete();
        }
        finish();
    }
}
