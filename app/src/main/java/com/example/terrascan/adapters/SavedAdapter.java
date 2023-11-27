package com.example.terrascan.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.terrascan.activities.ProductActivity;
import com.example.terrascan.activities.ProductDetailActivity;
import com.example.terrascan.activities.SavedActivity;
import com.example.terrascan.databinding.ProductSingleViewBinding;
import com.example.terrascan.databinding.SavedSingleViewBinding;
import com.example.terrascan.models.Product;
import com.example.terrascan.models.Saved;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SavedAdapter extends RecyclerView.Adapter<SavedAdapter.SavedViewHolder> {
    private List<Saved> savedList;
    private Context context;
    private boolean isDeleteAnalysisVisible = false;
    OkHttpClient client = new OkHttpClient();

    public SavedAdapter(List<Saved> savedList, Context context) {

        this.savedList = savedList;
        this.context = context;
    }

    @NonNull
    @Override
    public SavedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SavedViewHolder(
                SavedSingleViewBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull SavedViewHolder holder, int position) {
        Saved saved = savedList.get(position);
        byte[] bytes = Base64.decode(saved.getSoilImage(), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        holder.binding.soilImage.setImageBitmap(bitmap);
        holder.binding.soilType.setText(saved.getSoilType());
        holder.binding.jenisTanah.setText(saved.getSoilType());
        holder.binding.akurasi.setText(saved.getAccuracy());
        holder.binding.iklim.setText(saved.getClimate());
        holder.binding.suhu.setText(saved.getTemperature());
        holder.binding.rekomendasiTanaman.setText(saved.getCropRecommendation());

        holder.binding.dots.setOnClickListener(v -> {
            // Toggle status tampilan deleteAnalysis
            isDeleteAnalysisVisible = !isDeleteAnalysisVisible;

            // Set visibilitas deleteAnalysis sesuai dengan status
            holder.binding.deleteAnalysis.setVisibility(isDeleteAnalysisVisible ? View.VISIBLE : View.GONE);

//            holder.binding.deleteAnalysis.setOnClickListener(view -> deleteClickListener(saved.getSavedId()));
        });

    }

    private void deleteClickListener(String savedId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Confirmation")
                .setMessage("Are you sure want to delete this product?")
                .setPositiveButton("Yes", (dialog, id) -> {
                    deleteAnalysis(savedId);
                })
                .setNegativeButton("No", (dialog, id) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }



    private void deleteAnalysis(String id){
        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("asia-south1.gcp.data.mongodb-api.com")
                .addPathSegment("app")
                .addPathSegment("application-0-xighs")
                .addPathSegment("endpoint")
                .addPathSegment("deleteSavedAnalysisById")
                .addQueryParameter("id", id)
                .build();

        Request request = new Request.Builder()
                .url(httpUrl.toString())
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if(response.isSuccessful()){

                    Intent i = new Intent(context, SavedActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    context.startActivity(i);
                } else {

                }
            }
        });
    }


    @Override
    public int getItemCount() {
        return savedList.size();
    }

    public static class SavedViewHolder extends  RecyclerView.ViewHolder{
        private final SavedSingleViewBinding binding;

        SavedViewHolder(SavedSingleViewBinding savedSingleViewBinding) {
            super(savedSingleViewBinding.getRoot());
            binding = savedSingleViewBinding;
        }
    }
}
