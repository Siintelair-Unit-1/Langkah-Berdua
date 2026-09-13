# Hubungkan Google Sheets dan pasangan

APK Android 0.4 dapat mencatat dan memindai struk tanpa layanan berbayar. Sinkronisasi memerlukan satu kali aktivasi Google Apps Script pada akun pemilik spreadsheet. Koneksi Google Drive di ChatGPT tidak otomatis memberi aplikasi Android akses ke spreadsheet. Status aplikasi tetap **belum dihubungkan** sampai aktivasi berhasil.

## Sekali saja, oleh pemilik

Lebih mudah melalui laptop atau komputer. Anda tidak perlu membuat akun layanan lain.

1. Buka [spreadsheet baru](https://sheets.new), beri nama **Langkah Berdua**. Biarkan akses file **Dibatasi**; jangan menjadikan spreadsheet publik.
2. Di spreadsheet pilih **Ekstensi → Apps Script**. Hapus contoh kode yang ada.
3. Buka [kode penghubung Code.gs](../backend/Code.gs). Klik tombol **Copy raw file** untuk menyalin seluruh kode, lalu tempel ke editor Apps Script dan simpan. Kode ini tidak mengandung kunci rahasia.
4. Pada pilihan fungsi di atas editor, pilih **setupOwner**, lalu **Jalankan**. Setujui akses spreadsheet untuk skrip yang Anda buat sendiri. Kembali ke tab spreadsheet: dialog menampilkan kode pemilik 64 karakter, berlaku 24 jam. Simpan kode sementara; jangan bagikan kode pemilik kepada pasangan. Fungsi ini membuat tab `LB_EVENTS` sebagai jurnal data.
5. Di editor Apps Script pilih **Deploy/Terapkan → New deployment/Penerapan baru → Web app/Aplikasi web**. Pilih **Execute as/Jalankan sebagai: Me/Saya** dan **Who has access/Siapa yang memiliki akses: Anyone/Siapa saja**. Klik **Deploy/Terapkan**, salin URL yang berakhir `/exec`. Endpoint menerima permintaan dari aplikasi, tetapi data tetap diperiksa dengan token perangkat rahasia. Membuka URL saja tidak menampilkan transaksi.
6. Buka aplikasi Android **Langkah Berdua 0.4 → Pengaturan → Hubungkan Google Sheets**. Isi nama, URL tadi dan kode pemilik. Ketuk **Hubungkan**. Tunggu status **Tersinkron**.

Jika akun Google organisasi tidak menyediakan pilihan “Anyone”, kebijakan organisasi mungkin membatasi penerapan. Gunakan akun Google pribadi yang Anda miliki. Jangan membagikan kata sandi, token perangkat, atau kode pemilik di percakapan maupun GitHub.

## Hubungkan pasangan

1. Di aplikasi pemilik pilih **Pengaturan → Kelola koneksi pasangan → Buat undangan pasangan → Salin undangan**.
2. Berikan undangan itu secara pribadi kepada pasangan. Undangan berlaku 24 jam dan hanya sekali pakai.
3. Pasangan memasang APK yang sama pada Android, memilih **Hubungkan Google Sheets**, mengisi nama, lalu menempel undangan pada kolom **Kode atau undangan**. Kolom URL boleh kosong ketika menggunakan undangan lengkap.
4. Saat mencatat pilih akses **Bersama**. Kedua ponsel akan memperbarui data saat aplikasi dibuka atau setiap sekitar 30 detik ketika aktif. Tombol **Sinkronkan sekarang** memperbarui segera.

Satu ruang untuk dua orang dan satu perangkat aktif per orang. Versi ini belum tersedia di iPhone dan belum memakai login Google/ChatGPT. Pasangan yang hanya memiliki iPhone belum dapat memasang APK ini.

## Scan struk

Pilih **Scan struk → Ambil foto/Pilih dari galeri**. Atur rotasi, pemotongan tepi, atau tulisan hitam putih, lalu **Baca struk**. Periksa toko, tanggal, total dan setiap barang. Kolom kosong berarti belum terbaca. Diskon pada barang tercermin dalam jumlah harga baris; diskon tambahan struk diisi terpisah. Aplikasi memperingatkan selisih total dan foto identik. Setelah Anda menyetujui, seluruh struk menjadi **satu** transaksi pengeluaran.

OCR berjalan di perangkat memakai model Latin yang disertakan dalam APK. Foto tidak dikirim ke layanan OCR. Foto asli berada di penyimpanan pribadi aplikasi pada ponsel pemindai; pasangan menerima rincian teks, belum foto. Simpan juga foto asli di galeri sebagai cadangan.

## Memeriksa sambungan

Gunakan catatan uji yang Anda buat sendiri, misalnya “Uji koneksi” Rp1, lalu hapus setelah selesai. Periksa bahwa catatan bersama muncul pada kedua perangkat, sedangkan catatan pribadi tidak muncul pada perangkat pasangan atau spreadsheet. Putuskan internet, buat satu catatan bersama, sambungkan lagi, lalu pastikan hanya satu catatan muncul. Jika kedua orang mengubah catatan yang sama, buka **Perubahan berbenturan** untuk memilih versi.

Pengujian otomatis menggunakan dua identitas uji telah disediakan dalam source. Koneksi ke spreadsheet Anda dan pengujian pada kedua ponsel tetap perlu dilakukan setelah penerapan diaktifkan.

## Data dan pemulihan

- Google Sheets menyimpan jurnal versi data bersama, bukan satu transaksi baru untuk setiap baris. Jangan menjumlahkan kolom nominal jurnal secara langsung; aplikasi memakai versi terbaru dari setiap ID.
- Jangan mengubah kolom atau menghapus baris `LB_EVENTS` secara manual. Gunakan aplikasi untuk transaksi. Untuk cadangan, buat salinan spreadsheet atau unduh XLSX melalui menu File. Cadangan ini adalah data; akses perangkat dan penerapan Apps Script perlu disiapkan lagi jika beralih ke salinan.
- Catatan pribadi dan target tabungan berada di ponsel. Ekspor CSV adalah salinan transaksi, belum cadangan lengkap untuk pemulihan otomatis versi 0.4. Menghapus aplikasi dapat menghapus data lokal dan foto.
- Pengiriman berulang memakai ID operasi tetap. Perubahan memakai pemeriksaan versi dan tidak menimpa versi pasangan secara diam-diam. Transaksi baru yang menunggu sinkronisasi belum dimasukkan dalam saldo tersimpan.
- Pemilik dapat mencabut akses pasangan. Data bersama tetap ada di spreadsheet, dan salinan yang pernah diterima pasangan tidak dapat ditarik kembali dari ponselnya.
- Jika ponsel pemilik hilang, jalankan `setupOwner` lagi dan hubungkan perangkat baru; akses perangkat pemilik lama diganti setelah kode ditebus. Untuk pasangan, cabut akses lama dan buat undangan baru.
- Layanan Apps Script mengikuti kuota Google. Jurnal dibatasi 20.000 perubahan pada versi ini. Aplikasi menampilkan kegagalan dan menyimpan perubahan menunggu ketika kuota/koneksi bermasalah.

## Dari versi 0.3

APK 0.4 dipasang berdampingan dengan 0.3 agar data lama tidak perlu dihapus. Pada 0.3 pilih **Pengaturan → Ekspor transaksi CSV**. Pada 0.4 pilih **Pengaturan → Impor CSV versi 0.3**. Transaksi diimpor sebagai pribadi. Target, daftar belanja dan foto belum ikut impor. File identik yang diimpor lagi tidak menambah transaksi yang sama.

## Rujukan teknis

[Google: penerapan Apps Script](https://developers.google.com/apps-script/guides/web), [pengalihan respons Content Service](https://developers.google.com/apps-script/guides/content), [ML Kit text recognition Android](https://developers.google.com/ml-kit/vision/text-recognition/v2/android).
