# Langkah Berdua Android

Versi **0.4 Sheets beta**, aplikasi Android native dengan tema Sage dan Krem, SQLite, OCR ML Kit yang disertakan dalam APK, serta penghubung Google Sheets melalui Apps Script. Tidak memakai WebView.

## Pakai aplikasi

APK dapat dipasang pribadi pada Android 7 atau lebih baru. Pencatatan pribadi dan OCR tidak membutuhkan akun berbayar. Untuk sinkronisasi pasangan, ikuti [aktivasi Google Sheets satu kali](docs/AKTIFKAN-GOOGLE-SHEETS.md). Backend **belum diterapkan ke akun pengguna** hanya dengan mengunduh APK atau menghubungkan Google Drive di ChatGPT. Tidak ada URL penerapan, token atau kredensial yang ditanam dalam APK/repository.

Tersedia dalam source: pemasukan/pengeluaran, pengubahan, sampah/pemulihan transaksi, daftar belanja, target/alokasi lokal, CSV transaksi, impor CSV 0.3, pilihan foto pembuka lokal dan tulisan “Semoga bisa disemogakan”. OCR kamera/galeri menyediakan rotasi, pemotongan tepi, kontras, koreksi manual rincian, peringatan selisih dan foto identik. Satu struk menjadi satu pengeluaran.

Setelah backend diaktifkan: dua identitas perangkat dengan undangan sekali pakai 24 jam, akses server memakai token acak tersimpan terenkripsi dengan Android Keystore, data bersama tersinkron saat aplikasi aktif, antrean offline, ID operasi untuk pengiriman ulang, versi untuk benturan perubahan, riwayat jurnal, dan pencabutan akses pasangan. Catatan pribadi tidak dikirim. Saldo menggunakan catatan pribadi tersimpan dan versi bersama yang sudah diterima server. Catatan bersama baru yang masih mengantre belum masuk saldo.

## Batas versi ini

Belum ada aplikasi iPhone, login Google/ChatGPT, anggaran bulanan, dompet/transfer, pembagian biaya, vendor pernikahan, pengingat, ekspor PDF, kustomisasi dashboard/kolom, atau sinkronisasi target. Foto bukti tersimpan di ponsel pemindai dan belum dibagikan ke pasangan. CSV bukan cadangan seluruh aplikasi dan belum dapat dipulihkan otomatis dari format 0.4. Tidak ada data contoh otomatis atau saldo simulasi.

Data lokal bisa hilang bila aplikasi dihapus. APK ini memakai signing key debug untuk instalasi pribadi; kunci dapat berbeda antar build dan belum menjamin pembaruan langsung di atas APK sebelumnya. App ID `id.langkahberdua.connected` memungkinkan pemasangan berdampingan dengan 0.3 (`id.langkahberdua.nativeapp`), sehingga data lama tidak perlu dihapus. Gunakan impor CSV 0.3 untuk transaksi lama.

## Source dan struktur data

- `app/src/main/java/id/langkahberdua/app`: tampilan Android, penyimpanan, penghubung HTTP, parser dan OCR.
- `backend/Code.gs`: backend Google Apps Script. Tab `LB_EVENTS` adalah jurnal append-only: ID operasi, sidik isi, ID catatan, versi, waktu, pembuat, judul, nominal, jenis, tanda dihapus, dan payload JSON. Setiap versi transaksi adalah histori, bukan pengeluaran baru. Script Properties menyimpan ID spreadsheet dan hash token/undangan.
- SQLite `records`: nilai diterima, perubahan menunggu, ID operasi, versi dasar, benturan, kesalahan, serta lokasi/hash foto lokal. UUID adalah identitas transaksi.
- `tests/`: pengujian Apps Script dengan dua identitas dan parser teks.
- `app/src/androidTest`: pengujian SQLite, konflik, pengulangan, OCR gambar sesungguhnya dan peluncuran Activity pada emulator.

## Build dan uji

JDK 17, Android SDK 35, Gradle 8.9, AGP 8.7.3. Jalankan:

```sh
node tests/backend.test.cjs
mkdir -p build/parser-test
javac -d build/parser-test app/src/main/java/id/langkahberdua/app/ReceiptParser.java app/src/main/java/id/langkahberdua/app/Csv.java tests/ReceiptParserTest.java
java -cp build/parser-test id.langkahberdua.app.ReceiptParserTest
gradle :app:assembleDebug
gradle :app:connectedDebugAndroidTest
```

Perintah terakhir membutuhkan emulator/perangkat Android. GitHub Actions menjalankan pengujian dan menyimpan APK serta laporan. Kelulusan tes otomatis tidak sama dengan verifikasi spreadsheet pengguna atau hasil foto pada setiap kamera. Gunakan dua ponsel setelah aktivasi untuk memeriksa sambungan nyata.

Rujukan: [ML Kit Android](https://developers.google.com/ml-kit/vision/text-recognition/v2/android), [Apps Script Web Apps](https://developers.google.com/apps-script/guides/web), [ScriptLock](https://developers.google.com/apps-script/reference/lock/lock-service).
