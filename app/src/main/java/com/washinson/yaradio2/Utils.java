package com.washinson.yaradio2;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

/**
 * Created by User on 10.04.2018.
 */

public class Utils {
    static public Locale getLocale(Context context){
        return context.getResources().getConfiguration().locale;
    }

    static public String getPlayId(Track track){
        String id = Browser.getCookieParam("device_id").replaceAll("\"", "");
        // XDD
        return id + ":" + track.getId() + ":" + String.valueOf(Math.random()).substring(2);
    }

    static public String getCover(int sizeX, String cover){
        return cover.replace("%%", sizeX + "x" + sizeX);
    }

    static public String decodeGZIP(byte[] bytes) throws IOException {
        GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(bytes));
        InputStreamReader reader = new InputStreamReader(gis);
        BufferedReader in = new BufferedReader(reader);
        StringBuilder result = new StringBuilder();
        String readed;
        while ((readed = in.readLine()) != null) {
            result.append(readed);
        }
        return result.toString();
    }

    public static byte[] getBytes(InputStream inStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = inStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();

        return buffer.toByteArray();
    }

    public static Station.Subtype makeSubtype(JSONObject object) throws JSONException {
        object = object.getJSONObject("station");
        JSONObject id = object.getJSONObject("id");

        String target = id.getString("type");
        String subtypeName = id.getString("tag");

        String type = subtypeName;
        if(object.has("parentId")){
            type = object.getJSONObject("parentId").getString("tag");
        }

        return new Station.Subtype(subtypeName, type, target, object.getString("name"), type, target);
    }

    public static Station.Type makeType(JSONArray object, String target, String type, JSONObject stationsJSON) throws JSONException {
        ArrayList<Station.Subtype> stations = new ArrayList<>();

        String typeView = type;
        if(stationsJSON.has(target + ":" + type))
            typeView = stationsJSON.getJSONObject(target + ":" + type)
                    .getJSONObject("station").getString("name");

        if(stationsJSON.has(target + ":" + type))
            stations.add(new Station.Subtype(stationsJSON.getJSONObject(target + ":" + type), typeView, target));
        else
            stations.add(new Station.Subtype(type, type, target, typeView, typeView, target));
        if(object != null) {
            for (int i = 0; i < object.length(); i++) {
                String name = object.getJSONObject(i).getString("tag");
                if(stationsJSON.has(target + ":" + name))
                    stations.add(new Station.Subtype(stationsJSON.getJSONObject(target + ":" + name), typeView, target));
                else {
                    stations.add(new Station.Subtype(name, type, target, name, typeView, target));
                }
            }
        }
        return new Station.Type(type, target, stations);
    }

    public static Station makeStation(String target, JSONArray types, JSONObject stations) throws JSONException {
        ArrayList<Station.Type> superStations1 = new ArrayList<>();
        if(types != null) {
            for(int i = 0; i < types.length();i++) {
                JSONObject data = types.getJSONObject(i);
                String Target = data.getString("type");
                String Type = data.getString("tag");
                JSONObject station = stations.getJSONObject(Target + ":" + Type).getJSONObject("station");
                Station.Type stations1;
                if(station.has("children"))
                    stations1 = makeType(station.getJSONArray("children"), Target, Type, stations);
                else
                    stations1 = makeType(null, Target, Type, stations);
                superStations1.add(stations1);
            }
        }
        return new Station(target, superStations1);
    }
}
