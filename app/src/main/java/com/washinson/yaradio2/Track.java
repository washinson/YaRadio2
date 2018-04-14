package com.washinson.yaradio2;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by User on 10.04.2018.
 */

public class Track {
    Station.Subtype station;

    private int id;
    private int albumId;
    private String album;
    private String title;
    private String batchId;
    private String artist;
    private String cover;
    private long durationMs;
    private boolean liked, finished = false;

    public long getDurationMs() { return durationMs; }

    public String getAlbum() { return album; }

    public Station.Subtype getStation() { return station; }

    public String getCover() { return cover; }

    public boolean isFinished() { return finished; }

    public Track(JSONObject jsonObject, Station.Subtype station) {
        try {
            this.station = station;
            if(jsonObject == null) return;
            JSONObject track = jsonObject.getJSONObject("track");
            id = track.getInt("id");
            albumId = track
                    .getJSONArray("albums")
                    .getJSONObject(0)
                    .getInt("id");
            album = track
                    .getJSONArray("albums")
                    .getJSONObject(0)
                    .getString("title");
            batchId = track.getString("batchId");
            title = track.getString("title");
            cover = track
                    .getString("coverUri");
            cover = "https://" + cover;

            durationMs = track.getLong("durationMs");

            JSONArray artists = track.getJSONArray("artists");
            StringBuilder artistNameBuilder = new StringBuilder();
            for (int i = 0; i < artists.length(); i++) {
                artistNameBuilder.append(artists.getJSONObject(i).getString("name"));
                if (i != artists.length() - 1) {
                    artistNameBuilder.append(", ");
                }
            }

            artist = artistNameBuilder.toString();

            liked = jsonObject.getBoolean("liked");
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setFinished(boolean finished) { this.finished = finished; }

    public void setLiked(boolean liked) { this.liked = liked; }

    public boolean isLiked() { return liked; }

    public String getTitle() { return title; }

    public String getArtist() { return artist; }

    public String getId() {
        return String.valueOf(id);
    }

    public String getAlbumId() {
        return String.valueOf(albumId);
    }

    public String getBatchId() {
        return batchId;
    }

    @Override
    public String toString() {
        return "Track{" +
                "id=" + id +
                ", albumId=" + albumId +
                ", title='" + title + '\'' +
                ", batchId='" + batchId + '\'' +
                ", artist='" + artist + '\'' +
                ", cover='" + cover + '\'' +
                ", liked=" + liked +
                '}';
    }
}
