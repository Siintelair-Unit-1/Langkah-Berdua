package id.langkahberdua.app;

import java.util.*;
import java.util.regex.*;
import java.text.SimpleDateFormat;

/** Conservative suggestions only. Missing numbers stay blank for manual review. */
public final class ReceiptParser {
 public static final class Item {public String name="",qty="",unit="",amount="";}
 public static final class Result {public String store="",date="",total="",discount="",tax="",fee="";public final List<Item> items=new ArrayList<>();}
 private static final Pattern MONEY=Pattern.compile("(?:Rp\\.?\\s*)?(-?\\d[\\d.,]*)\\s*$",Pattern.CASE_INSENSITIVE);
 public static Long money(String raw){
  if(raw==null)return null;String s=raw.trim().replaceAll("(?i)rp\\.?|\\s","");
  if(s.matches("\\d{1,3}(?:\\.\\d{3})+(?:,00)?"))s=s.replace(".","").replace(",00","");
  else if(s.matches("\\d{1,3}(?:,\\d{3})+(?:\\.00)?"))s=s.replace(",","").replace(".00","");
  else if(s.matches("\\d+(?:[,.]00)?"))s=s.replaceAll("[,.]00$","");else return null;
  try{long n=Long.parseLong(s);return n<=1000000000000L?n:null;}catch(NumberFormatException e){return null;}
 }
 public static String isoDate(String raw){
  for(String format:new String[]{"yyyy-MM-dd","dd/MM/yyyy","dd-MM-yyyy","dd.MM.yyyy"}){try{SimpleDateFormat f=new SimpleDateFormat(format,Locale.ROOT);f.setLenient(false);java.text.ParsePosition pos=new java.text.ParsePosition(0);Date d=f.parse(raw,pos);if(d!=null&&pos.getIndex()==raw.length())return new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT).format(d);}catch(Exception ignored){}}
  return "";
 }
 public static Result parse(String text){Result r=new Result();String[] lines=text.split("\\r?\\n");
  for(int i=0;i<lines.length;i++){String line=lines[i].trim(),lower=line.toLowerCase(Locale.ROOT);if(line.isEmpty())continue;
   if(r.store.isEmpty()&&i<3&&line.matches(".*[A-Za-z].*")&&!line.matches(".*\\d{3,}.*"))r.store=line.length()>120?line.substring(0,120):line;
   Matcher date=Pattern.compile("(?<!\\d)(\\d{4}-\\d{2}-\\d{2}|\\d{2}[/.-]\\d{2}[/.-]\\d{4})(?!\\d)").matcher(line);if(r.date.isEmpty()&&date.find())r.date=isoDate(date.group(1));
   Matcher m=MONEY.matcher(line);Long number=m.find()?money(m.group(1)):null;
   if(lower.matches(".*\\b(total|subtotal|sub total|tunai|cash|kembali|change|pajak|tax|ppn|diskon|discount|biaya|service|jumlah|bayar|debit|kredit)\\b.*")){
    if(number!=null){String n=Long.toString(number);
     if(lower.matches("^(grand\\s+total|total(?:\\s+(bayar|pembayaran|belanja))?)\\s*[:=rp.\\s]*[0-9].*"))r.total=n;
     else if(lower.matches("^(pajak|tax|ppn)\\b.*"))r.tax=n;
     else if(lower.matches("^(diskon|discount)\\b.*"))r.discount=n;
     else if(lower.matches("^(biaya|service)\\b.*"))r.fee=n;
    }continue;
   }
   if(number!=null&&i>0&&m.start()>1&&r.items.size()<60){String prefix=line.substring(0,m.start()).trim();if(prefix.matches(".*[A-Za-z].*")&&!prefix.toLowerCase(Locale.ROOT).matches(".*(telp|kasir|alamat|nota|invoice|tanggal|npwp|no\\.).*")){
    Item item=new Item();item.name=prefix;item.amount=Long.toString(number);
    Matcher detail=Pattern.compile("^(.*?)\\s+(\\d+)\\s*[xX@]\\s*([\\d.,]+)\\s*$").matcher(prefix);
    if(detail.matches()&&money(detail.group(3))!=null){item.name=detail.group(1).trim();item.qty=detail.group(2);item.unit=Long.toString(money(detail.group(3)));}
    r.items.add(item);
   }}
  }return r;
 }
}
