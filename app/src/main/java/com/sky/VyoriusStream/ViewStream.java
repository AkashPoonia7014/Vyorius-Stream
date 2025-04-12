package com.sky.VyoriusStream;

import android.app.PictureInPictureParams;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Rational;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hbisoft.hbrecorder.HBRecorder;
import com.hbisoft.hbrecorder.HBRecorderListener;
import com.sky.VyoriusStream.databinding.ActivityViewStreamBinding;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IVLCVout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


public class ViewStream extends AppCompatActivity implements HBRecorderListener{

    private static final int SCREEN_RECORD_REQUEST_CODE = 777;
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final int PERMISSION_REQ_POST_NOTIFICATIONS = 33;
    private static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = PERMISSION_REQ_ID_RECORD_AUDIO + 1;
    private static final int PERMISSION_REQ_ID_FOREGROUND_SERVICE_MEDIA_PROJECTION = PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE + 1;
    private boolean hasPermissions = false;
    private boolean hasAudioPermissions = false;
    boolean isAudioEnabled = true;
    private LibVLC libVLC;
    private MediaPlayer mediaPlayer;
    SurfaceView svVideo;
    ActivityViewStreamBinding binding;
    private ViewGroup.LayoutParams videoParams;
    private HBRecorder hbRecorder;
    private String rtspUrl;
    private FloatingActionButton recordButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityViewStreamBinding.inflate(getLayoutInflater());
        setContentView(binding.root);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });
        // to change background color of status bar (no need to check if version higher than Lollipop)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorText));

