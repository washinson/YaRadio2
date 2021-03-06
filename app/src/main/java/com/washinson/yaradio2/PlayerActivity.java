package com.washinson.yaradio2;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.graphics.drawable.ColorDrawable;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URI;

import static android.os.Environment.DIRECTORY_MUSIC;

public class PlayerActivity extends AppCompatActivity {

    PlayerService.PlayerServiceBinder playerServiceBinder;
    MediaControllerCompat mediaController;
    PlayerService playerService;

    ImageView cover;
    ImageButton play_pause,next,like,dislike,settings;
    TextView label;

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            playerServiceBinder = (PlayerService.PlayerServiceBinder) service;
            try {
                mediaController = new MediaControllerCompat(
                        PlayerActivity.this, playerServiceBinder.getMediaSessionToken());
                PlayerActivity.this.playerService = playerServiceBinder.getService();
                mediaController.registerCallback(
                        new MediaControllerCompat.Callback() {
                            @Override
                            public void onMetadataChanged(MediaMetadataCompat metadata) {
                                super.onMetadataChanged(metadata);
                                updateScreen(metadata);
                            }

                            @Override
                            public void onSessionDestroyed() {
                                super.onSessionDestroyed();
                                PlayerActivity.this.finish();
                            }
                        }
                );

                if(playerService.getTrack() == null ||
                        !playerService.getTrack().getStation().name.equals(PlayerService.subtype.name)){
                    playerService.track = null;
                    playerService.queue.clear();
                    mediaController.getTransportControls().play();
                } else {
                    updateScreen(mediaController.getMetadata());
                }
            }
            catch (RemoteException e) {
                mediaController = null;
                PlayerActivity.this.playerService = null;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            playerServiceBinder = null;
            mediaController = null;
            playerService = null;
        }
    };

    BroadcastReceiver stopped = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            PlayerActivity.this.finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        Window window = this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this,android.R.color.white));

        bindService(new Intent(this, PlayerService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        registerReceiver(stopped, new IntentFilter("stopped"));
        initButtons();
    }

    void initButtons(){
        cover = findViewById(R.id.cover);
        like = findViewById(R.id.likeButton);
        dislike = findViewById(R.id.dislikeButton);
        next = findViewById(R.id.nextButton);
        play_pause = findViewById(R.id.pause);
        label = findViewById(R.id.track_name);
        settings = findViewById(R.id.settings);

        like.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(AnimationUtils.loadAnimation(PlayerActivity.this, R.anim.image_click));
                if(playerService == null) return;
                sendBroadcast(new Intent("like"));
            }
        });
        dislike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(AnimationUtils.loadAnimation(PlayerActivity.this, R.anim.image_click));
                if(playerService == null) return;
                sendBroadcast(new Intent("dislike"));
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(AnimationUtils.loadAnimation(PlayerActivity.this, R.anim.image_click));
                if(mediaController == null) return;
                mediaController.getTransportControls().skipToNext();
            }
        });
        play_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(AnimationUtils.loadAnimation(PlayerActivity.this, R.anim.image_click));
                if(mediaController == null) return;
                if(mediaController.getPlaybackState() == null ||
                        mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PAUSED ||
                        mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_STOPPED){
                    mediaController.getTransportControls().play();
                }else if (mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING){
                    mediaController.getTransportControls().pause();
                }
            }
        });
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PlayerActivity.this, SubtypeSetting.class);
                startActivity(intent);
            }
        });
        label.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ClipboardManager clipboard = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);
                if(playerService == null)
                    return;

                Track track = playerService.getTrack();

                if(track == null)
                    return;

                ClipData clip = ClipData.newPlainText(getString(R.string.track_info), track.getArtist() + " - " + track.getTitle());
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(PlayerActivity.this, getString(R.string.copied), Toast.LENGTH_SHORT).show();
                }
            }
        });
        cover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(PlayerActivity.this);
                builder.setMessage(getString(R.string.download_track))
                        .setTitle(getString(R.string.download_title));

                builder.setPositiveButton(getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int requested = ContextCompat.checkSelfPermission(
                                PlayerActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        if(requested == android.content.pm.PackageManager.PERMISSION_DENIED){
                            int ACCESS_EXTERNAL_STORAGE_STATE = 1;
                            ActivityCompat.requestPermissions(PlayerActivity.this,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    ACCESS_EXTERNAL_STORAGE_STATE);
                        } else {
                            try {
                                loadTrack();
                            } catch (Exception e) {
                                AlertDialog.Builder alertBuilder1 = new AlertDialog.Builder(PlayerActivity.this);
                                alertBuilder1.setMessage(getString(R.string.error))
                                        .setTitle(e.getMessage())
                                        .create().show();
                                e.printStackTrace();
                            }
                        }
                        dialogInterface.cancel();
                    }
                });

                builder.setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });

                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });
    }

    void loadTrack() throws Exception {
        Mp3Downloader.getInstance().loadTrack();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        try{
            switch (requestCode) {
                case 1: {
                    if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        loadTrack();
                    } else {
                        //Toast.makeText(getApplicationContext(), "Please grant permission.", Toast.LENGTH_LONG).show();
                        throw new Exception(getString(R.string.permission_denied));
                    }
                    break;
                }
            }
        } catch (Exception e) {
            AlertDialog.Builder alertBuilder1 = new AlertDialog.Builder(PlayerActivity.this);
            alertBuilder1.setMessage(getString(R.string.error))
                .setTitle(e.getMessage())
                    .create().show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        unregisterReceiver(stopped);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mediaController != null && mediaController.getMetadata() != null)
            updateScreen(mediaController.getMetadata());
    }

    void updateScreen(MediaMetadataCompat metadataCompat){
        if(metadataCompat == null || playerService == null || playerService.getTrack() == null || mediaController == null
                || mediaController.getPlaybackState() == null)
            return;

        label.setText(metadataCompat.getText(MediaMetadataCompat.METADATA_KEY_ARTIST) +
                " - " + metadataCompat.getString(MediaMetadataCompat.METADATA_KEY_TITLE));

        cover.setImageBitmap(metadataCompat.getBitmap(MediaMetadataCompat.METADATA_KEY_ART));

        if(playerService.getTrack().isLiked())
            like.setImageDrawable(getDrawable(R.drawable.icon_liked));
        else
            like.setImageDrawable(getDrawable(R.drawable.icon_like));

        if(mediaController.getPlaybackState().getState() ==  PlaybackStateCompat.STATE_PLAYING){
            play_pause.setImageDrawable(getDrawable(R.drawable.ic_pause));
        }else{
            play_pause.setImageDrawable(getDrawable(R.drawable.ic_play));
        }
    }
}
