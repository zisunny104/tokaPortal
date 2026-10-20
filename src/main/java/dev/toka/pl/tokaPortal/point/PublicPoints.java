package dev.toka.pl.tokaPortal.point;
import com.google.gson.*;
import dev.toka.storage.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
/** Persistent public destinations. */
public final class PublicPoints implements AutoCloseable {
 public synchronized long planCount(){return state.getAsJsonArray("points").size();}
 private final DocumentStore store;
 private final dev.toka.pl.tokaPortal.provider.storage.WriteQueue writer;
 private JsonObject state;
 public PublicPoints(Path directory, Backend backend) throws IOException {this(directory,backend,null);}
 /** writer 為 null 時同步寫入。 */
 public PublicPoints(Path directory, Backend backend, dev.toka.pl.tokaPortal.provider.storage.WriteQueue writer) throws IOException {
  this.writer=writer;
  store=DocumentStore.open(new StoreConfig(backend,directory,1));
  try { state=normalize(store.load("public-points").orElseGet(()->JsonParser.parseString("{\"schema\":1,\"points\":[]}").getAsJsonObject())); }
  catch(IOException|RuntimeException e){store.close();throw e;}
 }
 private static String text(JsonObject row,String key,boolean required){
  JsonElement v=row.get(key); if(v==null||!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isString())throw new IllegalArgumentException("Invalid "+key);
  String s=v.getAsString();if(s.length()>8192||(required&&s.isBlank()))throw new IllegalArgumentException("Invalid "+key);return s;
 }
 private static JsonObject normalize(JsonObject input){
  if(input.get("schema")==null||!input.get("schema").toString().equals("1")||!input.has("points")||!input.get("points").isJsonArray())throw new IllegalArgumentException("Invalid public-points schema");
  if(input.getAsJsonArray("points").size()>10000)throw new IllegalArgumentException("Too many points");
  TreeMap<String,JsonObject> rows=new TreeMap<>();
  for(JsonElement item:input.getAsJsonArray("points")){
   JsonObject row=item.getAsJsonObject(),out=new JsonObject();
   for(String key:List.of("name","creator","type","info","world"))out.addProperty(key,text(row,key,key.equals("name")||key.equals("world")));
   // owner（建立者 UUID）為選填，舊資料沒有；不合法的值直接略過
   JsonElement owner=row.get("owner");if(owner!=null&&owner.isJsonPrimitive()&&owner.getAsJsonPrimitive().isString()&&HomePoint.parseUuid(owner.getAsString())!=null)out.addProperty("owner",owner.getAsString());
   if(out.get("name").getAsString().length()>255||out.get("world").getAsString().length()>255)throw new IllegalArgumentException("Name too long");
   for(String key:List.of("x","y","z","yaw","pitch")){
    JsonElement value=row.get(key);if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Invalid "+key);
    double n=value.getAsDouble();if(!Double.isFinite(n)||Math.abs(n)>30000000)throw new IllegalArgumentException("Invalid "+key);out.addProperty(key,n==0?0d:n);
   }
   if(rows.put(out.get("name").getAsString(),out)!=null)throw new IllegalArgumentException("Duplicate name");
  }
  JsonObject root=new JsonObject();root.addProperty("schema",1);JsonArray list=new JsonArray();rows.values().forEach(list::add);root.add("points",list);return root;
 }
 public synchronized String findJson(String name){for(JsonElement row:state.getAsJsonArray("points"))if(row.getAsJsonObject().get("name").getAsString().equals(name))return row.toString();return null;}
 public synchronized boolean createJson(String rowJson)throws IOException{return create(JsonParser.parseString(rowJson).getAsJsonObject());}
 public synchronized boolean replaceJson(String rowJson)throws IOException{
  JsonObject row=JsonParser.parseString(rowJson).getAsJsonObject(),next=state.deepCopy();
  String name=text(row,"name",true);boolean found=false;
  JsonArray list=next.getAsJsonArray("points");
  for(int i=0;i<list.size();i++)if(list.get(i).getAsJsonObject().get("name").getAsString().equals(name)){list.set(i,row);found=true;break;}
  if(!found)return false;persist(normalize(next));return true;
 }
 public synchronized String snapshotJson(){return state.toString();}
 public synchronized boolean verify(String json){return state.equals(normalize(JsonParser.parseString(json).getAsJsonObject()));}
 public synchronized int replaceIfEmptyOrEqual(String json)throws IOException{
  JsonObject next=normalize(JsonParser.parseString(json).getAsJsonObject());
  if(!state.getAsJsonArray("points").isEmpty()&&!state.equals(next))throw new IOException("Destination is not empty or identical");
  persist(next);return next.getAsJsonArray("points").size();
 }
 private void write(JsonObject next)throws IOException{
  store.save("public-points",next);
  JsonObject actual=normalize(store.load("public-points").orElseThrow(()->new IOException("Missing readback")));
  if(!actual.equals(next))throw new IOException("Public-points readback mismatch");
 }
 private void persist(JsonObject next)throws IOException{
  if(writer==null){write(next);state=next;return;}
  state=next;writer.submit("log.points.saveFailed",()->write(next));
 }
 public synchronized Map<Object,teleportPoint> points(){Map<Object,teleportPoint> out=new LinkedHashMap<>();for(JsonElement row:state.getAsJsonArray("points")){teleportPoint point=new teleportPoint(row.getAsJsonObject());out.put(point.getName(),point);}return Collections.unmodifiableMap(out);}
 public synchronized boolean create(JsonObject row)throws IOException{JsonObject next=state.deepCopy();for(JsonElement old:next.getAsJsonArray("points"))if(old.getAsJsonObject().get("name").equals(row.get("name")))return false;next.getAsJsonArray("points").add(row);persist(normalize(next));return true;}
 public synchronized boolean delete(String name)throws IOException{JsonObject next=state.deepCopy();boolean removed=next.getAsJsonArray("points").asList().removeIf(row->row.getAsJsonObject().get("name").getAsString().equals(name));if(removed)persist(next);return removed;}