//        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_AUDIO_PERMISSION);
//        }


        initView();
        setActionOnView();
        hbRecorder = new HBRecorder(this, this);
        if (hbRecorder.isBusyRecording()) {
            recordButton.setImageResource(R.drawable.ic_stop_24);
            recordButton.setColorFilter(ContextCompat.getColor(this, R.color.colorGrey), PorterDuff.Mode.SRC_IN);
        }
        setOnClickListeners();
        



    }

    private void setOnClickListeners() {
        recordButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // first check if permissions were granted
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // SDK 34
                    if (isAudioEnabled) {
                        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS, PERMISSION_REQ_POST_NOTIFICATIONS)
                                && checkSelfPermission(android.Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)
                                && checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION, PERMISSION_REQ_ID_FOREGROUND_SERVICE_MEDIA_PROJECTION)) {
                            hasPermissions = true;

                        }
                    }else{
                        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS, PERMISSION_REQ_POST_NOTIFICATIONS)
                                && checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION, PERMISSION_REQ_ID_FOREGROUND_SERVICE_MEDIA_PROJECTION)) {
                            hasPermissions = true;

                        }
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // SDK 33
                    if (isAudioEnabled) {
                        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS, PERMISSION_REQ_POST_NOTIFICATIONS)
                                && checkSelfPermission(android.Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)) {
                            hasPermissions = true;

                        }
                    }else{
                        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS, PERMISSION_REQ_POST_NOTIFICATIONS)) {
                            hasPermissions = true;
                        }
                    }

                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)) {
                        hasPermissions = true;


                    }
                } else {
                    if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)
                            && checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE)) {
                        hasPermissions = true;

                    }
                }

                if (hasPermissions) {
                    // check if recording is in progress and stop it if it is
                    if (hbRecorder.isBusyRecording()) {
                        hbRecorder.stopScreenRecording();
                        recordButton.setImageResource(R.drawable.ic_record_24);
                        recordButton.setColorFilter(ContextCompat.getColor(this, R.color.colorRed), PorterDuff.Mode.SRC_IN);
                    } else {
                        // else start recording
                        if (!hasAudioPermissions && isAudioEnabled) {
                            if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)){
                                hasPermissions = true;

                                startRecordingScreen();
                            }

                        }else {
                            startRecordingScreen();
                        }
                    }
                }
            } else {
                Toast.makeText(this, "This library requires API 21", Toast.LENGTH_LONG).show();
            }
        });
    }



    private boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            // Special case: for POST_NOTIFICATIONS on SDK 33+
            if (permission.equals(android.Manifest.permission.POST_NOTIFICATIONS)
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                // Show custom rationale if needed
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                    new AlertDialog.Builder(this)
                            .setTitle("Permission Required")
                            .setMessage("This app needs notification permission to show recording notifications.")
                            .setPositiveButton("Allow", (dialog, which) -> {
                                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
                            })
                            .setNegativeButton("Deny", null)
                            .show();
                } else {
                    // Request directly
                    ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
                }

                return false;
            } else {
                ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
                return false;
            }
        }
        return true;
    }

    private void startRecordingScreen() {

        quickSettings();
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent permissionIntent = mediaProjectionManager != null ? mediaProjectionManager.createScreenCaptureIntent() : null;
        startActivityForResult(permissionIntent, SCREEN_RECORD_REQUEST_CODE);

        recordButton.setImageResource(R.drawable.ic_stop_24);
        recordButton.setColorFilter(ContextCompat.getColor(this, R.color.colorGrey), PorterDuff.Mode.SRC_IN);
    }
    private void quickSettings() {
        hbRecorder.setAudioBitrate(128000);
        hbRecorder.setAudioSamplingRate(44100);
        hbRecorder.recordHDVideo(false);
        hbRecorder.isAudioEnabled(true);
        hbRecorder.setVideoEncoder("H264");
        hbRecorder.setOutputFormat("MPEG_4");

        //Customise Notification

        //hbRecorder.setNotificationSmallIconVector(R.drawable.ic_baseline_videocam_24);
        hbRecorder.setNotificationTitle(getString(R.string.stop_recording_notification_title));
        hbRecorder.setNotificationDescription(getString(R.string.stop_recording_notification_message));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQ_POST_NOTIFICATIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (isAudioEnabled) {
                        checkSelfPermission(android.Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO);
                    }else {
                        checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE);
                    }
                } else {
                    hasPermissions = false;
                    Toast.makeText(this, "No permission for"  + android.Manifest.permission.POST_NOTIFICATIONS, Toast.LENGTH_SHORT).show();
                }
                break;
            case PERMISSION_REQ_ID_RECORD_AUDIO:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE);
                } else {
                    hasPermissions = false;
                    Toast.makeText(this, "No permission for " + android.Manifest.permission.RECORD_AUDIO, Toast.LENGTH_SHORT).show();
                }
                break;
            case PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    hasPermissions = true;
                    startRecordingScreen();
                } else {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION, PERMISSION_REQ_ID_FOREGROUND_SERVICE_MEDIA_PROJECTION);
                    } else {
                        hasPermissions = false;

                        Toast.makeText(this, "No permission for " + android.Manifest.permission.WRITE_EXTERNAL_STORAGE, Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case PERMISSION_REQ_ID_FOREGROUND_SERVICE_MEDIA_PROJECTION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasPermissions = true;
                    startRecordingScreen();
                } else {
                    hasPermissions = false;

                    Toast.makeText(this, "No permission for " + android.Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION, Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == SCREEN_RECORD_REQUEST_CODE) {
                if (resultCode == RESULT_OK) {
                    //Set file path or Uri depending on SDK version
                    //Start screen recording
                    setOutputPath();
                    hbRecorder.startScreenRecording(data, resultCode);

                }else{
                    recordButton.setImageResource(R.drawable.ic_stop_24);
                    recordButton.setColorFilter(ContextCompat.getColor(this, R.color.colorGrey), PorterDuff.Mode.SRC_IN);
                }
            }
        }
    }

    ContentResolver resolver;
    ContentValues contentValues;
    Uri mUri;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setOutputPath() {
        String filename = generateFileName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver = getContentResolver();
            contentValues = new ContentValues();
            contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Vyorius/");
            contentValues.put(MediaStore.Video.Media.TITLE, filename);
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");

            mUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues);
            //FILE NAME SHOULD BE THE SAME
            hbRecorder.setFileName(filename);
            hbRecorder.setOutputUri(mUri);
        }else{

            hbRecorder.setOutputPath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) +"/");
        }
    }


    private String generateFileName() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault());
        Date curDate = new Date(System.currentTimeMillis());
        return formatter.format(curDate).replace(" ", "");
    }

    private void initView() {
        rtspUrl = getIntent().getStringExtra("rtspURL");

        svVideo = findViewById(R.id.svVideo);
        recordButton = binding.recordButton;

        binding.textView.bringToFront();
        binding.textView.invalidate();
        binding.textView.setText(rtspUrl);
    }
    private void setActionOnView() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
        binding.blackBackButton.setOnClickListener(v -> {
            callback.handleOnBackPressed();
        });

        svVideo.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {

                createPlayer(holder, rtspUrl);
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {

                releasePlayer();

            }
        });

        binding.pipButton.setOnClickListener(v -> {
            enterPipMode();
        });
    }

    private void enterPipMode() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Hardcoded a aspect ratio
            Rational aspectRatio = new Rational(150,250);
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build();
            enterPictureInPictureMode(params);
        } else {
            Toast.makeText(this, "Picture-in-Picture mode is not supported on your device.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);

        FrameLayout container = findViewById(R.id.root);

        if (isInPictureInPictureMode) {
            binding.blackBackButton.setVisibility(View.GONE);
            binding.textView.setVisibility(View.GONE);
            binding.pipButton.setVisibility(View.GONE);
            binding.recordButton.setVisibility(View.GONE);


        } else {
            binding.blackBackButton.setVisibility(View.VISIBLE);
            binding.textView.setVisibility(View.VISIBLE);
            binding.pipButton.setVisibility(View.VISIBLE);
            binding.recordButton.setVisibility(View.VISIBLE);
        }
    }

    private void createPlayer(SurfaceHolder holder, String url) {

        releasePlayer();
        setSvVideoSize();

        ArrayList<String> options = new ArrayList<>();
        options.add("--aout=opensles");
        options.add("--audio-time-stretch");
        options.add("--no-drop-late-frames");
        options.add("--no-skip-frames");
        options.add("-vvv");
        options.add(":network-caching=150");
        options.add(":rtsp-tcp");


        libVLC = new LibVLC(this, options);
        mediaPlayer = new org.videolan.libvlc.MediaPlayer(libVLC);

        Media media = new Media(libVLC, Uri.parse(url));
        media.addOption(":codec=avcodec"); // added when no video output in android 11
//        media.setHWDecoderEnabled(false, false); // For android 11

//        media.setHWDecoderEnabled(true, false);  // if used, no video in android 11
        mediaPlayer.setMedia(media);

//        Surface setup
//        SurfaceHolder holder = svVideo.getHolder();

        IVLCVout vlcVout = mediaPlayer.getVLCVout();
        vlcVout.setVideoView(svVideo);
        vlcVout.setWindowSize(videoParams.width,videoParams.height);
        vlcVout.setVideoSurface(holder.getSurface(), holder);

//        vlcVout.addCallback(new IVLCVout.Callback() {
//            @Override
//            public void onSurfacesCreated(IVLCVout vlcVout) {
//            }
////            rtsp://10.1.8.212:5540/ch0
//
//            @Override
//            public void onSurfacesDestroyed(IVLCVout vlcVout) {}
//        });

        vlcVout.attachViews();

        Toast.makeText(this, "Getting stream ready...", Toast.LENGTH_SHORT).show();
        mediaPlayer.play();

        mediaPlayer.setEventListener(event -> {
            switch (event.type) {
                case org.videolan.libvlc.MediaPlayer.Event.Playing:
                    runOnUiThread(() -> new Handler(Looper.getMainLooper()).postDelayed(() ->
                            Toast.makeText(this, "Stream ready, will play shortly", Toast.LENGTH_SHORT).show(), 4000));
                    break;

                case org.videolan.libvlc.MediaPlayer.Event.EncounteredError:
                    runOnUiThread(() ->
                            Toast.makeText(this, "Failed to play stream", Toast.LENGTH_SHORT).show());
                    break;

                case org.videolan.libvlc.MediaPlayer.Event.EndReached:
                case org.videolan.libvlc.MediaPlayer.Event.Stopped:
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Stream ended", Toast.LENGTH_SHORT).show();
                        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 1500);
                    });
                    break;
            }
        });
