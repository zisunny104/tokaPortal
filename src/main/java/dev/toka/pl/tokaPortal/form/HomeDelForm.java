package dev.toka.pl.tokaPortal.form;

import cn.nukkit.Player;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindowSimple;
import dev.toka.pl.tokaPortal.point.HomePoint;

import static dev.toka.pl.tokaPortal.utils.Portal.delHome;
import static dev.toka.pl.tokaPortal.utils.Utils.TITLE_PORTAL_HOME_DEL;

public class HomeDelForm extends FormWindowSimple implements BaseForm {

    HomePoint home;

    public HomeDelForm(HomePoint home) {
        super(TITLE_PORTAL_HOME_DEL, "確認要刪除住家' %name '嗎?".replace("%name", home.getName()));
        this.home = home;
        this.addButton(new ElementButton("確定"));
        this.addButton(new ElementButton("取消"));
    }

    @Override
    public void onFormResponse(PlayerFormRespondedEvent event) {
        Player player = event.getPlayer();
        String button = ((FormResponseSimple) event.getResponse()).getClickedButton().getText();
        switch (button) {
            case "確定":
                delHome(player, home);
                break;
            case "取消":
            default:
        }
    }

    @Override
    public void onFormClose(PlayerFormRespondedEvent event) {
    }
}