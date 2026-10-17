package dev.toka.pl.tokaPortal.form.home;

import dev.toka.pl.tokaPortal.Main;
import cn.nukkit.Player;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.response.FormResponseModal;
import cn.nukkit.form.window.FormWindowModal;
import dev.toka.pl.tokaPortal.form.BaseForm;

import static dev.toka.pl.tokaPortal.utils.Portal.delHome;

public class HomeDelForm extends FormWindowModal implements BaseForm {

    String name;

    public HomeDelForm(String name) {
        super(Main.text("ui.home.del.title"),
                Main.text("ui.home.del.confirm", name),
                Main.text("ui.common.confirm"),
                Main.text("ui.common.cancel"));
        this.name = name;
    }

    @Override
    public void onFormResponse(PlayerFormRespondedEvent event) {
        Player player = event.getPlayer();
        if (((FormResponseModal) event.getResponse())
                .getClickedButtonText().equals(Main.text("ui.common.confirm"))) {
            delHome(player, name);
        }
    }

    @Override
    public void onFormClose(PlayerFormRespondedEvent event) {
    }
}
