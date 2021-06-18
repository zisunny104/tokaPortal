package dev.toka.pl.tokaPortal.form;

import cn.nukkit.Player;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.element.ElementDropdown;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.form.window.FormWindowCustom;
import dev.toka.pl.tokaPortal.point.HomePoint;
import dev.toka.pl.tokaPortal.utils.Portal;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static dev.toka.pl.tokaPortal.Main.getProvider;
import static dev.toka.pl.tokaPortal.utils.Utils.TITLE_PORTAL_HOME_LIST;

public class HomeListForm extends FormWindowCustom implements BaseForm {

    public HomeListForm(Player player) {
        super(TITLE_PORTAL_HOME_LIST);
        HomePoint[] homes = getProvider().getHomePointsByCreator(player);
        if (homes.length > 0) {
            List<String> homeNames = Arrays.stream(homes).map(HomePoint::getName).collect(Collectors.toList());
            this.addElement(new ElementLabel("選取需要傳送的住家後\n點擊下方'送出'按鈕 進行傳送"));
            this.addElement(new ElementDropdown("住家列表", homeNames));
        } else {
            this.addElement(new ElementLabel("您沒有可傳送的住家"));
        }
    }

    @Override
    public void onFormResponse(PlayerFormRespondedEvent event) {
        Player player = event.getPlayer();
        if (this.getResponse().getDropdownResponse(1) != null) {
            String name = this.getResponse().getDropdownResponse(1).getElementContent();
            if (name == null) {
                return;
            }
            Portal.toPoint(getProvider().getHomePoint(name), player);
        }
    }

    @Override
    public void onFormClose(PlayerFormRespondedEvent event) {

    }
}
