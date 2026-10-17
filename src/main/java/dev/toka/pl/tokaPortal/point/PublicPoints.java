package dev.toka.pl.tokaPortal.point;
import com.google.gson.*;
import dev.toka.storage.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
/** Persistent public destinations. */
public final class PublicPoints implements AutoCloseable {
 private final DocumentStore store;
 private JsonObject state;
 public PublicPoints(Path directory, Backend backend) throws IOException {
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
 private void persist(JsonObject next)throws IOException{
  store.save("public-points",next);
  JsonObject actual=normalize(store.load("public-points").orElseThrow(()->new IOException("Missing readback")));
  if(!actual.equals(next))throw new IOException("Public-points readback mismatch");state=next;
 }
 public synchronized Map<Object,teleportPoint> points(){Map<Object,teleportPoint> out=new LinkedHashMap<>();for(JsonElement row:state.getAsJsonArray("points")){teleportPoint point=new teleportPoint(row.getAsJsonObject());out.put(point.getName(),point);}return Collections.unmodifiableMap(out);}
 public synchronized boolean create(JsonObject row)throws IOException{JsonObject next=state.deepCopy();for(JsonElement old:next.getAsJsonArray("points"))if(old.getAsJsonObject().get("name").equals(row.get("name")))return false;next.getAsJsonArray("points").add(row);persist(normalize(next));return true;}
 public synchronized boolean delete(String name)throws IOException{JsonObject next=state.deepCopy();boolean removed=next.getAsJsonArray("points").asList().removeIf(row->row.getAsJsonObject().get("name").getAsString().equals(name));if(removed)persist(next);return removed;}
 public synchronized void close()throws IOException{store.close();}
}
