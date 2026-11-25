package net.arna.jcraft.fabric.common.component.living;

import org.ladysnake.cca.api.v8.component.CardinalComponent;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.CommonTickingComponent;
import net.arna.jcraft.api.component.living.CommonHamonComponent;

public interface HamonComponent extends CommonHamonComponent, CardinalComponent, AutoSyncedComponent, CommonTickingComponent {

}
