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
    public static final int defVal = 192;

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

        int quality = sharedPreferences.getInt("quality", defVal);

        if(quality == 192){
            RadioButton button = root.findViewById(R.id.radioButton0);
            button.setChecked(true);
        }
        if(quality == 128){
            RadioButton button = root.findViewById(R.id.radioButton1);
            button.setChecked(true);
        }
        if(quality == 64){
            RadioButton button = root.findViewById(R.id.radioButton2);
            button.setChecked(true);
        }
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if(i == R.id.radioButton0){
                    sharedPreferences.edit().putInt("quality", 192).apply();
                } else if (i == R.id.radioButton1){
                    sharedPreferences.edit().putInt("quality", 128).apply();
                } else if (i == R.id.radioButton2){
                    sharedPreferences.edit().putInt("quality", 64).apply();
                }
            }
        });
        return root;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
