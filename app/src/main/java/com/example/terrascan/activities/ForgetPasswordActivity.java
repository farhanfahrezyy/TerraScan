package com.example.terrascan.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.InputType;

import com.example.terrascan.databinding.ActivityForgetPasswordBinding;

import java.util.concurrent.atomic.AtomicBoolean;

public class ForgetPasswordActivity extends AppCompatActivity {
    private ActivityForgetPasswordBinding binding;
    private AtomicBoolean isPasswordVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        isPasswordVisible = new AtomicBoolean(false);
        setListeners();
    }

    private void setListeners() {
        binding.layoutSignIn.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        binding.toggleNewPassword.setOnClickListener(v -> toggleNewPassword());

        binding.toggleConfirmPassword.setOnClickListener(v -> toggleConfirmPassword());
    }

    private void toggleNewPassword() {
        isPasswordVisible.set(!isPasswordVisible.get());
        if (isPasswordVisible.get()) {
            binding.inputNewPassword.setInputType(InputType.TYPE_CLASS_TEXT);
        } else {
            binding.inputNewPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
    }

    private void toggleConfirmPassword() {
        isPasswordVisible.set(!isPasswordVisible.get());
        if (isPasswordVisible.get()) {
            binding.inputConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT);
        } else {
            binding.inputConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
    }
}