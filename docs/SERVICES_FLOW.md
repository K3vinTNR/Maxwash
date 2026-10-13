# Alur Layanan di Beranda

Menu utama **Layanan** menggantikan tombol Pesanan di Beranda. Tab bawah Pesanan dan tautan Semua Pesanan tetap membuka daftar pesanan.

Alur: **Layanan → Penjemputan → Pilih Layanan → Konfirmasi Pembayaran → Beranda → Lacak Pesanan**. Konfirmasi pembayaran membuat pesanan lokal untuk prototipe.

- Penjemputan: pilihan Jemput ke Lokasi / Antar Mandiri, alamat dan catatan driver, jadwal, serta pilihan outlet. Alamat awal berasal dari profil; alamat kosong perlu dilengkapi untuk penjemputan. Antar Mandiri memakai alamat outlet.
- Pilih Layanan: empat pilihan Kiloan dari katalog Room, tarif dan estimasi pengerjaan sesuai katalog, pilihan parfum, deterjen tambahan, catatan pakaian dan berat estimasi. Kategori tanpa isi katalog menampilkan keadaan kosong.
- Konfirmasi Pembayaran: layanan/outlet/bobot yang dipilih, kode promo, tiga pilihan metode pembayaran, rincian subtotal/tambahan/potongan dan total. Parfum termasuk layanan; deterjen opsional menambah Rp 2.000. Kode `NEWCLEAN5K` mengurangi estimasi maksimal Rp 5.000 dan total tidak menjadi negatif.

Susunan kartu, warna teal/mint, pilihan layanan dan tombol bawah mengikuti tiga gambar referensi pengguna. Nama/alamat/ID akun dan pesanan contoh dalam gambar tidak ditanam ke aplikasi. Tidak ada timer pembayaran maupun token pesanan buatan.

## Peta Penjemputan

