package com.washinson.yaradio2;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class SettingFragment extends Fragment {
    private static final String TAG = "traffic";
    public static final String defVal = "aac_192";

    public SettingFragment() {
        // Required empty public constructor
    }

    SharedPreferences sharedPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getActivity().getSharedPreferences(TAG, Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_setting, container, false);
        RadioGroup radioGroup = root.findViewById(R.id.radio_group);

        String quality = sharedPreferences.getString("quality", defVal);

        //if(quality.equals("mp3_192")){
        //    RadioButton button = root.findViewById(R.id.radioButtonMP3_192);
        //    button.setChecked(true);
        //}
        if(quality.equals("aac_192")){
            RadioButton button = root.findViewById(R.id.radioButtonAAC_192);
            button.setChecked(true);
        }
        if(quality.equals("aac_128")){
            RadioButton button = root.findViewById(R.id.radioButtonAAC_128);
            button.setChecked(true);
        }
        if(quality.equals("aac_64")){
            RadioButton button = root.findViewById(R.id.radioButtonAAC_64);
            button.setChecked(true);
        }
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                //if(i == R.id.radioButtonMP3_192){
                //    sharedPreferences.edit().putString("quality", "mp3_192").apply();
                //} else if (i == R.id.radioButtonAAC_192){
                if (i == R.id.radioButtonAAC_192){
                    sharedPreferences.edit().putString("quality", "aac_192").apply();
                } else if (i == R.id.radioButtonAAC_128){
                    sharedPreferences.edit().putString("quality", "aac_128").apply();
                } else if (i == R.id.radioButtonAAC_64){
                    sharedPreferences.edit().putString("quality", "aac_64").apply();
                }
            }
        });
        return root;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
