package dev.toka.pl.tokaPortal.form.home;

import dev.toka.pl.tokaPortal.Main;
import cn.nukkit.Player;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.element.ElementDropdown;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.form.window.FormWindowCustom;
import dev.toka.pl.tokaPortal.form.BaseForm;
import dev.toka.pl.tokaPortal.point.HomePoint;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static dev.toka.pl.tokaPortal.Main.getProvider;

public class HomeEditListForm extends FormWindowCustom implements BaseForm {

    public HomeEditListForm(Player player) {
        super(Main.text("ui.home.editList.title"));
        HomePoint[] homes = getProvider().getHomePointsByCreator(player);
        if (homes.length > 0) {
            List<String> homeNames = Arrays.stream(homes).map(HomePoint::getName).collect(Collectors.toList());
            this.addElement(new ElementLabel(Main.text("ui.home.editList.hint")));
            this.addElement(new ElementDropdown(Main.text("ui.home.editList.dropdown"), homeNames));
        } else {
            this.addElement(new ElementLabel(Main.text("ui.home.editList.empty")));
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
            player.showFormWindow(new HomeEditForm(name));
        }
    }

    @Override
    public void onFormClose(PlayerFormRespondedEvent event) {

    }
}
