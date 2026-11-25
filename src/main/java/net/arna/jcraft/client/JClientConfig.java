package net.arna.jcraft.client;

import lombok.Getter;
import lombok.Setter;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.arna.jcraft.JCraft;

// Annotations to use here: https://shedaniel.gitbook.io/cloth-config/auto-config/annotations
@SuppressWarnings("FieldMayBeFinal")
@Getter
@Setter
@Config(name = JCraft.MOD_ID)
public class JClientConfig implements ConfigData {
    @Getter
    @ConfigEntry.Gui.Excluded
    private static JClientConfig instance = new JClientConfig();

    private UIPos uiPosition = UIPos.RIGHT;
    private UIDuration uiDuration = UIDuration.COOLDOWN;
    // private boolean clientsidePrediction = false;
    private int horizontalHudOffset = 0;
    private int verticalHudOffset = 0;
    private float hudScale = 1.0f;
    private boolean iconHudPeekAllMoves = true;
    private boolean iconHud = true;
    private boolean animeHudEnabled = true;
    private boolean onomatopoeiaEnabled = true;
    private boolean inputBufferEnabled = true;
    private boolean colorShiftEnabled = true;
    private boolean standAuras = true;
    private boolean disableNarrator = true;
    private boolean timeEraseShader = true;
    private boolean epitaphOverlay = true;
    private int dummyDamageIndicatorColor = 0xffff4444; // bright red
    private int dummyDamageIndicatorColorShadow = 0xff401010; // dark red shadow
    private boolean comboCounter = true;
    private boolean dynamicFirstPersonStandOpacity = true;
    private float firstPersonStandOpacityMult = 1.0f;
    private boolean showStandUserWarningPlayer = true;
    private boolean showStandUserWarningMob = true;
    private boolean disableMihAfterimages = false;

    public float getClampedHudScale() {
        return net.minecraft.util.Mth.clamp(this.hudScale, 0.75f, 1.50f);
    }

    public static void load() {
        instance = AutoConfig.getConfigHolder(JClientConfig.class).getConfig();
    }

    /*
    public void setClientsidePrediction(boolean clientsidePrediction) {
        this.clientsidePrediction = clientsidePrediction;
        net.arna.jcraft.common.network.JNetwork.sendToServer(JPacketRegistry.C2S_PREDICTION_TRIGGER, PredictionTriggerPacket.write(clientsidePrediction));
    }
     */

    public enum UIPos {
        LEFT,
        RIGHT,
        MIDDLE
    }

    public enum UIDuration {
        ALWAYS,
        COOLDOWN,
        NEVER
    }
}
