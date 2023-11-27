package com.example.terrascan.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.example.terrascan.R;
import com.example.terrascan.databinding.ActivityAnalyzeResultBinding;
import com.example.terrascan.utilities.Constants;
import com.example.terrascan.utilities.PreferenceManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AnalyzeResultActivity extends AppCompatActivity {
    private ActivityAnalyzeResultBinding binding;
    private PreferenceManager preferenceManager;
    private BottomSheetBehavior bottomSheetBehavior;
    private OkHttpClient client = new OkHttpClient();
    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    private String encodedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAnalyzeResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());

        bottomSheetBehavior = BottomSheetBehavior.from(binding.designBottomSheet);

        loadData();
        setListeners();
    }

    private void setListeners() {
        binding.saveButton.setOnClickListener(v -> {
            binding.saveButton.setImageResource(R.drawable.ic_bookmark);
            saveAnalysis();
        });

        binding.homeButton.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), HomeActivity.class);
            startActivity(i);
            finish();
        });
    }

    private void loadData() {

        byte[] byteArray = getIntent().getExtras().getByteArray("imageResult");
        encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        binding.capturePreview.setImageBitmap(bitmap);

        binding.soilResult.setText(getIntent().getExtras().getString("soilResult"));

        binding.accurationResult.setText(getIntent().getExtras().getString("confidenceResult"));

        String seasonResult = getIntent().getExtras().getString("seasonResult");
        if(seasonResult.equals("Musim Panas")) {
            binding.climateIcon.setImageResource(R.drawable.ic_summer);
            binding.climateTitle.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.yellow));
            binding.climateResult.setText(seasonResult);
        } else {
            binding.climateIcon.setImageResource(R.drawable.ic_rainy);
            binding.climateTitle.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.blue));
            binding.climateResult.setText(seasonResult);
        }

        String tempResult = getIntent().getExtras().getString("tempResult") + "℃";
        binding.temperatureResult.setText(tempResult);

        binding.recommendationResult.setText(getIntent().getExtras().getString("recommendationResult"));

        Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up);
        binding.capturePreview.setAnimation(anim);
        binding.designBottomSheet.startAnimation(anim);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setPeekHeight(1500);
        bottomSheetBehavior.setMaxHeight(2000);

    }

    private void saveAnalysis() {
        loading(true);
        JSONObject jsonBody;
        try {
            jsonBody = new JSONObject();
            jsonBody.put("userId", preferenceManager.getString(Constants.KEY_USER_ID));
            jsonBody.put("soilImage", encodedImage);
            jsonBody.put("soilType", getIntent().getExtras().getString("soilResult"));
            jsonBody.put("accuracy", getIntent().getExtras().getString("confidenceResult"));
            jsonBody.put("climate", getIntent().getExtras().getString("seasonResult"));
            jsonBody.put("temperature", getIntent().getExtras().getString("tempResult"));
            jsonBody.put("cropRecommendation", getIntent().getExtras().getString("recommendationResult"));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://asia-south1.gcp.data.mongodb-api.com/app/application-0-xighs/endpoint/insertSavedAnalysis")
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        showToast("Success save result");
                        loading(false);
                    });
                } else {
                    runOnUiThread(() -> {
                        showToast("Failed to save result");
                        loading(false);
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call call, IOException e) {
                runOnUiThread(() -> {
                    showToast("Failed to save result");
                    loading(false);
                });
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void loading(Boolean isLoading) {
        if(isLoading) {
            binding.loadingText.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.loadingText.setVisibility(View.INVISIBLE);
        }
    }
}