Peta asli menggunakan [TomTom Orbis Map Display API v2](https://docs.tomtom.com/map-display-api/documentation/tomtom-orbis-maps/v2/raster/raster-tile), dengan Leaflet 1.9.4 yang dibundel lokal sebagai renderer di Android WebView. Peta memakai tile raster bergaya street-light dan tidak membutuhkan WebGL. Tile 512px dipilih untuk layar dengan devicePixelRatio di atas 1, sedangkan layar lainnya memakai 256px. Peta dapat digeser dan diperbesar; kontrol, atribusi TomTom/penyedia data dan informasi hak cipta tetap tersedia. Pilihan **Alamat Anda / Outlet** menampilkan satu titik untuk alamat aktif. Antar Mandiri langsung menampilkan alamat outlet. Tombol Buka Maps membuka alamat yang sedang ditampilkan dalam aplikasi peta/browser.

Alamat yang telah disimpan di draft dicari dengan [TomTom Orbis Geocoding API v2](https://docs.tomtom.com/geocoding-api/documentation/tomtom-orbis-maps/v2/geocode) pada background IO, dibatasi ke Indonesia dan hasil alamat/jalan/persimpangan. GeoJSON longitude/latitude dikonversi ke urutan koordinat aplikasi. Pencarian tidak dilakukan pada setiap huruf yang diketik. Titik adalah perkiraan hasil pencarian alamat, bukan GPS perangkat. Saat alamat kosong atau pencarian tidak berhasil, peta menampilkan gambaran Indonesia tanpa titik buatan dan memberikan keterangan yang sesuai. Pencarian yang gagal tidak memblokir navigasi layanan. Tidak memakai Android Geocoder maupun tile OpenStreetMap langsung.

Peta memerlukan koneksi internet, izin manifest INTERNET dan API key yang memiliki akses ke Map Display/Geocoding Orbis v2. Isi `TOMTOM_API_KEY=key_Anda` di `local.properties` (sudah diabaikan Git), atau set environment variable `TOMTOM_API_KEY`, lalu build ulang. Environment variable yang tidak kosong diprioritaskan. Jangan memasukkan key ke file yang di-commit. Key aplikasi Android berada dalam BuildConfig sehingga tetap perlu pembatasan akses/kuota yang sesuai di akun TomTom.

Tanpa key, tidak ada permintaan geocoding/tile; kartu peta menampilkan status belum tersedia dan navigasi layanan tetap berfungsi. Tampilan memuat/gagal, batas waktu, dan tombol Coba Lagi tersedia. Status siap mengikuti pemuatan tile, bukan sekadar HTML selesai dimuat. WebView memakai HTML/JS/CSS lokal dengan origin `appassets.androidplatform.net` dan CSP yang membatasi sumber gambar ke `api.tomtom.com`. Native interception meminta tile melalui HTTPS dengan `TomTom-Api-Key`, `TomTom-Api-Version: 2` dan Accept image/png; key tidak dimasukkan ke URL, HTML atau JavaScript. Permintaan tile hanya mengizinkan host/path/query yang ditentukan dan transport tidak mengikuti redirect. Geocoding memakai header yang sama serta `Attributes: results.position`. Batas koneksi 5 detik dan pembacaan 7 detik; koordinat tidak disimpan ke Room. File/content access, mixed content dan geolokasi browser dinonaktifkan, serta tidak memakai native JavaScript bridge. Error SSL selalu dibatalkan. Link penyedia dibuka di browser. Lisensi Leaflet dan hash aset tersedia di `app/src/main/assets/map/`. Schema Room dan cakupan pembayaran tetap sama.

Pilihan disimpan dalam `CheckoutDraft` dan `CheckoutDraftSaver` pada `rememberSaveable`, sehingga navigasi kembali dan Activity recreation mempertahankan pilihan sebelum pembayaran. Untuk prototipe, tombol **Bayar Sekarang** menganggap pembayaran berhasil tanpa payment gateway untuk ketiga metode pembayaran. Repository menyimpan pesanan akun aktif, event awal **Pesanan Diterima** dan notifikasi dalam satu transaksi Room. Layanan, bobot, outlet dan total mengikuti checkout; total sudah termasuk deterjen tambahan dan potongan promo. Alur ini tidak mengubah profil dan tidak melakukan penagihan nyata.

Setelah penyimpanan berhasil, aplikasi langsung membuka **Beranda** dan menampilkan pesanan terbaru pada kartu aktif. Tombol **Lacak Pesanan** membuka **Order Tracking** dengan ID pesanan yang sama, termasuk total, outlet dan timeline. Halaman checkout dihapus dari back stack dan berat/promo/catatan pakaian/tambahan draft dikosongkan untuk pesanan berikutnya. Penyimpanan berjalan di ViewModel dan tombol bayar dinonaktifkan selama proses, sehingga Activity recreation dan ketukan berulang tidak membuat pesanan ganda. Kegagalan penyimpanan menampilkan pesan error dan mempertahankan draft untuk dicoba kembali. Status laundry tidak bergerak otomatis.

Alamat dan catatan yang masih memakai nilai profil mengikuti perubahan profil yang telah disimpan, termasuk setelah draft Penjemputan dibuka. Alamat kosong diisi dari profil. Alamat/catatan yang diedit khusus di Penjemputan dipertahankan; catatan yang sengaja dikosongkan juga dipertahankan. Nilai profil sebelumnya disimpan dengan `rememberSaveable` per akun agar Activity recreation tidak menghilangkan perbedaan antara nilai profil dan pilihan khusus. Perubahan profil tidak mereset layanan, bobot, jadwal, outlet atau metode pembayaran. Peta menerima alamat draft terbaru untuk pencarian lokasi.

Implementasi: `PickupScreen.kt`, `ChooseServiceScreen.kt`, `ConfirmPaymentScreen.kt`, komponen checkout bersama, dan tiga rute baru dalam `MaxwashApp.kt`. Schema Room tidak berubah.

## Verifikasi pesanan checkout 13 Oktober 2026

- Build APK debug, APK tes dan unit: **BUILD SUCCESSFUL**, **1 menit 1 detik**; pembaruan APK setelah penyesuaian tes juga berhasil dalam **39 detik**.
- **18 unit test lulus**, tanpa kegagalan atau error.
- **13 tes emulator lulus** melalui runner ADB dalam **55,116 detik**: **11 tes repository dan 2 tes UI checkout**. Cakupan meliputi transaksi pesanan/event/notifikasi, total dengan promo/tambahan untuk ketiga metode pembayaran, validasi gagal, persistensi setelah database dibuka ulang/login ulang dan isolasi akun.
- Tes UI membuktikan klik bayar berulang membuat tepat satu pesanan, Beranda menampilkan ID/layanan/berat pesanan terbaru, Lacak Pesanan membuka ID/total yang sama, serta Activity recreation mempertahankan Beranda dan detail. Draft pesanan berikutnya tidak dapat dilanjutkan sebelum berat diisi kembali.
- Log: [verification/checkout-orders-instrumentation.txt](verification/checkout-orders-instrumentation.txt). Screenshot diperiksa di `output/screenshots/checkout-orders/`: `checkout-order-home.png` dan `checkout-order-tracking.png`. Tes menggunakan database terpisah tanpa menghapus data aplikasi pengguna. Schema Room tetap versi 1.

## Riwayat verifikasi dialog pembayaran 13 Oktober 2026

Verifikasi berikut berlaku untuk versi dialog sebelumnya, sebelum pembayaran dihubungkan dengan pembuatan pesanan dan navigasi langsung ke Beranda.

- Build APK debug, APK tes dan unit: **BUILD SUCCESSFUL**, **1 menit 54 detik**. Command: `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest --max-workers=1 --console=plain --no-daemon`.
- **18 unit test lulus** tanpa kegagalan, error atau tes yang dilewati.
- **2 tes UI checkout lulus** di emulator `emulator-5554` melalui runner ADB dalam **68,637 detik**. Tes alur layanan memeriksa dialog pembayaran berhasil, metode/total terpilih, Activity recreation dan kembali ke Beranda. Tes Antar Mandiri dan kategori kosong juga tetap lulus. Database tes tidak menerima pesanan tambahan.

## Verifikasi 11 Oktober 2026

- Build APK debug, APK tes, unit dan lint: **BUILD SUCCESSFUL**, **1 menit 30 detik**, 85 task. Command: `:app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest :app:lintDebug --max-workers=1 --console=plain --no-daemon`.
- **13 unit test lulus**; tiga tes baru memeriksa berat/tarif/tambahan, batas potongan promo dan validasi alamat/bobot.
- **2 tes UI baru lulus** di emulator Pixel 10 Pro XL dalam **33,063 detik**. Alur dari Beranda sampai Konfirmasi Pembayaran memeriksa alamat/outlet, layanan, parfum, tambahan, promo, metode pembayaran, total, navigasi kembali dan Activity recreation. Alur Antar Mandiri dan kategori kosong juga diuji. Tombol akhir tidak menambah pesanan dalam database tes.
- Log: [verification/services-flow-instrumentation.txt](verification/services-flow-instrumentation.txt). Tes hanya memakai database terpisah dan tidak membuat akun dalam database aplikasi yang terpasang.
- Lint: **0 error / 13 warning**; warning berada pada label Activity, informasi versi dependensi, resource template dan saran KTX untuk halaman Lokasi lama.
- Screenshot diperiksa di `output/screenshots/services-flow/`, mencakup Penjemputan, layanan/pilihan tambahan, Konfirmasi Pembayaran dan rincian tagihan. Tampilan dapat discroll; tombol lanjut tetap berada di bagian bawah.

## Riwayat Verifikasi Peta Sebelum Migrasi TomTom

- **15 unit test lulus**, termasuk validasi koordinat/batas viewport dan format URL saat locale memakai koma desimal.
- **4 tes UI alur lulus**: pergantian alamat/outlet, buka Maps, alamat tidak ditemukan, navigasi checkout, pilihan Antar Mandiri dan Activity recreation. Log: [verification/pickup-map-instrumentation.txt](verification/pickup-map-instrumentation.txt).
- Tes jaringan memakai Android Geocoder asli untuk alamat Senopati, memeriksa ukuran native/HTML yang lebih besar dari nol, tile lokasi (bukan overview dunia) yang berhasil dimuat, marker dan status siap. Renderer melakukan penyesuaian ukuran setelah WebView dilayout oleh Compose. Screenshot `output/screenshots/services-flow/pickup-real-map.png` telah diperiksa secara visual: jalan, pin, tombol zoom dan atribusi terlihat.
- Tes alur memakai pencarian alamat fixture agar tidak bergantung jaringan; tes jaringan terpisah memerlukan internet dan backend Geocoder Android. Semua tes menggunakan aplikasi tes/database terpisah, tanpa membuat akun pada database aplikasi pengguna.
- Verifikasi terakhir APK akhir: **OK (5 tests)** dalam **66,781 detik**, terdiri dari empat tes alur dan satu tes jaringan. Build/lint sebelumnya sukses, **0 error / 13 warning**. APK siap dicoba di `output/apk/maxwash-pickup-map-debug.apk`; salinan ini tersimpan terpisah dari output build Gradle. Log jaringan terpisah: [verification/pickup-map-network.txt](verification/pickup-map-network.txt), serta data viewport/tile: [verification/pickup-map-page.txt](verification/pickup-map-page.txt).

## Verifikasi TomTom 11 Oktober 2026

- Build final APK aplikasi/tes, unit test dan lint: **BUILD SUCCESSFUL**, **2 menit 26 detik**, 89 task. **15 unit test lulus**, lint **0 error / 13 warning** yang sudah ada sebelumnya.
- **11 tes emulator lulus** dalam **73,299 detik**: lima tes client TomTom (urutan GeoJSON, encoding alamat/header, hasil invalid/kosong, key ditolak/koneksi gagal, dan pembatasan tujuan header), satu tes jaringan asli dan lima tes navigasi Layanan. Tes navigasi mencakup pilihan alamat/outlet, alamat tidak tersedia, key kosong, Antar Mandiri, pilihan layanan/rincian pembayaran, kembali dan Activity recreation. Log: [verification/tomtom-map-instrumentation.txt](verification/tomtom-map-instrumentation.txt).
- Setelah resolusi tile disesuaikan untuk layar padat piksel, **enam tes client/jaringan terkait lulus kembali** pada APK final dalam **23,309 detik**. Log: [verification/tomtom-map-final-network.txt](verification/tomtom-map-final-network.txt).
- Tes jaringan memakai key lokal yang diberikan pengguna dan endpoint TomTom asli. Alamat Senopati berhasil dicari, viewport native/HTML berukuran 408 × 300, empat tile zoom 15 berukuran 512px berhasil dimuat, marker serta atribusi/informasi peta tampil. Screenshot `output/screenshots/services-flow/pickup-tomtom-map.png` diperiksa secara visual. Bukti viewport dan URL tile tanpa key: [verification/pickup-tomtom-page.txt](verification/pickup-tomtom-page.txt).
- Tes navigasi memakai fixture alamat dan transport tanpa permintaan jaringan berbayar. Tes hanya memakai database terpisah; tidak membuat akun atau pesanan dalam database pengguna.
- `local.properties` diabaikan Git; pemeriksaan file sumber/log menemukan **tidak ada nilai key** di luar konfigurasi lokal/output build yang diabaikan. Key tidak ditulis dalam URL, HTML atau JavaScript peta.
- APK final terpasang di emulator dengan install `-r` tanpa menghapus data. Salinan siap dicoba: `output/apk/maxwash-tomtom-debug.apk`.

## Perbaikan Alamat Profil ke Penjemputan — 11 Oktober 2026

- Bug berhasil direproduksi pada APK sebelumnya: pelanggan baru menyimpan alamat/catatan di Profil, tetapi Penjemputan tetap menampilkan `Tambahkan alamat penjemputan`. Tes gagal pada pemeriksaan alamat dalam **25,417 detik**. Log sebelum perbaikan: [verification/profile-pickup-address-before.txt](verification/profile-pickup-address-before.txt).
- Penyebab: draft hanya diinisialisasi berdasarkan ID akun; perubahan profil pada akun yang sama tidak memperbarui alamat draft. `MaxwashApp` kini mengamati alamat/catatan profil dan menyinkronkan nilai default melalui `CheckoutDraft.syncProfileLocation`, dengan nilai profil sebelumnya disimpan per akun. Alamat khusus dan catatan yang sengaja dikosongkan tetap dipertahankan.
- Build APK aplikasi/tes, unit dan lint **BUILD SUCCESSFUL** dalam **1 menit 53 detik**. **18 unit test lulus**, lint **0 error / 13 warning** yang sudah ada sebelumnya.
- **4 tes UI lulus** dalam **78,261 detik**: alamat/catatan baru langsung muncul setelah disimpan di Profil, perubahan alamat berikutnya dan Activity recreation, alamat penjemputan khusus yang bertahan setelah profil berubah/recreation, serta regresi alur layanan dan preferensi profil. Pencarian peta menerima alamat profil terbaru; pemeriksaan ini memakai fixture tanpa permintaan TomTom berbayar. Log sesudah perbaikan: [verification/profile-pickup-address-after.txt](verification/profile-pickup-address-after.txt).
- Semua tes memakai database terpisah. Perbaikan tidak mengubah schema atau menghapus data akun. APK terpasang dengan install `-r`; salinan terbaru: `output/apk/maxwash-profile-address-debug.apk`.
