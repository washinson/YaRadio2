package com.washinson.yaradio2;

import android.support.v4.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by User on 10.04.2018.
 */

public class Station {
    String name;
    ArrayList<Type> types;

    public Station(String name, ArrayList<Type> types) {
        this.name = name;
        this.types = types;
    }

    static class Type {
        String name, targetName;
        ArrayList<Subtype> subtypes;

        public Type(String name, String targetName, ArrayList<Subtype> subtypes) {
            this.name = name;
            this.targetName = targetName;
            this.subtypes = subtypes;
        }
    }

    static class Subtype {
        String name, typeName, targetName;
        Settings settings;

        public Subtype(String name, String typeName, String targetName) {
            this.name = name;
            this.typeName = typeName;
            this.targetName = targetName;
        }

        public Settings getSettings() throws Exception {
            if(settings != null)
                return settings;
            String response = Manager.getInstance().get("https://radio.yandex.ru/api/v2.1/handlers/radio/"
                    + targetName + "/" + name + "/settings", null, null);
            JSONObject object = new JSONObject(response);
            JSONObject restrictions2 = object.getJSONObject("station").getJSONObject("restrictions2");

            JSONArray moodEnergy = restrictions2.getJSONObject("moodEnergy").getJSONArray("possibleValues");
            JSONArray diversity = restrictions2.getJSONObject("diversity").getJSONArray("possibleValues");
            JSONArray language = restrictions2.getJSONObject("language").getJSONArray("possibleValues");

            ArrayList<Pair<String, String>> languageList = new ArrayList<>(),
                    moodList = new ArrayList<>(), diversityList = new ArrayList<>();

            for(int i = 0;i<moodEnergy.length();i++) {
                JSONObject type = moodEnergy.getJSONObject(i);
                moodList.add(new Pair<>(type.getString("value"), type.getString("name")));
            }
            for(int i = 0;i<diversity.length();i++) {
                JSONObject type = diversity.getJSONObject(i);
                diversityList.add(new Pair<>(type.getString("value"), type.getString("name")));
            }
            for(int i = 0;i<language.length();i++) {
                JSONObject type = language.getJSONObject(i);
                languageList.add(new Pair<>(type.getString("value"), type.getString("name")));
            }

            settings = new Settings(object.getJSONObject("settings2"), languageList, moodList, diversityList);
            return settings;
        }
    }

    static class Settings {
        String language, moodEnergy, diversity;
        ArrayList<Pair<String, String>> languageList, moodList, diversityList;

        Settings(JSONObject settings, ArrayList<Pair<String, String>> languageList,
                 ArrayList<Pair<String, String>> moodList,
                 ArrayList<Pair<String, String>> diversityList) throws JSONException {
            language = settings.getString("language");
            moodEnergy = settings.getString("moodEnergy");
            diversity = settings.getString("diversity");
            this.languageList = languageList;
            this.diversityList = diversityList;
            this.moodList = moodList;
        }

        public String getLanguage() {
            return language;
        }

        public String getMoodEnergy() {
            return moodEnergy;
        }

        public String getDiversity() {
            return diversity;
        }
    }
}
