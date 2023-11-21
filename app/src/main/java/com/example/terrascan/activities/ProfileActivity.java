package com.example.terrascan.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Toast;

import com.example.terrascan.R;
import com.example.terrascan.databinding.ActivityProfileBinding;
import com.example.terrascan.utilities.Constants;
import com.example.terrascan.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(getApplicationContext());
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadUserDetails();
        setListeners();
    }

    private void setListeners() {
        binding.homeButtonp.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), HomeActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK );
            startActivity(i);
            finish();

        });

        binding.logOut.setOnClickListener(v -> logOut());

        binding.editProfile.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), EditProfileActivity.class);
            startActivity(i);
        });

        binding.scanButton.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), CameraActivity.class));
        });

    }

    private void loadUserDetails() {
        binding.valueUsername.setText(preferenceManager.getString(Constants.KEY_USERNAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE_PROFILE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
        binding.valueEmail.setText(preferenceManager.getString(Constants.KEY_EMAIL));
        binding.valuePhoneNumber.setText(preferenceManager.getString(Constants.KEY_PHONE_NUMBER));

    }

    private void logOut() {
        showToast("Signing out...");
        preferenceManager.clear();
        Intent i = new Intent(getApplicationContext(), SignInActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}