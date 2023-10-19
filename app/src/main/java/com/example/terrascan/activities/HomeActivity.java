package com.example.terrascan.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.models.SlideModel;
import com.example.terrascan.databinding.ActivityHomeBinding;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    ActivityHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        imageSlider();
        setListeners();
    }

    private void imageSlider() {
        List<SlideModel> slideModels = new ArrayList<>();
        slideModels.add(new SlideModel("https://picsum.photos/id/870/200/300?grayscale&blur=2", "", ScaleTypes.FIT));
        slideModels.add(new SlideModel("https://picsum.photos/200/300/?blur=2", "", ScaleTypes.FIT));
        slideModels.add(new SlideModel("https://picsum.photos/200/300?grayscale", "", ScaleTypes.FIT));

        binding.imageSlider.setImageList(slideModels);
    }

    private void setListeners() {
        binding.profileButton.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), ProfileActivity.class)));
    }
}