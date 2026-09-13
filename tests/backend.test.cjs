const assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),crypto=require('node:crypto');
const properties=new Map(),rows=[];let held=false,locks=0;
const sheet={getLastRow:()=>rows.length,getRange:(row,col,n,m)=>({getValues:()=>rows.slice(row-1,row-1+n).map(r=>r.slice(col-1,col-1+m)),setValues:values=>values.forEach((v,i)=>rows[row-1+i]=v),setBackground(){return this},setFontColor(){return this}}),appendRow:r=>rows.push(r),setFrozenRows:()=>{}};
const props={getProperty:k=>properties.get(k)||null,setProperty:(k,v)=>properties.set(k,v),deleteProperty:k=>properties.delete(k)};
const spreadsheet={getId:()=> 'test-spreadsheet',getSheetByName:()=>rows.length?sheet:null,insertSheet:()=>sheet};
const context=vm.createContext({console,Map,Date,JSON,Number,Object,String,Array,isNaN,Error,
 PropertiesService:{getScriptProperties:()=>props},
 SpreadsheetApp:{getActiveSpreadsheet:()=>spreadsheet,openById:id=>{assert.equal(id,'test-spreadsheet');return spreadsheet},flush:()=>{},getUi:()=>({alert:()=>{},ButtonSet:{OK:1}})},
 LockService:{getScriptLock:()=>({waitLock:()=>{held=true;locks++},tryLock:()=>{held=true;locks++;return true},hasLock:()=>held,releaseLock:()=>held=false})},
 Utilities:{getUuid:()=>crypto.randomUUID(),DigestAlgorithm:{SHA_256:1},Charset:{UTF_8:1},computeDigest:(_,v)=>Array.from(crypto.createHash('sha256').update(v).digest())},
 ContentService:{MimeType:{JSON:1},createTextOutput:s=>({text:s,setMimeType(){return this}})}
});
vm.runInContext(fs.readFileSync('backend/Code.gs','utf8'),context);
function call(b){return JSON.parse(context.doPost({postData:{contents:JSON.stringify(b)}}).text)}
function invite(role,code){context.setProperty(role+'_INVITE',{hash:context.hash(code),expires:Date.now()+3600000})}
const owner='1'.repeat(64),partner='2'.repeat(64),ownerCode='3'.repeat(64),partnerCode='4'.repeat(64);
context.setupOwner();invite('OWNER',ownerCode);
assert.equal(call({action:'sync',token:owner}).code,'AUTH');
assert.equal(call({action:'connect',code:ownerCode,newToken:owner,name:'Pemilik uji'}).ok,true);
assert.equal(call({action:'connect',code:ownerCode,newToken:owner,name:'Pemilik uji'}).ok,true,'Lost connect response can retry');
assert.equal(call({action:'connect',code:ownerCode,newToken:partner,name:'Penyusup'}).ok,false,'One-use invite');
invite('PARTNER',partnerCode);
assert.equal(call({action:'connect',code:partnerCode,newToken:partner,name:'Pasangan uji'}).ok,true);
assert.equal(call({action:'invite',token:owner}).code,'FULL');
const record={uuid:crypto.randomUUID(),title:'Belanja uji',amount:30000,kind:'expense',date:'2026-09-13',visibility:'shared',deleted:false,note:'',category:'Makanan',receipt:''};
const op=crypto.randomUUID();const request={action:'save',token:owner,opId:op,version:0,record};
assert.equal(call(request).record.version,1);assert.equal(rows.length,2);
assert.equal(call(request).replayed,true);assert.equal(rows.length,2,'Retry does not append');
assert.equal(call({...request,record:{...record,amount:99}}).code,'INVALID','Same operation cannot carry different amount');
assert.equal(call({action:'sync',token:partner}).records[0].amount,30000,'Second identity sees shared data');
const edited={...record,amount:40000};
assert.equal(call({action:'save',token:partner,opId:crypto.randomUUID(),version:1,record:edited}).record.version,2);
assert.equal(call({...request,opId:crypto.randomUUID(),version:1}).code,'CONFLICT','Stale edit rejected');
assert.equal(call(request).record.amount,40000,'Replay returns latest accepted value');
for(const bad of [{...record,visibility:'private'},{...record,amount:0},{...record,amount:1.5},{...record,date:'2026-02-30'},{...record,title:' '.repeat(100)},{...record,photo:'/private.jpg'}])assert.equal(call({...request,opId:crypto.randomUUID(),record:bad}).code,'INVALID');
const deleted={...edited,deleted:true};assert.equal(call({...request,opId:crypto.randomUUID(),version:2,record:deleted}).record.deleted,true);
assert.equal(call({...request,opId:crypto.randomUUID(),version:3,record:edited}).record.version,4,'Restore uses same record id');
assert.equal(call({action:'sync',token:partner}).records.length,1,'History not counted as more records');
assert.equal(call({action:'disconnectPartner',token:partner}).code,'DENIED');
assert.equal(call({action:'disconnectPartner',token:owner}).ok,true);
assert.equal(call({action:'sync',token:partner}).code,'AUTH');
invite('PARTNER',partnerCode);context.setProperty('PARTNER_INVITE',{hash:context.hash(partnerCode),expires:Date.now()-1});
assert.equal(call({action:'connect',code:partnerCode,newToken:partner,name:'Pasangan uji'}).code,'INVITE');
assert.equal(context.cell('=IMPORTXML("x")').startsWith("'"),true);
assert.equal(held,false);assert.ok(locks>20);
console.log('PASS: two identities, private-data rejection, expiry, one-use invitations, replay deduplication, conflicts, deletion, restoration, revocation, lock release.');
