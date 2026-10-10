# Analisis MAXWASH sebelum implementasi

Catatan perubahan lingkup: permintaan pengguna selanjutnya mengganti data mock dengan Room local storage. Ketentuan awal tanpa database di bawah adalah riwayat prototipe; implementasi saat ini dijelaskan dalam [ROOM_STORAGE.md](ROOM_STORAGE.md).

Analisis: 10 Oktober 2026. Ketiga dokumen dibaca sebelum kode aplikasi diubah.

## Sumber dan batas lingkup

- PRD ( UTS ONLY).pdf, versi 1.2, 25 halaman: perilaku prototipe customer.
- Soal_UTS_IF570_LAB_GSL2026-27.pdf, 12 halaman: persyaratan dan rubrik UTS.
- MAXWASH LAUNDRY.pdf, 15 halaman: screenshot proposal; halaman 5–14 ditelaah visual.
- Instruksi submission di dokumen dicatat sebagai kebutuhan tugas, bukan izin untuk mengunggah atau menghubungi siapa pun.
- Repository awal: Android Kotlin/Compose, package org.umn.maxwash, satu Greeting default. Perubahan awal komentar MainActivity dan .idea milik pengguna dipertahankan.
- Tidak ditemukan AGENTS.md. SDK 37, Android Studio JBR 25, Gradle 9.5 tersedia. Belum ada device aktif saat analisis; AVD View_Output tersedia.

## Perbedaan antarsumber dan keputusan

| Perbedaan | Dampak | Keputusan implementasi |
| --- | --- | --- |
| Login proposal passwordless OTP/WhatsApp/SMS/Google/Apple; PRD menggunakan email/nomor dan password | Tidak mungkin menyalin perilaku keduanya sekaligus | Warna, logo, kartu dan hierarki login proposal dipertahankan; autentikasi demo lokal menggunakan password sesuai PRD, tanpa OTP/layanan eksternal |
| Register tidak memiliki screenshot tersendiri | Tidak dapat menyatakan fidelity langsung | Lima field PRD dalam bahasa Indonesia memakai gaya kartu/input Login |
| QR customer tidak ada dalam screenshot Profile | Perlu elemen tambahan | Kartu identitas/QR visual di bawah data diri dan dialog pembesaran; bukan scanner atau token backend |
| Tracking proposal 5 tahap; PRD 6 tahap | Label/timeline dan persentase perlu diubah | Enam tahap PRD dengan visual panel teal dan timeline proposal; customer tidak mengubah status |
| Pickup proposal mengatur kurir/jadwal/gratis ongkir; PRD hanya lokasi outlet | Fitur native wajib perlu adaptasi | Peta lokal ilustratif, dua marker, alamat, jarak, pemilihan outlet, refresh posisi simulasi dan feedback Direction; tanpa permission GPS |
| Proposal berisi pembayaran, pesan ulang, kasir, promo nyata | Di luar lingkup PRD UTS | Aksi relevan menjadi feedback demo atau detail informasi lokal; tidak membuat pesanan atau pembayaran |
| Nama Chris/Dimas/Sarah, nomor/order/tanggal bervariasi di mockup | Data antarlayar tidak konsisten jika disalin apa adanya | Dataset PRD CUST-001 / Andi Pratama / MW-1001–1006 konsisten; alamat Bintaro/Senopati mengikuti proposal |
| Font, nilai warna, ikon vektor dan aset foto asli tidak diberikan | Pixel-perfect tidak dapat diverifikasi dari metadata | Estimasi visual, font sans-serif platform dan ikon Material outlined. Avatar lokal inisial dan peta Compose ilustratif dilaporkan sebagai selisih visual |

Perbedaan di atas telah dilaporkan di chat sebelum keputusan implementasi diterapkan.

## Daftar persyaratan dan penerimaan

| ID | Kebutuhan | Verifikasi yang direncanakan |
| --- | --- | --- |
| R01 | My Orders LazyColumn, ID, layanan, berat, tanggal, outlet, status; filter Semua/Aktif/Selesai dan pencarian | Filter mengembalikan dataset benar, empty state, list→detail |
| R02 | Order Details memilih repository dengan argument orderId; enam status/timeline konsisten; back; invalid ID error | Dua ID berbeda dan unknown ID; UI nav/back |
| R03 | Register nama, telepon numeric-only, email, password min 8, konfirmasi; keyboard benar, error langsung, submit diblokir | Unit test validasi + Compose form |
| R04 | Laundry Location peta/marker simulasi, outlet, alamat, jarak, Direction, selection/refresh | UI selection berubah, feedback demo, tanpa hardware permission |
| R05 | Profile nama/telepon/ID/QR, edit lokal, simpan, logout, QR dialog | Data edit konsisten dengan Home dan QR; logout mengosongkan back stack |
| R06 | Login demo, Home ringkasan/navigasi, Notifications list/read/unread→detail | Uji alur terintegrasi dan notifikasi |
| R07 | Custom Material 3 colors, typography, shapes; mempertahankan bahasa visual proposal | Screenshot dibandingkan per layar |
| R08 | NavHost/NavController, argument dinamis, back stack tidak duplikat | Compose navigation tests |
| R09 | ViewModel untuk state lintas layar/configuration; rememberSaveable input/UI; tanpa database/backend | Rotasi dan navigasi; restart proses boleh reset prototipe lokal |
| R10 | Kotlin data classes dan mock repository; enam pesanan berbeda, notifikasi valid, dua outlet | Unit test relasi, timeline, ID/filter |
| R11 | Responsif, scroll, inset sistem/keyboard, empty/error state | Emulator serta viewport compact/landscape/font besar jika tersedia |
| R12 | Build APK dan tes benar-benar dijalankan; catat command/hasil | assembleDebug, testDebugUnitTest, lintDebug, connected tests jika emulator tersedia |
| R13 | README.txt, anggota/kontribusi, source/APK, video ≤5 menit dan ZIP | Siapkan README dan skrip demo; nama lengkap/kontribusi perlu konfirmasi manusia; jangan mengarang |

## Rencana implementasi bertahap

1. Foundation: theme, shapes, tipografi, ikon, reusable header/nav/card/input.
2. Data/state: domain models, mock repository, validasi murni, shared ViewModel.
3. Lima layar wajib: list/detail terlebih dahulu, register, lokasi, profil/QR.
4. Alur lengkap: Login/Home/Notifications, argument passing, local profile/read state, logout.
5. Verifikasi: build, unit/Compose tests, instal/run emulator, screenshot layar dan laporan fidelity.
6. Dokumentasi: hasil nyata, keterbatasan, demo rubrik, README; submission bukan otomatis lengkap bila video/kontribusi belum tersedia.

## Target visual dari screenshot

Teal utama sekitar #056B93, teal terang #007FAB, teks navy #142635, latar #F8F9FF, input lavender-biru #F0F4FF, mint #6AF4C0. Kartu 14–20 dp, input 12 dp, chips kapsul, button 12 dp (auth lebih bulat). Margin konten 16 dp, spacing utama 12–16 dp, header logo mesin cuci dan wordmark MAXWASH, icon bell dan profil; empat menu Beranda/Pesanan/Riwayat/Profil. Ukuran menggunakan estimasi screenshot, bukan klaim token Figma asli.
