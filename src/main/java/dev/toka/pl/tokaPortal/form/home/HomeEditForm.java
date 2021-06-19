package dev.toka.pl.tokaPortal.form.home;

import cn.nukkit.Player;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindowSimple;
import dev.toka.pl.tokaPortal.form.BaseForm;

import static dev.toka.pl.tokaPortal.utils.Utils.TITLE_PORTAL_HOME_EDIT;

public class HomeEditForm extends FormWindowSimple implements BaseForm {
    String name;

    public HomeEditForm(String name) {
        super(TITLE_PORTAL_HOME_EDIT.replace("%name", name),
                "請選擇要對住家進行的動作");
        this.name = name;
        this.addButton(new ElementButton("更改名稱"));
        this.addButton(new ElementButton("刪除住家"));
        this.addButton(new ElementButton("返回列表"));
    }

    @Override
    public void onFormResponse(PlayerFormRespondedEvent event) {
        Player player = event.getPlayer();
        String button = ((FormResponseSimple) event.getResponse()).getClickedButton().getText();
        switch (button) {
            case "刪除住家":
                player.showFormWindow(new HomeDelForm(name));
                break;
            case "更改名稱":
                player.sendMessage("[傳送]這項功能還沒有被寫出來喔((");
            case "返回列表":
            default:
                player.showFormWindow(new HomeEditListForm(player));
        }
    }

    @Override
    public void onFormClose(PlayerFormRespondedEvent event) {

    }
}