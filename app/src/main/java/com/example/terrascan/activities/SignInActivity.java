package com.example.terrascan.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.terrascan.databinding.ActivitySignInBinding;

public class SignInActivity extends AppCompatActivity {

    ActivitySignInBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setListeners();
    }

    private void setListeners() {
        binding.buttonLogin.setOnClickListener(v -> {
            if(isValidSignIn()) {
                signIn();
            }
        });
    }

    private void signIn() {

    }

    private boolean isValidSignIn() {
        return true;
    }
}