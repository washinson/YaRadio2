package com.washinson.yaradio2;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import com.google.android.exoplayer2.extractor.MpegAudioHeader;
import com.google.android.exoplayer2.extractor.ts.MpegAudioReader;
import com.google.android.exoplayer2.util.Util;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;
import com.mpatric.mp3agic.app.Mp3Pics;
import com.squareup.picasso.Picasso;

import org.cmc.music.common.ID3WriteException;
import org.cmc.music.metadata.ImageData;
import org.cmc.music.metadata.MusicMetadata;
import org.cmc.music.metadata.MusicMetadataSet;
import org.cmc.music.myid3.MyID3;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.flac.metadatablock.MetadataBlockDataPicture;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldDataInvalidException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.ID3v1Tag;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.jaudiotagger.tag.images.AndroidArtwork;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Vector;


import static android.os.Environment.DIRECTORY_MUSIC;

public class Mp3Downloader {
    static Mp3Downloader instance = null;
    PlayerService context;
    HashMap<Long, Track> queue = new HashMap<>();
    BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            DownloadManager downloadManager =
                    (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

            if(id == -1) return;

            Track track = queue.get(id); queue.remove(id);
            if(downloadManager == null) return;
            Uri uri = downloadManager.getUriForDownloadedFile(id);
            new TT(track, uri).start();
        }
    };

    private class TT extends Thread {
        Track track;
        Uri uri;

        public TT(Track track, Uri uri) {
            this.track = track;
            this.uri = uri;
        }

        @Override
        public void run() {
            MP3File mp3 = null;

            String filePath;

            if (uri != null && "content".equals(uri.getScheme())) {
                Cursor cursor = context.getContentResolver().query(uri, new String[] { MediaStore.MediaColumns.DATA }, null, null, null);
                if(cursor == null) return;
                cursor.moveToFirst();
                filePath = cursor.getString(0);
                cursor.close();
            } else {
                filePath = uri.getPath();
            }

            try {
                mp3 = (MP3File) AudioFileIO.read(new File(filePath));
            } catch (CannotReadException | TagException | IOException | ReadOnlyFileException | InvalidAudioFrameException e) {
                return;
            }

            ID3v24Tag tag = new ID3v24Tag();

            try {
                tag.setField(FieldKey.ARTIST, track.getArtist());
                tag.setField(FieldKey.TITLE, track.getTitle());
                tag.setField(FieldKey.ALBUM, track.getAlbum());
            } catch (FieldDataInvalidException e) {
                return;
            }

            try {
                Bitmap bmp = Picasso.get().load(Utils.getCover(600, track.getCover())).get();
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                byte[] byteArray = stream.toByteArray();
                bmp.recycle();

                Artwork artwork = ArtworkFactory.getNew();
                artwork.setPictureType(3);
                artwork.setDescription("");
                artwork.setMimeType("image/jpeg");
                artwork.setBinaryData(byteArray);

                tag.setField(artwork);
            } catch (IOException | FieldDataInvalidException e) {
                e.printStackTrace();
            }

            mp3.setTag(tag);

            try {
                AudioFileIO.write(mp3);
            } catch (CannotWriteException ignore) {
            }
        }
    }
    private Mp3Downloader(PlayerService context) {
        this.context = context;
        this.context.registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    public static void init(PlayerService context){
        instance = new Mp3Downloader(context);
    }

    public static Mp3Downloader getInstance() {
        return instance;
    }

    void loadTrack() throws Exception {
        if(context == null || context.getTrack() == null ||
                context.getTrack().getQualityInfo() == null ||
                context.getTrack().getQualityInfo().qualities.isEmpty())
            return;
        //String path = context.getTrack().getQualityInfo().byQuality("mp3_192");
        Track track = context.getTrack();

        File file = new File((Environment.getExternalStoragePublicDirectory(DIRECTORY_MUSIC).getAbsolutePath()
                + "/YaRadio/" + track.getStation().name + "/" + track.getTitle() + " - " + track.getArtist() + ".mp3").replace(" ", "_"));
        if(file.exists()) {
            throw new Exception(context.getString(R.string.already_yet));
        }

        String path = "https://api.music.yandex.net/tracks/" + track.getId() + "/download-info";

        String json = Manager.getInstance().get(path, null, track);

        JSONObject jsonObject = new JSONObject(json);

        PlayerService.QualityInfo qualityInfo = PlayerService.QualityInfo.fromJSON(jsonObject);

        track.setQualityInfo(qualityInfo);

        String src = qualityInfo.byQuality("mp3_192") + "&format=json";

        okhttp3.Request.Builder builder = new okhttp3.Request.Builder().get().url(src);
        builder.addHeader("Host", "storage.mds.yandex.net");
        Manager.getInstance().setDefaultHeaders(builder);

        String result = Manager.getInstance().get(src, builder.build(), track);
        JSONObject downloadInformation = new JSONObject(result);
        PlayerService.DownloadInfo info = PlayerService.DownloadInfo.fromJSON(downloadInformation);
        String downloadPath = info.getSrc();

        DownloadManager downloadManager =
                (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(android.net.Uri.parse(downloadPath));
        request.setTitle(track.getTitle());
        request.setDescription(track.getArtist());
        request.setDestinationUri(android.net.Uri.fromFile(file));
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setVisibleInDownloadsUi(true);
        //request.setDestinationInExternalFilesDir(PlayerActivity.this,
        //        DIRECTORY_MUSIC, file.getPath());
        if (downloadManager != null) {
            queue.put(downloadManager.enqueue(request), track);
        }
    }
}
