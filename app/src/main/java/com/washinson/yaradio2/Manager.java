package com.washinson.yaradio2;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.ViewConfiguration;
import android.webkit.CookieManager;

import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;

import okhttp3.CacheControl;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by User on 10.04.2018.
 */

class Manager {
    static final String trackStarted = "trackStarted";
    static final String dislike = "dislike";
    static final String like = "like";
    static final String unlike = "unlike";
    static final String trackFinished = "trackFinished";
    static final String skip = "skip";

    OkHttpClient okHttpClient;
    CookieJar cookieJar;

    private static Manager instance = null;
    public static String browser = "";
    private Manager(final Context context){
        cookieJar =
                new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(context));
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .cookieJar(cookieJar);
        okHttpClient = builder.build();
    }

    public static void init(Context context){
        instance = new Manager(context);
    }

    public static Manager getInstance() {
        return instance;
    }

    private String orAndVal(Track track) {
        if(track == null) return "";
        return "/" + track.getStation().targetName + "/" + track.getStation().name;
    }

    public ArrayDeque<Track> getTracks(Track track, Track nextTrack, Station.Subtype subtype) throws Exception {
        String url = "https://radio.yandex.ru/api/v2.1/handlers/radio" + orAndVal(track) + "/tracks?queue=";
        if(track.getTitle() != null) {
            url = url + track.getId() + ":" + track.getAlbumId();
            if(nextTrack != null){
                url = url + "," + nextTrack.getId() + ":" + nextTrack.getAlbumId();
            }
        }
        String response = Manager.getInstance().get(
                url, null, track);
        JSONObject tracks = new JSONObject(response);
        JSONArray array = tracks.getJSONArray("tracks");
        ArrayDeque<Track> trackList = new ArrayDeque<>();
        for(int i = 0; i < array.length(); i++) {
            JSONObject trackObject = array.getJSONObject(i);
            if(trackObject.getString("type").equals("track")) {
                trackList.add(new Track(trackObject, subtype));
            }
        }
        return trackList;
    }

    /*public String updateInfo(String moodEnergy, String diversity, String language, Track track, Browser.Auth auth) throws Exception {
        Log.d("Ya", "Update station : " + moodEnergy + " " + diversity + " " +  language);
        String path = "https://radio.yandex.ru/api/v2.1/handlers/radio" + orAndVal(track) + "/settings";
        JSONObject postData = null;
        try {
            postData = new JSONObject();
            postData.put("language", language);
            postData.put("moodEnergy", moodEnergy);
            postData.put("diversity", diversity);
            postData.put("sign", auth.sign);
            postData.put("external-domain", "radio.yandex.ru");
            postData.put("overembed", "no");
        } catch(Exception e){
            e.printStackTrace();
        }

        return post(path, postData, null, "application/x-www-form-urlencoded", track);
    }*/

    public String updateInfo(String moodEnergy, String diversity, String language, Track track, Browser.Auth auth) throws Exception {
        Log.d("Ya", "Update station : " + moodEnergy + " " + diversity + " " +  language);
        String path = "https://radio.yandex.ru/api/v2.1/handlers/radio" + orAndVal(track) + "/settings";
        PostConfig postData = new PostConfig();

        postData.put("language", language);
        postData.put("moodEnergy", moodEnergy);
        postData.put("diversity", diversity);
        postData.put("sign", auth.sign);
        postData.put("external-domain", "radio.yandex.ru");
        postData.put("overembed", "no");

        return post(path, RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), postData.toString())
                , null, "application/x-www-form-urlencoded", track);
    }

    public String sayAboutTrack(Track track, double duration, Browser.Auth auth, String word) {
        try {
            Log.d("Ya", word + " : Track duration: " + duration);
            String path = "https://radio.yandex.ru/api/v2.1/handlers/radio" + orAndVal(track) + "/feedback/" + word + "/"
                    + track.getId() + ":" + track.getAlbumId();

            PostConfig postData = new PostConfig();
            setDefaultPostDataTrack(postData, track, auth);
            if (duration != -1)
                postData.put("totalPlayed", String.valueOf(duration));
            return post(path, RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), postData.toString())
                    , null, "application/x-www-form-urlencoded", null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    Request getGetConnection(String url, Track track) throws IOException {
        Request.Builder builder = new Request.Builder();
        builder.url(url).get();

        setDefaultHeaders(builder);

        builder.addHeader("Referer", "https://radio.yandex.ru" + orAndVal(track));
        builder.addHeader("X-Retpath-Y", "https://radio.yandex.ru" + orAndVal(track));
        builder.addHeader("Cookie",
                getCookiesString(okHttpClient.cookieJar().loadForRequest(HttpUrl.parse("https://radio.yandex.ru"))));
        return builder.build();
    }

    private Request getPostRequest(String url, String contentType, Track track, RequestBody requestBody) throws Exception {
        Request.Builder builder = new Request.Builder();
        builder.url(url).post(requestBody);

        setDefaultHeaders(builder);

        builder.addHeader("Referer", "https://radio.yandex.ru" + orAndVal(track));
        builder.addHeader("X-Retpath-Y", "https://radio.yandex.ru" + orAndVal(track));
        builder.addHeader("Cookie",
                getCookiesString(okHttpClient.cookieJar().loadForRequest(HttpUrl.parse("https://radio.yandex.ru"))));
        builder.addHeader("Origin", "https://radio.yandex.ru");
        builder.addHeader("Content-Type", contentType);

        return builder.build();
    }

    public String get(String url, Request connection, Track track) throws Exception {
        if(connection==null)connection = getGetConnection(url, track);
        HttpRequest httpRequest = new HttpRequest();
        return httpRequest.execute(connection).get();
    }

    public String post(String url, RequestBody body,
                       Request connection, String connectionType, Track track) throws Exception {
        if(connection==null) connection = getPostRequest(url, connectionType, track, body);
        HttpRequest request = new HttpRequest();
        return request.execute(connection).get();
    }

    public void setDefaultPostDataTrack(PostConfig postData, Track track, Browser.Auth auth){
        postData.put("timestamp", String.valueOf(new Date().getTime()));
        postData.put("from", "radio-web-"
                + track.getStation().targetName + "-"+ track.getStation().name +"-direct");
        postData.put("sign", auth.sign);
        postData.put("external-domain", "radio.yandex.ru");
        postData.put("overembed", "no");
        postData.put("batchId", track.getBatchId());
        postData.put("trackId", track.getId());
        postData.put("albumId", track.getAlbumId());
    }

    private String getCookiesString(List<Cookie> cookies) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for(Cookie cookie : cookies){
            builder.append(cookie.name()).append("=").append(cookie.value());
            if(i != cookies.size() - 1) builder.append("; ");
            i++;
        }
        return builder.toString();
    }

    public void setDefaultHeaders(Request.Builder connection) {
        connection.addHeader("Accept", "application/json; q=1.0, text/*; q=0.8, */*; q=0.1");
        connection.addHeader("Accept-Encoding", "gzip, deflate, sdch, br");
        connection.addHeader("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.6,en;q=0.4");
        //connection.addHeader("Cache-Control", "max-age=0");
        connection.cacheControl(CacheControl.parse(
                new Headers.Builder().add("Cache-Control", "max-age=0").build()));
        connection.addHeader("Connection", "keep-alive");
        connection.addHeader("Host", "radio.yandex.ru");
        connection.addHeader("User-Agent", browser);
        connection.addHeader("X-Requested-With", "XMLHttpRequest");
    }

    class HttpRequest extends AsyncTask<Request, Void, String> {
        @Override
        protected String doInBackground(Request... httpURLConnections) {
            Request connection = httpURLConnections[0];
            String res = null;
            Response response = null;
            try {
                response = okHttpClient.newCall(connection).execute();
                byte[] q = Utils.getBytes(response.body().byteStream());
                String contentEncoding = response.header("Content-Encoding");

                if(contentEncoding != null && contentEncoding.equals("gzip"))
                    res = Utils.decodeGZIP(q);
                else
                    res = new String(q);

            } catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                if(response != null)
                    response.close();
            }
            return res;
        }
    }

        /*HttpURLConnection getGetConnection(String url, Track track) throws IOException {
        URL uri = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoInput(true);
        connection.setUseCaches(true);

        setDefaultHeaders(connection);

        String cookies = getCookiesString();
        connection.setRequestProperty("Referer", "https://radio.yandex.ru" + orAndVal(track));
        connection.setRequestProperty("X-Retpath-Y", "https://radio.yandex.ru" + orAndVal(track));
        connection.setRequestProperty("Cookie", cookies);
        return connection;
    }

    private HttpURLConnection getPostRequest(String url, String contentType, Track track) throws Exception {
        URL uri = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) uri.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setUseCaches(true);

        setDefaultHeaders(connection);

        String cookies = getCookiesString();
        connection.setRequestProperty("Referer", "https://radio.yandex.ru" + orAndVal(track));
        connection.setRequestProperty("X-Retpath-Y", "https://radio.yandex.ru" + orAndVal(track));
        connection.setRequestProperty("Cookie", cookies);
        connection.setRequestProperty("Origin", "https://radio.yandex.ru");
        connection.setRequestProperty("Content-Type", contentType);

        return connection;
    }

    public String get(String url, HttpURLConnection connection, Track track) throws Exception {
        if(connection==null)connection = getGetConnection(url, track);
        HttpRequest request = new HttpRequest();
        return request.execute(connection).get();
    }

    public String post(String url, JSONObject body,
                       HttpURLConnection connection, String connectionType, Track track) throws Exception {
        if(connection==null) connection = getPostRequest(url, connectionType, track);
        HttpRequest request = new HttpRequest(body);
        return request.execute(connection).get();
    }*/

    //public void setDefaultHeaders(HttpURLConnection connection) {
    //    connection.setRequestProperty("Accept", "application/json; q=1.0, text/*; q=0.8, */*; q=0.1");
    //    connection.setRequestProperty("Accept-Encoding", "gzip, deflate, sdch, br");
    //    connection.setRequestProperty("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.6,en;q=0.4");
    //    connection.setRequestProperty("Cache-Control", "max-age=0");
    //    connection.setRequestProperty("Connection", "keep-alive");
    //    connection.setRequestProperty("Host", "radio.yandex.ru");
    //    connection.setRequestProperty("User-Agent", browser);
    //    connection.setRequestProperty("X-Requested-With", "XMLHttpRequest");
    //}

    /*class HttpRequest extends AsyncTask<HttpURLConnection, Void, String> {
        JSONObject requestBody = null;

        HttpRequest() {}

        HttpRequest(JSONObject requestBody)
        {
            this.requestBody = requestBody;
        }

        @Override
        protected String doInBackground(HttpURLConnection... httpURLConnections) {
            HttpURLConnection connection = httpURLConnections[0];
            String res = null;
            try {
                if(requestBody != null)
                {
                    connection.setDoOutput(true);

                    OutputStream os = connection.getOutputStream();

                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(os, "UTF-8"));
                    writer.write(Utils.getPostDataString(requestBody));
                    writer.flush();
                    writer.close();
                    os.close();
                }
                connection.connect();
                byte[] q = Utils.getBytes(connection.getInputStream());
                String contentEncoding = connection.getHeaderField("Content-Encoding");

                if(contentEncoding != null && contentEncoding.equals("gzip"))
                    res = Utils.decodeGZIP(q);
                else
                    res = new String(q);
            } catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                connection.disconnect();
            }
            return res;
        }
    }*/
}
