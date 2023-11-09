package com.example.terrascan.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.terrascan.databinding.ActivityRequestChangeProductBinding;

public class RequestChangeProductActivity extends AppCompatActivity {

    ActivityRequestChangeProductBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRequestChangeProductBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());



    }
}