package id.langkahberdua.app;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.widget.*;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.*;
import java.security.MessageDigest;

final class ReceiptScanner {
 interface Callback {void ready(String raw,String path,String hash);void message(String message);}
 final Activity activity;final Callback callback;File cameraFile;
 ReceiptScanner(Activity a,Callback c){activity=a;callback=c;}
 void choose(){new AlertDialog.Builder(activity).setTitle("Scan struk").setItems(new String[]{"Ambil foto struk","Pilih dari galeri"},(d,n)->{if(n==0)camera();else{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);activity.startActivityForResult(i,201);}}).show();}
 private void camera(){try{File dir=new File(activity.getFilesDir(),"receipts");dir.mkdirs();cameraFile=File.createTempFile("camera-",".jpg",dir);Uri uri=FileProvider.getUriForFile(activity,activity.getPackageName()+".files",cameraFile);Intent i=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT,uri);i.setClipData(ClipData.newRawUri("Struk",uri));i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);activity.startActivityForResult(i,202);}catch(Exception e){callback.message("Kamera tidak tersedia. Pilih foto struk dari galeri.");}}
 void result(int request,int result,Intent data){if(result!=Activity.RESULT_OK)return;try{File file;if(request==202){if(cameraFile==null)throw new IOException();file=cameraFile;}else if(request==201&&data!=null){File dir=new File(activity.getFilesDir(),"receipts");dir.mkdirs();file=File.createTempFile("receipt-",".jpg",dir);try(InputStream in=activity.getContentResolver().openInputStream(data.getData());OutputStream out=new FileOutputStream(file)){byte[] b=new byte[8192];int n,total=0;while((n=in.read(b))!=-1){total+=n;if(total>25000000)throw new IOException();out.write(b,0,n);}}}else return;preview(file);}catch(Exception e){callback.message("Foto tidak dapat dibuka atau lebih dari 25 MB. Pilih gambar lain.");}}
 void preview(File file)throws Exception {
  BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getPath(),options);if(options.outWidth<=0||options.outHeight<=0)throw new IOException();options.inSampleSize=1;while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>2000)options.inSampleSize*=2;options.inJustDecodeBounds=false;
  Bitmap original=BitmapFactory.decodeFile(file.getPath(),options);if(original==null)throw new IOException();ExifInterface exif=new ExifInterface(file);Matrix rotation=new Matrix();rotation.postRotate(exif.getRotationDegrees());if(exif.isFlipped())rotation.postScale(-1,1);final Bitmap[] working={Bitmap.createBitmap(original,0,0,original.getWidth(),original.getHeight(),rotation,true)};
  LinearLayout form=new LinearLayout(activity);form.setOrientation(1);form.setPadding(24,8,24,16);TextView hint=new TextView(activity);hint.setText("Luruskan struk dan atur tepi yang dipotong. Foto asli tetap disimpan di ponsel. OCR berjalan di perangkat.");form.addView(hint);
  ImageView image=new ImageView(activity);image.setAdjustViewBounds(true);image.setImageBitmap(working[0]);form.addView(image,new LinearLayout.LayoutParams(-1,420));
  SeekBar[] crop=new SeekBar[4];String[] labels={"Potong atas","Potong bawah","Potong kiri","Potong kanan"};
  for(int i=0;i<4;i++){TextView label=new TextView(activity);label.setText(labels[i]+" (0–30%)");form.addView(label);crop[i]=new SeekBar(activity);crop[i].setMax(30);form.addView(crop[i]);}
  CheckBox contrast=new CheckBox(activity);contrast.setText("Perjelas tulisan hitam putih");form.addView(contrast);
  Button turn=new Button(activity);turn.setText("Putar 90°");turn.setOnClickListener(v->{Matrix m=new Matrix();m.postRotate(90);working[0]=Bitmap.createBitmap(working[0],0,0,working[0].getWidth(),working[0].getHeight(),m,true);for(SeekBar c:crop)c.setProgress(0);image.setImageBitmap(working[0]);});form.addView(turn);
  Button show=new Button(activity);show.setText("Pratinjau potongan");show.setOnClickListener(v->image.setImageBitmap(transform(working[0],crop,contrast.isChecked())));form.addView(show);
  ScrollView scroll=new ScrollView(activity);scroll.addView(form);AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Siapkan foto struk").setView(scroll).setNegativeButton("Batal",null).setPositiveButton("Baca struk",null).create();dialog.show();
  dialog.getButton(-1).setOnClickListener(v->{dialog.getButton(-1).setEnabled(false);dialog.getButton(-1).setText("Membaca…");Bitmap bitmap=transform(working[0],crop,contrast.isChecked());com.google.mlkit.vision.text.TextRecognizer reader=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);reader.process(InputImage.fromBitmap(bitmap,0)).addOnSuccessListener(result->{reader.close();dialog.dismiss();try{callback.ready(result.getText(),file.getAbsolutePath(),hash(file));}catch(Exception e){callback.message("Foto tidak dapat disimpan. Coba lagi.");}}).addOnFailureListener(error->{reader.close();dialog.getButton(-1).setEnabled(true);dialog.getButton(-1).setText("Baca struk");callback.message("OCR gagal. Coba foto lebih terang atau isi transaksi secara manual.");});});
 }
 private Bitmap transform(Bitmap b,SeekBar[] crop,boolean contrast){int top=b.getHeight()*crop[0].getProgress()/100,bottom=b.getHeight()*crop[1].getProgress()/100,left=b.getWidth()*crop[2].getProgress()/100,right=b.getWidth()*crop[3].getProgress()/100;Bitmap cut=Bitmap.createBitmap(b,left,top,b.getWidth()-left-right,b.getHeight()-top-bottom);if(!contrast)return cut;Bitmap out=Bitmap.createBitmap(cut.getWidth(),cut.getHeight(),Bitmap.Config.ARGB_8888);ColorMatrix gray=new ColorMatrix();gray.setSaturation(0);ColorMatrix bright=new ColorMatrix(new float[]{1.4f,0,0,0,-40,0,1.4f,0,0,-40,0,0,1.4f,0,-40,0,0,0,1,0});gray.postConcat(bright);Paint paint=new Paint();paint.setColorFilter(new ColorMatrixColorFilter(gray));new Canvas(out).drawBitmap(cut,0,0,paint);return out;}
 static String hash(File f)throws Exception {MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)md.update(b,0,n);}StringBuilder s=new StringBuilder();for(byte b:md.digest())s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();}
}
