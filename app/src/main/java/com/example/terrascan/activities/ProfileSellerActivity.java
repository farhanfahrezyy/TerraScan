package com.example.terrascan.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.terrascan.databinding.ActivityProfileSellerBinding;

public class ProfileSellerActivity extends AppCompatActivity {

    ActivityProfileSellerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileSellerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}