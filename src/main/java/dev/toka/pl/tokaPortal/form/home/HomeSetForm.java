package dev.toka.pl.tokaPortal.form.home;

import dev.toka.pl.tokaPortal.Main;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.window.FormWindowCustom;
import dev.toka.pl.tokaPortal.form.BaseForm;

import static dev.toka.pl.tokaPortal.utils.Portal.setHome;

public class HomeSetForm extends FormWindowCustom implements BaseForm {

    public HomeSetForm() {
        super(Main.text("ui.home.set.title"));
        this.addElement(new ElementInput(Main.text("ui.home.set.input"), Main.text("ui.home.set.placeholder")));
    }

    @Override
    public void onFormResponse(PlayerFormRespondedEvent event) {
        setHome(event.getPlayer(), this.getResponse().getInputResponse(0));
    }

    @Override
    public void onFormClose(PlayerFormRespondedEvent event) {
    }
}
