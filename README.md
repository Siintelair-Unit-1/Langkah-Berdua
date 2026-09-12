# Langkah Berdua Android

Versi 0.3 lokal beta: antarmuka Android native tanpa browser/WebView.

Tersedia: pilihan foto pembuka dari galeri lokal dan “Semoga bisa disemogakan”, transaksi pemasukan/pengeluaran tersimpan SQLite, pengubahan, sampah/pemulihan, saldo, ekspor CSV melalui pemilih dokumen Android, target/alokasi tabungan dan daftar belanja lokal.

Belum tersedia: login, sinkronisasi pasangan, OCR, dompet/transfer, database cloud, aplikasi iPhone. Semua data versi ini berada di perangkat; menghapus aplikasi dapat menghapus data. Tidak ada data contoh atau saldo simulasi. CSV adalah ekspor transaksi, bukan cadangan seluruh aplikasi.

Build: JDK 17, Android SDK 35, Gradle 8.9. Jalankan `gradle :app:assembleDebug`. GitHub Actions menghasilkan APK bertanda tangan debug untuk uji instalasi pribadi; bukan rilis Play Store. Signing key build debug dapat berbeda antarrun sehingga pembaruan versi mendatang belum menjamin pemasangan di atas APK ini.

App ID `id.langkahberdua.nativeapp` terpisah dari pembungkus browser lama agar instalasi baru tidak mengganti aplikasi lama.
