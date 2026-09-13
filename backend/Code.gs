/* Langkah Berdua: bind to YOUR Google Spreadsheet; deploy as Apps Script web app.
 * Only shared records enter this sheet. Private entries stay on each phone.
 * A single append-only journal + ScriptLock provides version checks and retry deduplication.
 */
const HEADERS=['operation_id','fingerprint','record_id','version','changed_at','actor_id','actor_name','title','amount_idr','kind','deleted','record_json'];
function onOpen(){SpreadsheetApp.getUi().createMenu('Langkah Berdua').addItem('Siapkan koneksi pemilik','setupOwner').addToUi();}
function setupOwner(){
  const lock=LockService.getScriptLock();lock.waitLock(20000);
  let code;
  try{
    const p=PropertiesService.getScriptProperties(), ss=SpreadsheetApp.getActiveSpreadsheet();
    if(!ss)throw Error('Buka Apps Script melalui menu Ekstensi di spreadsheet Anda.');
    p.setProperty('SPREADSHEET_ID',ss.getId());
    let sh=ss.getSheetByName('LB_EVENTS');
    if(!sh){sh=ss.insertSheet('LB_EVENTS');sh.appendRow(HEADERS);sh.setFrozenRows(1);sh.getRange(1,1,1,HEADERS.length).setBackground('#617c66').setFontColor('#ffffff');}
    if(JSON.stringify(sh.getRange(1,1,1,HEADERS.length).getValues()[0])!==JSON.stringify(HEADERS))throw Error('Kolom LB_EVENTS tidak sesuai. Gunakan spreadsheet khusus Langkah Berdua.');
    code=randomToken();p.setProperty('OWNER_INVITE',JSON.stringify({hash:hash(code),expires:Date.now()+24*3600000}));
    // Existing owner keeps access until the new code is redeemed (device recovery).
  }finally{lock.releaseLock();}
  SpreadsheetApp.getUi().alert('Koneksi pemilik', 'Kode berlaku 24 jam. Masukkan HANYA di aplikasi Anda, jangan dibagikan.\n\n'+code+'\n\nJika sudah pernah terhubung, kode baru mengganti akses perangkat pemilik lama setelah digunakan.',SpreadsheetApp.getUi().ButtonSet.OK);
}
function randomToken(){return Utilities.getUuid().replace(/-/g,'')+Utilities.getUuid().replace(/-/g,'');}
function hash(value){return Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,String(value),Utilities.Charset.UTF_8).map(b=>('0'+((b+256)%256).toString(16)).slice(-2)).join('');}
function fail(code,message,extra){return Object.assign({ok:false,code:code,error:message},extra||{});}
function json(value){return ContentService.createTextOutput(JSON.stringify(value)).setMimeType(ContentService.MimeType.JSON);}
function doGet(){return json({ok:true,service:'Langkah Berdua Sheets',version:4,message:'Endpoint aplikasi; data memerlukan token perangkat.'});}
function doPost(e){
  const lock=LockService.getScriptLock();
  try{
    if(!e||!e.postData||e.postData.contents.length>60000)return json(fail('INVALID','Permintaan terlalu besar.'));
    const body=JSON.parse(e.postData.contents);
    if(!lock.tryLock(20000))return json(fail('BUSY','Server sibuk. Coba lagi.'));
    return json(handle(body));
  }catch(err){return json(fail('SERVER','Penyimpanan belum siap atau sedang bermasalah. Periksa konfigurasi Apps Script.'));}
  finally{if(lock.hasLock())lock.releaseLock();}
}
function property(name){const raw=PropertiesService.getScriptProperties().getProperty(name);return raw?JSON.parse(raw):null;}
function setProperty(name,data){PropertiesService.getScriptProperties().setProperty(name,JSON.stringify(data));}
function members(){return ['OWNER','PARTNER'].map(property).filter(Boolean);}
function safeName(name){return typeof name==='string'&&name.trim().length>0&&name.length<=80;}
function handle(b){
  if(!b||typeof b!=='object')return fail('INVALID','Permintaan tidak valid.');
  if(b.action==='connect')return connect(b);
  if(typeof b.token!=='string'||!/^([a-f0-9]{64})$/.test(b.token))return fail('AUTH','Akses perangkat tidak valid.');
  const user=members().find(u=>u.tokenHash===hash(b.token));if(!user)return fail('AUTH','Akses telah dicabut. Hubungkan ulang perangkat.');
  if(b.action==='invite'){
    if(property('PARTNER'))return fail('FULL','Pasangan sudah terhubung.');
    const code=randomToken(), expires=Date.now()+24*3600000;
    setProperty('PARTNER_INVITE',{hash:hash(code),expires:expires});return {ok:true,code:code,expires:expires};
  }
  if(b.action==='disconnectPartner'){
    if(user.role!=='owner')return fail('DENIED','Hanya pemilik yang dapat mencabut akses pasangan.');
    PropertiesService.getScriptProperties().deleteProperty('PARTNER');PropertiesService.getScriptProperties().deleteProperty('PARTNER_INVITE');
    return {ok:true};
  }
  const sh=journal(), rows=sh.getLastRow()>1?sh.getRange(2,1,sh.getLastRow()-1,HEADERS.length).getValues():[];
  if(rows.length>20000)return fail('LIMIT','Batas jurnal tercapai. Ekspor dan arsipkan dahulu bersama pengembang.');
  const latest=new Map();for(const row of rows){if(row[11]){const record=JSON.parse(row[11]);latest.set(record.uuid,record);}}
  if(b.action==='sync')return {ok:true,records:Array.from(latest.values()),members:members().map(u=>({id:u.id,name:u.name,role:u.role})),user:{id:user.id,name:user.name,role:user.role}};
  if(b.action!=='save')return fail('INVALID','Aksi tidak dikenal.');
  const r=b.record, invalid=validateRecord(r);if(invalid)return fail('INVALID',invalid);
  if(typeof b.opId!=='string'||!/^[-a-f0-9]{36}$/.test(b.opId)||!Number.isInteger(b.version)||b.version<0)return fail('INVALID','Versi atau ID operasi tidak valid.');
  const canonical={};Object.keys(r).sort().forEach(k=>canonical[k]=r[k]);
  const fp=hash(JSON.stringify({user:user.id,version:b.version,record:canonical}));
  const previous=rows.find(row=>row[0]===b.opId);
  if(previous)return previous[1]===fp?{ok:true,replayed:true,record:latest.get(r.uuid)}:fail('INVALID','ID operasi sudah dipakai untuk isi berbeda.');
  if(rows.length>=20000)return fail('LIMIT','Batas jurnal tercapai. Ekspor dan arsipkan dahulu bersama pengembang.');
  const old=latest.get(r.uuid);
  if((old?old.version:0)!==b.version)return fail('CONFLICT','Catatan telah diubah pasangan. Tinjau kedua versi.',{current:old||null});
  const saved=Object.assign({},r,{version:b.version+1,ownerId:old?old.ownerId:user.id,updatedBy:user.name,updatedAt:new Date().toISOString()});
  const row=[b.opId,fp,r.uuid,saved.version,saved.updatedAt,user.id,cell(user.name),cell(r.title),r.amount,r.kind,r.deleted?1:0,JSON.stringify(saved)];
  // This one range write is the durable operation. A retry finds its opId before writing.
  sh.getRange(sh.getLastRow()+1,1,1,row.length).setValues([row]);SpreadsheetApp.flush();
  return {ok:true,record:saved};
}
function journal(){const id=PropertiesService.getScriptProperties().getProperty('SPREADSHEET_ID');if(!id)throw Error('setup required');const sh=SpreadsheetApp.openById(id).getSheetByName('LB_EVENTS');if(!sh)throw Error('journal missing');return sh;}
function cell(value){value=String(value);return /^[=+@\-\t\r]/.test(value)?"'"+value:value;}
function connect(b){
  if(typeof b.code!=='string'||!/^([a-f0-9]{64})$/.test(b.code)||typeof b.newToken!=='string'||!/^([a-f0-9]{64})$/.test(b.newToken)||!safeName(b.name))return fail('INVALID','Periksa nama dan kode koneksi 64 karakter.');
  const tokenHash=hash(b.newToken),codeHash=hash(b.code);
  const retry=members().find(u=>u.tokenHash===tokenHash&&u.joinHash===codeHash);
  if(retry)return {ok:true,user:{id:retry.id,name:retry.name,role:retry.role}};
  const owner=property('OWNER_INVITE'),partner=property('PARTNER_INVITE');
  let role=owner&&owner.hash===codeHash?'OWNER':partner&&partner.hash===codeHash?'PARTNER':null;
  if(!role)return fail('INVITE','Kode salah atau sudah dipakai.');
  const invitation=role==='OWNER'?owner:partner;
  if(invitation.expires<Date.now())return fail('INVITE','Kode sudah kedaluwarsa. Buat kode baru.');
  if(role==='PARTNER'&&property('PARTNER'))return fail('FULL','Ruang sudah berisi dua orang.');
  if(role==='PARTNER'&&!property('OWNER'))return fail('INVITE','Hubungkan pemilik dahulu.');
  const existing=property(role),u={id:existing?existing.id:Utilities.getUuid(),name:b.name.trim(),role:role==='OWNER'?'owner':'partner',tokenHash:tokenHash,joinHash:codeHash};
  setProperty(role,u);PropertiesService.getScriptProperties().deleteProperty(role+'_INVITE');
  return {ok:true,user:{id:u.id,name:u.name,role:u.role}};
}
function validateRecord(r){
  if(!r||!/^[-a-f0-9]{36}$/.test(r.uuid)||r.visibility!=='shared')return 'Hanya catatan bersama yang boleh dikirim.';
  if(typeof r.title!=='string'||r.title.trim().length===0||r.title.length>120)return 'Judul tidak valid.';
  if(!['income','expense','shopping'].includes(r.kind)||!Number.isSafeInteger(r.amount)||r.amount<0||r.amount>1000000000000)return 'Nominal atau jenis tidak valid.';
  if(r.kind==='shopping'&&(r.amount>1||r.date!==''))return 'Status barang tidak valid.';
  if(r.kind!=='shopping'&&(r.amount<1||typeof r.date!=='string'||!/^\d{4}-\d{2}-\d{2}$/.test(r.date)||isNaN(Date.parse(r.date+'T00:00:00Z'))||new Date(r.date+'T00:00:00Z').toISOString().slice(0,10)!==r.date))return 'Tanggal atau nominal transaksi tidak valid.';
  if(typeof r.deleted!=='boolean'||typeof r.note!=='string'||r.note.length>16000||typeof r.category!=='string'||r.category.length>80)return 'Isi catatan tidak valid.';
  if(typeof r.receipt!=='string'||r.receipt.length>20000||Object.keys(r).some(k=>!['uuid','title','amount','kind','date','visibility','deleted','note','category','receipt'].includes(k)))return 'Struktur catatan tidak valid.';
  return null;
}
