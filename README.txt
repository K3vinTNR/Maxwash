MAXWASH - Aplikasi laundry lokal (Room) / UTS IF570L
Tanggal pengerjaan: 10 Oktober 2026

DESKRIPSI
Aplikasi pelanggan laundry Android menggunakan Kotlin dan Jetpack Compose.
Menampilkan profil/QR visual, pesanan, enam tahapan status, notifikasi lokal,
registrasi/login lokal, pencatatan pesanan, dan peta lokasi outlet simulasi.
Menggunakan database Room/SQLite di perangkat. Akun, profil, preferensi, pesanan,
timeline, outlet, katalog tarif/parfum/promo, notifikasi dan sesi tersimpan lokal.
Peta/QR tetap ilustratif; tidak menggunakan backend, GPS, push atau pembayaran.

ANGGOTA (sesuai teks proposal; nama lengkap belum seluruhnya dikonfirmasi)
1. Michael - 00000106013
2. Kevin Tanar - 00000105724
3. Chrissander A.G - 00000106640
4. Inocentius Noel P - 00000106169
Nama kelompok: MENUNGGU KONFIRMASI.

PEMBAGIAN TUGAS DAN KONTRIBUSI
Kontribusi nyata masing-masing anggota dan sistem kerja kelompok belum diberikan.
Bagian ini WAJIB diisi berdasarkan pekerjaan nyata sebelum submission.
Jangan menganggap file ini sebagai README submission final.
Implementasi dalam sesi ini dikerjakan dengan bantuan Codex; kontribusi tersebut
tidak otomatis boleh diatribusikan kepada salah satu anggota kelompok.

LAYAR DAN RUBRIK
- List: My Orders/Pesanan Saya dan Riwayat; LazyColumn, search, filter.
- Detail: Order Tracking; argument orders/{orderId} memilih data akun aktif di Room.
- Form: Register; nama, HP numerik, email, password, konfirmasi, error langsung.
- Lokasi: daftar outlet dan jarak dari Room; peta ilustrasi, tanpa GPS.
- Free choice: Profile & QR; edit lokal, preferensi, QR dialog, logout.
- Tambahan: Splash, Login, Home, Notifications dengan read/unread state.
- Custom Material 3: Color.kt, Type.kt, Theme.kt mendefinisikan warna/tipografi/shapes.
- State: ViewModel bersama; rememberSaveable untuk form, dialog, preferensi UI.
- Data: Entity + DAO + MaxwashDatabase + RoomRepository, Flow dan coroutine.
  Seed demo_data.json diimpor satu kali: 6 pesanan, 5 notifikasi, 2 outlet,
  katalog layanan/parfum/promo. UI membaca database, bukan asset atau list statis.
- Catat Pesanan: pilih layanan/outlet/berat; biaya dihitung dari tarif Room.
  Pesanan, event awal dan notifikasi dibuat dalam satu transaksi.
- Statistik Home/list dihitung dari pesanan akun aktif, termasuk bulan berjalan.

AKUN DEMO
Email: andi@example.com
Nomor HP: 081234567890
Password: Maxwash123
Tombol "Coba akun demo" tersedia. Registrasi membuat akun lokal terpisah dan
langsung membuka Home dengan pesanan/notifikasi kosong. Email dan HP unik.
Password akun disimpan sebagai hash PBKDF2 dengan salt acak, bukan plaintext.
Akun/profil/sesi bertahan setelah aplikasi ditutup atau proses dimulai ulang.
Login demo membuka akun demo tersimpan tanpa mereset profil atau status dibaca.
Logout hanya menghapus sesi; akun dan data tetap tersimpan. Menghapus data app
atau uninstall menghapus database lokal. Belum ada sinkronisasi antar perangkat.

BUILD DAN TES
Buka folder proyek di Android Studio; SDK 37 dan JDK 25 sesuai konfigurasi awal.
Windows PowerShell:
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --max-workers=1
.\gradlew.bat :app:connectedDebugAndroidTest --max-workers=1
Alternatif runner yang dipakai dalam verifikasi sesi ini (emulator/device aktif):
.\gradlew.bat :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r org.umn.maxwash.test/org.umn.maxwash.MaxwashTestRunner
APK: app/build/outputs/apk/debug/app-debug.apk
Laporan unit: app/build/reports/tests/testDebugUnitTest/index.html
Laporan connected hanya ada bila connectedDebugAndroidTest benar-benar dijalankan.
Status/hasil runner ADB dicatat di docs/VERIFICATION.md; bukan laporan connected Gradle.
Migrasi Room: build sukses, 10 unit test dan 13 tes emulator lulus; lint 0 error
/ 12 warning. Penjelasan arsitektur dan batas pengujian: docs/ROOM_STORAGE.md.
docs/VERIFICATION.md mencatat pengujian prototipe sebelumnya.
UI tests menggunakan database in-memory terpisah; tes repository menggunakan
database sementara yang dibuka ulang, tanpa menghapus data akun aplikasi.

ACUAN DAN ADAPTASI
Analisis ketiga PDF, daftar persyaratan, konflik dan rencana bertahap:
docs/REQUIREMENTS.md.
Login/QR/Register/Location/timeline memerlukan adaptasi PRD dari screenshot
proposal. Selisih visual akhir dicatat di docs/VISUAL_REVIEW.md.
Screenshot acuan berasal dari PDF; font/vektor/aset Figma asli tidak tersedia.

SUBMISSION
Instruksi UTS mensyaratkan source project, APK, README dengan nama lengkap dan
kontribusi nyata, video demo <=5 menit, dan ZIP [GroupName]_MAXWASH.zip.
Panduan demo: docs/DEMO.md. Nama/kontribusi belum lengkap; jangan mengklaim
submission lengkap sebelum semua berkas dan informasi tersebut tersedia.
Paket persiapan: output/uts/MAXWASH_DRAFT.zip. Source berada dalam
MAXWASH/AndroidStudioProject, APK dalam MAXWASH/APK, bukti dalam
MAXWASH/Verification. ZIP draft belum berisi video demo submission.
