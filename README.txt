MAXWASH - Aplikasi laundry lokal (Room) / UTS IF570L
Tanggal pembaruan: 13 Oktober 2026

DESKRIPSI
Aplikasi pelanggan laundry Android menggunakan Kotlin dan Jetpack Compose.
Menampilkan profil, pesanan, enam tahapan status, notifikasi lokal,
registrasi/login lokal, pencatatan pesanan, dan informasi lokasi outlet.
Menggunakan database Room/SQLite di perangkat. Akun, profil, preferensi, pesanan,
timeline, outlet, katalog tarif/parfum/promo, notifikasi dan sesi tersimpan lokal.
Petunjuk arah membuka aplikasi peta/browser. Belum menggunakan backend, push
atau pembayaran. Tidak ada akun bawaan atau halaman demo.

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
- Lokasi: daftar outlet/alamat dari Room; petunjuk arah membuka peta/browser.
- Free choice: Profil; edit lokal, preferensi, logout.
- Tambahan: Splash, Login, Home, Notifications dengan read/unread state.
- Beranda > Layanan > Penjemputan > Pilih Layanan > Konfirmasi Pembayaran.
  Tampilan dan navigasi mengikuti tiga gambar referensi; pilihan dibawa antar
  halaman dan bertahan setelah Activity recreation. Bayar Sekarang melewati
  pembayaran nyata untuk prototipe dan menyimpan pesanan, total serta event awal
  ke Room. Beranda langsung menampilkan pesanan baru; Lacak Pesanan membuka
  Order Tracking dengan ID yang sama. Lihat docs/SERVICES_FLOW.md.
  Peta Penjemputan: TomTom Orbis v2 interaktif, pencarian alamat/outlet melalui
  TomTom Geocoding, status memuat/gagal dan buka Maps. Memerlukan internet.
  Isi TOMTOM_API_KEY di local.properties (diabaikan Git) atau environment,
  lalu build ulang. Key perlu akses Map Display dan Geocoding Orbis v2.
  Alamat/catatan Penjemputan mengikuti profil yang disimpan; pilihan alamat
  khusus untuk penjemputan tetap dipertahankan, termasuk saat Activity recreation.
- Custom Material 3: Color.kt, Type.kt, Theme.kt mendefinisikan warna/tipografi/shapes.
- State: ViewModel bersama; rememberSaveable untuk form, dialog, preferensi UI.
- Data: Entity + DAO + MaxwashDatabase + RoomRepository, Flow dan coroutine.
  catalog_data.json diimpor satu kali: 2 outlet dan katalog layanan/parfum/promo.
  Tidak membuat akun, pesanan, riwayat atau notifikasi contoh.
  UI membaca database, bukan asset atau list statis.
- Catat Pesanan: pilih layanan/outlet/berat; biaya dihitung dari tarif Room.
  Pesanan, event awal dan notifikasi dibuat dalam satu transaksi.
- Statistik Home/list dihitung dari pesanan akun aktif, termasuk bulan berjalan.

AKUN PELANGGAN
Daftar akun melalui halaman Registrasi, lalu masuk memakai email/nomor HP
dan password yang didaftarkan. Akun baru membuka Home dengan daftar
pesanan/notifikasi kosong. Email dan HP unik.
Password akun disimpan sebagai hash PBKDF2 dengan salt acak, bukan plaintext.
Akun/profil/sesi bertahan setelah aplikasi ditutup atau proses dimulai ulang.
Saat versi baru dibuka, akun demo versi lama dihapus berdasarkan metadata ID,
beserta pesanan/riwayat/notifikasinya. Sesi akun demo dikosongkan. Akun yang
dibuat lewat registrasi beserta data dan sesi aktifnya tetap tersimpan.
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
Hasil terkini dan runner ADB dicatat di docs/ROOM_STORAGE.md; bukan laporan
connected Gradle. Penghapusan demo 11 Oktober 2026: build sukses, 10 unit test dan
17 tes emulator lulus (8 UI + 9 repository); lint 0 error / 13 warning.
Log emulator: docs/verification/account-cleanup-instrumentation.txt.
Penjelasan arsitektur dan batas pengujian: docs/ROOM_STORAGE.md.
Pembaruan alur Layanan: build sukses, 13 unit test dan 2 tes UI baru lulus;
lint 0 error / 13 warning. Cakupan dan hasil: docs/SERVICES_FLOW.md.
Checkout ke Beranda/Order Tracking: build sukses, 18 unit test dan 13 tes emulator
(11 repository + 2 UI checkout) lulus. Log: docs/verification/checkout-orders-instrumentation.txt.
Peta asli: Leaflet 1.9.4 dibundel lokal, tile/pencarian alamat dari TomTom Orbis v2.
Key dikirim dalam header HTTPS native, tanpa masuk URL/HTML/JS. Tanpa key,
peta menampilkan status belum tersedia dan navigasi layanan tetap berjalan.
Tes jaringan peta memerlukan internet dan key TomTom yang aktif;
bukti dan verifikasi tambahan ada di docs/SERVICES_FLOW.md.
docs/VERIFICATION.md mencatat pengujian prototipe sebelumnya.
UI tests menggunakan database in-memory terpisah; tes repository menggunakan
database sementara yang dibuka ulang, tanpa menghapus data akun aplikasi.

ACUAN DAN ADAPTASI
Analisis ketiga PDF, daftar persyaratan, konflik dan rencana bertahap:
docs/REQUIREMENTS.md.
Login/Register/Location/timeline memerlukan adaptasi PRD dari screenshot
proposal. Selisih visual akhir dicatat di docs/VISUAL_REVIEW.md.
Screenshot acuan berasal dari PDF; font/vektor/aset Figma asli tidak tersedia.

SUBMISSION
Instruksi UTS mensyaratkan source project, APK, README dengan nama lengkap dan
kontribusi nyata, video demo <=5 menit, dan ZIP [GroupName]_MAXWASH.zip.
Panduan penggunaan: docs/WALKTHROUGH.md. Nama/kontribusi belum lengkap; jangan mengklaim
submission lengkap sebelum semua berkas dan informasi tersebut tersedia.
Paket persiapan: output/uts/MAXWASH_DRAFT.zip. Source berada dalam
MAXWASH/AndroidStudioProject, APK dalam MAXWASH/APK, bukti dalam
MAXWASH/Verification. ZIP draft belum berisi video demo submission.
