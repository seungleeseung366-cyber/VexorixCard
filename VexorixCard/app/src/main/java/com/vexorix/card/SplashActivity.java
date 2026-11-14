package com.vexorix.card;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.VideoView;

/** Layar awal: memutar video res/raw/intro.mp4 lalu membuka game. Ketuk layar untuk skip. */
public class SplashActivity extends Activity {
    private boolean done = false;
    private VideoView video;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int id = getResources().getIdentifier("intro", "raw", getPackageName());
        if (id == 0) { // tidak ada video -> langsung ke game
            next();
            return;
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        video = new VideoView(this);
        root.addView(video, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        setContentView(root);

        video.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + id));
        video.setOnCompletionListener(mp -> next());
        video.setOnErrorListener((mp, what, extra) -> { next(); return true; });
        root.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { next(); }
        });
        video.start();
    }

    private void next() {
        if (done) return;
        done = true;
        startActivity(new Intent(this, AndroidLauncher.class));
        finish();
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!done) { // pengguna keluar saat video berjalan
            done = true;
            finish();
        }
    }
}
