package dev.toka.pl.tokaPortal.plan;
import com.djrapitops.plan.capability.CapabilityService;
import com.djrapitops.plan.extension.*;
import com.djrapitops.plan.extension.annotation.*;
import com.djrapitops.plan.extension.builder.ExtensionDataBuilder;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

@PluginInfo(name="tokaPortal", iconName="location-arrow")
public final class PlanExtension implements DataExtension, AutoCloseable {
    private final PlanSettings settings;
    private final Supplier<PlanSnapshot> snapshots;
    private final Function<String,String> text;
    private volatile boolean active = true;
    private ExtensionService registered;
    public PlanExtension(PlanSettings settings, Supplier<PlanSnapshot> snapshots, Function<String,String> text) {
        this.settings=settings;this.snapshots=snapshots;this.text=text;
    }
    public boolean register() {
        if (!CapabilityService.getInstance().hasCapability("DATA_EXTENSION_BUILDER_API")) return false;
        ExtensionService service=ExtensionService.getInstance();service.register(this);registered=service;return true;
    }
    public PlanSnapshot snapshot() {
        PlanSnapshot value=snapshots.get();
        if (!active || value==null) throw new NotReadyException();
        return value;
    }
    @Override public CallEvents[] callExtensionMethodsOn() {
        return new CallEvents[]{CallEvents.PLAYER_JOIN,CallEvents.PLAYER_LEAVE,CallEvents.PLAYER_PERIODICAL,
                CallEvents.SERVER_EXTENSION_REGISTER,CallEvents.SERVER_PERIODICAL};
    }
    @DataBuilderProvider public ExtensionDataBuilder player(UUID id) {
        PlanSnapshot value=snapshot();ExtensionDataBuilder b=newExtensionDataBuilder();
        if(settings.homes()) b.addValue(Long.class,b.valueBuilder(text.apply("plan.playerHomes")).buildNumber(value.homes().getOrDefault(id,0L)));
        return b;
    }
    @DataBuilderProvider public ExtensionDataBuilder server() {
        PlanSnapshot value=snapshot();ExtensionDataBuilder b=newExtensionDataBuilder();
        if(settings.homes())b.addValue(Long.class,b.valueBuilder(text.apply("plan.totalHomes")).buildNumber(value.totalHomes()));
        if(settings.publicPoints())b.addValue(Long.class,b.valueBuilder(text.apply("plan.publicPoints")).buildNumber(value.publicPoints()));
        return b;
    }
    @Override public void close(){active=false;if(registered!=null){registered.unregister(this);registered=null;}}
}
