package com.example.terrascan.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.example.terrascan.databinding.ActivityProductDetailBinding;
import com.example.terrascan.utilities.Constants;
import com.example.terrascan.utilities.PreferenceManager;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ProductDetailActivity extends AppCompatActivity {
    private ActivityProductDetailBinding binding;
    private PreferenceManager preferenceManager;
    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());
        if(preferenceManager.getString(Constants.KEY_ACCOUNT_TYPE).equals(Constants.KEY_ADMIN_ACCOUNT)) {
            binding.layoutDeleteEdit.setVisibility(View.VISIBLE);
        }

        loadDetails();
        setListeners();
    }

    private void loadDetails() {
        byte[] bytes = Base64.decode(getIntent().getExtras().getString("encodeProductImage"), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.productImage.setImageBitmap(bitmap);
        binding.productName.setText(getIntent().getExtras().getString("productName"));
        binding.productDesc.setText(getIntent().getExtras().getString("productDesc"));
        String productPrice = getIntent().getExtras().getString("productPrice");
        int priceValue = Integer.parseInt(productPrice);
        String formattedPrice = "Rp " + String.format(new Locale("id", "ID"), "%,d", priceValue).replace(',', '.');
        binding.productPrice.setText(formattedPrice);
        binding.sellerLocation.setText(getIntent().getExtras().getString("sellerLocation"));
    }

    private void setListeners() {
        binding.layoutContact.setOnClickListener(v -> openWhatsApp());

        binding.backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        binding.editProduct.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), ProductEditActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("productId", getIntent().getExtras().getString("productId"));
            bundle.putString("productName", getIntent().getExtras().getString("productName"));
            bundle.putString("productDesc", getIntent().getExtras().getString("productDesc"));
            bundle.putString("productPrice", getIntent().getExtras().getString("productPrice"));
            bundle.putString("sellerName", getIntent().getExtras().getString("sellerName"));
            bundle.putString("sellerLocation", getIntent().getExtras().getString("sellerLocation"));
            bundle.putString("sellerPhoneNumber", getIntent().getExtras().getString("sellerPhoneNumber"));
            bundle.putString("encodeProductImage", getIntent().getExtras().getString("encodeProductImage"));
            i.putExtras(bundle);
            startActivity(i);
        });

        binding.deleteProduct.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void deleteProduct(){
        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("asia-south1.gcp.data.mongodb-api.com")
                .addPathSegment("app")
                .addPathSegment("application-0-xighs")
                .addPathSegment("endpoint")
                .addPathSegment("deleteProductById")
                .addQueryParameter("id", getIntent().getExtras().getString("productId"))
                .build();

        Request request = new Request.Builder()
                .url(httpUrl.toString())
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> showToast("Failed to delete product"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if(response.isSuccessful()){
                    runOnUiThread(() -> {
                        showToast("Success delete product");
                        Intent intent = new Intent(getApplicationContext(), ProductActivity.class);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    runOnUiThread(() -> showToast("Failed to delete product"));
                }
            }
        });
    }

    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirmation")
                .setMessage("Are you sure want to delete this product?")
                .setPositiveButton("Yes", (dialog, id) -> {
                    deleteProduct();
                })
                .setNegativeButton("No", (dialog, id) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void openWhatsApp() {
        String phoneNumber = getIntent().getExtras().getString("sellerPhoneNumber");

        String message = "Halo, saya tertarik dengan produk " + getIntent().getExtras().getString("productName") + ". Apakah produk ini tersedia?";

        String url = "https://wa.me/" + phoneNumber + "?text=" + Uri.encode(message);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}