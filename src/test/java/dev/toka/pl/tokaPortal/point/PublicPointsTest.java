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
}
