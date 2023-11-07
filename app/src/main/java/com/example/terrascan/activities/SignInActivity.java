package com.example.terrascan.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.terrascan.R;
import com.example.terrascan.databinding.ActivitySignInBinding;
import com.example.terrascan.utilities.Constants;
import com.example.terrascan.utilities.MD5Hash;
import com.example.terrascan.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;
    private AtomicBoolean isPasswordVisible;
    OkHttpClient client = new OkHttpClient();

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
            binding.togglePassword.setImageResource(R.drawable.ic_eye_off);
        } else {
            binding.inputPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            binding.togglePassword.setImageResource(R.drawable.ic_eye);
        }
    }

    private void signIn() {
        loading(true);

        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("asia-south1.gcp.data.mongodb-api.com")
                .addPathSegment("app")
                .addPathSegment("application-0-xighs")
                .addPathSegment("endpoint")
                .addPathSegment("getUserByEmailPassword")
                .addQueryParameter(Constants.KEY_EMAIL, binding.inputEmail.getText().toString())
                .addQueryParameter(Constants.KEY_PASSWORD, MD5Hash.md5(binding.inputPassword.getText().toString()))
                .build();

        Request request = new Request.Builder()
                .url(httpUrl.toString())
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showToast("Unable to sign in");
                    loading(false);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()){
                    String responseData = response.body().string();
                    if(responseData.equals("null")){
                        runOnUiThread(() -> {
                            showToast("Failed to sign in, please try again");
                            loading(false);
                        });
                    } else {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseData);
                            String userId = jsonResponse.getString("_id");
                            String accountType = jsonResponse.getString("accountType");
                            String username = jsonResponse.getString("username");
                            String imageProfile = jsonResponse.getString("imageProfile");
                            String email = jsonResponse.getString("email");
                            String phoneNumber = jsonResponse.getString("phoneNumber");
                            preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                            preferenceManager.putString(Constants.KEY_USER_ID, userId);
                            preferenceManager.putString(Constants.KEY_ACCOUNT_TYPE, accountType);
                            preferenceManager.putString(Constants.KEY_USERNAME, username);
                            preferenceManager.putString(Constants.KEY_IMAGE_PROFILE, imageProfile);
                            preferenceManager.putString(Constants.KEY_EMAIL, email);
                            preferenceManager.putString(Constants.KEY_PHONE_NUMBER, phoneNumber);
                            Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else {
                    runOnUiThread(() -> {
                        showToast("Failed to sign in, please try again");
                        loading(false);
                    });
                }
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