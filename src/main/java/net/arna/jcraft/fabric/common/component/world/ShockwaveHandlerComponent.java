package net.arna.jcraft.fabric.common.component.world;

import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.CommonTickingComponent;
import net.arna.jcraft.api.component.world.CommonShockwaveHandlerComponent;

public interface ShockwaveHandlerComponent extends CommonShockwaveHandlerComponent, Component, AutoSyncedComponent, CommonTickingComponent {
}
