package id.langkahberdua.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import java.text.NumberFormat;
import java.util.*;

/** Native offline beta. Financial records stay on this device. */
public class MainActivity extends Activity {
  private SQLiteDatabase db;
  private LinearLayout body;
  private String tab="Beranda";
  private final int sage=Color.rgb(97,124,102),ink=Color.rgb(38,60,48),cream=Color.rgb(247,244,235);
  private final Handler handler=new Handler(Looper.getMainLooper());
  private final Runnable enter=()->screen();
  private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
  @Override public void onCreate(Bundle s){super.onCreate(s);
    db=openOrCreateDatabase("langkah-native.db",MODE_PRIVATE,null);
    db.execSQL("CREATE TABLE IF NOT EXISTS entries(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,amount INTEGER NOT NULL,kind TEXT NOT NULL,date TEXT NOT NULL,deleted INTEGER NOT NULL DEFAULT 0)");
    if(s==null)splash();else {tab=s.getString("tab","Beranda");screen();}
  }
  @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putString("tab",tab);}
  @Override protected void onDestroy(){handler.removeCallbacks(enter);if(db!=null)db.close();super.onDestroy();}
  private TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(ink);t.setPadding(0,dp(7),0,dp(7));return t;}
  private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
  private void rounded(View v,int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(18));v.setBackground(d);}
  private Button button(String title,Runnable action){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextColor(Color.WHITE);b.setMinHeight(dp(48));rounded(b,sage);b.setOnClickListener(v->action.run());return b;}
  private void splash(){FrameLayout root=new FrameLayout(this);root.setBackgroundColor(sage);
    ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);
    try { String uri=getSharedPreferences("settings",0).getString("photo",""); if(!uri.isEmpty()){try(java.io.InputStream in=getContentResolver().openInputStream(android.net.Uri.parse(uri))){BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=2;image.setImageBitmap(BitmapFactory.decodeStream(in,null,o));}} }catch(Exception e){image.setBackgroundColor(sage);}
    root.addView(image,new FrameLayout.LayoutParams(-1,-1));
    LinearLayout copy=column();copy.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);copy.setPadding(dp(24),dp(40),dp(24),dp(90));
    copy.setBackgroundColor(0x550F2419);
    TextView title=text("Langkah Berdua",32);title.setTypeface(Typeface.SERIF,Typeface.BOLD);title.setTextColor(Color.WHITE);copy.addView(title);
    TextView motto=text("Semoga bisa disemogakan",20);motto.setTypeface(Typeface.SERIF,Typeface.ITALIC);motto.setTextColor(Color.WHITE);copy.addView(motto);
    ProgressBar progress=new ProgressBar(this);copy.addView(progress,new LinearLayout.LayoutParams(dp(32),dp(32)));
    root.addView(copy,new FrameLayout.LayoutParams(-1,-1));setContentView(root);handler.postDelayed(enter,2000);
  }
  private String money(long n){return NumberFormat.getCurrencyInstance(new Locale("id","ID")).format(n).replace(",00","");}
  private long sum(String kind){try(Cursor c=db.rawQuery("SELECT COALESCE(SUM(amount),0) FROM entries WHERE kind=? AND deleted=0",new String[]{kind})){c.moveToFirst();return c.getLong(0);}}
  private void screen(){
    LinearLayout root=column();root.setBackgroundColor(cream);
    root.setPadding(dp(18),dp(12),dp(18),dp(8));
    root.addView(text("LANGKAH BERDUA",12));TextView heading=text(tab,29);heading.setTypeface(null,Typeface.BOLD);root.addView(heading);
    ScrollView scroll=new ScrollView(this);body=column();scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    if(tab.equals("Beranda"))home();else if(tab.equals("Transaksi"))transactions();else if(tab.equals("Rencana"))plans();else if(tab.equals("Belanja"))shopping();else settings();
    LinearLayout nav=new LinearLayout(this);String[] names={"Beranda","Transaksi","Rencana","Belanja","Pengaturan"};
    for(String name:names){TextView t=text(name,11);t.setGravity(Gravity.CENTER);t.setMinHeight(dp(56));if(name.equals(tab)){rounded(t,0xFFE2E9DD);t.setTypeface(null,Typeface.BOLD);}t.setOnClickListener(v->{tab=name;screen();});nav.addView(t,new LinearLayout.LayoutParams(0,-2,1));}
    root.addView(nav);setContentView(root);
  }
  private void card(String title,String value){LinearLayout c=column();rounded(c,Color.WHITE);c.setPadding(dp(18),dp(10),dp(18),dp(14));c.addView(text(title,14));c.addView(text(value,26));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(8),0,dp(12));body.addView(c,p);}
  private void home(){card("Saldo berdasarkan pencatatan",money(sum("income")-sum("expense")));card("Pemasukan",money(sum("income")));card("Pengeluaran",money(sum("expense")));body.addView(button("+ Pengeluaran",()->edit(0,"","","expense","")));body.addView(text("",4));body.addView(button("+ Pemasukan",()->edit(0,"","","income","")));body.addView(text("Tersimpan di perangkat ini. Sinkronisasi pasangan dan login online belum aktif.",14));}
  private void transactions(){body.addView(button("Tambah transaksi",()->edit(0,"","","expense","")));list(false);}
  private void list(boolean trash){String sql="SELECT id,title,amount,kind,date FROM entries WHERE deleted=? AND kind IN ('income','expense') ORDER BY date DESC,id DESC";
    try(Cursor c=db.rawQuery(sql,new String[]{trash?"1":"0"})){if(c.getCount()==0)body.addView(text(trash?"Sampah kosong.":"Belum ada transaksi.",16));while(c.moveToNext()){long id=c.getLong(0),amount=c.getLong(2);String title=c.getString(1),kind=c.getString(3),date=c.getString(4);TextView row=text(title+"\n"+money(amount)+" · "+(kind.equals("income")?"Pemasukan":"Pengeluaran")+"\n"+date,16);row.setPadding(dp(12),dp(12),dp(12),dp(12));row.setOnClickListener(v->{if(trash){db.execSQL("UPDATE entries SET deleted=0 WHERE id=?",new Object[]{id});screen();}else edit(id,title,Long.toString(amount),kind,date);});body.addView(row);}}
  }
  private EditText input(LinearLayout form,String label,String value,boolean numeric){form.addView(text(label,14));EditText e=new EditText(this);e.setSingleLine(true);e.setText(value);e.setTextSize(16);if(numeric)e.setInputType(2);form.addView(e);return e;}
  private void edit(long id,String title,String amount,String kind,String date){LinearLayout f=column();f.setPadding(dp(20),0,dp(20),dp(12));EditText name=input(f,"Judul",title,false),value=input(f,"Nominal Rupiah",amount,true);
    Spinner type=new Spinner(this);type.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Pengeluaran","Pemasukan"}));type.setSelection(kind.equals("income")?1:0);f.addView(type);
    Calendar cal=Calendar.getInstance();if(!date.isEmpty()){String[] p=date.split("-");cal.set(Integer.parseInt(p[0]),Integer.parseInt(p[1])-1,Integer.parseInt(p[2]));}
    Button choose=new Button(this);Runnable label=()->choose.setText(String.format(Locale.ROOT,"%02d/%02d/%04d",cal.get(5),cal.get(2)+1,cal.get(1)));label.run();choose.setOnClickListener(v->new DatePickerDialog(this,(view,y,m,d)->{cal.set(y,m,d);label.run();},cal.get(1),cal.get(2),cal.get(5)).show());f.addView(choose);
    ScrollView scroll=new ScrollView(this);scroll.addView(f);AlertDialog d=new AlertDialog.Builder(this).setTitle(id==0?"Catat transaksi":"Ubah transaksi").setView(scroll).setPositiveButton("Simpan",null).setNegativeButton("Batal",null).setNeutralButton(id==0?"":"Hapus",null).create();d.show();d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    d.getButton(-1).setOnClickListener(v->{try{long n=Long.parseLong(value.getText().toString());if(n<=0||n>1000000000000L)throw new IllegalArgumentException();String nameValue=name.getText().toString().trim();if(nameValue.isEmpty()||nameValue.length()>120){name.setError("Isi judul, maksimal 120 karakter");return;}ContentValues cv=new ContentValues();cv.put("title",nameValue);cv.put("amount",n);cv.put("kind",type.getSelectedItemPosition()==1?"income":"expense");cv.put("date",String.format(Locale.ROOT,"%04d-%02d-%02d",cal.get(1),cal.get(2)+1,cal.get(5)));if(id==0)db.insertOrThrow("entries",null,cv);else db.update("entries",cv,"id=?",new String[]{Long.toString(id)});d.dismiss();screen();}catch(NumberFormatException e){value.setError("Masukkan angka Rupiah tanpa titik");}catch(IllegalArgumentException e){value.setError("Nominal harus 1–1.000.000.000.000");}});
    if(id>0)d.getButton(-3).setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Pindahkan catatan ke sampah? Dapat dipulihkan dari Pengaturan.").setNegativeButton("Batal",null).setPositiveButton("Hapus",(a,b)->{db.execSQL("UPDATE entries SET deleted=1 WHERE id=?",new Object[]{id});d.dismiss();screen();}).show());
  }
  private void plans(){android.content.SharedPreferences p=getSharedPreferences("settings",0);long target=p.getLong("target",0),saved=p.getLong("saved",0);card("Target tabungan",money(target));card("Dana dialokasikan",money(saved));body.addView(text("Kekurangan: "+money(Math.max(0,target-saved))+"\nAlokasi tabungan tidak dihitung sebagai pengeluaran.",16));body.addView(button("Atur target dan alokasi",()->{LinearLayout f=column();f.setPadding(dp(20),0,dp(20),0);EditText t=input(f,"Target Rupiah",Long.toString(target),true),s=input(f,"Dana dialokasikan",Long.toString(saved),true);AlertDialog d=new AlertDialog.Builder(this).setTitle("Target tabungan").setView(f).setNegativeButton("Batal",null).setPositiveButton("Simpan",null).create();d.show();d.getButton(-1).setOnClickListener(v->{try{long a=Long.parseLong(t.getText().toString()),b=Long.parseLong(s.getText().toString());if(a<0||b<0||a>1000000000000L||b>1000000000000L)throw new NumberFormatException();p.edit().putLong("target",a).putLong("saved",b).apply();d.dismiss();screen();}catch(NumberFormatException e){t.setError("Isi angka Rupiah yang valid");}}); }));}
  private void shopping(){body.addView(button("Tambah barang",()->{LinearLayout f=column();f.setPadding(dp(20),0,dp(20),0);EditText name=input(f,"Nama dan jumlah barang","",false);AlertDialog d=new AlertDialog.Builder(this).setTitle("Daftar belanja").setView(f).setNegativeButton("Batal",null).setPositiveButton("Tambah",null).create();d.show();d.getButton(-1).setOnClickListener(v->{String n=name.getText().toString().trim();if(n.isEmpty()){name.setError("Isi nama barang");return;}db.execSQL("INSERT INTO entries(title,amount,kind,date) VALUES(?,0,'shopping','')",new Object[]{n});d.dismiss();screen();});}));try(Cursor c=db.rawQuery("SELECT id,title,amount FROM entries WHERE kind='shopping' AND deleted=0 ORDER BY id DESC",null)){while(c.moveToNext()){long id=c.getLong(0);CheckBox item=new CheckBox(this);item.setText(c.getString(1));item.setChecked(c.getInt(2)==1);item.setTextSize(16);item.setPadding(0,dp(8),0,dp(8));item.setOnCheckedChangeListener((b,checked)->db.execSQL("UPDATE entries SET amount=? WHERE id=?",new Object[]{checked?1:0,id}));item.setOnLongClickListener(v->{new AlertDialog.Builder(this).setMessage("Hapus barang ini?").setNegativeButton("Batal",null).setPositiveButton("Hapus",(a,b)->{db.execSQL("UPDATE entries SET deleted=1 WHERE id=?",new Object[]{id});screen();}).show();return true;});body.addView(item);}}body.addView(text("Tekan lama barang untuk menghapus. Mencentang barang tidak membuat transaksi otomatis.",14));}
  private void settings(){body.addView(button("Pilih foto layar pembuka",()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,102);}));body.addView(text("Versi native 0.3 · penyimpanan lokal\n\nCatatan berada di Android ini. Login, sinkronisasi pasangan, scan OCR, dan transfer dompet belum tersedia dalam versi ini. Data lokal dapat hilang jika aplikasi dihapus.",16));body.addView(button("Ekspor transaksi CSV",this::export));body.addView(text("Pulihkan transaksi: ketuk catatan di bawah.",16));list(true);}
  private void export(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("text/csv");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,"Langkah-Berdua.csv");startActivityForResult(i,101);}
  private String csv(String s){if(s.matches("^[=+@\\-\\t\\r].*"))s="'"+s;return "\""+s.replace("\"","\"\"")+"\"";}
  @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null)return;if(request==102){try{getContentResolver().takePersistableUriPermission(data.getData(),Intent.FLAG_GRANT_READ_URI_PERMISSION);getSharedPreferences("settings",0).edit().putString("photo",data.getData().toString()).apply();Toast.makeText(this,"Foto pembuka tersimpan di perangkat",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"Foto tidak dapat digunakan. Coba foto lain.",Toast.LENGTH_LONG).show();}return;}if(request!=101)return;try(java.io.OutputStream out=getContentResolver().openOutputStream(data.getData());Cursor c=db.rawQuery("SELECT title,amount,kind,date FROM entries WHERE deleted=0 AND kind IN ('expense','income') ORDER BY date,id",null)){StringBuilder b=new StringBuilder("\uFEFFJudul,Nominal,Jenis,Tanggal\r\n");while(c.moveToNext())b.append(csv(c.getString(0))).append(',').append(c.getLong(1)).append(',').append(c.getString(2)).append(',').append(c.getString(3)).append("\r\n");out.write(b.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));Toast.makeText(this,"CSV tersimpan",Toast.LENGTH_LONG).show();}catch(Exception e){Toast.makeText(this,"Ekspor gagal. Coba lokasi penyimpanan lain.",Toast.LENGTH_LONG).show();}}
}
