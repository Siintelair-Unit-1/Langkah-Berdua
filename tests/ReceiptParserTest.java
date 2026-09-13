package id.langkahberdua.app;
public class ReceiptParserTest {
 static void eq(Object a,Object b){if(!java.util.Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
 public static void main(String[] args){
  eq(ReceiptParser.money("Rp 15.000,00"),15000L);eq(ReceiptParser.money("15000"),15000L);eq(ReceiptParser.money("15,000.00"),15000L);eq(ReceiptParser.money("1.50"),null);eq(ReceiptParser.money("-10"),null);
  eq(ReceiptParser.isoDate("30/02/2026"),"");eq(ReceiptParser.isoDate("13/09/2026"),"2026-09-13");
  ReceiptParser.Result r=ReceiptParser.parse("TOKO UJI\n13/09/2026\nSusu 2 x 15.000 30.000\nSUBTOTAL 30.000\nDiskon 2.000\nPajak 1.000\nTOTAL 29.000\nTUNAI 50.000\nKEMBALI 21.000");
  eq(r.total,"29000");eq(r.date,"2026-09-13");eq(r.items.size(),1);eq(r.items.get(0).qty,"2");eq(r.items.get(0).unit,"15000");eq(r.items.get(0).amount,"30000");eq(r.discount,"2000");eq(r.tax,"1000");
  ReceiptParser.Result missing=ReceiptParser.parse("TOKO UJI\nRoti 10.000\nTUNAI 20.000");eq(missing.total,"");eq(missing.date,"");eq(missing.items.get(0).qty,"");
  eq(Csv.read("Judul,Nominal\r\n\"Roti, susu\",20000\r\n").get(1).get(0),"Roti, susu");eq(Csv.read("\"dua\"\"kutip\",3").get(0).get(0),"dua\"kutip");
  System.out.println("PASS: IDR parsing, missing OCR values, receipt totals, items, impossible dates and CSV escaping.");
 }
}
