package dev.toka.pl.tokaPortal.form.home;

import dev.toka.pl.tokaPortal.Main;
import cn.nukkit.Player;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindowSimple;
import dev.toka.pl.tokaPortal.form.BaseForm;
import dev.toka.pl.tokaPortal.point.HomePoint;
import dev.toka.pl.tokaPortal.utils.PortalWindow;

import static dev.toka.pl.tokaPortal.Main.getProvider;
import static dev.toka.pl.tokaPortal.utils.Portal.toPoint;

/** 住家傳送清單：按鈕 0=返回，之後依序為住家（以索引對應 homes）。 */
public class HomeListForm extends FormWindowSimple implements BaseForm {

    private final HomePoint[] homes;

    public HomeListForm(Player player) {
        super(Main.text("ui.home.list.title"), "");
        this.homes = getProvider().getHomePointsByCreator(player);
        this.setContent(Main.text(homes.length > 0 ? "ui.home.list.hint" : "ui.home.list.empty"));
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
            PortalWindow.sendPortalMainWindow(player);
            return;
        }
        int i = id - 1;
        if (i < 0 || i >= homes.length) return;
        toPoint(getProvider().getHomePoint(homes[i].getName()), player);
    }

    @Override
    public void onFormClose(PlayerFormRespondedEvent event) {
    }
}
