package com.example.terrascan.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;

import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
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
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE_PROFILE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
    }

    private void imageSlider() {
        List<SlideModel> slideModels = new ArrayList<>();
        slideModels.add(new SlideModel("https://picsum.photos/id/870/200/300?grayscale&blur=2", null, ScaleTypes.FIT));
        slideModels.add(new SlideModel("https://picsum.photos/200/300/?blur=2", null, ScaleTypes.FIT));
        slideModels.add(new SlideModel("https://picsum.photos/200/300?grayscale", null, ScaleTypes.FIT));

        binding.imageSlider.setImageList(slideModels);
    }

    private void setListeners() {
        binding.profileButton.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), ProfileActivity.class)));
        binding.community.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), CommunityActivity.class)));
    }
}