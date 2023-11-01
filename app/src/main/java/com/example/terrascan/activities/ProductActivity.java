package com.example.terrascan.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.terrascan.adapters.ProductAdapter;
import com.example.terrascan.databinding.ActivityProductBinding;
import com.example.terrascan.models.Product;
import com.example.terrascan.utilities.Constants;
import com.example.terrascan.utilities.MD5Hash;
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

public class ProductActivity extends AppCompatActivity {
    private PreferenceManager preferenceManager;
    private ActivityProductBinding binding;
    private RecyclerView.LayoutManager layoutManager;
    private ProductAdapter productAdapter;
    private List<Product> productList;
    OkHttpClient client = new OkHttpClient();
    private String responseData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        if(preferenceManager.getString(Constants.KEY_ACCOUNT_TYPE).equals(Constants.KEY_ADMIN_ACCOUNT)) {
            binding.addProduct.setVisibility(View.VISIBLE);
        }

        layoutManager = new GridLayoutManager(getApplicationContext(), 2);
        binding.productRecyclerView.setLayoutManager(layoutManager);
        getProduct();
        setListeners();
    }

    private void setListeners() {
        binding.profileButton.setOnClickListener(v -> {
            if(preferenceManager.getString(Constants.KEY_ACCOUNT_TYPE).equals(Constants.KEY_NORMAL_ACCOUNT)) {
                startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
            } else if(preferenceManager.getString(Constants.KEY_ACCOUNT_TYPE).equals(Constants.KEY_SELLER_ACCOUNT)) {
                startActivity(new Intent(getApplicationContext(), ProfileSellerActivity.class));
            } else {
                startActivity(new Intent(getApplicationContext(), ProfileAdminActivity.class));
            }
        });

        binding.homeButton.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), HomeActivity.class));
            finish();
        });

        binding.refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getProduct();
                binding.refreshLayout.setRefreshing(false);
            }
        });
    }

    private void getProduct() {
        binding.productRecyclerView.setVisibility(View.INVISIBLE);
        binding.progessBar.setVisibility(View.VISIBLE);
        binding.failedProgress.setVisibility(View.INVISIBLE);
        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("https")
                .host("asia-south1.gcp.data.mongodb-api.com")
                .addPathSegment("app")
                .addPathSegment("application-0-xighs")
                .addPathSegment("endpoint")
                .addPathSegment("getAllProduct")
                .build();

        Request request = new Request.Builder()
                .url(httpUrl.toString())
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> {
                    System.out.println(e);
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
                        System.out.println("SUKSES");
                        binding.productRecyclerView.setVisibility(View.VISIBLE);
                        setRecyclerView();
                    });
                }
            }
        });
    }

    private void setRecyclerView() {
        System.out.println(responseData);
        if(!responseData.isEmpty()) {
            productList = new ArrayList<>();
            try {
                JSONArray jsonArray = new JSONArray(responseData);

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    Product product = new Product();
                    product.productId = jsonObject.getString("_id");
                    product.productName = jsonObject.getString("productName");
                    product.productDesc = jsonObject.getString("productDesc");
                    product.productPrice = jsonObject.getInt("productPrice");
                    product.sellerName = jsonObject.getString("sellerName");
                    product.sellerLocation = jsonObject.getString("sellerLocation");
                    product.encodeProductImage = jsonObject.getString("encodeProductImage");

                    productList.add(product);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            productAdapter = new ProductAdapter(productList);
            binding.productRecyclerView.setAdapter(productAdapter);
            binding.productRecyclerView.setHasFixedSize(true);
        }
    }

}