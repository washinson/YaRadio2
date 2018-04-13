package com.washinson.yaradio2;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.Target;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class PlayerService extends Service {
    private int NOTIFICATION_ID = 121;
    String TAG = "YaPlayer";

    static Station.Subtype subtype;
    Track track, nextTrack;
    ArrayDeque<Track> queue = new ArrayDeque<>();
    ArrayList<Track> history = new ArrayList<>();

    MediaPlayer mp;
    MediaSessionCompat mediaSession;
    PlaybackStateCompat.Builder mStateBuilder = new PlaybackStateCompat.Builder().setActions(
            PlaybackStateCompat.ACTION_PLAY
                    | PlaybackStateCompat.ACTION_STOP
                    | PlaybackStateCompat.ACTION_PAUSE
                    | PlaybackStateCompat.ACTION_PLAY_PAUSE
                    | PlaybackStateCompat.ACTION_SKIP_TO_NEXT);
    MediaSessionCallback mediaSessionCallback;
    private MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
    private AudioManager audioManager;
    private Browser.Auth auth;

    SharedPreferences sharedPreferences;


    void pause(){
        if(mediaSession.getController().getPlaybackState().getState() != PlaybackStateCompat.STATE_PLAYING)
            return;
        mp.pause();
        mediaSession.setPlaybackState(
                mStateBuilder.setState(PlaybackStateCompat.STATE_PAUSED,
                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1).build());
    }

    void stop() {
        mp.release();
        mediaSession.setActive(false);
        mediaSession.setPlaybackState(
                mStateBuilder.setState(PlaybackStateCompat.STATE_STOPPED,
                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1).build());
    }

    void play(boolean isPlayed) {
        if(!isPlayed){
            mediaSession.getController().getTransportControls().skipToNext();
            return;
        }
        if(mediaSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING)
            return;
        mp.start();
        mediaSession.setPlaybackState(
                mStateBuilder.setState(PlaybackStateCompat.STATE_PLAYING,
                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1).build());
    }

    void skip() {
        try {
            mp.reset();
            startTrack(track);
            mediaSession.setPlaybackState(
                    mStateBuilder.setState(PlaybackStateCompat.STATE_PLAYING,
                            PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1).build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void startTrack(Track track) throws Exception {
        Manager.getInstance().sayAboutTrack(track,-1,
                getAuth(), Manager.trackStarted);

        Log.i(TAG,"----");
        Log.i(TAG,"Current track: " + track.toString());
        Log.i(TAG,"----");

        //String path = "https://radio.yandex.ru/api/v2.1/handlers/track/"
        //        + track.getId() + ":" + track.getAlbumId() + "/radio-web-"
        //        + track.getStation().targetName + "-" + track.getStation().name
        //        + "-direct/download/m?hq=0&external-domain=radio.yandex.ru&overembed=no";

        String path = "https://api.music.yandex.net/tracks/" + track.getId() + "/download-info";

        String json = Manager.getInstance().get(path, null, track);

        JSONObject jsonObject = new JSONObject(json);

        //String src = jsonObject.getString("src") + "&format=json";
        QualityInfo qualityInfo = QualityInfo.fromJSON(jsonObject);

        int quality = sharedPreferences.getInt("quality", SettingFragment.defVal);
        String src = qualityInfo.byQuality(quality) + "&format=json";

        okhttp3.Request.Builder builder = new okhttp3.Request.Builder().get().url(src);
        builder.addHeader("Host", "storage.mds.yandex.net");
        Manager.getInstance().setDefaultHeaders(builder);

        String result = Manager.getInstance().get(src, builder.build(), track);
        JSONObject downloadInformation = new JSONObject(result);
        DownloadInfo info = DownloadInfo.fromJSON(downloadInformation);
        String downloadPath = info.getSrc();

        try {
            mp.setDataSource(downloadPath);
            mp.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void prepare(){
        if(track == null){
            track = new Track(null, subtype);
            updateTracks();
            track = queue.getFirst();
            queue.removeFirst();
            nextTrack = queue.getFirst();
        } else if(queue.size() == 0) {
            nextTrack = null;
            updateTracks();
            track = queue.getFirst();
            queue.removeFirst();
            nextTrack = queue.getFirst();
        } else {
            queue.removeFirst();
            updateTracks();
            track = nextTrack;
            nextTrack = queue.getFirst();
        }
        notifyTrack(track);
    }

    public void updateTracks(){
        while(queue.size() == 0){
            try {
                queue.addAll(Manager.getInstance().getTracks(track, nextTrack, subtype));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    Target target = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            if(mediaSession==null || mediaSession.getController() == null ||
                    mediaSession.getController().getPlaybackState() == null) return;
            MediaMetadataCompat metadata = metadataBuilder
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.getTitle())
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION,
                            firstUpperCase(track.getStation().name))
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.getArtist())
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.getAlbum())
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
                    .build();
            mediaSession.setMetadata(metadata);
            refreshNotificationAndForegroundStatus(mediaSession.getController().getPlaybackState().getState());
        }

        @Override
        public void onBitmapFailed(Exception e, Drawable errorDrawable) {

        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {

        }
    };

    public String firstUpperCase(String word){
        if(word == null || word.isEmpty()) return "";//или return word;
        return word.substring(0, 1).toUpperCase() + word.substring(1);
    }

    void notifyTrack(final Track track) {
        Picasso.get().load(Utils.getCover(300, track.getCover())).into(target);
        MediaMetadataCompat metadata = metadataBuilder
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.getTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION,
                        firstUpperCase(track.getStation().name))
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.getAlbum())
                .build();
        mediaSession.setMetadata(metadata);
        mediaSession.setActive(true);
        mediaSession.setPlaybackState(
                mStateBuilder.setState(PlaybackStateCompat.STATE_PLAYING,
                        PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1).build());
    }

    @Override
    public void onCreate() {
        super.onCreate();

        auth = new Browser.Auth();
        auth.Init();

        sharedPreferences = getSharedPreferences("traffic", Context.MODE_PRIVATE);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mp = new MediaPlayer();
        mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                mediaPlayer.release();
                return false;
            }
        });
        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
            }
        });
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                track.setFinished(true);
                Manager.getInstance().sayAboutTrack(track, mediaPlayer.getCurrentPosition() / 1000.0,
                        auth, Manager.trackFinished);
                mediaSession.getController().getTransportControls().skipToNext();
            }
        });

        mediaSession = new MediaSessionCompat(this, TAG);
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mediaSessionCallback = new MediaSessionCallback(this,
                this, mediaSession, audioManager);

        mediaSession.setCallback(mediaSessionCallback);

        Intent activityIntent = new Intent(getApplicationContext(), PlayerActivity.class);
        mediaSession.setSessionActivity(
                PendingIntent.getActivity(getApplicationContext(), 0, activityIntent, 0));

        Intent mediaButtonIntent = new Intent(
                Intent.ACTION_MEDIA_BUTTON, null, getApplicationContext(), MediaButtonReceiver.class);
        mediaSession.setMediaButtonReceiver(
                PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent, 0));
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        mediaSession.getController().getTransportControls().stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        mp.release();
        mediaSessionCallback = null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new PlayerServiceBinder();
    }

    void refreshNotificationAndForegroundStatus(int playbackState) {
        switch (playbackState) {
            case PlaybackStateCompat.STATE_PLAYING: {
                Notification notification = getNotification(playbackState);
                if(notification == null) return;
                startForeground(NOTIFICATION_ID, notification);
                break;
            }
            case PlaybackStateCompat.STATE_PAUSED: {
                // На паузе мы перестаем быть foreground, однако оставляем уведомление,
                // чтобы пользователь мог play нажать
                Notification notification = getNotification(playbackState);
                if(notification == null) return;
                NotificationManagerCompat.from(PlayerService.this).notify(NOTIFICATION_ID, notification);
                stopForeground(false);
                break;
            }
            default: {
                // Все, можно прятать уведомление
                stopForeground(true);
                break;
            }
        }
        mediaSession.setMetadata(mediaSession.getController().getMetadata());
    }

    Notification getNotification(int playbackState) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        MediaControllerCompat controller = mediaSession.getController();
        MediaMetadataCompat mediaMetadata = controller.getMetadata();
        if(mediaMetadata == null || track == null) return null;
        MediaDescriptionCompat description = mediaMetadata.getDescription();

        builder.setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setLargeIcon(description.getIconBitmap())
                .setSubText(mediaMetadata.getText(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION))
                .setContentIntent(controller.getSessionActivity())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_STOP));

        // ...play/pause
        if (playbackState == PlaybackStateCompat.STATE_PLAYING)
            builder.addAction(
                    new NotificationCompat.Action(
                            R.drawable.icon_pause, getString(R.string.pause),
                            MediaButtonReceiver.buildMediaButtonPendingIntent(
                                    this,
                                    PlaybackStateCompat.ACTION_PLAY_PAUSE)));
        else
            builder.addAction(
                    new NotificationCompat.Action(
                            R.drawable.icon_play, getString(R.string.play),
                            MediaButtonReceiver.buildMediaButtonPendingIntent(
                                    this,
                                    PlaybackStateCompat.ACTION_PLAY_PAUSE)));

        // ...на следующий трек
        builder.addAction(
                new NotificationCompat.Action(R.drawable.icon_skip, getString(R.string.next),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                this,
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT)));

        builder.addAction(new NotificationCompat.Action(
                R.drawable.icon_dislike ,getString(R.string.dislike_track),
                PendingIntent.getBroadcast(this, 0, new Intent("dislike"), 0)
        ));

        if(!track.isLiked()) {
            builder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_like, getString(R.string.like_track),
                    PendingIntent.getBroadcast(this, 0, new Intent("like"), 0)
            ));
        } else {
            builder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_liked, getString(R.string.like_track),
                    PendingIntent.getBroadcast(this, 0, new Intent("like"), 0)
            ));
        }

        builder.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                // В компактном варианте показывать Action с данным порядковым номером.
                .setShowActionsInCompactView(0, 1, 3)
                // Отображать крестик в углу уведомления для его закрытия.
                // Это связано с тем, что для API < 21 из-за ошибки во фреймворке
                // пользователь не мог смахнуть уведомление foreground-сервиса
                // даже после вызова stopForeground(false).
                // Так что это костыль.
                // На API >= 21 крестик не отображается, там просто смахиваем уведомление.
                .setShowCancelButton(true)
                // Указываем, что делать при нажатии на крестик или смахивании
                .setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                this,
                                PlaybackStateCompat.ACTION_STOP))
                // Передаем токен. Это важно для Android Wear. Если токен не передать,
                // кнопка на Android Wear будет отображаться, но не будет ничего делать
                .setMediaSession(mediaSession.getSessionToken()));

        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));

        // Не отображать время создания уведомления. В нашем случае это не имеет смысла
        builder.setShowWhen(false);

        // Это важно. Без этой строчки уведомления не отображаются на Android Wear
        // и криво отображаются на самом телефоне.
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);

        // Не надо каждый раз вываливать уведомление на пользователя
        builder.setOnlyAlertOnce(true);
        return builder.build();
    }

    public Browser.Auth getAuth() { return auth; }

    public MediaPlayer getMp() {
        return mp;
    }

    public Track getTrack() {
        return track;
    }

    public MediaSessionCompat getMediaSession() {
        return mediaSession;
    }

    private static class QualityInfo {
        HashMap<Integer, JSONObject> qualities = new HashMap<>();

        public static QualityInfo fromJSON(JSONObject src) throws JSONException {
            QualityInfo info = new QualityInfo();
            JSONArray jsonArray = src.getJSONArray("result");
            for(int i = 0; i < jsonArray.length(); i++){
                JSONObject temp = jsonArray.getJSONObject(i);

                info.qualities.put(temp.getInt("bitrateInKbps"), temp);
            }
            return info;
        }

        public String byQuality(int quality) throws JSONException {
            return qualities.get(quality).getString("downloadInfoUrl");
        }
    }

    private static class DownloadInfo {
        String s;
        String ts;
        String path;
        String host;
        static final String SALT = "XGRlBW9FXlekgbPrRHuSiA";

        public static DownloadInfo fromJSON(JSONObject jsonObject) throws JSONException {
            DownloadInfo info = new DownloadInfo();
            info.s = jsonObject.getString("s");
            info.ts = jsonObject.getString("ts");
            info.path = jsonObject.getString("path");
            info.host = jsonObject.getString("host");
            return info;
        }

        /**
         * Generating path to track
         */
        public String getSrc() {
            try {
                String toHash = SALT + path.substring(1) + s;
                byte[] toHashBytes = toHash.getBytes();
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] hashBytes = md.digest(toHashBytes);
                BigInteger bigInt = new BigInteger(1, hashBytes);
                String md5Hex = bigInt.toString(16);
                while (md5Hex.length() < 32) {
                    md5Hex = "0" + md5Hex;
                }
                return "https://" + host + "/get-mp3/" + md5Hex + "/" + ts + path;
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return "";
        }
    }

    public class PlayerServiceBinder extends Binder {
        public PlayerService getService(){
            return PlayerService.this;
        }
        public MediaSessionCompat.Token getMediaSessionToken() {
            return mediaSession.getSessionToken();
        }
    }
}
