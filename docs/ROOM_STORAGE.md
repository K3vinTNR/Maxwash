# Penyimpanan lokal MAXWASH

Implementasi pada 10 Oktober 2026 mengganti `MockRepository` dengan Room 2.8.5 dan KSP. Semua data yang ditampilkan berasal dari database `maxwash.db` di penyimpanan privat aplikasi.

## Data dan alur

`MaxwashApplication` menyediakan satu repository dan satu instance database. `MaxwashViewModel` memuat database sebelum navigasi, memulihkan sesi, dan mengamati perubahan tabel melalui Room Flow. Operasi tulis menggunakan coroutine dan transaksi; layar menampilkan kegagalan penyimpanan dan hanya melanjutkan login/registrasi/simpan setelah transaksi berhasil.

| Tabel | Isi |
| --- | --- |
| customers | Identitas, alamat, preferensi, outlet langganan, membership/poin, hash dan salt password |
| orders | Pesanan milik customer, layanan/outlet, berat, waktu, status, biaya dan estimasi |
| status_events | Riwayat tahapan setiap pesanan |
| notifications | Pesan terkait pesanan dan status dibaca |
| outlets | Nama, alamat, jam operasional dan jarak contoh |
| services | Nama layanan, tarif per kilogram dan durasi |
| fragrances / promotions | Pilihan parfum dan informasi promo |
| app_session | Akun aktif dan outlet yang dipilih |
| metadata | Penanda bootstrap katalog |

Foreign key dan indeks menjaga relasi customer–order–notifikasi serta outlet/layanan. Email dinormalisasi ke lowercase; email dan nomor HP memiliki unique index. Query notifikasi dibatasi ke pesanan akun aktif. Akun hanya dibuat melalui registrasi, dengan ID sendiri dan daftar pesanan/notifikasi kosong.

`Catat Pesanan` mengambil tarif dan durasi dari Room, memvalidasi berat, menghitung total, lalu menyimpan pesanan, event awal dan notifikasi dalam satu transaksi. Ini pencatatan lokal; belum mengirim pesanan ke outlet. Status selanjutnya tetap data tracking tersimpan, tanpa simulasi perubahan status otomatis.

Home dan list menghitung jumlah pesanan, berat dan jumlah aktif dari data akun. Ringkasan bulan ini menggunakan timestamp pesanan dan batas bulan Asia/Jakarta. Detail, nota, aktivitas dan notifikasi memilih ID dari database, tanpa ID contoh yang ditanam di layar.

## Bootstrap dan persistensi

`app/src/main/assets/catalog_data.json` hanya berisi outlet, layanan, parfum dan promo. Repository mengimpornya **satu kali**, bersama penanda `metadata.initialized`, dalam transaksi. Tidak ada akun, pesanan, riwayat atau notifikasi bawaan. Sesudah itu layar hanya membaca Room. Asset hanya menjadi sumber bootstrap; mengeditnya setelah database dibuat tidak mengubah database pengguna.

Pembaruan 11 Oktober 2026 menghapus login demo dan data akun demo versi lama saat `initialize()`. ID dibaca dari metadata lama `demoCustomerId`, sehingga perubahan nama/email akun lama tidak menghindari pembersihan. Penghapusan customer menggunakan foreign key CASCADE untuk menghapus pesanan, riwayat dan notifikasi terkait. Jika sesi aktif milik akun tersebut, sesi dikosongkan; akun registrasi lain, data dan sesi aktifnya tetap tersimpan. Metadata lama dihapus dalam transaksi yang sama. Schema tetap versi 1 karena struktur tabel tidak berubah.

Registrasi mendukung beberapa akun pada satu perangkat. Password akun disimpan sebagai PBKDF2-HMAC-SHA256, 120.000 iterasi, salt acak 16 byte dan hash 256 bit. Tidak ada kredensial bawaan dalam asset; password pengguna tidak disimpan sebagai plaintext.

Profil, preferensi, notifikasi dibaca, outlet pilihan, pesanan dan sesi bertahan setelah database/proses dibuka ulang. Logout mengosongkan sesi dan back stack, tanpa menghapus akun. Data prototype lama hanya berada di memori sehingga tidak ada database lama yang bisa dikonversi; perubahan sesi lama tidak dapat dipulihkan setelah proses berhenti.

Database mulai pada schema versi 1. Schema hasil KSP disimpan di `app/schemas/org.umn.maxwash.data.local.MaxwashDatabase/1.json`. Tidak memakai destructive migration atau akses database di main thread. Perubahan schema berikutnya perlu menaikkan versi dan menambahkan migration yang mempertahankan data.

QR tiruan, peta ilustrasi, jarak/estimasi perjalanan simulasi, tombol refresh tanpa perubahan data dan tombol kontak demo dihapus. Layar lokasi menampilkan alamat serta jam outlet tersimpan; petunjuk arah membuka URL Google Maps melalui ACTION_VIEW. Belum ada backend, sinkronisasi, push notification atau transaksi pembayaran. UI copy, warna, ikon dan enum tahapan merupakan definisi aplikasi, sedangkan isi akun/katalog/transaksi disimpan di Room.

## Verifikasi

Hasil terbaru pada 11 Oktober 2026 setelah penghapusan demo:

