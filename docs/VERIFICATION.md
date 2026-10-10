# Verifikasi MAXWASH

Laporan ini adalah hasil prototipe sebelum migrasi Room. Untuk perilaku penyimpanan dan hasil tes versi saat ini, lihat [ROOM_STORAGE.md](ROOM_STORAGE.md). Klaim reset mock setelah restart di laporan historis ini sudah tidak berlaku pada aplikasi Room.

Pengujian nyata pada 10 Oktober 2026, Windows, Android Studio JBR 25, Gradle 9.5, SDK 37. Hasil di bawah membedakan build, unit test, dan pengujian aplikasi di emulator.

## Build dan analisis statis

Command terakhir setelah penyesuaian dropdown parfum:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest --offline --console=plain --no-daemon
```

Hasil: **BUILD SUCCESSFUL**, 7 menit 30 detik; 81 task, 19 dieksekusi, 62 up-to-date. APK debug dan APK instrumented test dibuat. APK akhir benar-benar dipasang (`Success`) dan dijalankan dengan `am start -W` (`Status: ok`, cold launch 41,033 ms pada emulator). APK tes juga dipasang (`Success`).

- Unit test: **14 tests, 0 failures, 0 errors, 0 skipped**, dari `app/build/test-results/testDebugUnitTest/TEST-org.umn.maxwash.MaxwashUnitTest.xml`.
- Cakupan unit: validasi wajib/numerik/email/password, pencarian/filter, lookup ID, enam status dan timeline, relasi outlet/notifikasi, read state, edit profil, login/register/logout, refresh lokasi.
- Lint: **0 error, 12 warning, 1 note** dari SARIF dengan level default tiap rule. Warning: activity label redundan, informasi versi dependensi lebih baru, tujuh warna template yang tidak terpakai. Note: `AutoboxingStateCreation` untuk counter refresh lokasi. Tidak ada warning yang disembunyikan; dependensi awal proyek tidak di-upgrade hanya untuk menghilangkan notice versi.
- Laporan unit: `app/build/reports/tests/testDebugUnitTest/index.html`.
- Laporan lint: `app/build/reports/lint-results-debug.html`.

Resolusi offline pertama gagal karena artefak lint material-icons belum tersedia dalam cache. Dependensi kemudian berhasil diunduh melalui build online; build akhir menggunakan offline cache. Tidak ada perubahan backend atau layanan nyata.

Sesudah perbaikan penantian snackbar pada tes Location, `assembleDebug` dan `assembleDebugAndroidTest` kembali **BUILD SUCCESSFUL** dalam 4 menit 2 detik, 67 task (9 executed, 58 up-to-date). Digunakan override sementara JVM 768 MB, compiler Kotlin in-process, satu worker dan no-daemon. Satu percobaan command sebelumnya gagal karena argumen `-P` tidak dikutip dengan benar di PowerShell; command dikoreksi. Perubahan sesudah full check hanya tes Location dan whitespace MainActivity; tidak ada perubahan perilaku aplikasi.

## Instrumented UI test

Runner yang benar-benar digunakan:

```powershell
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w -r -e class org.umn.maxwash.MaxwashFlowTest org.umn.maxwash.test/androidx.test.runner.AndroidJUnitRunner
```

Full suite final: **OK (6 tests)**, **0 failures**, durasi **121,208 detik**, dari `tmp/ui-tests-verified-final.txt`. Semua assertion registrasi, profil, lokasi/Direction, pesanan, notifikasi dan Activity recreation benar-benar dijalankan dan lulus. APK aplikasi final dan APK tes final terpasang (`Success`). Ini hasil satu full suite yang sama, bukan penggabungan hasil run terpisah.

Riwayat kegagalan tetap dicatat: full suite sebelumnya lulus 5/6 (176,209 detik), dengan klik Direction tertutup snackbar; rerun khusus juga pernah timeout saat capture redraw 2 detik dan saat menunggu snackbar. Fixture kemudian memajukan jam virtual Compose 6 detik sebelum memeriksa snackbar hilang. Capture gambar Compose memiliki fallback ke screenshot native bila API redraw gagal; fallback hanya untuk bukti gambar dan tidak melewati assertion fungsional. APK tes dengan perbaikan ini dibangun sukses dalam 1 menit 47 detik (50 task, 4 executed, 46 up-to-date), kemudian full suite final di atas dijalankan. Tidak ada klaim bahwa percobaan sebelumnya lulus.

Enam alur yang diuji:

1. Register: empty/invalid submit diblokir; penolakan nomor nonnumerik, email salah dan konfirmasi berbeda; registrasi valid tercermin di Home/Profile.
2. Profile: edit nama, parfum/toggle, simpan dan navigasi ulang, QR dialog, logout/back stack.
3. Location: pilih Senopati, refresh jarak, dialog Direction lokal.
4. Orders: ID MW-1001 dan MW-1003 menghasilkan detail berbeda; filter selesai dan Back mempertahankan daftar.
5. Notifications: membuka order sesuai ID, read state, mark all, empty unread filter.
6. Activity recreation: filter dan sesi bertahan; pencarian kosong menghasilkan empty state.

Emulator: AVD View_Output / Pixel 10 Pro XL, image API 37.1, x86_64; read-only, headless, GPU host, viewport 840×1860 px, density 320. Emulator awal mengalami SystemUI ANR dan satu process-kill akibat tekanan memori saat Gradle berjalan bersamaan. Paket Google/Play Store yang tidak dibutuhkan dinonaktifkan hanya di guest read-only untuk verifikasi. Build terakhir dilakukan dengan menutup emulator sementara. Tidak ada klaim pengujian perangkat fisik.

Runner dijalankan langsung melalui ADB untuk menghindari compiler Gradle aktif bersama emulator. `connectedDebugAndroidTest` tidak dieksekusi; jangan menganggap laporan connected Gradle tersedia.

## Screenshot dan batas pengujian

Delapan layar utama serta dialog QR/Direction telah dirender dan ditelaah. Screenshot tambahan merekam error Register, bagian bawah timeline dan preferensi Profile. Hasil visual/manual ada di `docs/VISUAL_REVIEW.md`, `output/screenshots/` dan `output/comparison/`. Overview: `output/maxwash-screens.png`.

Belum diuji: perangkat fisik, landscape, viewport/font scale lain, semua versi API minimum–target, process-death restoration, atau QR scanning. ViewModel/rememberSaveable mempertahankan state selama sesi dan Activity recreation; proses baru mengembalikan mock data awal. Peta, Direction, contact, nota dan promo adalah simulasi lokal.

## Kesiapan berkas UTS

Source, APK debug, README draft, analisis persyaratan, panduan demo dan bukti verifikasi tersedia. Paket persiapan dibuat sebagai `output/uts/MAXWASH_DRAFT.zip`, memuat source project, APK, README draft, screenshot dan laporan tes/lint; bukan ZIP submission final. Nama kelompok, nama lengkap beberapa anggota, kontribusi nyata tiap anggota serta sistem kerja kelompok belum dikonfirmasi. Video demo submission belum direkam. Panduan `docs/DEMO.md` bukan bukti video telah direkam. Tidak ada submission/upload ke e-learning yang dilakukan.
