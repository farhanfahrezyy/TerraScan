package com.example.terrascan.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.terrascan.databinding.ActivityProfileAdminBinding;

public class ProfileAdminActivity extends AppCompatActivity {
    ActivityProfileAdminBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileAdminBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}