- APK debug, APK tes, unit test dan lint: **BUILD SUCCESSFUL** dalam **1 menit 27 detik**, 85 task. Command: `:app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest :app:lintDebug --max-workers=1 --console=plain --no-daemon`.
- Unit: **10 tes, 0 kegagalan, 0 error**.
- Emulator Pixel 10 Pro XL, APK akhir dipasang ulang tanpa menghapus data aplikasi: **OK (17 tests)** dalam **85,355 detik**, terdiri dari **8 UI + 9 repository**. Log: [verification/account-cleanup-instrumentation.txt](verification/account-cleanup-instrumentation.txt).
- Regression mencakup bootstrap tanpa akun/transaksi bawaan, penghapusan akun lama setelah profilnya diubah, CASCADE pesanan/riwayat/notifikasi, reset sesi akun lama, preservasi akun registrasi/data/sesi aktif, metadata lama tanpa customer, serta inisialisasi berulang.
- UI login hanya menawarkan autentikasi biasa dan registrasi. Profil tidak memuat QR tiruan; detail tidak memuat kontak demo. Tes petunjuk arah memeriksa ACTION_VIEW ke URL peta dengan activity monitor, tanpa membuka aplikasi eksternal selama tes.
- Lint: **0 error / 13 warning** (label Activity, versi dependensi, resource warna template, saran ekstensi KTX untuk Uri). Tidak ada rule yang disembunyikan.
- Isi APK diperiksa: hanya `assets/catalog_data.json`; tidak memuat asset data demo, kredensial demo, tombol demo, loader seed lama atau peta tiruan. Screenshot Login, Profil dan Lokasi diperiksa di `output/screenshots/account-cleanup/`.

Percobaan emulator awal meluluskan 8 tes UI, tetapi JUnit menolak setup repository yang tidak mengembalikan void. Return type `initialize()` ditegaskan sebagai `Unit`; build dan seluruh 17 tes di atas dijalankan kembali setelah perbaikan.

Riwayat hasil pada 10 Oktober 2026 sebelum penghapusan demo, Windows/JBR 25/SDK 37 dan emulator Pixel 10 Pro XL Android 17:

- APK debug dan APK tes: **BUILD SUCCESSFUL**. Build akhir + lint selesai dalam 1 menit 29 detik.
- Unit: **10 tests, 0 failures, 0 errors**, mencakup validasi form, filter/pencarian, progress, hash password dan batas bulan WIB. Laporan: `app/build/reports/tests/testDebugUnitTest/index.html`.
- Emulator, runner ADB penuh pada APK akhir: **OK (13 tests)** dalam **70,502 detik**, terdiri dari **7 tes UI + 6 tes repository**. Log: [verification/room-instrumentation.txt](verification/room-instrumentation.txt).
- Tes repository menutup dan membuka ulang database di disk untuk memverifikasi akun, hash password, sesi, profil/preferensi, outlet, pesanan dan read state. Juga memverifikasi seed tidak diulang, email/HP duplikat ditolak, akun terisolasi, transaksi invalid tidak mengubah data, serta perubahan katalog dipancarkan ke observer.
- Tes UI mencakup registrasi, profil/preferensi/logout, lokasi, list/detail/filter/back, notifikasi/read state, Activity recreation dan pencatatan pesanan akun baru. UI/navigation sesudah transaksi dijalankan pada main thread.
- Lint: **0 error / 12 warning**, tanpa menyembunyikan rule. Warning terkait label Activity, versi dependensi/tooling dan resource warna template yang tidak terpakai. Laporan: `app/build/reports/lint-results-debug.html`.

Tes UI memakai database in-memory pada `TestMaxwashApplication`, sedangkan tes repository membuat database sementara dan benar-benar menutup/membukanya ulang. Keduanya tidak mereset database akun aplikasi yang terpasang. Pengujian ini tidak mencakup perangkat fisik atau penghentian proses Android saat sebuah transaksi sedang berjalan.

Riwayat perbaikan: lint awal mengalami error internal analisis, lalu berhasil saat dijalankan dengan satu worker dan sumber tidak diubah selama analisis. `connectedDebugAndroidTest` terhenti dengan 0 tes karena pemasangan ulang APK menghentikan proses instrumentasi; laporan connected itu bukan hasil lulus. Runner ADB berikutnya menemukan 6 kegagalan navigasi pada main thread, yang diperbaiki dengan dispatcher Main untuk coroutine UI. Runner ADB terakhir di atas menjalankan seluruh 13 tes dan lulus.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --max-workers=1 --console=plain --no-daemon
.\gradlew.bat :app:connectedDebugAndroidTest --max-workers=1 --console=plain --no-daemon

# Runner yang dipakai untuk hasil 13 tes di atas, setelah APK terpasang:
adb -s emulator-5554 shell am instrument -w -r org.umn.maxwash.test/org.umn.maxwash.MaxwashTestRunner
```

Panduan implementasi: [Room AndroidX](https://developer.android.com/jetpack/androidx/releases/room), [built-in Kotlin / KSP](https://developer.android.com/build/migrate-to-built-in-kotlin), dan [KSP 2.3.6](https://github.com/google/ksp/releases/tag/2.3.6).
