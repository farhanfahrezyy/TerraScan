package com.example.terrascan.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.terrascan.databinding.ActivitySignInBinding;
import com.example.terrascan.utilities.Constants;
import com.example.terrascan.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.atomic.AtomicBoolean;

public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;
    private AtomicBoolean isPasswordVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        if(preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
            startActivity(intent);
            finish();
        }
        isPasswordVisible = new AtomicBoolean(false);

        setListeners();
    }

    private void setListeners() {
        binding.togglePassword.setOnClickListener(v -> togglePassword());

        binding.layoutSignUp.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), SignUpActivity.class)));

        binding.buttonLogin.setOnClickListener(v -> {
            if(isValidSignInDetails()) {
                signIn();
            }
        });

        binding.forgotPassword.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), ForgetPasswordActivity.class)));
    }

    private void togglePassword() {
        isPasswordVisible.set(!isPasswordVisible.get());
        if (isPasswordVisible.get()) {
            binding.inputPassword.setInputType(InputType.TYPE_CLASS_TEXT);
        } else {
            binding.inputPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
    }

    private void signIn() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, binding.inputEmail.getText().toString())
                .whereEqualTo(Constants.KEY_PASSWORD, binding.inputPassword.getText().toString())
                .get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful() && task.getResult() != null
                            && task.getResult().getDocuments().size() > 0) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                        preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
                        preferenceManager.putString(Constants.KEY_ACCOUNT_TYPE, documentSnapshot.getString(Constants.KEY_ACCOUNT_TYPE));
                        preferenceManager.putString(Constants.KEY_USERNAME, documentSnapshot.getString(Constants.KEY_USERNAME));
                        preferenceManager.putString(Constants.KEY_IMAGE_PROFILE, documentSnapshot.getString(Constants.KEY_IMAGE_PROFILE));
                        preferenceManager.putString(Constants.KEY_EMAIL, binding.inputEmail.getText().toString());
                        preferenceManager.putString(Constants.KEY_PHONE_NUMBER, documentSnapshot.getString(Constants.KEY_PHONE_NUMBER));
                        Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } else {
                        loading(false);
                        showToast("Unable to sign in");
                    }
                });
    }

    private void loading(Boolean isLoading) {
        if(isLoading) {
            binding.buttonLogin.setVisibility(View.INVISIBLE);
            binding.progessBar.setVisibility(View.VISIBLE);
        } else {
            binding.progessBar.setVisibility(View.INVISIBLE);
            binding.buttonLogin.setVisibility(View.VISIBLE);
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private Boolean isValidSignInDetails() {
        if(binding.inputEmail.toString().trim().isEmpty()) {
            showToast("Enter email");
            return false;
        } else if(!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()) {
            showToast("Enter valid email");
            return false;
        } else if(binding.inputPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter password");
            return false;
        } else {
            return true;
        }
    }
}