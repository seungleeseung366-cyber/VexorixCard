# Vexorix Card

Game battle kartu (libGDX, Android). Kartu dibuat/diubah lewat `app/src/main/assets/cards.json`.

## Edit kartu
Tiap kartu: `id, name, cost, attack, hp, effect, value, image`
- `effect`: `none` | `dmg` (damage ke hero lawan) | `heal` (pulihkan hero sendiri) | `draw` (ambil kartu)
- `image`: opsional, mis. `cards/flame_knight.png` (taruh PNG di `app/src/main/assets/cards/`). Kosong = kotak berwarna.
- Untuk multiplayer, kedua pemain harus memakai APK dengan `cards.json` yang sama.

## Multiplayer (Firebase)
1. console.firebase.google.com -> buat proyek -> Build -> Realtime Database -> Create database.
2. Tab Rules, isi:  `{ "rules": { ".read": true, ".write": true } }`  lalu Publish.
3. Salin URL database (mis. `https://xxxx-default-rtdb.firebaseio.com`) ke `app/src/main/assets/config.json` -> `firebaseUrl`.

## Build APK
Push ke GitHub (branch `main`) -> tab Actions -> "Build APK" -> unduh artifact `VexorixCard-apk`.
