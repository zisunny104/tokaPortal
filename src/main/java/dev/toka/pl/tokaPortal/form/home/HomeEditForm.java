package dev.toka.pl.tokaPortal.form.home;

import dev.toka.pl.tokaPortal.Main;
import cn.nukkit.Player;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindowSimple;
import dev.toka.pl.tokaPortal.form.BaseForm;

public class HomeEditForm extends FormWindowSimple implements BaseForm {
    String name;

    public HomeEditForm(String name) {
        super(Main.text("ui.home.edit.title", name),
                Main.text("ui.home.edit.prompt"));
        this.name = name;
        this.addButton(new ElementButton(Main.text("ui.home.edit.delete")));
        this.addButton(new ElementButton(Main.text("ui.home.edit.back")));
    }

    @Override
    public void onFormResponse(PlayerFormRespondedEvent event) {
        Player player = event.getPlayer();
        String button = ((FormResponseSimple) event.getResponse()).getClickedButton().getText();
        if (button.equals(Main.text("ui.home.edit.delete"))) {
            player.showFormWindow(new HomeDelForm(name));
        } else {
            player.showFormWindow(new HomeEditListForm(player));
        }
    }

    @Override
    public void onFormClose(PlayerFormRespondedEvent event) {

    }
}
