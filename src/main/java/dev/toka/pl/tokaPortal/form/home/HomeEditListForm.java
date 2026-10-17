package dev.toka.pl.tokaPortal.form.home;

import dev.toka.pl.tokaPortal.Main;
import cn.nukkit.Player;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindowSimple;
import dev.toka.pl.tokaPortal.form.BaseForm;
import dev.toka.pl.tokaPortal.point.HomePoint;

import static dev.toka.pl.tokaPortal.Main.getProvider;

/** 住家編輯清單：按鈕 0=返回（回到傳送清單），之後依序為住家。 */
public class HomeEditListForm extends FormWindowSimple implements BaseForm {

    private final HomePoint[] homes;

    public HomeEditListForm(Player player) {
        super(Main.text("ui.home.editList.title"), "");
        this.homes = getProvider().getHomePointsByCreator(player);
        this.setContent(Main.text(homes.length > 0 ? "ui.home.editList.hint" : "ui.home.editList.empty"));
        this.addButton(new ElementButton(Main.text("ui.home.back")));
        for (HomePoint h : homes) {
            this.addButton(new ElementButton(h.getName()));
        }
    }

    @Override
    public void onFormResponse(PlayerFormRespondedEvent event) {
        Player player = event.getPlayer();
        int id = ((FormResponseSimple) event.getResponse()).getClickedButtonId();
        if (id == 0) {
            player.showFormWindow(new HomeListForm(player));
            return;
        }
        int i = id - 1;
        if (i < 0 || i >= homes.length) return;
        player.showFormWindow(new HomeEditForm(homes[i].getName()));
    }

    @Override
    public void onFormClose(PlayerFormRespondedEvent event) {
    }
}
