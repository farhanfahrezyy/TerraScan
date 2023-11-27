package com.example.terrascan.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Bundle;
import android.view.View;

import com.example.terrascan.adapters.ProductAdapter;
import com.example.terrascan.adapters.SavedAdapter;
import com.example.terrascan.databinding.ActivitySavedBinding;
import com.example.terrascan.models.Product;
import com.example.terrascan.models.Saved;
import com.example.terrascan.utilities.Constants;
import com.example.terrascan.utilities.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SavedActivity extends AppCompatActivity {
    private ActivitySavedBinding binding;
    private SavedAdapter savedAdapter;
    private List<Saved> savedList;
    private PreferenceManager preferenceManager;
    OkHttpClient client = new OkHttpClient();
    private String responseData;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySavedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());

        setListeners();
        getSaved();
    }

    private void setListeners() {
        binding.backButton2.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getSaved();
                binding.refreshLayout.setRefreshing(false);
            }
        });
    }

    private void getSaved() {
        binding.savedRecyclerView.setVisibility(View.INVISIBLE);
        binding.progessBar.setVisibility(View.VISIBLE);
        binding.failedProgress.setVisibility(View.INVISIBLE);
        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("asia-south1.gcp.data.mongodb-api.com")
                .addPathSegment("app")
                .addPathSegment("application-0-xighs")
                .addPathSegment("endpoint")
                .addPathSegment("getSavedAnalysisById")
                .addQueryParameter("userId", preferenceManager.getString(Constants.KEY_USER_ID))
                .build();

        Request request = new Request.Builder()
                .url(httpUrl.toString())
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    binding.refreshLayout.setRefreshing(false);
                    binding.progessBar.setVisibility(View.INVISIBLE);
                    binding.failedProgress.setVisibility(View.VISIBLE);
                });

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()){
                    binding.progessBar.setVisibility(View.INVISIBLE);
                    binding.failedProgress.setVisibility(View.INVISIBLE);
                    responseData = response.body().string();
                    runOnUiThread(()-> {
                        binding.savedRecyclerView.setVisibility(View.VISIBLE);
                        setRecyclerView();
                    });
                }
            }
        });
    }

    private void setRecyclerView() {
        if(!responseData.isEmpty()) {
            savedList = new ArrayList<>();
            try {
                System.out.println(responseData);
                JSONArray jsonArray = new JSONArray(responseData);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    Saved saved = new Saved();
                    saved.savedId = jsonObject.getString("_id");
                    saved.soilImage = jsonObject.getString("soilImage");
                    saved.soilType = jsonObject.getString("soilType");
                    saved.accuracy = jsonObject.getString("accuracy");
                    saved.climate = jsonObject.getString("climate");
                    saved.temperature = jsonObject.getString("temperature");
                    saved.cropRecommendation = jsonObject.getString("cropRecommendation");

                    savedList.add(saved);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            savedAdapter = new SavedAdapter(savedList, getApplicationContext());
            binding.savedRecyclerView.setAdapter(savedAdapter);
            binding.savedRecyclerView.setHasFixedSize(true);
        }
    }
}