 // ---- 擁有者（UUID）遷移：與住家相同規則。creator 一律是最後已知名稱，舊版 jar 仍讀得懂 ----
 private static String lower(String name){return name==null?"":name.toLowerCase(java.util.Locale.ROOT);}
 private static java.util.UUID ownerOf(JsonObject row){return row.has("owner")?HomePoint.parseUuid(row.get("owner").getAsString()):null;}
 private static String creatorOf(JsonObject row){return row.get("creator").getAsString();}
 /** 已有「不同 UUID」的傳送點使用同一個名稱；candidate 為 null 時只要有任何 UUID 持有就算。 */
 private boolean disputed(String lowerName,java.util.UUID candidate){
  for(JsonElement item:state.getAsJsonArray("points")){JsonObject row=item.getAsJsonObject();java.util.UUID owner=ownerOf(row);
   if(owner!=null&&!owner.equals(candidate)&&lower(creatorOf(row)).equals(lowerName))return true;}
  return false;
 }
 /** 尚無 UUID 的 legacy 傳送點，依名稱分組。 */
 public synchronized java.util.List<dev.toka.pl.tokaPortal.provider.IDataProvider.LegacyGroup> legacyGroups(){
  Map<String,int[]> count=new TreeMap<>();Map<String,String> shown=new HashMap<>();
  for(JsonElement item:state.getAsJsonArray("points")){JsonObject row=item.getAsJsonObject();if(ownerOf(row)!=null)continue;
   String key=lower(creatorOf(row));count.computeIfAbsent(key,k->new int[1])[0]++;shown.putIfAbsent(key,creatorOf(row));}
  java.util.List<dev.toka.pl.tokaPortal.provider.IDataProvider.LegacyGroup> groups=new ArrayList<>();
  count.forEach((key,n)->groups.add(new dev.toka.pl.tokaPortal.provider.IDataProvider.LegacyGroup(shown.get(key),n[0],disputed(key,null))));
  return groups;
 }
 /** 套用名稱（小寫）對 UUID 的解析結果；同名已有不同 UUID 的傳送點視為爭議，不處理。 */
 public synchronized dev.toka.pl.tokaPortal.provider.IDataProvider.Assigned assignOwners(Map<String,java.util.UUID> resolved){
  int assigned=0,conflicts=0;java.util.List<String> names=new ArrayList<>();Set<String> seen=new HashSet<>();
  JsonObject next=state.deepCopy();
  for(JsonElement item:next.getAsJsonArray("points")){JsonObject row=item.getAsJsonObject();if(ownerOf(row)!=null)continue;
   String key=lower(creatorOf(row));java.util.UUID id=resolved.get(key);if(id==null)continue;
   if(disputed(key,id)){if(seen.add(key)){conflicts++;names.add(creatorOf(row));}continue;}
   row.addProperty("owner",id.toString());assigned++;}
  if(assigned>0)save(next);
  return new dev.toka.pl.tokaPortal.provider.IDataProvider.Assigned(assigned,conflicts,names);
 }
 /** 管理員指定：把該名稱（不分大小寫）的 legacy 傳送點全部交給玩家。 */
 public synchronized int claimLegacy(String name,java.util.UUID owner,String ownerName){
  int claimed=0;JsonObject next=state.deepCopy();
  for(JsonElement item:next.getAsJsonArray("points")){JsonObject row=item.getAsJsonObject();
   if(ownerOf(row)==null&&lower(creatorOf(row)).equals(lower(name))){row.addProperty("owner",owner.toString());row.addProperty("creator",ownerName);claimed++;}}
  if(claimed>0)save(next);
  return claimed;
 }
 /** 玩家登入：更新最後已知名稱，並取回同名的 legacy 傳送點（有爭議不處理）。 */
 public synchronized dev.toka.pl.tokaPortal.provider.IDataProvider.Joined onPlayerJoin(java.util.UUID id,String name){
  int renamed=0,claimed=0;boolean conflict=false;String key=lower(name);boolean contested=disputed(key,id);
  JsonObject next=state.deepCopy();
  for(JsonElement item:next.getAsJsonArray("points")){JsonObject row=item.getAsJsonObject();java.util.UUID owner=ownerOf(row);
   if(id.equals(owner)){if(!name.equals(creatorOf(row))){row.addProperty("creator",name);renamed++;}}
   else if(owner==null&&lower(creatorOf(row)).equals(key)){
    if(contested)conflict=true;else{row.addProperty("owner",id.toString());row.addProperty("creator",name);claimed++;}}}
  if(renamed>0||claimed>0)save(next);
  return new dev.toka.pl.tokaPortal.provider.IDataProvider.Joined(renamed,claimed,conflict);
 }
 private void save(JsonObject next){try{persist(normalize(next));}catch(IOException e){throw new IllegalStateException("Public point save failed",e);}}
 public synchronized void close()throws IOException{store.close();}
}
