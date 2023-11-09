package com.example.terrascan.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.example.terrascan.R;
import com.example.terrascan.databinding.ActivityAnalyzeResultBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

public class AnalyzeResultActivity extends AppCompatActivity {
    private ActivityAnalyzeResultBinding binding;
    private BottomSheetBehavior bottomSheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAnalyzeResultBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bottomSheetBehavior = BottomSheetBehavior.from(binding.designBottomSheet);

        loadData();
    }

    private void loadData() {

        byte[] byteArray = getIntent().getExtras().getByteArray("imageResult");
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

        binding.temperatureResult.setText(getIntent().getExtras().getString("tempResult"));

        binding.recommendationResult.setText(getIntent().getExtras().getString("recommendationResult"));

        Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_up);
        binding.capturePreview.setAnimation(anim);
        binding.designBottomSheet.startAnimation(anim);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        bottomSheetBehavior.setHideable(false);
        bottomSheetBehavior.setPeekHeight(1500);
        bottomSheetBehavior.setMaxHeight(2000);

    }
}