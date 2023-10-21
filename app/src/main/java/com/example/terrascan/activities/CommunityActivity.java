package com.example.terrascan.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;

import com.example.terrascan.adapters.ChatAdapter;
import com.example.terrascan.databinding.ActivityCommunityBinding;
import com.example.terrascan.models.ChatMessage;
import com.example.terrascan.utilities.Constants;
import com.example.terrascan.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class CommunityActivity extends AppCompatActivity {
    private ActivityCommunityBinding binding;
    private PreferenceManager preferenceManager;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private FirebaseFirestore database;
    private String encodedImage;
    private String downloadUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCommunityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
        init();
        listenMessages();
    }

    private void setListeners() {
        binding.layoutSend.setOnClickListener(v -> sendMessage());
        binding.layoutSendImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            galleryPick.launch(intent);
        });
    }

    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages,
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void listenMessages() {
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .addSnapshotListener(eventListener);
    }

    @SuppressLint("NotifyDataSetChanged")
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if(error != null) {
            return;
        }
        if(value != null) {
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if(documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderUsername = documentChange.getDocument().getString(Constants.KEY_USERNAME);
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.profileImage = documentChange.getDocument().getString(Constants.KEY_IMAGE_PROFILE);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessage.messageType = documentChange.getDocument().getString(Constants.KEY_MESSAGE_TYPE);
                    if (documentChange.getDocument().contains(Constants.KEY_ENCODED_IMAGE)) {
                        chatMessage.encodeImage = documentChange.getDocument().getString(Constants.KEY_ENCODED_IMAGE);
                    }
                    if (documentChange.getDocument().contains(Constants.KEY_VIDEO_URL)) {
                        chatMessage.videoUrl = documentChange.getDocument().getString(Constants.KEY_VIDEO_URL);
                    }
                    chatMessages.add(chatMessage);
                }
            }
            chatMessages.sort(Comparator.comparing(obj -> obj.dateObject));
            if (count == 0) {
                chatAdapter.notifyDataSetChanged();
            } else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
    };

    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void sendMessage() {
        if(encodedImage != null) {
            HashMap<String, Object> message = new HashMap<>();
            message.put(Constants.KEY_USERNAME, preferenceManager.getString(Constants.KEY_USERNAME));
            message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            message.put(Constants.KEY_IMAGE_PROFILE, preferenceManager.getString(Constants.KEY_IMAGE_PROFILE));
            message.put(Constants.KEY_MESSAGE_TYPE, Constants.KEY_MESSAGE_TYPE_IMAGE);
            message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
            message.put(Constants.KEY_ENCODED_IMAGE, encodedImage);
            message.put(Constants.KEY_TIMESTAMP, new Date());
            database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
            binding.inputMessage.setText(null);
            binding.imagePreview.setVisibility(View.GONE);
            encodedImage = null;
        } else if (downloadUrl != null) {
            HashMap<String, Object> message = new HashMap<>();
            message.put(Constants.KEY_USERNAME, preferenceManager.getString(Constants.KEY_USERNAME));
            message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            message.put(Constants.KEY_IMAGE_PROFILE, preferenceManager.getString(Constants.KEY_IMAGE_PROFILE));
            message.put(Constants.KEY_MESSAGE_TYPE, Constants.KEY_MESSAGE_TYPE_VIDEO);
            message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
            message.put(Constants.KEY_VIDEO_URL, downloadUrl);
            message.put(Constants.KEY_TIMESTAMP, new Date());
            database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
            binding.inputMessage.setText(null);
            binding.imagePreview.setVisibility(View.GONE);
            encodedImage = null;
        } else if(!binding.inputMessage.getText().toString().isEmpty()) {
            HashMap<String, Object> message = new HashMap<>();
            message.put(Constants.KEY_USERNAME, preferenceManager.getString(Constants.KEY_USERNAME));
            message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            message.put(Constants.KEY_IMAGE_PROFILE, preferenceManager.getString(Constants.KEY_IMAGE_PROFILE));
            message.put(Constants.KEY_MESSAGE_TYPE, Constants.KEY_MESSAGE_TYPE_TEXT);
            message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
            message.put(Constants.KEY_TIMESTAMP, new Date());
            database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
            binding.inputMessage.setText(null);
        }

    }

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 500;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private final ActivityResultLauncher<Intent> galleryPick = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        Uri selectedMediaUri = result.getData().getData();
                        if (selectedMediaUri != null) {
                            String mediaType = getContentResolver().getType(selectedMediaUri);
                            if (mediaType != null && (mediaType.startsWith("image/") || mediaType.startsWith("video/"))) {
                                try {
                                    if (mediaType.startsWith("image/")) {
                                        InputStream inputStream = getContentResolver().openInputStream(selectedMediaUri);
                                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                        binding.imagePreview.setImageBitmap(bitmap);
                                        binding.imagePreview.setVisibility(View.VISIBLE);
                                        encodedImage = encodeImage(bitmap);
                                    } else if (mediaType.startsWith("video/")) {
                                        FirebaseStorage storage = FirebaseStorage.getInstance();
                                        StorageReference storageRef = storage.getReference();
                                        String fileName = "videos/" + System.currentTimeMillis() + ".mp4";

                                        StorageReference videoRef = storageRef.child(fileName);
                                        videoRef.putFile(selectedMediaUri)
                                                .addOnSuccessListener(taskSnapshot -> {
                                                    videoRef.getDownloadUrl()
                                                            .addOnSuccessListener(uri -> {
                                                                downloadUrl = uri.toString();
                                                            })
                                                            .addOnFailureListener(e -> {
                                                            });
                                                })
                                                .addOnFailureListener(e -> {
                                                });
                                    }
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
    );

}