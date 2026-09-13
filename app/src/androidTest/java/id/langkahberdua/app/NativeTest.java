package id.langkahberdua.app;
import android.content.*;
import android.graphics.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.util.concurrent.TimeUnit;
import java.io.*;

@RunWith(AndroidJUnit4.class) public class NativeTest {
 Context context;Store store;
 @Before public void before(){context=InstrumentationRegistry.getInstrumentation().getTargetContext();context.deleteDatabase("langkah-sheets.db");store=new Store(context);}
 @After public void after(){store.db.close();}
 JSONObject record(String visibility){return Store.make("Data uji",30000,"expense","2026-09-13",visibility,"","Belanja","");}
 JSONObject accepted(JSONObject r,int version)throws Exception{return Store.copy(r).put("version",version).put("updatedBy","Pasangan uji");}
 @Test public void privateNeverQueues(){store.save(record("private"),"","");assertEquals(30000,store.sum("expense"));assertEquals(0,store.pendingCount());}
 @Test public void sharedRetryAndReopen()throws Exception {JSONObject r=record("shared");store.save(r,"","");String id=r.getString("uuid"),op=store.get(id).op;assertEquals(0,store.sum("expense"));store.db.close();store=new Store(context);assertEquals(op,store.get(id).op);store.ack(id,op,accepted(r,1));store.ack(id,op,accepted(r,1));assertEquals(30000,store.sum("expense"));assertEquals(1,store.all().size());assertEquals(0,store.pendingCount());}
 @Test public void staleFormCannotOverwritePartner()throws Exception {JSONObject r=record("shared");store.pull(new JSONArray().put(accepted(r,1)));JSONObject partner=accepted(r,2).put("amount",40000);store.pull(new JSONArray().put(partner));try{store.save(accepted(r,1),"","");fail("Old form must be rejected");}catch(IllegalStateException expected){}assertEquals(40000,store.sum("expense"));}
 @Test public void pendingPullAndExplicitConflict()throws Exception {JSONObject r=record("shared");store.pull(new JSONArray().put(accepted(r,1)));JSONObject mine=accepted(r,1).put("amount",50000);store.save(mine,"","");String id=r.getString("uuid"),op=store.get(id).op;JSONObject partner=accepted(r,2).put("amount",40000);store.pull(new JSONArray().put(partner));assertEquals(50000,store.get(id).pending.getLong("amount"));store.conflict(id,op,partner);store.resolve(id,false);assertEquals(40000,store.sum("expense"));assertEquals(0,store.pendingCount());}
 @Test public void localPhotoNeverEntersWire()throws Exception {JSONObject r=record("shared");store.save(r,"/private/photo.jpg","abc");assertFalse(store.get(r.getString("uuid")).pending.toString().contains("photo.jpg"));assertFalse(Store.wire(r).has("hash"));}
 @Test public void ocrReadsActualBitmap()throws Exception {Bitmap b=Bitmap.createBitmap(1000,900,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(b);canvas.drawColor(Color.WHITE);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(Color.BLACK);p.setTextSize(54);p.setTypeface(Typeface.MONOSPACE);canvas.drawText("TOKO UJI",55,100,p);canvas.drawText("13/09/2026",55,200,p);canvas.drawText("Susu   30.000",55,310,p);canvas.drawText("Roti   20.000",55,410,p);canvas.drawText("TOTAL  50.000",55,530,p);TextRecognizer reader=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);try{Text result=Tasks.await(reader.process(InputImage.fromBitmap(b,0)),45,TimeUnit.SECONDS);assertTrue(result.getText().toUpperCase().contains("TOTAL"));assertEquals("50000",ReceiptParser.parse(result.getText()).total);}finally{reader.close();}}
 @Test public void nativeActivityLaunches()throws Exception {Intent i=new Intent(context,MainActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);android.app.Activity activity=InstrumentationRegistry.getInstrumentation().startActivitySync(i);android.os.SystemClock.sleep(2200);InstrumentationRegistry.getInstrumentation().waitForIdleSync();Bitmap b=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(b);File dir=context.getExternalFilesDir(null);try(OutputStream out=new FileOutputStream(new File(dir,"beranda.png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}InstrumentationRegistry.getInstrumentation().runOnMainSync(activity::finish);}
}
