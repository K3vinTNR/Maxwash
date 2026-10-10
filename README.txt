MAXWASH - Prototipe UI UTS IF570L
Tanggal pengerjaan: 10 Oktober 2026

DESKRIPSI
Aplikasi pelanggan laundry Android menggunakan Kotlin dan Jetpack Compose.
Menampilkan profil/QR visual, pesanan, enam tahapan status, notifikasi lokal,
registrasi/login demo, dan peta lokasi outlet simulasi. Tidak menggunakan backend,
database, GPS, kamera, push notification, pembayaran, atau layanan login nyata.

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
- Detail: Order Tracking; argument orders/{orderId} memilih mock repository.
- Form: Register; nama, HP numerik, email, password, konfirmasi, error langsung.
- Native-feature UI: Laundry Location; dua marker dan jarak demo, tanpa GPS.
- Free choice: Profile & QR; edit lokal, preferensi, QR dialog, logout.
- Tambahan: Splash, Login, Home, Notifications dengan read/unread state.
- Custom Material 3: Color.kt, Type.kt, Theme.kt mendefinisikan warna/tipografi/shapes.
- State: ViewModel bersama; rememberSaveable untuk form, dialog, preferensi UI.
- Data: Kotlin data classes + MockRepository, 6 pesanan/6 status, 5 notifikasi,
  2 outlet; list/detail/notifikasi memakai ID yang sama.

AKUN DEMO
Email: andi@example.com
Nomor HP: 081234567890
Password: Maxwash123
Tombol "Coba akun demo" tersedia. Registrasi membuat satu akun simulasi lokal dan
langsung membuka Home. Akun/profil lokal bertahan selama sesi dan rotasi; restart
proses aplikasi mengembalikan data awal. Menggunakan akun demo mereset identitas
ke Andi. Logout menghapus halaman utama dari back stack.

BUILD DAN TES
Buka folder proyek di Android Studio; SDK 37 dan JDK 25 sesuai konfigurasi awal.
Windows PowerShell:
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
Alternatif runner yang dipakai dalam verifikasi sesi ini (emulator/device aktif):
.\gradlew.bat :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r -e class org.umn.maxwash.MaxwashFlowTest org.umn.maxwash.test/androidx.test.runner.AndroidJUnitRunner
APK: app/build/outputs/apk/debug/app-debug.apk
Laporan unit: app/build/reports/tests/testDebugUnitTest/index.html
Laporan connected hanya ada bila connectedDebugAndroidTest benar-benar dijalankan.
Status/hasil runner ADB dicatat di docs/VERIFICATION.md; bukan laporan connected Gradle.
Hasil nyata final: build sukses, 14 unit test lulus, full suite 6 UI test lulus
(121,208 detik), lint 0 error / 12 warning / 1 note. Batas pengujian ada di laporan.

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
