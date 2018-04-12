package com.washinson.yaradio2;

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
import java.util.zip.GZIPInputStream;

/**
 * Created by User on 10.04.2018.
 */

public class Utils {
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

    public static String getPostDataString(JSONObject params) throws Exception {

        StringBuilder result = new StringBuilder();
        boolean first = true;

        Iterator<String> itr = params.keys();

        while(itr.hasNext()){

            String key= itr.next();
            Object value = params.get(key);

            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(key, "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(value.toString(), "UTF-8"));

        }
        return result.toString();
    }

    public static Station.Type makeType(JSONArray object, String target, String type) throws JSONException {
        ArrayList<Station.Subtype> stations = new ArrayList<>();
        stations.add(new Station.Subtype(type, type, target));
        if(object != null) {
            for (int i = 0; i < object.length(); i++) {
                stations.add(new Station.Subtype(object.getJSONObject(i).getString("tag"), type, target));
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
                JSONObject station = stations.getJSONObject(Target + ":" + Type);
                Station.Type stations1 = null;
                if(station.getJSONObject("station").has("children"))
                    stations1 = makeType(station.getJSONObject("station").getJSONArray("children"), Target, Type);
                else
                    stations1 = makeType(null, Target, Type);
                superStations1.add(stations1);
            }
        }
        return new Station(target, superStations1);
    }
}
