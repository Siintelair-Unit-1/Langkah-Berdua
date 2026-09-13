package id.langkahberdua.app;
import java.util.*;
final class Csv {
 static List<List<String>> read(String s){List<List<String>> rows=new ArrayList<>();List<String> row=new ArrayList<>();StringBuilder value=new StringBuilder();boolean quoted=false;for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c=='"'){if(quoted&&i+1<s.length()&&s.charAt(i+1)=='"'){value.append('"');i++;}else quoted=!quoted;}else if(c==','&&!quoted){row.add(value.toString());value.setLength(0);}else if((c=='\r'||c=='\n')&&!quoted){if(c=='\r'&&i+1<s.length()&&s.charAt(i+1)=='\n')i++;row.add(value.toString());value.setLength(0);rows.add(row);row=new ArrayList<>();}else value.append(c);}if(quoted)throw new IllegalArgumentException("Tanda kutip CSV tidak lengkap");if(value.length()>0||!row.isEmpty()){row.add(value.toString());rows.add(row);}return rows;}
 static String cell(String s){if(s.matches("^[=+@\\-\\t\\r].*"))s="'"+s;return "\""+s.replace("\"","\"\"")+"\"";}
}
