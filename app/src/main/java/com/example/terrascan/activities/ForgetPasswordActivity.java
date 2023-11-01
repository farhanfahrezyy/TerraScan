package com.example.terrascan.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.terrascan.databinding.ActivityForgetPasswordBinding;
import com.example.terrascan.utilities.Constants;
import com.example.terrascan.utilities.MD5Hash;
import com.example.terrascan.utilities.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ForgetPasswordActivity extends AppCompatActivity {
    private ActivityForgetPasswordBinding binding;
    private AtomicBoolean isPasswordVisible;
    private PreferenceManager preferenceManager;
    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());

        isPasswordVisible = new AtomicBoolean(false);
        setListeners();
    }

    private void setListeners() {
        binding.layoutSignIn.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        binding.toggleNewPassword.setOnClickListener(v -> toggleNewPassword());

        binding.toggleConfirmPassword.setOnClickListener(v -> toggleConfirmPassword());

        binding.buttonChange.setOnClickListener(v -> {
            if(isValidFormDetails()) {
                changePassword();
            }
        });
    }

    private void changePassword() {
        loading(true);

        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("asia-south1.gcp.data.mongodb-api.com")
                .addPathSegment("app")
                .addPathSegment("application-0-xighs")
                .addPathSegment("endpoint")
                .addPathSegment("updatePasswordByEmailPhoneNumber")
                .addQueryParameter(Constants.KEY_EMAIL, binding.inputEmail.getText().toString())
                .addQueryParameter(Constants.KEY_PHONE_NUMBER, binding.inputPhoneNumber.getText().toString())
                .build();

        JSONObject jsonBody;
        try {
            jsonBody = new JSONObject();
            jsonBody.put(Constants.KEY_PASSWORD, MD5Hash.md5(binding.inputNewPassword.getText().toString()));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url(httpUrl.toString())
                .put(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showToast("Unable to change password");
                    loading(false);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()){
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseData);
                        String result = jsonResponse.getString("matchedCount");
                        if(result.equals("0")){
                            runOnUiThread(() -> {
                                showToast("Account not found, please check your email and phone number");
                                loading(false);
                            });
                        } else {
                            runOnUiThread(() -> {
                                showToast("Change password successfully");
                                Intent i = new Intent(getApplicationContext(), SignInActivity.class);
                                startActivity(i);
                                finish();
                            });
                        }
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                } else {
                    runOnUiThread(() -> {
                        showToast("Unable to change password, please try again");
                        loading(false);
                    });
                }
            }
        });
    }

    private void loading(Boolean isLoading) {
        if(isLoading) {
            binding.buttonChange.setVisibility(View.INVISIBLE);
            binding.progessBar.setVisibility(View.VISIBLE);
        } else {
            binding.progessBar.setVisibility(View.INVISIBLE);
            binding.buttonChange.setVisibility(View.VISIBLE);
        }
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

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private Boolean isValidFormDetails() {
        if(binding.inputEmail.getText().toString().trim().isEmpty()) {
            showToast("Enter email");
            return false;
        } else if(!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()) {
            showToast("Enter valid email");
            return false;
        } else if(binding.inputPhoneNumber.getText().toString().trim().isEmpty()) {
            showToast("Enter phone number");
            return false;
        } else if(binding.inputNewPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter password");
            return false;
        } else if(binding.inputConfirmPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter confirmation password");
            return false;
        } else if(!binding.inputNewPassword.getText().toString().trim().equals(binding.inputConfirmPassword.getText().toString().trim())) {
            showToast("Password and confirm password not same");
            return false;
        } else {
            return true;
        }
    }
}