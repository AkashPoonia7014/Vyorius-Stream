package com.sky.VyoriusStream;

import android.content.Context;
import android.content.Intent;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsetsController;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.sky.VyoriusStream.databinding.ActivityMainBinding;

import android.media.MediaPlayer;

import java.io.IOException;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    String[] items = {"RTSP"}; // To add more protocols in the future
    private ActivityMainBinding binding;
    private EditText rtspURL;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.main);

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set dark icons for light status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Objects.requireNonNull(getWindow().getInsetsController()).setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            );
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.list_item, items);
        binding.dropdownField.setAdapter(adapter);

        setActionOnViews();
    }



    private void setActionOnViews() {
        //    ToDo
        binding.editTextInputLayout.setHint("RTSP Url");

        rtspURL = binding.rtspURL;
        rtspURL.requestFocus();

        binding.streamButton.setOnClickListener(view -> {

            Log.d("count", "Main Activity: "+rtspURL.getText().toString());
            if (isValidRtspUrl(rtspURL.getText().toString())) {

                Intent intent = new Intent(getApplicationContext(), ViewStream.class);
                intent.putExtra("rtspURL", rtspURL.getText().toString());
                startActivity(intent);

            } else {
                Toast.makeText(this, "Enter valid Url", Toast.LENGTH_SHORT).show();
            }
        });

    }

//    private boolean isValidRtspUrl(String url) {
//        if (url == null) return false;
//
//        Uri uri = Uri.parse(url);
//
//        String temp = "temp";
//        if (url.length() >= 7)
//            temp = url.substring(7);
//
//        return uri != null
//                && "rtsp".equalsIgnoreCase(uri.getScheme())
//                && uri.getHost() != null
//                && !temp.isEmpty();
//
//    }

    public boolean isValidRtspUrl(String url) {
        if (url == null || url.trim().isEmpty()) return false;

        try {
            Uri uri = Uri.parse(url);
            return "rtsp".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && !uri.getHost().isEmpty();
        } catch (Exception e) {
            return false;
        }

    }

    public void testRtspStream(Context context, String rtspUrl) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(rtspUrl);
            mediaPlayer.setOnPreparedListener(mp -> {
                Toast.makeText(context, "RTSP stream is reachable", Toast.LENGTH_SHORT).show();
                mp.release();
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Toast.makeText(context, "Invalid or unreachable RTSP stream", Toast.LENGTH_SHORT).show();
                return true;
            });
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Malformed RTSP URL", Toast.LENGTH_SHORT).show();
        }
    }

}