//        libVLC = new LibVLC(this);
//
//        Media media = new Media(libVLC, Uri.parse(url));
//        media.addOption("--aout=opensles");
//        media.addOption("--audio-time-stretch");
//        media.addOption("-vvv");
//        media.addOption(":network-caching=150");
//        media.addOption(":rtsp-tcp");
//
//        mediaPlayer = new org.videolan.libvlc.MediaPlayer(libVLC);
//        mediaPlayer.setMedia(media);
//
//
//        mediaPlayer.getVLCVout().setVideoSurface(holder.getSurface(), holder);
//        mediaPlayer.getVLCVout().attachViews();
//
//
//        mediaPlayer.play();
    }
    private void setSvVideoSize() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        videoParams = svVideo.getLayoutParams();
        videoParams.width = displayMetrics.widthPixels;
        videoParams.height = displayMetrics.heightPixels;
    }
    private void releasePlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (libVLC != null) {
            libVLC.release();
            libVLC = null;
        }
    }

    // Helps with picture in picture not able to get full view of video, but it creates a new mediaPlayer instance in pip mode
    // ToDo later
//    public Rational getPipRatio() {
//        DisplayMetrics metrics = getResources().getDisplayMetrics();
//        return new Rational(Math.round(metrics.xdpi), Math.round(metrics.ydpi));
//    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }
    @Override
    public void finish() {
        super.finish();
        releasePlayer();
    }

    @Override
    public void HBRecorderOnStart() {

    }

    @Override
    public void HBRecorderOnComplete() {

        recordButton.setImageResource(R.drawable.ic_record_24);
        recordButton.setColorFilter(ContextCompat.getColor(this, R.color.colorRed), PorterDuff.Mode.SRC_IN);
        Toast.makeText(this, "Saved Successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void HBRecorderOnError(int errorCode, String reason) {

    }

    @Override
    public void HBRecorderOnPause() {

    }

    @Override
    public void HBRecorderOnResume() {

    }
}

/*
// Put in the onCreate if you want to change icon colors of status bar
        // To Change the icon colors of the status bar
        // this if checks if android version is higher or equal to 11
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            Objects.requireNonNull(getWindow().getInsetsController()).setSystemBarsAppearance(
//                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
//                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
//            );
//        }
//        else  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  // no need of this cause my minimum sdk is greater than that so it is always true so we direct else instead of else
//        else {
//            getWindow().getDecorView().setSystemUiVisibility(
//                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
//                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
//            );
//        }

*/