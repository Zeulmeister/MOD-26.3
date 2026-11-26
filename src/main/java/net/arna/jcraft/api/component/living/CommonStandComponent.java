package net.arna.jcraft.api.component.living;

import net.arna.jcraft.api.attack.enums.MoveClass;
import net.arna.jcraft.api.component.JComponent;
import net.arna.jcraft.api.stand.StandType;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.stand.OriginProfile;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface CommonStandComponent extends JComponent {
    @Nullable
    StandType getType();

    default void setType(final @Nullable StandType type) {
        setTypeAndSkin(type, 0, false);
    }

    void setTypeAndSkin(final @Nullable StandType type, final int skin, final boolean loading);

    int getSkin();

    void setSkin(final int skin);

    boolean isTagged();

    void setTagged(boolean tagged);

    @Nullable
    StandEntity<?, ?> getStand();

    void setStand(final @Nullable StandEntity<?, ?> stand);

    Map<MoveClass, String> getOriginLoadout();

    @Nullable
    String getOriginSlot(final MoveClass slot);

    int getOriginLoadoutVersion();

    void setOriginLoadout(final Map<MoveClass, String> loadout);

    void setOriginSlot(final MoveClass slot, final @Nullable String skillId);

    void clearOriginLoadout();

    int getOriginGlowRgb();

    void setOriginGlowRgb(final int rgb);

    @Nullable
    OriginProfile getOriginProfile(final int index);

    void setOriginProfile(final int index, final @Nullable OriginProfile profile);

    void saveOriginProfile(final int index, final String name);

    boolean loadOriginProfile(final int index);

    void clearOriginProfile(final int index);
}
