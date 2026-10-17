package dev.toka.pl.tokaPortal.form.home;

import dev.toka.pl.tokaPortal.Main;
import cn.nukkit.Player;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindowSimple;
import dev.toka.pl.tokaPortal.form.BaseForm;

/** 單一住家的動作選單：按鈕 0=返回列表，1=刪除住家。 */
public class HomeEditForm extends FormWindowSimple implements BaseForm {
    String name;

    public HomeEditForm(String name) {
        super(Main.text("ui.home.edit.title", name),
                Main.text("ui.home.edit.prompt"));
        this.name = name;
        this.addButton(new ElementButton(Main.text("ui.home.edit.back")));
        this.addButton(new ElementButton(Main.text("ui.home.edit.delete")));
    }

    @Override
    public void onFormResponse(PlayerFormRespondedEvent event) {
        Player player = event.getPlayer();
        int id = ((FormResponseSimple) event.getResponse()).getClickedButtonId();
        if (id == 1) {
            player.showFormWindow(new HomeDelForm(name));
        } else {
            player.showFormWindow(new HomeEditListForm(player));
        }
    }

    @Override
    public void onFormClose(PlayerFormRespondedEvent event) {

    }
}
