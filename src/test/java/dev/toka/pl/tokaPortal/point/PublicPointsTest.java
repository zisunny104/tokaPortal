package dev.toka.pl.tokaPortal.point;
import dev.toka.storage.Backend;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class PublicPointsTest {
 private static final String DATA="{\"schema\":1,\"points\":[{\"name\":\"spawn\",\"creator\":\"admin\",\"type\":\"point\",\"info\":\"出生點\",\"world\":\"chuklok\",\"x\":-12.5,\"y\":64,\"z\":0,\"yaw\":90,\"pitch\":0}]}";
 @Test void backendsPreserveAndRejectConflicts()throws Exception{
  for(Backend backend:Backend.values()){
   Path dir=Files.createTempDirectory(Path.of("target"),"public-points-");
   try(PublicPoints points=new PublicPoints(dir,backend)){
    assertEquals(1,points.replaceIfEmptyOrEqual(DATA));assertTrue(points.verify(DATA));
    assertEquals(1,points.replaceIfEmptyOrEqual(DATA));
    assertThrows(java.io.IOException.class,()->points.replaceIfEmptyOrEqual(DATA.replace("admin","someone")));
    assertTrue(points.verify(DATA));
    assertNull(points.findJson("missing"));
    assertFalse(points.replaceJson(points.findJson("spawn").replace("spawn","absent")));
    assertFalse(points.createJson(points.findJson("spawn")));
    assertTrue(points.replaceJson(points.findJson("spawn")));
   }
   try(PublicPoints points=new PublicPoints(dir,backend)){assertTrue(points.verify(DATA));assertEquals("出生點",points.points().get("spawn").getInfo());assertTrue(points.delete("spawn"));assertFalse(points.delete("spawn"));}
  }
 }
 @Test void keepsOptionalOwnerAndIgnoresInvalidOne()throws Exception{
  String id="3f2b8c1e-9a4d-4e6f-8b7a-1c2d3e4f5a6b";
  String creator="\"creator\":\"admin\"";
  try(PublicPoints points=new PublicPoints(Files.createTempDirectory(Path.of("target"),"public-points-owner-"),Backend.JSON)){
   points.replaceIfEmptyOrEqual(DATA.replace(creator,creator+",\"owner\":\""+id+"\""));
   assertEquals(java.util.UUID.fromString(id),points.points().get("spawn").getOwner());
  }
  try(PublicPoints points=new PublicPoints(Files.createTempDirectory(Path.of("target"),"public-points-old-"),Backend.JSON)){
   points.replaceIfEmptyOrEqual(DATA.replace(creator,creator+",\"owner\":\"not-a-uuid\""));
   assertNull(points.points().get("spawn").getOwner());
   assertEquals("admin",points.points().get("spawn").getCreator());
  }
 }
 @Test void rejectsDuplicateInvalidAndNonFinite()throws Exception{
  Path dir=Files.createTempDirectory(Path.of("target"),"public-points-invalid-");
  try(PublicPoints points=new PublicPoints(dir,Backend.JSON)){
   assertThrows(IllegalArgumentException.class,()->points.replaceIfEmptyOrEqual(DATA.replace("-12.5","1e999")));
   assertThrows(IllegalArgumentException.class,()->points.replaceIfEmptyOrEqual(DATA.replace("chuklok","")));
   var root=com.google.gson.JsonParser.parseString(DATA).getAsJsonObject();root.getAsJsonArray("points").add(root.getAsJsonArray("points").get(0).deepCopy());
   assertThrows(IllegalArgumentException.class,()->points.replaceIfEmptyOrEqual(root.toString()));assertEquals(0,points.points().size());
  }
 }

 private static final java.util.UUID A=java.util.UUID.fromString("00000000-0000-0000-0000-00000000000a"),B=java.util.UUID.fromString("00000000-0000-0000-0000-00000000000b");
 private static String row(String name,String creator,String owner){
  return "{\"name\":\""+name+"\",\"creator\":\""+creator+"\",\""+"type\":\"point\",\"info\":\"\",\"world\":\"w\",\"x\":1,\"y\":64,\"z\":2,\"yaw\":0,\"pitch\":0"+(owner==null?"":",\"owner\":\""+owner+"\"")+"}";
 }
 private static PublicPoints open(String... rows)throws Exception{
  PublicPoints points=new PublicPoints(Files.createTempDirectory(Path.of("target"),"public-points-legacy-"),Backend.JSON);
  points.replaceIfEmptyOrEqual("{\"schema\":1,\"points\":["+String.join(",",rows)+"]}");return points;
 }
 @Test void migrationAssignsResolvedNamesAndKeepsTheOthersLegacy()throws Exception{
  try(PublicPoints points=open(row("p1","Alice",null),row("p2","alice",null),row("p3","Ghost",null))){
   var result=points.assignOwners(java.util.Map.of("alice",A));
   assertEquals(2,result.assigned());assertEquals(0,result.disputed());
   assertEquals(A,points.points().get("p1").getOwner());assertEquals(A,points.points().get("p2").getOwner());
   assertNull(points.points().get("p3").getOwner());assertEquals("Ghost",points.points().get("p3").getCreator());
   assertEquals(1,points.legacyGroups().size());
  }
 }
 @Test void disputedNamesAreNotAssignedOrClaimedOnJoin()throws Exception{
  try(PublicPoints points=open(row("p1","Bob",null),row("p2","bob",B.toString()))){
   var result=points.assignOwners(java.util.Map.of("bob",A));
   assertEquals(0,result.assigned());assertEquals(1,result.disputed());assertEquals(java.util.List.of("Bob"),result.disputedNames());
   assertTrue(points.legacyGroups().getFirst().disputed());
   var joined=points.onPlayerJoin(A,"Bob");
   assertEquals(0,joined.claimed());assertTrue(joined.disputed());assertNull(points.points().get("p1").getOwner());
   assertEquals(1,points.claimLegacy("BOB",A,"Bob"));assertEquals(A,points.points().get("p1").getOwner());
  }
 }
 @Test void joiningClaimsSameNameIgnoringCaseAndUpdatesTheName()throws Exception{
  try(PublicPoints points=open(row("p1","carol",null),row("p2","Old",A.toString()))){
   var joined=points.onPlayerJoin(B,"Carol");
   assertEquals(1,joined.claimed());assertFalse(joined.disputed());
   assertEquals(B,points.points().get("p1").getOwner());assertEquals("Carol",points.points().get("p1").getCreator());
   assertEquals(1,points.onPlayerJoin(A,"New").renamed());assertEquals("New",points.points().get("p2").getCreator());
   assertEquals(0,points.onPlayerJoin(A,"New").renamed());
  }
 }
 @Test void ownerIsDecidedByUuidOnceSetAndByNameForLegacy()throws Exception{
  try(PublicPoints points=open(row("p1","Alice",A.toString()),row("p2","Bob",null))){
   assertFalse(points.points().get("p1").isCreator(B,"Alice"));
   assertTrue(points.points().get("p1").isCreator(A,"Renamed"));
   assertTrue(points.points().get("p2").isCreator(B,"bob"));
  }
 }
 @Test void migrationIsWrittenThroughTheQueueAndSurvivesReload()throws Exception{
  Path dir=Files.createTempDirectory(Path.of("target"),"public-points-queue-");
  var writer=new dev.toka.pl.tokaPortal.provider.storage.WriteQueue((key,error)->fail(key,error));
  try(PublicPoints points=new PublicPoints(dir,Backend.JSON,writer)){
   points.replaceIfEmptyOrEqual("{\"schema\":1,\"points\":["+row("p1","Dave",null)+"]}");
   points.assignOwners(java.util.Map.of("dave",A));assertTrue(writer.drain(5000));
  }
  try(PublicPoints points=new PublicPoints(dir,Backend.JSON)){assertEquals(A,points.points().get("p1").getOwner());assertEquals("Dave",points.points().get("p1").getCreator());}
  writer.shutdown();
 }
}
