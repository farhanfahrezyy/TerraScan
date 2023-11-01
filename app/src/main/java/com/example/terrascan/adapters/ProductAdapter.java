package com.example.terrascan.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.terrascan.R;
import com.example.terrascan.databinding.ItemContainerSentMessageBinding;
import com.example.terrascan.databinding.ProductSingleViewBinding;
import com.example.terrascan.models.Product;
import com.example.terrascan.utilities.Constants;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private List<Product> productList;

    public ProductAdapter(List<Product> productList) {
        this.productList = productList;
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
        holder.binding.productPrice.setText(String.valueOf(product.getProductPrice()));
        holder.binding.sellerLocation.setText(product.getSellerLocation());
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
