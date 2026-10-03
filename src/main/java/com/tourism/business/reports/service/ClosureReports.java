package com.tourism.business.reports.service;
import com.tourism.business.common.ApiException;
import com.tourism.business.finance.service.FinanceService;
import java.io.*;
import java.time.*;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
@Service
public class ClosureReports {
    private final FinanceService finance;private final Map<String,Map<String,String>> labels;
    public ClosureReports(FinanceService finance,ObjectMapper mapper)throws IOException{this.finance=finance;try(var in=getClass().getResourceAsStream("/reports/labels.json")){labels=mapper.readValue(in,new tools.jackson.core.type.TypeReference<Map<String,Map<String,String>>>(){});}}
    private String t(String locale,String key){return labels.getOrDefault(locale,labels.get("es")).getOrDefault(key,key);}
    private String value(String locale,Object value){return value==null?"":value.toString();}
    private String field(String locale,String key,Object v){if(v==null)return "";if(List.of("method","kind").contains(key))return t(locale,v.toString());if(List.of("closedAt","createdAt").contains(key))return Instant.parse(v.toString()).atZone(ZoneId.of("America/Costa_Rica")).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX"));return value(locale,v);}
    @SuppressWarnings("unchecked") private Map<String,Object> snapshot(long id){var s=(Map<String,Object>)finance.closure(id).get("snapshot");if(!s.containsKey("schemaVersion"))throw new ApiException(409,"LEGACY_CLOSURE_REPORT");return s;}
    @SuppressWarnings("unchecked") private List<Map<String,Object>> items(Map<String,Object>s,String section){return (List<Map<String,Object>>)s.get(section);}
    @SuppressWarnings("unchecked") private List<List<String>> summary(Map<String,Object>s,String locale){var rows=new ArrayList<List<String>>();if(Boolean.TRUE.equals(s.get("legacy")))rows.add(List.of(t(locale,"notice"),t(locale,"legacySnapshot")));for(String key:List.of("closureId","businessName","businessDate","closedAt","timezone","actorName","recipient","currency","openingFloat","countedCash","difference","note"))rows.add(List.of(t(locale,key),field(locale,key,s.get(key))));((Map<String,Object>)s.get("totals")).forEach((k,v)->rows.add(List.of(t(locale,k),value(locale,v))));for(var method:items(s,"methods"))for(String k:List.of("payments","expenses","refunds","adjustments","net"))rows.add(List.of(t(locale,method.get("method").toString())+" / "+t(locale,k),value(locale,method.get(k))));rows.add(List.of(t(locale,"notice"),t(locale,"noticeText")));return rows;}
    public byte[] generate(long id,String format){if(!List.of("pdf","xlsx").contains(format))throw new ApiException(400,"INVALID_REPORT_FORMAT");var s=snapshot(id);try{return format.equals("pdf")?pdf(s):xlsx(s);}catch(IOException e){throw new ApiException(503,"REPORT_GENERATION_FAILED");}}
    private byte[] xlsx(Map<String,Object>s)throws IOException{
        String locale=s.get("locale").toString();try(var book=new XSSFWorkbook();var bytes=new ByteArrayOutputStream()){
            var props=book.getProperties().getCoreProperties();props.setCreator(s.get("actorName").toString());props.setTitle(t(locale,"report")+" "+s.get("closureId"));props.setCreated(Optional.of(Date.from(Instant.parse(s.get("closedAt").toString()))));props.setModified(Optional.of(Date.from(Instant.parse(s.get("closedAt").toString()))));
            var header=book.createCellStyle();header.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());header.setFillPattern(FillPatternType.SOLID_FOREGROUND);var font=book.createFont();font.setColor(IndexedColors.WHITE.getIndex());font.setBold(true);header.setFont(font);header.setWrapText(true);var body=book.createCellStyle();body.setWrapText(true);body.setVerticalAlignment(VerticalAlignment.TOP);
            var summary=book.createSheet("Summary");summary.setColumnWidth(0,38*256);summary.setColumnWidth(1,85*256);write(summary,0,List.of(t(locale,"field"),t(locale,"value")),header);int n=1;for(var row:summary(s,locale))write(summary,n++,row,body);summary.createFreezePane(0,1);summary.setColumnWidth(0,38*256);summary.setColumnWidth(1,85*256);
            for(String section:List.of("payments","expenses","refunds","adjustments","attendance")){var sheet=book.createSheet(Character.toUpperCase(section.charAt(0))+section.substring(1));var rows=items(s,section);var columns=new LinkedHashSet<String>();rows.forEach(r->columns.addAll(r.keySet()));if(columns.isEmpty())columns.add("id");var keys=new ArrayList<>(columns);for(int col=0;col<keys.size();col++)sheet.setColumnWidth(col,(List.of("reason","description","reference","name","actorName").contains(keys.get(col))?48:24)*256);write(sheet,0,keys.stream().map(k->t(locale,k)).toList(),header);int index=1;for(var row:rows)write(sheet,index++,keys.stream().map(k->field(locale,k,row.get(k))).toList(),body);sheet.createFreezePane(0,1);sheet.setAutoFilter(new CellRangeAddress(0,Math.max(0,index-1),0,keys.size()-1));for(int col=0;col<keys.size();col++)sheet.setColumnWidth(col,(List.of("reason","description","reference","name","actorName").contains(keys.get(col))?48:24)*256);sheet.getPrintSetup().setLandscape(true);sheet.setRepeatingRows(new CellRangeAddress(0,0,-1,-1));}
            book.write(bytes);return bytes.toByteArray();
        }
    }
    private void write(Sheet sheet,int index,List<String> values,CellStyle style){var row=sheet.createRow(index);int lines=1;for(int i=0;i<values.size();i++){var cell=row.createCell(i,CellType.STRING);cell.setCellValue(values.get(i));cell.setCellStyle(style);int width=Math.max(20,sheet.getColumnWidth(i)/256);lines=Math.max(lines,(values.get(i).length()+width-1)/width);}row.setHeightInPoints(Math.min(409,Math.max(22,lines*15)));}
    private byte[] pdf(Map<String,Object>s)throws IOException{
        String locale=s.get("locale").toString();try(var doc=new PDDocument();var bytes=new ByteArrayOutputStream();var fontData=getClass().getResourceAsStream("/fonts/NotoSans-Regular.ttf")){
            var font=PDType0Font.load(doc,fontData);var metadata=new PDDocumentInformation();metadata.setTitle(t(locale,"report")+" "+s.get("closureId"));metadata.setAuthor(s.get("actorName").toString());var instant=GregorianCalendar.from(Instant.parse(s.get("closedAt").toString()).atZone(ZoneOffset.UTC));metadata.setCreationDate(instant);metadata.setModificationDate(instant);doc.setDocumentInformation(metadata);
            var lines=new ArrayList<String>();lines.add(t(locale,"summary"));for(var row:summary(s,locale))lines.add(row.get(0)+": "+row.get(1));
            for(String section:List.of("payments","expenses","refunds","adjustments","attendance")){lines.add("\f"+t(locale,section));var rows=items(s,section);if(rows.isEmpty())lines.add(t(locale,"empty"));for(var row:rows){lines.add("--------------------------------");for(var entry:row.entrySet())if(entry.getValue()!=null)lines.add(t(locale,entry.getKey())+": "+field(locale,entry.getKey(),entry.getValue()));}}
            var wrapped=new ArrayList<String>();for(String line:lines){if(line.startsWith("\f")){line=line.substring(1);while(wrapped.size()%44!=0)wrapped.add("");}StringBuilder text=new StringBuilder();for(int cp:line.codePoints().toArray()){String glyph=Character.isISOControl(cp)?" ":font.hasGlyph(cp)?new String(Character.toChars(cp)):"[U+"+Integer.toHexString(cp).toUpperCase(Locale.ROOT)+"]";if(font.getStringWidth(text.toString()+glyph)/1000*10>495){wrapped.add(text.toString());text.setLength(0);}text.append(glyph);}wrapped.add(text.toString());}
            int perPage=44,total=Math.max(1,(wrapped.size()+perPage-1)/perPage);for(int page=0;page<total;page++){var p=new PDPage(PDRectangle.A4);doc.addPage(p);try(var out=new PDPageContentStream(doc,p)){out.setNonStrokingColor(24/255f,61/255f,51/255f);out.beginText();out.setFont(font,16);out.newLineAtOffset(50,790);out.showText(t(locale,"report")+" #"+s.get("closureId"));out.setFont(font,10);out.newLineAtOffset(0,-22);out.showText(s.get("businessDate")+" | CRC | America/Costa_Rica");out.newLineAtOffset(0,-28);for(int i=page*perPage;i<Math.min(wrapped.size(),(page+1)*perPage);i++){out.showText(wrapped.get(i));out.newLineAtOffset(0,-15);}out.endText();out.beginText();out.setFont(font,9);out.newLineAtOffset(50,32);out.showText(t(locale,"page")+" "+(page+1)+" / "+total);out.endText();}}
            doc.save(bytes);return bytes.toByteArray();
        }
    }
}
