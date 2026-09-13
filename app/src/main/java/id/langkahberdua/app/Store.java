package id.langkahberdua.app;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;
import java.util.*;

/** The accepted value and pending operation are separate: retries never duplicate money. */
final class Store {
 final SQLiteDatabase db;
 Store(Context c){db=c.openOrCreateDatabase("langkah-sheets.db",0,null);db.execSQL("CREATE TABLE IF NOT EXISTS records(id TEXT PRIMARY KEY, accepted TEXT NOT NULL, pending TEXT, op TEXT, base INTEGER, conflict TEXT, error TEXT, photo TEXT NOT NULL DEFAULT '', hash TEXT NOT NULL DEFAULT '')");}
 static JSONObject copy(JSONObject j){try{return new JSONObject(j.toString());}catch(JSONException e){throw new IllegalArgumentException(e);}}
 static JSONObject make(String title,long amount,String kind,String date,String visibility,String note,String category,String receipt){
  try{return new JSONObject().put("uuid",UUID.randomUUID().toString()).put("title",title).put("amount",amount).put("kind",kind).put("date",date).put("visibility",visibility).put("deleted",false).put("note",note).put("category",category).put("receipt",receipt);}catch(JSONException e){throw new IllegalArgumentException(e);}
 }
 static JSONObject wire(JSONObject r){JSONObject out=new JSONObject();try{for(String k:new String[]{"uuid","title","amount","kind","date","visibility","deleted","note","category","receipt"})out.put(k,r.get(k));return out;}catch(JSONException e){throw new IllegalArgumentException(e);}}
 static final class Row {
  JSONObject accepted,pending,conflict;String id,op,error,photo,hash;long base;
  JSONObject display(){return pending==null?accepted:pending;}
 }
 private Row row(Cursor c)throws JSONException {Row r=new Row();r.id=c.getString(0);r.accepted=new JSONObject(c.getString(1));r.pending=c.isNull(2)?null:new JSONObject(c.getString(2));r.op=c.getString(3);r.base=c.getLong(4);r.conflict=c.isNull(5)?null:new JSONObject(c.getString(5));r.error=c.getString(6);r.photo=c.getString(7);r.hash=c.getString(8);return r;}
 synchronized List<Row> all(){List<Row> out=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT id,accepted,pending,op,base,conflict,error,photo,hash FROM records ORDER BY rowid DESC",null)){while(c.moveToNext())out.add(row(c));}catch(JSONException e){throw new IllegalStateException("Data lokal rusak. Jangan hapus aplikasi.",e);}return out;}
 synchronized Row get(String id){for(Row r:all())if(r.id.equals(id))return r;return null;}
 synchronized void save(JSONObject value,String photo,String hash){
  String id=value.optString("uuid");Row old=get(id);if(old!=null&&old.pending!=null)throw new IllegalStateException("Tunggu sinkronisasi atau selesaikan benturan perubahan dahulu.");
  if(old!=null&&value.optString("visibility").equals("shared")&&value.optLong("version",0)!=old.accepted.optLong("version",0))throw new IllegalStateException("Catatan baru diubah pasangan. Tutup formulir lalu buka kembali untuk meninjau perubahan.");
  ContentValues cv=new ContentValues();cv.put("id",id);cv.put("photo",photo==null?"":photo);cv.put("hash",hash==null?"":hash);
  if(value.optString("visibility").equals("shared")){
   // New shared records have no accepted version, so they do not affect the balance yet.
   JSONObject accepted=old==null?new JSONObject():old.accepted;
   cv.put("accepted",accepted.toString());cv.put("pending",wire(value).toString());cv.put("base",accepted.optLong("version",0));cv.put("op",UUID.randomUUID().toString());
  }else cv.put("accepted",value.toString());
  db.insertWithOnConflict("records",null,cv,SQLiteDatabase.CONFLICT_REPLACE);
 }
 synchronized void ack(String id,String op,JSONObject record){Row r=get(id);if(r==null||!Objects.equals(op,r.op))return;ContentValues cv=clearQueue();cv.put("accepted",record.toString());db.update("records",cv,"id=?",new String[]{id});}
 private ContentValues clearQueue(){ContentValues c=new ContentValues();for(String k:new String[]{"pending","op","base","conflict","error"})c.putNull(k);return c;}
 synchronized void conflict(String id,String op,JSONObject current){Row r=get(id);if(r==null||!Objects.equals(r.op,op))return;ContentValues cv=new ContentValues();cv.put("conflict",current==null?"{}":current.toString());cv.put("error","CONFLICT");db.update("records",cv,"id=?",new String[]{id});}
 synchronized void failed(String id,String op,String error){Row r=get(id);if(r==null||!Objects.equals(r.op,op))return;ContentValues cv=new ContentValues();cv.put("error",error);db.update("records",cv,"id=?",new String[]{id});}
 synchronized void pull(JSONArray records)throws JSONException {db.beginTransaction();try{for(int i=0;i<records.length();i++){JSONObject v=records.getJSONObject(i);String id=v.getString("uuid");Row old=get(id);if(old!=null&&old.pending!=null)continue;if(old!=null&&!old.accepted.optString("visibility").equals("shared"))continue;ContentValues cv=new ContentValues();cv.put("id",id);cv.put("accepted",v.toString());cv.put("photo",old==null?"":old.photo);cv.put("hash",old==null?"":old.hash);db.insertWithOnConflict("records",null,cv,SQLiteDatabase.CONFLICT_REPLACE);}db.setTransactionSuccessful();}finally{db.endTransaction();}}
 synchronized void resolve(String id,boolean keepLocal){Row r=get(id);if(r==null||r.conflict==null)return;ContentValues cv=clearQueue();cv.put("accepted",r.conflict.toString());if(keepLocal){cv.put("pending",r.pending.toString());cv.put("base",r.conflict.optLong("version",0));cv.put("op",UUID.randomUUID().toString());}db.update("records",cv,"id=?",new String[]{id});}
 synchronized void discardPending(String id){Row r=get(id);if(r==null)return;if(r.accepted.length()==0)db.delete("records","id=?",new String[]{id});else db.update("records",clearQueue(),"id=?",new String[]{id});}
 synchronized boolean duplicate(String hash){if(hash==null||hash.isEmpty())return false;for(Row r:all())if(hash.equals(r.hash)&&!r.display().optBoolean("deleted"))return true;return false;}
 synchronized long sum(String kind){long n=0;for(Row r:all()){JSONObject a=r.accepted;if(!a.optBoolean("deleted")&&a.optString("kind").equals(kind))n+=a.optLong("amount");}return n;}
 synchronized int pendingCount(){int n=0;for(Row r:all())if(r.pending!=null)n++;return n;}
 synchronized void clearShared(){for(Row r:all())if(r.display().optString("visibility").equals("shared"))db.delete("records","id=?",new String[]{r.id});}
}
