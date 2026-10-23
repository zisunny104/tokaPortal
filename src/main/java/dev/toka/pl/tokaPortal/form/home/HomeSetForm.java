package dev.toka.pl.tokaPortal.form.home;

import dev.toka.pl.tokaPortal.Main;
import cn.nukkit.Player;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.form.window.FormWindowCustom;
import dev.toka.pl.tokaPortal.form.BaseForm;

import static dev.toka.pl.tokaPortal.utils.Portal.setHome;

public class HomeSetForm extends FormWindowCustom implements BaseForm {

    public HomeSetForm(Player player) {
        super(Main.text("ui.home.set.title"));
        var at = player.getLocation();
        int homes = Main.getProvider() == null ? 0 : Main.getProvider().getHomePointsByCreator(player).length;
        // There is no cap on homes in the config, so the label shows how many the player already has.
        this.addElement(new ElementLabel(Main.text("ui.home.set.here", at.getLevel().getName(), String.format("%,d", at.getFloorX()),
                String.format("%,d", at.getFloorY()), String.format("%,d", at.getFloorZ()), String.format("%,d", homes))));
        this.addElement(new ElementInput(Main.text("ui.home.set.input"), Main.text("ui.home.set.placeholder")));
    }

    @Override
    public void onFormResponse(PlayerFormRespondedEvent event) {
        setHome(event.getPlayer(), this.getResponse().getInputResponse(1));
    }

    @Override
    public void onFormClose(PlayerFormRespondedEvent event) {
    }
}
