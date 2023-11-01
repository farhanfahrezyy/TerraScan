package com.example.terrascan.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.terrascan.R;
import com.example.terrascan.activities.ProductDetailActivity;
import com.example.terrascan.databinding.ItemContainerSentMessageBinding;
import com.example.terrascan.databinding.ProductSingleViewBinding;
import com.example.terrascan.models.Product;
import com.example.terrascan.utilities.Constants;

import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private List<Product> productList;
    private Context context;

    public ProductAdapter(List<Product> productList, Context context) {
        this.productList = productList;
        this.context = context;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ProductViewHolder(
                ProductSingleViewBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        byte[] bytes = Base64.decode(product.getEncodeProductImage(), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        holder.binding.productImage.setImageBitmap(bitmap);
        holder.binding.productTitle.setText(product.getProductName());
        String productPrice = String.valueOf(product.getProductPrice());
        int priceValue = Integer.parseInt(productPrice);
        String formattedPrice = "Rp " + String.format(new Locale("id", "ID"), "%,d", priceValue).replace(',', '.');
        holder.binding.productPrice.setText(formattedPrice);
        holder.binding.sellerLocation.setText(product.getSellerLocation());

        holder.binding.layoutProduct.setOnClickListener(v -> {
            Intent i = new Intent(context, ProductDetailActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("productId", product.getProductId());
            bundle.putString("productName", product.getProductName());
            bundle.putString("productDesc", product.getProductDesc());
            bundle.putString("productPrice", String.valueOf(product.getProductPrice()));
            bundle.putString("sellerName", product.getSellerName());
            bundle.putString("sellerLocation", product.getSellerLocation());
            bundle.putString("sellerPhoneNumber", product.getSellerPhoneNumber());
            bundle.putString("encodeProductImage", product.getEncodeProductImage());
            i.putExtras(bundle);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends  RecyclerView.ViewHolder{
        private final ProductSingleViewBinding binding;

        ProductViewHolder(ProductSingleViewBinding productSingleViewBinding) {
            super(productSingleViewBinding.getRoot());
            binding = productSingleViewBinding;
        }
    }
}
