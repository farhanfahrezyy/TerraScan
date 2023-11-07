package com.example.terrascan.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.terrascan.R;
import com.example.terrascan.databinding.ActivitySignUpBinding;
import com.example.terrascan.utilities.Constants;
import com.example.terrascan.utilities.MD5Hash;
import com.example.terrascan.utilities.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
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

public class SignUpActivity extends AppCompatActivity {
    private ActivitySignUpBinding binding;
    private PreferenceManager preferenceManager;
    private String encodedImage;
    private AtomicBoolean isPasswordVisible;
    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());
        isPasswordVisible = new AtomicBoolean(false);
        getEncodedProfile();
        setListeners();
    }

    private void setListeners() {
        binding.togglePassword.setOnClickListener(v -> togglePassword());

        binding.layoutSignIn.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), SignInActivity.class)));

        binding.buttonSignUp.setOnClickListener(v -> {
            if(isValidSignUpDetail()) {
                isEmailAvailable(new com.example.terrascan.interfaces.Callback<Boolean>() {
                    @Override
                    public void onResponse(Boolean result) {
                        System.out.println(result);
                        if (result) {
                            runOnUiThread(() -> {
                                signUp();
                            });
                        } else {
                            showToast("This email already exists, please use another email");
                        }
                    }
                });
            }
        });
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

    private void getEncodedProfile() {
        Drawable drawable =  ContextCompat.getDrawable(this, R.drawable.ic_profile);

        Bitmap bitmap = drawableToBitmap(drawable);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private void signUp() {
        loading(true);
        JSONObject jsonBody;
        try {
            jsonBody = new JSONObject();
            jsonBody.put(Constants.KEY_ACCOUNT_TYPE, Constants.KEY_NORMAL_ACCOUNT);
            jsonBody.put(Constants.KEY_USERNAME, binding.inputUsername.getText().toString());
            jsonBody.put(Constants.KEY_EMAIL, binding.inputEmail.getText().toString());
            jsonBody.put(Constants.KEY_PHONE_NUMBER, binding.inputPhoneNumber.getText().toString());
            jsonBody.put(Constants.KEY_PASSWORD, MD5Hash.md5(binding.inputPassword.getText().toString()));
            jsonBody.put(Constants.KEY_IMAGE_PROFILE, encodedImage);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://asia-south1.gcp.data.mongodb-api.com/app/application-0-xighs/endpoint/insertUser")
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    JSONObject jsonResponse;
                    try {
                        jsonResponse = new JSONObject(responseData);
                        String userId = jsonResponse.getString("insertedId");
                        System.out.println(userId);
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                        preferenceManager.putString(Constants.KEY_USER_ID, userId);
                        preferenceManager.putString(Constants.KEY_ACCOUNT_TYPE, Constants.KEY_NORMAL_ACCOUNT);
                        preferenceManager.putString(Constants.KEY_USERNAME, binding.inputUsername.getText().toString());
                        preferenceManager.putString(Constants.KEY_EMAIL, binding.inputEmail.getText().toString());
                        preferenceManager.putString(Constants.KEY_PHONE_NUMBER, binding.inputPhoneNumber.getText().toString());
                        preferenceManager.putString(Constants.KEY_IMAGE_PROFILE, encodedImage);
                        Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    runOnUiThread(() -> {
                        showToast("Failed to sign up");
                        loading(false);
                    });
                }
            }

            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                runOnUiThread(() -> {
                    showToast("Failed to sign up");
                    loading(false);
                });
            }
        });
    }

    private void loading(Boolean isLoading) {
        if(isLoading) {
            binding.buttonSignUp.setVisibility(View.INVISIBLE);
            binding.progessBar.setVisibility(View.VISIBLE);
        } else {
            binding.progessBar.setVisibility(View.INVISIBLE);
            binding.buttonSignUp.setVisibility(View.VISIBLE);
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private Boolean isValidSignUpDetail() {
        if(binding.inputEmail.getText().toString().trim().isEmpty()) {
            showToast("Enter email");
            return false;
        } else if(!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()) {
            showToast("Enter valid email");
            return false;
        } else if(binding.inputPhoneNumber.getText().toString().trim().isEmpty()) {
            showToast("Enter phone number");
            return false;
        } else if(binding.inputPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter password");
            return false;
        } else {
            return true;
        }
    }

    private void isEmailAvailable(com.example.terrascan.interfaces.Callback<Boolean> callback) {
        loading(true);
        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("asia-south1.gcp.data.mongodb-api.com")
                .addPathSegment("app")
                .addPathSegment("application-0-xighs")
                .addPathSegment("endpoint")
                .addPathSegment("getEmailAvailability")
                .addQueryParameter(Constants.KEY_EMAIL, binding.inputEmail.getText().toString())
                .build();

        Request request = new Request.Builder()
                .url(httpUrl.toString())
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    System.out.println("gagal okhttp");
                    showToast("Unable to check email");
                    callback.onResponse(false);
                    loading(false);
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()){
                    String responseData = response.body().string();
                    if(responseData.equals("null")){
                        runOnUiThread(() -> {
                            System.out.println("email kosong");
                            callback.onResponse(true);
                        });
                    } else {
                        runOnUiThread(() -> {
                            System.out.println("email sudah ada");
                            callback.onResponse(false);
                            loading(false);
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        showToast("Unable to check email");
                        System.out.println("Respon gagal");
                        callback.onResponse(false);
                        loading(false);
                    });
                }
            }
        });
    }

}