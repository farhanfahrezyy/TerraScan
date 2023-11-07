package com.example.terrascan.activities;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.example.terrascan.R;
import com.example.terrascan.databinding.ActivityCameraBinding;
import com.example.terrascan.utilities.Constants;
import com.example.terrascan.utilities.MD5Hash;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.common.util.concurrent.ListenableFuture;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CameraActivity extends AppCompatActivity {
    private ActivityCameraBinding binding;
    int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private ImageCapture imageCapture;
    OkHttpClient client = new OkHttpClient();
    private BottomSheetBehavior bottomSheetBehavior;

    private FusedLocationProviderClient fusedLocationProviderClient;
    private final static int REQUEST_CODE = 100;
    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean result) {
            if (result) {
                startCamera(cameraFacing);
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCameraBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(android.Manifest.permission.CAMERA);
        } else {
            startCamera(cameraFacing);
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());

        bottomSheetBehavior = BottomSheetBehavior.from(binding.designBottomSheet);

        setListeners();
    }

    private void setListeners() {
        binding.flipCamera.setOnClickListener(v -> {
            if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
                cameraFacing = CameraSelector.LENS_FACING_FRONT;
            } else {
                cameraFacing = CameraSelector.LENS_FACING_BACK;
            }
            startCamera(cameraFacing);
        });

        binding.galleryButton.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(i);
        });
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == RESULT_OK) {
                    if(result.getData()  != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                            getLastLocation();
                            binding.capturePreview.setImageBitmap(bitmap);
                            Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up);

                            binding.captureBackground.setVisibility(View.VISIBLE);
                            binding.capturePreview.setVisibility(View.VISIBLE);
                            binding.capturePreview.startAnimation(anim);

                            binding.designBottomSheet.startAnimation(anim);
                            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                            bottomSheetBehavior.setHideable(false);
                            bottomSheetBehavior.setPeekHeight(1000);
//                            encodedImage = encodeImage(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
    );

    public void startCamera(int cameraFacing) {
        int aspectRatio = aspectRatio(binding.cameraPreview.getWidth(), binding.cameraPreview.getHeight());
        ListenableFuture<ProcessCameraProvider> listenableFuture = ProcessCameraProvider.getInstance(this);

        listenableFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = (ProcessCameraProvider) listenableFuture.get();

                Preview preview = new Preview.Builder().setTargetAspectRatio(aspectRatio).build();

                imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(cameraFacing).build();

                cameraProvider.unbindAll();

                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

                binding.buttonCamera.setOnClickListener(v -> {
                    takePicture(imageCapture);
                });

                binding.flashButton.setOnClickListener(v -> {
                    setFlashIcon(camera);
                });

                preview.setSurfaceProvider(binding.cameraPreview.getSurfaceProvider());
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    public void takePicture(ImageCapture imageCapture) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        imageCapture.takePicture(new ImageCapture.OutputFileOptions.Builder(outputStream)
                .build(), Executors.newCachedThreadPool(), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {
                byte[] imageBytes = outputStream.toByteArray();

                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                int newDimension = Math.min(width, height);
                Bitmap squareBitmap = Bitmap.createBitmap(bitmap, (width - newDimension) / 2, (height - newDimension) / 2, newDimension, newDimension);

                int previewWidth = 1000;
                int previewHeight = squareBitmap.getHeight() * previewWidth / squareBitmap.getWidth();
                Bitmap croppedBitmap = Bitmap.createScaledBitmap(squareBitmap, previewWidth, previewHeight, false);

                ByteArrayOutputStream compressedOutputStream = new ByteArrayOutputStream();
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, compressedOutputStream);

                runOnUiThread(() -> {
                    getLastLocation();
                    binding.capturePreview.setImageBitmap(bitmap);
                    Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up);

                    binding.captureBackground.setVisibility(View.VISIBLE);
                    binding.capturePreview.setVisibility(View.VISIBLE);
                    binding.capturePreview.startAnimation(anim);

                    binding.designBottomSheet.startAnimation(anim);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    bottomSheetBehavior.setHideable(false);
                    bottomSheetBehavior.setPeekHeight(1000);
                });

            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showToast("Failed to save: " + exception.getMessage());
                    }
                });
                startCamera(cameraFacing);
            }
        });
    }

    private void setFlashIcon(Camera camera) {
        if (camera.getCameraInfo().hasFlashUnit()) {
            if (camera.getCameraInfo().getTorchState().getValue() == 0) {
                camera.getCameraControl().enableTorch(true);
                binding.flashButton.setImageResource(R.drawable.ic_flash_off);
            } else {
                camera.getCameraControl().enableTorch(false);
                binding.flashButton.setImageResource(R.drawable.ic_flash_on);
            }
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showToast("Flash is not available currently");
                }
            });
        }
    }

    private int aspectRatio(int width, int height) {
        double previewRatio = (double) Math.max(width, height) / Math.min(width, height);
        if (Math.abs(previewRatio - 4.0 / 3.0) <= Math.abs(previewRatio - 16.0 / 9.0)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void getLastLocation() {
        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if(location != null) {
                                Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                                try {
                                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                                    binding.inputKota.setText(addresses.get(0).getSubAdminArea());
                                    getWeather(addresses.get(0).getSubAdminArea());
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    });
        } else {
            askPermission();
        }
    }

    private void askPermission() {
        ActivityCompat.requestPermissions(CameraActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        if(requestCode==REQUEST_CODE) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                showToast("Required permission");
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void getWeather(String city) {
        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("api.openweathermap.org")
                .addPathSegment("data")
                .addPathSegment("2.5")
                .addPathSegment("weather")
                .addQueryParameter("q", city)
                .addQueryParameter("appid", "9836446030f7144d1eaf07ed70317df0")
                .build();

        Request request = new Request.Builder()
                .url(httpUrl.toString())
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@androidx.annotation.NonNull Call call, @androidx.annotation.NonNull IOException e) {
                runOnUiThread(() -> {
                    showToast("Failed to get weather information");
                });
            }

            @Override
            public void onResponse(@androidx.annotation.NonNull Call call, @androidx.annotation.NonNull Response response) throws IOException {
                if(response.isSuccessful()){
                    String responseData = response.body().string();
                    if(responseData.equals("null")){
                        runOnUiThread(() -> {
                            showToast("Failed to get weather, please try again");
                        });
                    } else {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseData);
                            JSONObject mainObject = jsonResponse.getJSONObject("main");
                            double temperature = mainObject.getDouble("temp")-273;
                            System.out.println("Temperature: " + temperature);
                            binding.inputSuhu.setText(temperature+"℃");
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else {
                    runOnUiThread(() -> {
                        showToast("Failed to get weather data, please try again");
                    });
                }
            }
        });
    }
}