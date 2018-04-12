package com.washinson.yaradio2;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ExpandableListAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class SubtypeSetting extends AppCompatActivity {
    RadioGroup mood;
    RadioGroup diversity;
    RadioGroup language;

    PlayerService.PlayerServiceBinder playerServiceBinder;
    MediaControllerCompat mediaController;
    PlayerService playerService;

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            playerServiceBinder = (PlayerService.PlayerServiceBinder) service;
            try {
                mediaController = new MediaControllerCompat(
                        SubtypeSetting.this, playerServiceBinder.getMediaSessionToken());
                SubtypeSetting.this.playerService = playerServiceBinder.getService();
                mediaController.registerCallback(
                        new MediaControllerCompat.Callback() {
                            public void onSessionDestroyed() {
                                super.onSessionDestroyed();
                                SubtypeSetting.this.finish();
                            }
                        }
                );
                updateUI();
            }
            catch (Exception e) {
                mediaController = null;
                SubtypeSetting.this.playerService = null;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            playerServiceBinder = null;
            mediaController = null;
            playerService = null;
        }
    };

    void updateUI() throws Exception {
        if(playerService == null)
            return;
        int q = 0;
        for (Pair<String, String> i : playerService.getTrack().getStation().getSettings().moodList) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setText(i.second);
            radioButton.setId(q++);
            if(i.first.equals(playerService.getTrack().getStation().getSettings().moodEnergy))
                radioButton.setChecked(true);
            else
                radioButton.setChecked(false);
            mood.addView(radioButton);
        }
        q = 0;
        for (Pair<String, String> i : playerService.getTrack().getStation().getSettings().diversityList) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setText(i.second);
            radioButton.setId(q++);
            if(i.first.equals(playerService.getTrack().getStation().getSettings().diversity))
                radioButton.setChecked(true);
            else
                radioButton.setChecked(false);
            diversity.addView(radioButton);
        }
        q = 0;
        for (Pair<String, String> i : playerService.getTrack().getStation().getSettings().languageList) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setText(i.second);
            radioButton.setId(q++);
            if(i.first.equals(playerService.getTrack().getStation().getSettings().language))
                radioButton.setChecked(true);
            else
                radioButton.setChecked(false);
            language.addView(radioButton);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subtype_setting);

        Window window = this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this,R.color.colorPrimary));

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mood = findViewById(R.id.mood);
        diversity = findViewById(R.id.discover);
        language = findViewById(R.id.language);

        mood.clearCheck();
        diversity.clearCheck();
        language.clearCheck();

        bindService(new Intent(this, PlayerService.class), serviceConnection, BIND_AUTO_CREATE);

        Button update = findViewById(R.id.update_info);
        update.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(playerService == null) return;
                try {
                    int moodi = mood.getCheckedRadioButtonId();
                    int diversityi = diversity.getCheckedRadioButtonId();
                    int languagei = language.getCheckedRadioButtonId();
                    String moods = playerService.getTrack().getStation()
                            .getSettings().moodList.get(moodi).first;
                    String diversitys = playerService.getTrack().getStation()
                            .getSettings().diversityList.get(diversityi).first;
                    String languages = playerService.getTrack().getStation()
                            .getSettings().languageList.get(languagei).first;
                    Manager.getInstance().updateInfo(moods, diversitys, languages, playerService.getTrack(),
                            playerService.getAuth());
                    playerService.queue.clear();
                    playerService.track.station.settings = null;
                    playerService.nextTrack = null;
                    Toast.makeText(SubtypeSetting.this, "Updated", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
