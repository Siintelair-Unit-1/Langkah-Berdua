package id.langkahberdua.app;
import android.app.*;
import android.content.*;
import android.widget.*;
import android.util.Base64;
import org.json.*;
import java.nio.charset.StandardCharsets;

final class ConnectionScreen {
 final MainActivity a;ConnectionScreen(MainActivity a){this.a=a;}
 void show(){if(!a.cloud.connected()){join();return;}
  LinearLayout f=a.form();f.addView(a.text("Terhubung sebagai "+a.cloud.p.getString("name","")+". Catatan bersama berada di spreadsheet pemilik. Undangan hanya untuk pasangan dan berlaku 24 jam.",15));
  f.addView(a.button("Buat undangan pasangan",()->action("invite",r->{try{String invite="LB1:"+Base64.encodeToString(new JSONObject().put("url",a.cloud.url()).put("code",r.getString("code")).toString().getBytes(StandardCharsets.UTF_8),Base64.URL_SAFE|Base64.NO_WRAP);LinearLayout box=a.form();TextView t=a.text(invite,13);t.setTextIsSelectable(true);box.addView(a.text("Berlaku 24 jam dan satu kali pakai. Berikan hanya kepada pasangan.",14));box.addView(t);box.addView(a.button("Salin undangan",()->{((ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Undangan pasangan",invite));a.message("Undangan disalin");}));ScrollView scroll=new ScrollView(a);scroll.addView(box);a.show(new AlertDialog.Builder(a).setTitle("Undang pasangan").setView(scroll).setPositiveButton("Tutup",null).create());}catch(Exception e){a.message("Undangan tidak dapat dibaca.");}})));
  if(a.cloud.role().equals("owner"))f.addView(a.button("Putuskan akses pasangan",()->a.confirm("Pasangan tidak dapat membaca atau mengubah data baru. Salinan yang sudah diunduh tetap dapat dimilikinya. Data bersama di spreadsheet tetap ada. Lanjutkan?",()->action("disconnectPartner",r->a.message("Akses pasangan dicabut.")))));
  AlertDialog d=a.dialog("Koneksi pasangan",f,"Tutup");d.getButton(-1).setOnClickListener(v->d.dismiss());
  f.addView(a.button("Keluar dari ruang di ponsel ini",()->a.confirm("Salinan dan perubahan bersama yang belum tersinkron di ponsel ini akan dihapus. Data di spreadsheet tetap ada. Catatan pribadi tetap ada. Ekspor dahulu jika perlu.",()->{if(a.busy){a.message("Tunggu sinkronisasi selesai.");return;}a.store.clearShared();a.cloud.logout();a.status="Google Sheets belum dihubungkan";d.dismiss();a.screen();})));
 }
 void join(){LinearLayout f=a.form();f.addView(a.text("Pemilik: isi URL penerapan dan kode pemilik dari Google Sheets. Pasangan: tempel undangan dari aplikasi pemilik. Satu ruang untuk dua orang, satu perangkat aktif per orang.",14));EditText name=a.input(f,"Nama Anda",a.cloud.p.getString("name",""),false),url=a.input(f,"URL Apps Script",a.cloud.url(),false),code=a.input(f,"Kode atau undangan","",false);code.setInputType(129);AlertDialog d=a.dialog("Hubungkan perangkat",f,"Hubungkan");
  d.getButton(-1).setOnClickListener(v->{if(a.busy){a.message("Tunggu proses koneksi selesai.");return;}String u=url.getText().toString().trim(),c=code.getText().toString().trim();try{if(c.startsWith("LB1:")){JSONObject invite=new JSONObject(new String(Base64.decode(c.substring(4),Base64.URL_SAFE),StandardCharsets.UTF_8));u=invite.getString("url");c=invite.getString("code");}if(!Cloud.validUrl(u)||!c.matches("[a-f0-9]{64}")||name.getText().toString().trim().isEmpty()||name.length()>80)throw new IllegalArgumentException();if(!a.cloud.url().isEmpty()&&!a.cloud.url().equals(u)&&a.store.all().stream().anyMatch(r->r.display().optString("visibility").equals("shared")))throw new IllegalArgumentException();}catch(Exception e){a.message("Periksa URL, nama, dan kode. Keluar dari ruang lama dahulu jika ingin mengganti spreadsheet.");return;}
   final String endpoint=u,join=c,display=name.getText().toString().trim();a.busy=true;d.setCancelable(false);d.getButton(-2).setEnabled(false);d.getButton(-1).setEnabled(false);a.worker.execute(()->{try{JSONObject result=a.cloud.connect(endpoint,join,display);a.runOnUiThread(()->{if(a.isDestroyed())return;a.busy=false;if(result.optBoolean("ok")){d.dismiss();a.status="Perangkat terhubung";a.sync();}else{d.setCancelable(true);d.getButton(-2).setEnabled(true);d.getButton(-1).setEnabled(true);a.message(result.optString("error"));}});}catch(Exception e){a.message(e.getMessage());a.runOnUiThread(()->{a.busy=false;if(!a.isDestroyed())d.setCancelable(true);d.getButton(-2).setEnabled(true);d.getButton(-1).setEnabled(true);});}});
  });
 }
 interface Result {void done(JSONObject r);}
 void action(String name,Result done){if(a.busy){a.message("Tunggu sinkronisasi selesai.");return;}a.busy=true;a.worker.execute(()->{try{JSONObject r=a.cloud.call(new JSONObject().put("action",name));a.runOnUiThread(()->{if(a.isDestroyed())return;if(r.optBoolean("ok"))done.done(r);else a.message(r.optString("error"));});}catch(Exception e){a.message(e.getMessage());}finally{a.busy=false;}});}
}
