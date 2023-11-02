package com.example.terrascan.adapters;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.terrascan.R;
import com.example.terrascan.databinding.ItemContainerReceivedMessageBinding;
import com.example.terrascan.databinding.ItemContainerSentMessageBinding;
import com.example.terrascan.models.ChatMessage;
import com.example.terrascan.utilities.Constants;

import java.util.HashMap;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<ChatMessage> chatMessages;
    private final String senderId;
    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;

    public ChatAdapter(List<ChatMessage> chatMessages, String senderId) {
        this.chatMessages = chatMessages;
        this.senderId = senderId;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType == VIEW_TYPE_SENT) {
            return new SentMessageViewHolder(
                    ItemContainerSentMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        } else {
            return new ReceivedMessageViewHolder(
                    ItemContainerReceivedMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        System.out.println(position);
        if(getItemViewType(position) == VIEW_TYPE_SENT) {
            SentMessageViewHolder sentHolder = (SentMessageViewHolder) holder;
            ChatMessage chatMessage = chatMessages.get(position);
            sentHolder.setData(chatMessage);
        } else {
            ReceivedMessageViewHolder receivedHolder = (ReceivedMessageViewHolder) holder;
            ChatMessage chatMessage = chatMessages.get(position);
            receivedHolder.setData(chatMessage);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        if(chatMessages.get(position).senderId.equals(senderId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemContainerSentMessageBinding binding;
        private String videoUrl;
        private VideoView videoView;
        private ImageView playPauseButton;
        private SeekBar seekBar;
        private boolean isPlaying = false;
        private final Handler handler = new Handler();

        SentMessageViewHolder(ItemContainerSentMessageBinding itemContainerSentMessageBinding) {
            super(itemContainerSentMessageBinding.getRoot());
            binding = itemContainerSentMessageBinding;
        }

        void setData(ChatMessage chatMessage) {
            if (chatMessage.messageType.equals(Constants.KEY_MESSAGE_TYPE_IMAGE)) {
                byte[] bytes = Base64.decode(chatMessage.encodeImage, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                binding.sentImageMessage.setImageBitmap(bitmap);
                binding.sentImageMessage.setVisibility(View.VISIBLE);
                if(chatMessage.message.isEmpty()) {
                    binding.textMessage.setVisibility(View.GONE);
                } else {
                    binding.textMessage.setText(chatMessage.message);
                    binding.textMessage.setVisibility(View.VISIBLE);
                }
                binding.textDateTime.setText(chatMessage.dateTime);
                binding.textDateTime.setVisibility(View.VISIBLE);
            } else if(chatMessage.messageType.equals(Constants.KEY_MESSAGE_TYPE_VIDEO)) {
                videoUrl = chatMessage.videoUrl;
                MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                mediaMetadataRetriever.setDataSource(videoUrl, new HashMap<>());
                Bitmap bmFrame = mediaMetadataRetriever.getFrameAtTime(0);
                binding.sentVideoMessage.setImageBitmap(bmFrame);
                binding.sentVideoMessage.setVisibility(View.VISIBLE);
                binding.playButton.setVisibility(View.VISIBLE);
                if(chatMessage.message.isEmpty()) {
                    binding.textMessage.setVisibility(View.GONE);
                } else {
                    binding.textMessage.setText(chatMessage.message);
                    binding.textMessage.setVisibility(View.VISIBLE);
                }
                binding.videoDateTime.setText(chatMessage.dateTime);
                binding.videoDateTime.setVisibility(View.VISIBLE);
            } else {
                binding.textMessage.setText(chatMessage.message);
                binding.textMessage.setVisibility(View.VISIBLE);
                binding.textDateTime.setText(chatMessage.dateTime);
                binding.textDateTime.setVisibility(View.VISIBLE);
            }

            binding.sentImageMessage.setOnClickListener(v -> {
                Dialog dialog = new Dialog(v.getContext());
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.dialog_image_preview);

                ImageView imageView = dialog.findViewById(R.id.fullscreenImageView);
                byte[] bytes = Base64.decode(chatMessage.encodeImage, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                imageView.setImageBitmap(bitmap);
                dialog.show();
            });

            binding.sentVideoMessage.setOnClickListener(v -> {
                Uri videoUri = Uri.parse(videoUrl);
                showVideoDialog(v.getContext(), videoUri);
            });

        }

        private void showVideoDialog(Context context, Uri videoUri) {
            Dialog videoDialog = new Dialog(context);
            videoDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            videoDialog.setContentView(R.layout.dialog_video_preview);

            videoView = videoDialog.findViewById(R.id.videoView);
            playPauseButton = videoDialog.findViewById(R.id.playPauseButton);
            seekBar = videoDialog.findViewById(R.id.seekBar);

            videoView.setVideoURI(videoUri);
            videoView.setOnPreparedListener(mp -> {
                int duration = videoView.getDuration();
                seekBar.setMax(duration);

                videoView.start();
                playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                isPlaying = true;
            });

            playPauseButton.setOnClickListener(v -> {
                if (isPlaying) {
                    videoView.pause();
                    playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                } else {
                    videoView.start();
                    playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                }
                isPlaying = !isPlaying;
            });

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        videoView.seekTo(progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            videoView.setOnCompletionListener(mp -> {
                isPlaying = false;
                playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                videoView.seekTo(0);
            });

            videoDialog.show();

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (videoView.isPlaying()) {
                        int currentPosition = videoView.getCurrentPosition();
                        seekBar.setProgress(currentPosition);
                    }
                    handler.postDelayed(this, 1000); // Update seekbar setiap 1 detik
                }
            }, 0);
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {

        private final ItemContainerReceivedMessageBinding binding;
        private String videoUrl;
        private VideoView videoView;
        private ImageView playPauseButton;
        private SeekBar seekBar;
        private boolean isPlaying = false;
        private final Handler handler = new Handler();

        ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding itemContainerReceivedMessageBinding) {
            super(itemContainerReceivedMessageBinding.getRoot());
            binding = itemContainerReceivedMessageBinding;
        }

        void setData(ChatMessage chatMessage) {
            binding.senderUsename.setText(chatMessage.senderUsername);
            byte[] profileBytes = Base64.decode(chatMessage.profileImage, Base64.DEFAULT);
            Bitmap profileBitmap = BitmapFactory.decodeByteArray(profileBytes, 0, profileBytes.length);
            binding.senderImageProfile.setImageBitmap(profileBitmap);
            if (chatMessage.messageType.equals(Constants.KEY_MESSAGE_TYPE_IMAGE)) {
                byte[] bytes = Base64.decode(chatMessage.encodeImage, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                binding.receivedImageMessage.setImageBitmap(bitmap);
                binding.receivedImageMessage.setVisibility(View.VISIBLE);
                if(chatMessage.message.isEmpty()) {
                    binding.textMessage.setVisibility(View.GONE);
                } else {
                    binding.textMessage.setText(chatMessage.message);
                    binding.textMessage.setVisibility(View.VISIBLE);
                }
                binding.textDateTime.setText(chatMessage.dateTime);
                binding.textDateTime.setVisibility(View.VISIBLE);
            } else if(chatMessage.messageType.equals(Constants.KEY_MESSAGE_TYPE_VIDEO)) {
                videoUrl = chatMessage.videoUrl;
                MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                mediaMetadataRetriever.setDataSource(videoUrl, new HashMap<>());
                Bitmap bmFrame = mediaMetadataRetriever.getFrameAtTime(0);
                binding.receivedVideoMessage.setImageBitmap(bmFrame);
                binding.receivedVideoMessage.setVisibility(View.VISIBLE);
                binding.playButton.setVisibility(View.VISIBLE);
                if(chatMessage.message.isEmpty()) {
                    binding.textMessage.setVisibility(View.GONE);
                } else {
                    binding.textMessage.setText(chatMessage.message);
                    binding.textMessage.setVisibility(View.VISIBLE);
                }
                binding.videoDateTime.setText(chatMessage.dateTime);
                binding.videoDateTime.setVisibility(View.VISIBLE);
            } else {
                binding.textMessage.setText(chatMessage.message);
                binding.textMessage.setVisibility(View.VISIBLE);
                binding.textDateTime.setText(chatMessage.dateTime);
                binding.textDateTime.setVisibility(View.VISIBLE);
            }

            binding.receivedImageMessage.setOnClickListener(v -> {
                Dialog dialog = new Dialog(v.getContext());
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.dialog_image_preview);

                ImageView imageView = dialog.findViewById(R.id.fullscreenImageView);
                byte[] bytes = Base64.decode(chatMessage.encodeImage, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                imageView.setImageBitmap(bitmap);
                dialog.show();
            });

            binding.receivedVideoMessage.setOnClickListener(v -> {
                Uri videoUri = Uri.parse(videoUrl);
                showVideoDialog(v.getContext(), videoUri);
            });

        }

        private void showVideoDialog(Context context, Uri videoUri) {
            Dialog videoDialog = new Dialog(context);
            videoDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            videoDialog.setContentView(R.layout.dialog_video_preview);

            videoView = videoDialog.findViewById(R.id.videoView);
            playPauseButton = videoDialog.findViewById(R.id.playPauseButton);
            seekBar = videoDialog.findViewById(R.id.seekBar);

            videoView.setVideoURI(videoUri);
            videoView.setOnPreparedListener(mp -> {
                int duration = videoView.getDuration();
                seekBar.setMax(duration);

                videoView.start();
                playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                isPlaying = true;
            });

            playPauseButton.setOnClickListener(v -> {
                if (isPlaying) {
                    videoView.pause();
                    playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                } else {
                    videoView.start();
                    playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
                }
                isPlaying = !isPlaying;
            });

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        videoView.seekTo(progress);
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            videoView.setOnCompletionListener(mp -> {
                isPlaying = false;
                playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                videoView.seekTo(0);
            });

            videoDialog.show();

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (videoView.isPlaying()) {
                        int currentPosition = videoView.getCurrentPosition();
                        seekBar.setProgress(currentPosition);
                    }
                    handler.postDelayed(this, 1000); // Update seekbar setiap 1 detik
                }
            }, 0);
        }

    }
}
