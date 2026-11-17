package com.vexorix.card;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AndroidLauncher extends AndroidApplication implements PlatformBridge {
    private static final int REQ_PICK = 4242;
    private VexorixGame game;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        game = new VexorixGame(this);
        initialize(game, config);
    }

    @Override
    public void pickImage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("image/*");
                startActivityForResult(Intent.createChooser(i, "Pilih foto profil"), REQ_PICK);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_PICK && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            try {
                saveAvatar(data.getData());
                game.onProfilePhotoChanged();
            } catch (Exception e) {
                // gagal membaca foto -> abaikan
            }
        }
    }

    /** Potong persegi dari tengah, bulatkan, simpan 256x256 ke profile.png di folder file aplikasi. */
    private void saveAvatar(Uri uri) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        InputStream in1 = getContentResolver().openInputStream(uri);
        BitmapFactory.decodeStream(in1, null, bounds);
        if (in1 != null) in1.close();

        int sample = 1;
        while (bounds.outWidth / (sample * 2) >= 512 && bounds.outHeight / (sample * 2) >= 512) sample *= 2;
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inSampleSize = sample;
        InputStream in2 = getContentResolver().openInputStream(uri);
        Bitmap src = BitmapFactory.decodeStream(in2, null, opt);
        if (in2 != null) in2.close();
        if (src == null) return;

        int size = Math.min(src.getWidth(), src.getHeight());
        int x = (src.getWidth() - size) / 2, y = (src.getHeight() - size) / 2;
        Bitmap out = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        c.drawCircle(128f, 128f, 128f, p);
        p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        c.drawBitmap(src, new Rect(x, y, x + size, y + size), new Rect(0, 0, 256, 256), p);

        FileOutputStream fo = new FileOutputStream(new File(getFilesDir(), "profile.png"));
        out.compress(Bitmap.CompressFormat.PNG, 100, fo);
        fo.close();
    }
}
