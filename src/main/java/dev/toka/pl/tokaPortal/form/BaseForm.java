package dev.toka.pl.tokaPortal.form;

import cn.nukkit.event.player.PlayerFormRespondedEvent;

public interface BaseForm {
    void onFormResponse(PlayerFormRespondedEvent event);

    void onFormClose(PlayerFormRespondedEvent event);
}
