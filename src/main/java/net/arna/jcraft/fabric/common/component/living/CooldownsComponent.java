package net.arna.jcraft.fabric.common.component.living;

import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.CommonTickingComponent;
import net.arna.jcraft.api.component.living.CommonCooldownsComponent;

public interface CooldownsComponent extends CommonCooldownsComponent, Component, AutoSyncedComponent, CommonTickingComponent {

}
