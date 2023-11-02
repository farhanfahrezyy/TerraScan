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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.terrascan.databinding.ActivityProductInsertBinding;
import com.example.terrascan.utilities.Constants;
import com.example.terrascan.utilities.MD5Hash;
import com.example.terrascan.utilities.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProductInsertActivity extends AppCompatActivity {
    private ActivityProductInsertBinding binding;
    private PreferenceManager preferenceManager;
    OkHttpClient client = new OkHttpClient();
    private String selectedSellerId, encodedImage;
    public static final MediaType JSON
            = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductInsertBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());

        loadSpinner();
        setListeners();
    }

    private void loadSpinner() {
        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("asia-south1.gcp.data.mongodb-api.com")
                .addPathSegment("app")
                .addPathSegment("application-0-xighs")
                .addPathSegment("endpoint")
                .addPathSegment("getAllSeller")
                .build();

        Request request = new Request.Builder()
                .url(httpUrl.toString())
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    showToast("Failed load seller data");
                });

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()){
                    String responseData = response.body().string();
                    if(responseData.equals("null")){
                        runOnUiThread(() -> {
                            showToast("No data found");
                        });
                    } else {
                        try {
                            JSONArray jsonArray = new JSONArray(responseData);

                            ArrayList<String> sellerNames = new ArrayList<>();
                            HashMap<String, String> sellerMap = new HashMap<>();

                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject seller = jsonArray.getJSONObject(i);
                                String sellerId = seller.getString("_id");
                                String sellerName = seller.getString("sellerName");
                                sellerNames.add(sellerName);
                                sellerMap.put(sellerId, sellerName);
                            }

                            runOnUiThread(() -> {
                                ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_item, sellerNames);
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                binding.spinnerSeller.setAdapter(adapter);

                                binding.spinnerSeller.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                    @Override
                                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                        String selectedSellerName = parent.getItemAtPosition(position).toString();
                                        selectedSellerId = getKeyFromValue(sellerMap, selectedSellerName);
                                    }

                                    @Override
                                    public void onNothingSelected(AdapterView<?> parent) {
                                        // Handle no selection
                                    }
                                });

                            });
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        });
    }

    private void setListeners() {
        binding.backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        binding.buttonSubmit.setOnClickListener(v -> {
            if(isValidFormData()) {
                addProduct();
            }
        });

        binding.layoutImage.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(i);
        });

        binding.uploadImage.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(i);
        });
    }

    private String encodeImage(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newDimension = Math.min(width, height);

        Bitmap squareBitmap = Bitmap.createBitmap(bitmap, (width - newDimension) / 2, (height - newDimension) / 2, newDimension, newDimension);

        int previewWidth = 500;
        int previewHeight = squareBitmap.getHeight() * previewWidth / squareBitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(squareBitmap, previewWidth, previewHeight, false);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();

        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == RESULT_OK) {
                    if(result.getData()  != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.productImage.setImageBitmap(bitmap);
                            encodedImage = encodeImage(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }
    );

    private void addProduct(){
        loading(true);
        JSONObject jsonBody;
        try {
            jsonBody = new JSONObject();
            jsonBody.put(Constants.KEY_PRODUCT_NAME, binding.inputProductName.getText().toString());
            jsonBody.put(Constants.KEY_PRODUCT_DESC, binding.inputProductDesc.getText().toString());
            jsonBody.put(Constants.KEY_PRODUCT_PRICE, binding.inputProductPrice.getText().toString());
            jsonBody.put(Constants.KEY_SELLER_ID, selectedSellerId);
            jsonBody.put(Constants.KEY_PRODUCT_IMAGE, encodedImage);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://asia-south1.gcp.data.mongodb-api.com/app/application-0-xighs/endpoint/insertProduct")
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        showToast("Success add product");
                        Intent intent = new Intent(getApplicationContext(), ProductActivity.class);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    runOnUiThread(() -> {
                        showToast("Failed to add product");
                        loading(false);
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call call, IOException e) {
                runOnUiThread(() -> {
                    showToast("Failed to add product");
                    loading(false);
                });
            }
        });
    }

    private String getKeyFromValue(HashMap<String, String> hashMap, String value) {
        for (Map.Entry<String, String> entry : hashMap.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void loading(Boolean isLoading) {
        if(isLoading) {
            binding.buttonSubmit.setVisibility(View.INVISIBLE);
            binding.progessBar.setVisibility(View.VISIBLE);
        } else {
            binding.progessBar.setVisibility(View.INVISIBLE);
            binding.buttonSubmit.setVisibility(View.VISIBLE);
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private Boolean isValidFormData() {
        if(binding.inputProductName.toString().trim().isEmpty()) {
            showToast("Enter product name");
            return false;
        } else if(binding.inputProductDesc.toString().trim().isEmpty()) {
            showToast("Enter product description");
            return false;
        } else if(binding.inputProductPrice.getText().toString().trim().isEmpty()) {
            showToast("Enter product price");
            return false;
        } else if(binding.inputProductPrice.getText().toString().length() >= 10) {
            showToast("Max price is 9 digits");
            return false;
        } else if(selectedSellerId.isEmpty()) {
            showToast("Select seller");
            return false;
        } else if(encodedImage.isEmpty()) {
            showToast("Select product image");
            return false;
        } else {
            return true;
        }
    }

}