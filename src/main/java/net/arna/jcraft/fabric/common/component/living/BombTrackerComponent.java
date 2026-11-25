package net.arna.jcraft.fabric.common.component.living;

import org.ladysnake.cca.api.v8.component.CardinalComponent;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.CommonTickingComponent;
import net.arna.jcraft.api.component.living.CommonBombTrackerComponent;

public interface BombTrackerComponent extends CommonBombTrackerComponent, CardinalComponent, AutoSyncedComponent, CommonTickingComponent {

}
