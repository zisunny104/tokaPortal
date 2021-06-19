package dev.toka.pl.tokaPortal.form.home;

import cn.nukkit.Player;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.response.FormResponseModal;
import cn.nukkit.form.window.FormWindowModal;
import dev.toka.pl.tokaPortal.form.BaseForm;

import static dev.toka.pl.tokaPortal.utils.Portal.delHome;
import static dev.toka.pl.tokaPortal.utils.Utils.TITLE_PORTAL_HOME_DEL;

public class HomeDelForm extends FormWindowModal implements BaseForm {

    static final String trueButtonText = "確定";
    static final String falseButtonText = "取消";
    String name;

    public HomeDelForm(String name) {
        super(TITLE_PORTAL_HOME_DEL,
                "確認要刪除住家' %name '嗎?".replace("%name", name),
                trueButtonText,
                falseButtonText);
        this.name = name;
    }

    @Override
    public void onFormResponse(PlayerFormRespondedEvent event) {
        Player player = event.getPlayer();
        if (((FormResponseModal) event.getResponse())
                .getClickedButtonText().equals(trueButtonText)) {
            delHome(player, name);
        }
    }

    @Override
    public void onFormClose(PlayerFormRespondedEvent event) {
    }
}