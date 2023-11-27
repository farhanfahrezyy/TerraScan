package com.example.terrascan.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.terrascan.R; // Ganti dengan package yang benar jika perlu
import com.example.terrascan.databinding.ActivityLearnBinding;

public class LearnActivity extends AppCompatActivity {

    private ActivityLearnBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLearnBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        // Mengakses elemen TextView dan ImageView dari XML
        TextView humusText = binding.texthumus;
        TextView vulkanikText = binding.vulkaniktext;
        ImageView humusImage = binding.learnImage;
        ImageView vulkanikImage = binding.learnImage2;

        // Mengatur teks untuk TextView berdasarkan ID
        humusText.setText("Humus adalah tanah yang sangat subur terbentuk dari lapukan daun dan batang pohon. Humus biasanya berwarna gelap dan dijumpai terutama pada lapisan tanah atas.");
        vulkanikText.setText("Tanah vulkanik terbentuk dari material-material seperti pasir dan juga abu vulkanik. Tanah vulkanik terbagi menjadi dua tipe, yaitu regosol dan latosol.");

        // Mengatur gambar untuk ImageView berdasarkan ID
        humusImage.setImageResource(R.drawable.gambar_tanah_humus);
        vulkanikImage.setImageResource(R.drawable.gambar_tanah_vulkanik);
        setonListeners();
    }
    private void setonListeners() {
        binding.backButton.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }
}
