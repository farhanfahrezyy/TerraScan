package com.example.terrascan.activities;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;

import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.example.terrascan.R;
import com.example.terrascan.databinding.ActivityHomeBinding;
import com.example.terrascan.utilities.Constants;
import com.example.terrascan.utilities.PreferenceManager;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private ActivityHomeBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());
        loadImageProfile();
        imageSlider();
        setListeners();
    }

    private void loadImageProfile() {
        if(preferenceManager.getString(Constants.KEY_IMAGE_PROFILE) != null) {
            byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE_PROFILE), Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            binding.imageProfile.setImageBitmap(bitmap);
        }
    }

    private void imageSlider() {
        List<SlideModel> slideModels = new ArrayList<>();
        slideModels.add(new SlideModel(R.drawable.slider5, null, ScaleTypes.FIT));
        slideModels.add(new SlideModel(R.drawable.slider5, null, ScaleTypes.FIT));
        slideModels.add(new SlideModel(R.drawable.slider5, null,ScaleTypes.FIT));

        binding.imageSlider.setImageList(slideModels);
    }

    private void setListeners() {
        binding.profileButton.setOnClickListener(v -> {
            if(preferenceManager.getString(Constants.KEY_ACCOUNT_TYPE).equals(Constants.KEY_NORMAL_ACCOUNT)) {
                startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
            } else if(preferenceManager.getString(Constants.KEY_ACCOUNT_TYPE).equals(Constants.KEY_SELLER_ACCOUNT)) {
                startActivity(new Intent(getApplicationContext(), ProfileSellerActivity.class));
            } else {
                startActivity(new Intent(getApplicationContext(), ProfileAdminActivity.class));
            }
        });

        binding.scanButton.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), CameraActivity.class));
        });

        binding.community.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), CommunityActivity.class)));

        binding.product.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), ProductActivity.class)));


        binding.saved.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), SavedActivity.class)));

        binding.learn.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), LearnActivity.class)));


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
                System.exit(1);
                finish();
            }
        });
    }
}