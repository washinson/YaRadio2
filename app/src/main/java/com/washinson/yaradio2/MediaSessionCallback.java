package com.washinson.yaradio2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.widget.Toast;

import com.washinson.yaradio2.PlayerService;

/**
 * Created by User on 08.04.2018.
 */

public class MediaSessionCallback extends MediaSessionCompat.Callback {
    Context mContext;
    private PlayerService service;
    private MediaSessionCompat mediaSession;
    private AudioManager audioManager;

    MediaSessionCallback(Context mContext, final PlayerService service,
                         MediaSessionCompat mediaSession, AudioManager audioManager) {
        this.mContext = mContext;
        this.service = service;
        this.mediaSession = mediaSession;
        this.audioManager = audioManager;
    }

    @Override
    public void onPause() {
        super.onPause();
        service.pause();
        service.refreshNotificationAndForegroundStatus(PlaybackStateCompat.STATE_PAUSED);
        try{
            mContext.unregisterReceiver(becomingNoisyReceiver);
        } catch (IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onSkipToNext() {
        super.onSkipToNext();
        if(service.getTrack() != null && !service.getTrack().isFinished()){
            Manager.getInstance().sayAboutTrack(service.getTrack(),service.getMp().getCurrentPosition()/1000.0,
                    service.getAuth(), Manager.skip);
            service.queue.clear();

            if(service.nextTrack != null)
                service.queue.add(service.nextTrack);
        }
        service.prepare();
        try{
            mContext.unregisterReceiver(becomingNoisyReceiver);
        } catch (IllegalArgumentException e){
            e.printStackTrace();
        } finally {
            mContext.registerReceiver(
                    becomingNoisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        }
        int audioFocusResult = audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        if (audioFocusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            return;
        mediaSession.setActive(true);
        service.skip();
        service.refreshNotificationAndForegroundStatus(PlaybackStateCompat.STATE_PLAYING);
    }

    @Override
    public void onPlay() {
        super.onPlay();
        boolean isPlayed = service.getTrack() != null;
        mContext.startService(new Intent(mContext.getApplicationContext(), PlayerService.class));

        try{
            mContext.unregisterReceiver(like);
        } catch (IllegalArgumentException e){
            e.printStackTrace();
        } finally {
            mContext.registerReceiver(like, new IntentFilter("like"));
        }
        try{
            mContext.unregisterReceiver(dislike);
        } catch (IllegalArgumentException e){
            e.printStackTrace();
        } finally {
            mContext.registerReceiver(dislike, new IntentFilter("dislike"));
        }
        try{
            mContext.unregisterReceiver(becomingNoisyReceiver);
        } catch (IllegalArgumentException e){
            e.printStackTrace();
        } finally {
            mContext.registerReceiver(
                    becomingNoisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        }

        int audioFocusResult = audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        if (audioFocusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            return;
        mediaSession.setActive(true);
        service.play(isPlayed);
        service.refreshNotificationAndForegroundStatus(PlaybackStateCompat.STATE_PLAYING);
    }

    @Override
    public void onStop() {
        super.onStop();
        mContext.sendBroadcast(new Intent("stopped"));
        service.track = null;
        if(mContext != null) {
            try{
                mContext.unregisterReceiver(like);
            } catch (IllegalArgumentException e){
                e.printStackTrace();
            }
            try{
                mContext.unregisterReceiver(dislike);
            } catch (IllegalArgumentException e){
                e.printStackTrace();
            }
            try{
                mContext.unregisterReceiver(becomingNoisyReceiver);
            } catch (IllegalArgumentException e){
                e.printStackTrace();
            }
        }
        audioManager.abandonAudioFocus(audioFocusChangeListener);
        service.refreshNotificationAndForegroundStatus(PlaybackStateCompat.STATE_STOPPED);
        mediaSession.setActive(false);
        service.stop();
        service.stopSelf();
    }

    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_GAIN:
                            MediaSessionCallback.this.onPlay();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            MediaSessionCallback.this.onPause();
                            break;
                        default:
                            MediaSessionCallback.this.onPause();
                            break;
                    }
                }
            };

    final BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                MediaSessionCallback.this.onPause();
            }
        }
    };

    BroadcastReceiver dislike = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            service.getTrack().setLiked(false);
            Manager.getInstance().sayAboutTrack(
                    service.getTrack(), service.getMp().getCurrentPosition() / 1000.0,
                    service.getAuth(), Manager.dislike);
            service.getMediaSession().getController().getTransportControls().skipToNext();
            service.refreshNotificationAndForegroundStatus(
                    service.getMediaSession().getController().getPlaybackState().getState());
        }
    };

    BroadcastReceiver like = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!service.getTrack().isLiked()) {
                service.getTrack().setLiked(true);
                Manager.getInstance().sayAboutTrack(
                        service.getTrack(), service.getMp().getCurrentPosition() / 1000.0,
                        service.getAuth(), Manager.like);
            }
            else {
                service.getTrack().setLiked(false);
                Manager.getInstance().sayAboutTrack(
                        service.getTrack(), service.getMp().getCurrentPosition() / 1000.0,
                        service.getAuth(), Manager.unlike);
            }
            service.getMediaSession().setMetadata(service.getMediaSession().getController().getMetadata());
            service.refreshNotificationAndForegroundStatus(
                    service.getMediaSession().getController().getPlaybackState().getState());
        }
    };
}

