package dev.toka.pl.tokaPortal.form;

import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.window.FormWindowCustom;
import dev.toka.pl.tokaPortal.utils.Utils;

import static dev.toka.pl.tokaPortal.utils.Portal.setHome;

public class HomeSetForm extends FormWindowCustom implements BaseForm {

    public HomeSetForm() {
        super(Utils.TITLE_PORTAL_HOME_SET);
        this.addElement(new ElementInput("設置住家名稱", "住家名稱"));
    }

    @Override
    public void onFormResponse(PlayerFormRespondedEvent event) {
        setHome(event.getPlayer(), this.getResponse().getInputResponse(0));
    }

    @Override
    public void onFormClose(PlayerFormRespondedEvent event) {
        //TODO 關閉回到指定頁面
    }
}
