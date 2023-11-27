package com.example.terrascan.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import com.example.terrascan.databinding.ActivityEditProfileBinding;
import com.example.terrascan.utilities.Constants;
import com.example.terrascan.utilities.MD5Hash;
import com.example.terrascan.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class EditProfileActivity extends AppCompatActivity {
    private ActivityEditProfileBinding binding;
    private PreferenceManager preferenceManager;
    private String encodedImage;
    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(getApplicationContext());
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loadProfileDetails();
        setListeners();
    }

    public void loadProfileDetails() {
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE_PROFILE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
        binding.inputUsername.setText(preferenceManager.getString(Constants.KEY_USERNAME));
        binding.inputEmail.setText(preferenceManager.getString(Constants.KEY_EMAIL));
        binding.inputPhoneNumber.setText(preferenceManager.getString(Constants.KEY_PHONE_NUMBER));
    }

    public void setListeners() {
        binding.layoutImage.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickProfileImage.launch(i);
        });

        binding.backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        binding.buttonSave.setOnClickListener(v -> {
            if(isValidFormDetails()) {
                saveProfile();
            }
        });
    }

    public void saveProfile() {
        loading(true);
        if(encodedImage == null) {
            encodedImage = preferenceManager.getString(Constants.KEY_IMAGE_PROFILE);
        }

        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("asia-south1.gcp.data.mongodb-api.com")
                .addPathSegment("app")
                .addPathSegment("application-0-xighs")
                .addPathSegment("endpoint")
                .addPathSegment("updateUserById")
                .addQueryParameter("id", preferenceManager.getString(Constants.KEY_USER_ID))
                .build();

        JSONObject jsonBody;
        try {
            jsonBody = new JSONObject();
            jsonBody.put(Constants.KEY_USERNAME, binding.inputUsername.getText().toString());
            jsonBody.put(Constants.KEY_EMAIL, binding.inputEmail.getText().toString());
            jsonBody.put(Constants.KEY_PHONE_NUMBER, binding.inputPhoneNumber.getText().toString());
            jsonBody.put(Constants.KEY_IMAGE_PROFILE, encodedImage);
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
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    preferenceManager.putString(Constants.KEY_USERNAME, binding.inputUsername.getText().toString());
                    preferenceManager.putString(Constants.KEY_IMAGE_PROFILE, encodedImage);
                    preferenceManager.putString(Constants.KEY_EMAIL, binding.inputEmail.getText().toString());
                    preferenceManager.putString(Constants.KEY_PHONE_NUMBER, binding.inputPhoneNumber.getText().toString());
                    Intent i = new Intent(getApplicationContext(), ProfileActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                } else {
                    runOnUiThread(() -> {
                        showToast("Gagal menyimpan");
                        loading(false);
                    });
                }
            }

            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                runOnUiThread(() -> {
                    showToast("Gagal menyimpan");
                    loading(false);
                });
            }
        });
    }

    private String encodeImage(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newDimension = Math.min(width, height);

        Bitmap squareBitmap = Bitmap.createBitmap(bitmap, (width - newDimension) / 2, (height - newDimension) / 2, newDimension, newDimension);

        int previewWidth = 150;
        int previewHeight = squareBitmap.getHeight() * previewWidth / squareBitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(squareBitmap, previewWidth, previewHeight, false);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();

        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }


    private final ActivityResultLauncher<Intent> pickProfileImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == RESULT_OK) {
                    if(result.getData()  != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageProfile.setImageBitmap(bitmap);
                            encodedImage = encodeImage(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
    );

    private void loading(Boolean isLoading) {
        if(isLoading) {
            binding.buttonSave.setVisibility(View.INVISIBLE);
            binding.progessBar.setVisibility(View.VISIBLE);
        } else {
            binding.progessBar.setVisibility(View.INVISIBLE);
            binding.buttonSave.setVisibility(View.VISIBLE);
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private Boolean isValidFormDetails() {
        if(binding.inputUsername.toString().trim().isEmpty()) {
            showToast("Enter username");
            return false;
        } else if(!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()) {
            showToast("Enter valid email");
            return false;
        } else if(binding.inputPhoneNumber.getText().toString().trim().isEmpty()) {
            showToast("Enter phone number");
            return false;
        } else {
            return true;
        }
    }
}