package id.langkahberdua.app;

import android.content.*;
import android.security.keystore.*;
import android.util.Base64;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import org.json.*;

final class Cloud {
 final SharedPreferences p;
 Cloud(Context c){p=c.getSharedPreferences("cloud",0);}
 static boolean validUrl(String s){return s.matches("https://script\\.google\\.com/macros/s/[A-Za-z0-9_-]+/exec");}
 String url(){return p.getString("url","");}
 boolean connected(){return p.getBoolean("connected",false);}
 String role(){return p.getString("role","");}
 String token()throws Exception {String s=p.getString("token","");if(s.isEmpty())return "";byte[] bytes=Base64.decode(s,Base64.NO_WRAP);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,java.util.Arrays.copyOfRange(bytes,0,12)));return new String(c.doFinal(java.util.Arrays.copyOfRange(bytes,12,bytes.length)),StandardCharsets.UTF_8);}
 private SecretKey key()throws Exception {KeyStore k=KeyStore.getInstance("AndroidKeyStore");k.load(null);if(!k.containsAlias("lb_device")){KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder("lb_device",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());g.generateKey();}return (SecretKey)k.getKey("lb_device",null);}
 private void setToken(String s)throws Exception {Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());ByteArrayOutputStream b=new ByteArrayOutputStream();b.write(c.getIV());b.write(c.doFinal(s.getBytes(StandardCharsets.UTF_8)));if(!p.edit().putString("token",Base64.encodeToString(b.toByteArray(),Base64.NO_WRAP)).commit())throw new IOException("Token tidak tersimpan.");}
 private String random(){byte[] b=new byte[32];new SecureRandom().nextBytes(b);StringBuilder s=new StringBuilder();for(byte a:b)s.append(String.format(java.util.Locale.ROOT,"%02x",a&255));return s.toString();}
 JSONObject connect(String url,String code,String name)throws Exception {
  if(!validUrl(url))throw new IOException("URL harus berasal dari penerapan Apps Script dan berakhir /exec.");
  if(connected()&&!url.equals(url()))throw new IOException("Keluar dari ruang lama dahulu.");
  if(!url.equals(url())||token().isEmpty()){setToken(random());if(!p.edit().putString("url",url).commit())throw new IOException("Pengaturan tidak tersimpan.");}
  JSONObject result=request(url,new JSONObject().put("action","connect").put("code",code).put("name",name).put("newToken",token()));
  if(result.optBoolean("ok")){JSONObject u=result.getJSONObject("user");p.edit().putBoolean("connected",true).putString("name",u.getString("name")).putString("role",u.getString("role")).commit();}return result;
 }
 JSONObject call(JSONObject body)throws Exception {body.put("token",token());return request(url(),body);}
 void logout(){p.edit().clear().commit();}
 static JSONObject request(String endpoint,JSONObject body)throws Exception {
  if(!validUrl(endpoint))throw new IOException("Koneksi Google Sheets belum diatur.");
  HttpURLConnection c=(HttpURLConnection)new URL(endpoint).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(20000);c.setReadTimeout(30000);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=utf-8");
  try{byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=c.getOutputStream()){out.write(bytes);}int status=c.getResponseCode();
   if(status>=300&&status<400){String location=c.getHeaderField("Location");URL u=new URL(location);if(!u.getProtocol().equals("https")||!u.getHost().equals("script.googleusercontent.com")||u.getPort()!=-1)throw new IOException("Penerapan belum dapat diakses aplikasi. Periksa izin Apps Script.");c.disconnect();c=(HttpURLConnection)u.openConnection();c.setConnectTimeout(20000);c.setReadTimeout(30000);c.setInstanceFollowRedirects(false);status=c.getResponseCode();}
   if(status!=200)throw new IOException("Google Sheets belum merespons ("+status+"). Catatan tetap di ponsel.");
   try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>8000000)throw new IOException("Data terlalu besar. Perlu pengarsipan.");}try{return new JSONObject(out.toString("UTF-8"));}catch(JSONException e){throw new IOException("Respons bukan data aplikasi. Periksa penerapan Apps Script.");}}
  }finally{c.disconnect();}
 }
}
