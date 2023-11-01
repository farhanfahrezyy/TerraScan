package com.example.terrascan.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;

import com.example.terrascan.databinding.ActivityProductDetailBinding;
import com.example.terrascan.utilities.Constants;
import com.example.terrascan.utilities.PreferenceManager;

import java.util.Locale;

public class ProductDetailActivity extends AppCompatActivity {
    private ActivityProductDetailBinding binding;
    private PreferenceManager preferenceManager;

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
        binding.backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }
}