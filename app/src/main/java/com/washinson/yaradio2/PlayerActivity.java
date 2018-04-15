package com.washinson.yaradio2;

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
import android.graphics.Path;
import android.graphics.drawable.ColorDrawable;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URI;

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
                if(playerService == null) return;
                sendBroadcast(new Intent("like"));
            }
        });
        dislike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(playerService == null) return;
                sendBroadcast(new Intent("dislike"));
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mediaController == null) return;
                mediaController.getTransportControls().skipToNext();
            }
        });
        play_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                        try {
                            if(playerService == null || playerService.getTrack() == null ||
                                    playerService.getTrack().getQualityInfo() == null ||
                                    playerService.getTrack().getQualityInfo().qualities.isEmpty())
                                return;
                            String path = playerService.getTrack().getQualityInfo().byQuality("mp3_192");
                            Track track = playerService.getTrack();

                            String src = path + "&format=json";

                            okhttp3.Request.Builder builder = new okhttp3.Request.Builder().get().url(src);
                            builder.addHeader("Host", "storage.mds.yandex.net");
                            Manager.getInstance().setDefaultHeaders(builder);

                            String result = Manager.getInstance().get(src, builder.build(), track);
                            JSONObject downloadInformation = new JSONObject(result);
                            PlayerService.DownloadInfo info = PlayerService.DownloadInfo.fromJSON(downloadInformation);
                            String downloadPath = info.getSrc();

                            DownloadManager downloadManager =
                                    (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                            DownloadManager.Request request = new DownloadManager.Request(android.net.Uri.parse(downloadPath));
                            request.setTitle(track.getTitle());
                            request.setDescription(track.getArtist());
                            File file = new File((Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath()
                                    + "/" + track.getTitle() + " - " + track.getArtist() + ".mp3").replace(" ", "_"));
                            request.setDestinationUri(android.net.Uri.fromFile(file));
                            request.allowScanningByMediaScanner();
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                            request.setVisibleInDownloadsUi(true);
                            if (downloadManager != null) {
                                downloadManager.enqueue(request);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
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
        if(metadataCompat == null || playerService == null || playerService.getTrack() == null)
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
