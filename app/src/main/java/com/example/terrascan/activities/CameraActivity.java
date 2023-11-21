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
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.terrascan.R;
import com.example.terrascan.databinding.ActivityCameraBinding;
import com.example.terrascan.utilities.Constants;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.common.util.concurrent.ListenableFuture;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CameraActivity extends AppCompatActivity {
    private ActivityCameraBinding binding;

    private String seasonParam, temperatureParam, soilResult, confidenceLevel;
    private Bitmap imageParam;
    private byte[] byteArray;
    private int imageSize = 150;

    private Interpreter tflite;

    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private ImageCapture imageCapture;
    OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS) // Adjust the connection timeout as needed
            .readTimeout(30, TimeUnit.SECONDS) // Adjust the read timeout as needed
            .writeTimeout(30, TimeUnit.SECONDS) // Adjust the write timeout as needed
            .build();
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

        try {
            tflite = new Interpreter(loadModelFile());

        } catch (IOException e) {
            e.printStackTrace();
        }

        int inputCount = tflite.getInputTensorCount();
        int outputCount = tflite.getOutputTensorCount();

        System.out.println("Input Count: " + inputCount);
        System.out.println("Output Count: " + outputCount);


        loadSeasonSpinner();
        setListeners();
    }

    private void loadSeasonSpinner() {
        String[] seasons = {"Musim Panas", "Musim Hujan"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, seasons);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.inputIklim.setAdapter(adapter);

        binding.inputIklim.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                seasonParam = (String) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setListeners() {
        binding.closeCamera.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

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

        binding.buttonSubmit.setOnClickListener(v -> {
            binding.loadingBackground.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.loadingText.setVisibility(View.VISIBLE);
            if(imageParam != null && !seasonParam.isEmpty()) {
                classifyImage(imageParam);
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent i = new Intent(getApplicationContext(), HomeActivity.class);
                startActivity(i);
                finish();
            }
        });
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = getAssets().openFd("TerraScanModel.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void classifyImage(Bitmap image) {
        int imageSize = 150;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] intValues = new int[imageSize * imageSize];
        image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
        int pixel = 0;

        for (int i = 0; i < imageSize; i++) {
            for (int j = 0; j < imageSize; j++) {
                int val = intValues[pixel++]; // RGB
                byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255));
                byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255));
                byteBuffer.putFloat((val & 0xFF) * (1.f / 255));
            }
        }

        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, imageSize, imageSize, 3}, DataType.FLOAT32);
        inputFeature0.loadBuffer(byteBuffer);

        tflite.run(inputFeature0.getBuffer(), inputFeature0.getBuffer());

        int numLabels = 2;
        List<Result> labelProbList = new ArrayList<>();
        float[] outputValues = inputFeature0.getFloatArray();

        for (int i = 0; i < numLabels; i++) {
            labelProbList.add(new Result(i, outputValues[i]));
        }

        labelProbList.sort((lhs, rhs) -> Float.compare(rhs.confidence, lhs.confidence));
        printTopLabel(labelProbList.get(0));
    }

    private class Result {
        int label;
        float confidence;

        Result(int label, float confidence) {
            this.label = label;
            this.confidence = confidence;
        }
    }

    private void printTopLabel(Result result) {
        String[] labelNames = {"Tanah Humus", "Tanah Vulkanik"};
        String topLabel = labelNames[result.label];
        float confidence = result.confidence;

        // Konversi float menjadi persen
        NumberFormat percentFormat = NumberFormat.getPercentInstance();
        String confidencePercent = percentFormat.format(confidence);

        soilResult = topLabel;
        confidenceLevel = confidencePercent;
        System.out.println("SoilResult: " + soilResult + ", Confidence: " + confidencePercent);
        getFinalResult();
    }


    private void getFinalResult() {
        JSONObject jsonBody;

        try {
            jsonBody = new JSONObject();
            jsonBody.put("model", "gpt-3.5-turbo");

            JSONArray messageArray = new JSONArray();
            String param = "Tanah: " + soilResult + ", Iklim: " + seasonParam + ", Suhu: " + temperatureParam;
            JSONObject systemObject = new JSONObject();
            systemObject.put("role", "system" );
            systemObject.put("content", "You are a soil expert who has knowledge about soil because you are a professional in that field. You know and can provide recommendations for plants that are suitable for planting in a certain type of soil when someone asks you. With some info provided such as soil type, climate, and average temperature, you can provide plant recommendations based on that information. When answering, please only provide 5 types of plants that are suitable for planting in the ground with this information. Also give a brief explanation of the reason, a maximum of 20 words for each plant. Please note that the plant recommendations you provide must be plants that are suitable for planting in the territory of Indonesia. Then answer the results with Indonesian Language only. Don't answer with English. Are you ready to give accurate plant recommendations? Here is the information:" );
            messageArray.put(systemObject);
            JSONObject userInput = new JSONObject();
            userInput.put("role", "user");
            userInput.put("content", param);
            messageArray.put(userInput);

            jsonBody.put("messages", messageArray);
            jsonBody.put("max_tokens", 500);
            jsonBody.put("top_p", 1);
            jsonBody.put("presence_penalty", 1);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer GANTI_DISINI")
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@androidx.annotation.NonNull Call call, @androidx.annotation.NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    System.out.println("SUKSES");
                    JSONObject jsonObject;
                    try {
                        assert response.body() != null;
                        jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        JSONObject responseMessage = jsonArray.getJSONObject(0).getJSONObject("message");
                        String responseContent = responseMessage.getString("content");

                        runOnUiThread(() -> {
                            Intent i = new Intent(getApplicationContext(), AnalyzeResultActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putByteArray("imageResult", byteArray);
                            bundle.putString("soilResult", soilResult);
                            bundle.putString("seasonResult", seasonParam);
                            bundle.putString("confidenceResult", confidenceLevel);
                            bundle.putString("tempResult", temperatureParam);
                            bundle.putString("recommendationResult", responseContent.trim());

                            i.putExtras(bundle);
                            startActivity(i);
                            finish();
                        });

                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    System.out.println("GAGAL 1");
                    runOnUiThread(() -> {
                        showToast("Gagal melakukan analisis");
                        binding.loadingText.setVisibility(View.GONE);
                        binding.loadingBackground.setVisibility(View.GONE);
                        binding.progressBar.setVisibility(View.GONE);
                    });
                }
            }

            @Override
            public void onFailure(@androidx.annotation.NonNull Call call, IOException e) {
                System.out.println("GAGAL 2");
                e.printStackTrace();
                runOnUiThread(() -> {
                    showToast("Gagal melakukan analisis");
                    binding.loadingText.setVisibility(View.GONE);
                    binding.loadingBackground.setVisibility(View.GONE);
                    binding.progressBar.setVisibility(View.GONE);
                });
            }
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

                            imageParam = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, false);

                            int width = bitmap.getWidth();
                            int height = bitmap.getHeight();
                            int newDimension = Math.min(width, height);
                            Bitmap squareBitmap = Bitmap.createBitmap(bitmap, (width - newDimension) / 2, (height - newDimension) / 2, newDimension, newDimension);

                            int previewWidth = 1000;
                            int previewHeight = squareBitmap.getHeight() * previewWidth / squareBitmap.getWidth();
                            Bitmap croppedBitmap = Bitmap.createScaledBitmap(squareBitmap, previewWidth, previewHeight, false);

                            ByteArrayOutputStream compressedOutputStream = new ByteArrayOutputStream();
                            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, compressedOutputStream);
                            byteArray = compressedOutputStream.toByteArray();

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

                Bitmap image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                int dimension = Math.min(image.getWidth(), image.getHeight());
                image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);

                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                imageParam = image;

                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                int newDimension = Math.min(width, height);
                Bitmap squareBitmap = Bitmap.createBitmap(bitmap, (width - newDimension) / 2, (height - newDimension) / 2, newDimension, newDimension);

                int previewWidth = 1000;
                int previewHeight = squareBitmap.getHeight() * previewWidth / squareBitmap.getWidth();
                Bitmap croppedBitmap = Bitmap.createScaledBitmap(squareBitmap, previewWidth, previewHeight, false);

                ByteArrayOutputStream compressedOutputStream = new ByteArrayOutputStream();
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, compressedOutputStream);
                byteArray = compressedOutputStream.toByteArray();

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
                    bottomSheetBehavior.setPeekHeight(1500);
                    bottomSheetBehavior.setMaxHeight(2000);
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
                                    System.out.println(addresses.get(0).getSubAdminArea());
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
                            double temperatureKelvin = mainObject.getDouble("temp");
                            double temperatureCelcius = temperatureKelvin - 273.15;

                            String formattedTemperature = String.format(Locale.US, "%.1f", temperatureCelcius);
                            binding.inputSuhu.setText(formattedTemperature);
                            temperatureParam = formattedTemperature;
                            System.out.println(formattedTemperature);
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