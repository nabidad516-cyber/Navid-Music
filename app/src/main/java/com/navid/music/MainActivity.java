package com.navid.music;

import android.Manifest;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.MediaStore;
import android.content.ContentUris;
import android.net.Uri;
import android.media.MediaPlayer;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;

public class MainActivity extends Activity {

    LinearLayout root, songsLayout;
    TextView nowPlaying;
    ArrayList<String> titles = new ArrayList<>();
    ArrayList<Uri> songUris = new ArrayList<>();
    MediaPlayer player;
    int current = -1;
    int themeIndex = 0;
    int[] backgrounds = {
        0xFF10121C, 0xFF19130F, 0xFF071A18, 0xFF1A1022
    };
    int[] accents = {
        0xFF8B5CF6, 0xFFFF8A3D, 0xFF26D7AE, 0xFFFF4FA3
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showPermission();
    }

    void showPermission() {
        String permission = Build.VERSION.SDK_INT >= 33
            ? Manifest.permission.READ_MEDIA_AUDIO
            : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (Build.VERSION.SDK_INT >= 23 &&
            checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{permission}, 10);
        } else {
            buildUI();
            scanSongs();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
        String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 10 && grantResults.length > 0 &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            buildUI();
            scanSongs();
        } else {
            buildUI();
            nowPlaying.setText("برای نمایش آهنگ‌ها اجازه دسترسی بده");
        }
    }

    void buildUI() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(18, 20, 18, 12);
        root.setBackgroundColor(backgrounds[themeIndex]);

        TextView title = new TextView(this);
        title.setText("♫  NAVID MUSIC");
        title.setTextColor(accents[themeIndex]);
        title.setTextSize(27);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 12, 0, 20);
        root.addView(title);

        nowPlaying = new TextView(this);
        nowPlaying.setText("آماده پخش موسیقی");
        nowPlaying.setTextColor(Color.WHITE);
        nowPlaying.setTextSize(16);
        nowPlaying.setGravity(Gravity.CENTER);
        nowPlaying.setPadding(8, 12, 8, 16);
        root.addView(nowPlaying);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        String[] labels = {"⏮", "▶ / ⏸", "⏭", "🎚"};
        for (int i = 0; i < labels.length; i++) {
            final int action = i;
            Button b = new Button(this);
            b.setText(labels[i]);
            b.setTextColor(Color.WHITE);
            b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(accents[themeIndex]));
            controls.addView(b, new LinearLayout.LayoutParams(0, 58, 1));
            b.setOnClickListener(v -> {
                if (action == 0) playSong(Math.max(0, current - 1));
                if (action == 1) togglePlay();
                if (action == 2) playSong(Math.min(titles.size() - 1, current + 1));
                if (action == 3) showEqualizer();
            });
        }
        root.addView(controls);

        Button themeButton = new Button(this);
        themeButton.setText("🎨 تغییر تم");
        root.addView(themeButton);
        themeButton.setOnClickListener(v -> {
            themeIndex = (themeIndex + 1) % backgrounds.length;
            root.setBackgroundColor(backgrounds[themeIndex]);
            title.setTextColor(accents[themeIndex]);
            for (int i = 0; i < controls.getChildCount(); i++) {
                ((Button) controls.getChildAt(i)).setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(accents[themeIndex]));
            }
        });

        TextView heading = new TextView(this);
        heading.setText("آهنگ‌های گوشی");
        heading.setTextColor(Color.WHITE);
        heading.setTextSize(19);
        heading.setPadding(0, 18, 0, 8);
        root.addView(heading);

        ScrollView scroll = new ScrollView(this);
        songsLayout = new LinearLayout(this);
        songsLayout.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(songsLayout);
        root.addView(scroll, new LinearLayout.LayoutParams(
            -1, 0, 1));

        setContentView(root);
    }

    void scanSongs() {
        titles.clear();
        songUris.clear();
        songsLayout.removeAllViews();

        Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        try (Cursor cursor = getContentResolver().query(
            collection, projection, selection, null,
            MediaStore.Audio.Media.TITLE + " ASC")) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
                int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String name = cursor.getString(titleCol);
                    String artist = cursor.getString(artistCol);
                    if (name == null) name = "آهنگ بدون نام";
                    if (artist == null || artist.equals("<unknown>")) artist = "هنرمند ناشناس";

                    titles.add(name);
                    songUris.add(ContentUris.withAppendedId(collection, id));

                    TextView item = new TextView(this);
                    item.setText("♫  " + name + "\n     " + artist);
                    item.setTextColor(Color.WHITE);
                    item.setTextSize(16);
                    item.setPadding(14, 14, 14, 14);
                    songsLayout.addView(item);
                    final int index = titles.size() - 1;
                    item.setOnClickListener(v -> playSong(index));
                }
            }
        } catch (Exception e) {
            nowPlaying.setText("خطا در خواندن فهرست آهنگ‌ها");
        }

        if (titles.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("آهنگی پیدا نشد؛ فایل‌های موسیقی را بررسی کن");
            empty.setTextColor(Color.LTGRAY);
            empty.setPadding(8, 20, 8, 20);
            songsLayout.addView(empty);
        } else {
            nowPlaying.setText(titles.size() + " آهنگ پیدا شد");
        }
    }

    void playSong(int index) {
        if (index < 0 || index >= songUris.size()) return;
        try {
            if (player != null) {
                player.release();
                player = null;
            }
            current = index;
            player = new MediaPlayer();
            player.setDataSource(this, songUris.get(index));
            player.setOnPreparedListener(mp -> {
                mp.start();
                nowPlaying.setText("در حال پخش:\n" + titles.get(current));
            });
            player.setOnCompletionListener(mp -> {
                if (current + 1 < songUris.size()) playSong(current + 1);
            });
            player.prepareAsync();
        } catch (Exception e) {
            nowPlaying.setText("پخش این آهنگ ممکن نیست");
        }
    }

    void togglePlay() {
        if (player == null) {
            playSong(0);
        } else if (player.isPlaying()) {
            player.pause();
            nowPlaying.setText("پخش متوقف شد");
        } else {
            player.start();
            nowPlaying.setText("در حال پخش:\n" + titles.get(current));
        }
    }

    void showEqualizer() {
        if (player == null) {
            nowPlaying.setText("اول یک آهنگ پخش کن");
            return;
        }
        try {
            android.media.audiofx.Equalizer eq =
                new android.media.audiofx.Equalizer(0, player.getAudioSessionId());
            eq.setEnabled(true);
            Toast.makeText(this,
                "اکولایزر فعال شد؛ تنظیمات پیشرفته در مرحله بعد",
                Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this,
                "اکولایزر در این دستگاه پشتیبانی نشد",
                Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}
