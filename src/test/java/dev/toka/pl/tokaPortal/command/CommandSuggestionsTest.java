package dev.toka.pl.tokaPortal.command;

import cn.nukkit.command.data.*;
import cn.nukkit.network.protocol.AvailableCommandsPacket;
import cn.nukkit.network.protocol.ProtocolInfo;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandSuggestionsTest {
    private CommandDataVersions data() {
        CommandDataVersions data = new CommandDataVersions();
        data.versions.add(new CommandData());
        return data;
    }

    @Test void replacesSharedOverloadsWithIndependentPlayerParameters() {
        var alice = data();
        var bob = data();
        var shared = new LinkedHashMap<String, CommandOverload>();
        alice.versions.getFirst().overloads = shared;
        bob.versions.getFirst().overloads = shared;
        CommandSuggestions.parameters(alice, new CommandParameter[]{
                CommandSuggestions.names("住家名稱", true, "TokaPortalHomes", List.of("Alice 的家"))});
        CommandSuggestions.parameters(bob, new CommandParameter[]{
                CommandSuggestions.names("住家名稱", true, "TokaPortalHomes", List.of("Bob 的家"))});
        assertTrue(shared.isEmpty());
        assertEquals(List.of("Alice 的家"), alice.versions.getFirst().overloads.get("choice0").input.parameters[0].enumData.getValues());
        assertEquals(List.of("Bob 的家"), bob.versions.getFirst().overloads.get("choice0").input.parameters[0].enumData.getValues());
    }

    @Test void suggestionsRetainManualInputAndOptionalArgument() {
        var data = CommandSuggestions.parameters(data(),
                new CommandParameter[]{CommandSuggestions.names("世界名稱", false, "TokaPortalWorlds", List.of("world", "世界"))},
                new CommandParameter[]{CommandParameter.newType("世界名稱", CommandParamType.STRING)});
        assertEquals(2, data.versions.getFirst().overloads.size());
        assertEquals(CommandParamType.STRING, data.versions.getFirst().overloads.get("choice1").input.parameters[0].type);
        assertTrue(CommandSuggestions.names("住家名稱", true, "TokaPortalHomes", List.of("家")).optional);
        assertNull(CommandSuggestions.parameters(null, new CommandParameter[0]));
    }

    @Test void coreEncodesSuggestionsAndManualInputInCommandPacket() {
        var metadata = data();
        CommandSuggestions.parameters(metadata,
                new CommandParameter[]{CommandSuggestions.names("世界名稱", false, "TokaPortalWorlds", List.of("world", "世界"))},
                new CommandParameter[]{CommandParameter.newType("世界名稱", CommandParamType.STRING)});
        AvailableCommandsPacket packet = new AvailableCommandsPacket();
        packet.protocol = ProtocolInfo.CURRENT_PROTOCOL;
        packet.commands = new LinkedHashMap<>();
        packet.commands.put("tpw", metadata);
        assertDoesNotThrow(packet::encode);
        assertTrue(packet.getBuffer().length > 0);
    }
}
