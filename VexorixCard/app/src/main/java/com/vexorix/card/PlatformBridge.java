package com.vexorix.card;

/** Jembatan dari game (libGDX) ke fitur Android. */
public interface PlatformBridge {
    /** Membuka galeri untuk memilih foto profil. Hasilnya disimpan sebagai profile.png lalu game diberi tahu. */
    void pickImage();
}
