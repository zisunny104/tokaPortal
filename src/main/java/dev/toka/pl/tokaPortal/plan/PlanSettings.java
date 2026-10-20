package dev.toka.pl.tokaPortal.plan;
import java.util.Map;
public record PlanSettings(boolean enabled, boolean homes, boolean publicPoints, int refreshTicks) {
    public static PlanSettings parse(Map<String, Object> root) {
        Map<?, ?> plan = root.get("plan") instanceof Map<?, ?> m ? m : Map.of();
        Object seconds = plan.get("refresh-seconds");
        int refresh = seconds instanceof Number n ? n.intValue() : 30;
        PlanControl.Options options=PlanControl.parse(root,java.util.Set.of("homes","public-points"));
        return new PlanSettings(options.enabled(),options.has("homes"),options.has("public-points"),Math.max(5, Math.min(3600, refresh)) * 20);
    }
}
