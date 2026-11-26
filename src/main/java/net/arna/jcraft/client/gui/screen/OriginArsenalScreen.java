package net.arna.jcraft.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.arna.jcraft.api.attack.enums.MoveClass;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.attack.moves.AbstractSimpleAttack;
import net.arna.jcraft.api.component.living.CommonStandComponent;
import net.arna.jcraft.api.registry.JSoundRegistry;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.arna.jcraft.api.stand.StandType;
import net.arna.jcraft.client.JCraftClient;
import net.arna.jcraft.client.gui.hud.JCraftAbilityHud;
import net.arna.jcraft.common.config.JServerConfig;
import net.arna.jcraft.common.entity.stand.OriginEntity;
import net.arna.jcraft.common.network.c2s.OriginLoadoutPacket;
import net.arna.jcraft.common.network.c2s.RadialMenuActionPacket;
import net.arna.jcraft.common.stand.OriginProfile;
import net.arna.jcraft.common.stand.OriginSkillCatalog;
import net.arna.jcraft.common.stand.OriginSynergy;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Interactive Dark-Obsidian & Violet Origin Arsenal screen featuring:
 * Tab 0: Skill Archive (9 binds, search, stand filter, categories, presets)
 * Tab 1: Live 3D Rotating Preview + Real-Time Chroma RGB Color Forge
 * Tab 2: 4 Saved Player Profiles (1-click loadout+skin+chroma) + 5 Passive Synergies
 */
@Environment(EnvType.CLIENT)
public class OriginArsenalScreen extends Screen {
    public static final int TAB_SKILLS = 0;
    public static final int TAB_STUDIO = 1;
    public static final int TAB_PROFILES = 2;

    private static final int ROW_H = 25;
    private static final int ROW_GAP = 3;
    private static final int SCROLL_STEP = 20;

    private static final int[] CHROMA_SWATCHES = {
            0xA828FF, // Void Violet
            0xFF223D, // Crimson Eclipse
            0x18DCFF, // Astral Cyan
            0xFFD228, // Solar Gold
            0x00FFAA, // Emerald Warp
            0xFFF8E6, // Celestial Ivory
            0xFF5CE1, // Neon Magenta
            0xDCE6FF  // Abyssal Silver
    };

    private static final String[] PROFILE_NAME_POOL = {
            "Boss Killer",
            "Time Control",
            "Mobility",
            "Sovereign",
            "PvP Rush"
    };

    private record PillRect(int x, int y, int w, int h, Runnable action) {
        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    private record ChromaBar(int channel, int x, int y, int w, int h) {
        boolean contains(double mx, double my) {
            return mx >= x - 2 && mx <= x + w + 2 && my >= y - 2 && my <= y + h + 2;
        }
    }

    private final @Nullable Screen parent;
    private final EnumMap<MoveClass, String> localLoadout = new EnumMap<>(MoveClass.class);
    private final List<StandType> availableStands = new ArrayList<>();
    private final List<OriginSkillCatalog.SkillEntry> filteredSkills = new ArrayList<>();
    private final List<PillRect> tabPills = new ArrayList<>();
    private final List<PillRect> categoryPills = new ArrayList<>();
    private final List<PillRect> presetPills = new ArrayList<>();
    private final List<PillRect> studioPills = new ArrayList<>();
    private final List<PillRect> profilePills = new ArrayList<>();
    private final List<ChromaBar> chromaBars = new ArrayList<>();

    private int activeTab = TAB_SKILLS;
    private MoveClass selectedSlot = MoveClass.SPECIAL1;
    private OriginSkillCatalog.SkillCategory selectedCategory = OriginSkillCatalog.SkillCategory.ALL;
    private int standFilterIdx = -1; // -1 = All Stands
    private String searchQuery = "";
    private int scrollPixels = 0;

    // Studio 3D & Chroma state
    private @Nullable OriginEntity previewOrigin;
    private boolean rendered3DPreview = false;
    private float previewYawDeg = 28.0f;
    private float previewPitchDeg = -8.0f;
    private boolean autoSpin = true;
    private boolean draggingPreview = false;
    private boolean chromaEnabled = false;
    private int chromaR = 168;
    private int chromaG = 40;
    private int chromaB = 255;
    private int draggingChromaChannel = -1;
    private boolean chromaDirtyOnRelease = false;

    // Profiles label drafts
    private final String[] profileDraftNames = {
            OriginProfile.defaultNameForSlot(0),
            OriginProfile.defaultNameForSlot(1),
            OriginProfile.defaultNameForSlot(2),
            OriginProfile.defaultNameForSlot(3)
    };

    // Cached synergies evaluated from current loadout
    private OriginSynergy.SynergySummary cachedSynergies = OriginSynergy.SynergySummary.EMPTY;

    // Widgets
    private EditBox searchBox;
    private Button standPrevBtn;
    private Button standCycleBtn;
    private Button standNextBtn;
    private Button standResetBtn;
    private Button scrollUpBtn;
    private Button scrollDownBtn;
    private Button skinCycleBtn;

    // Layout geometry
    private int panelLeft, panelTop, panelW, panelH;
    private int leftColX, leftColW, leftRowStartY, slotPitch, slotH;
    private int listX, listW, tabBarY, contentTop, contentBottom;
    private int catBarY, presetBarY, listY, listBottom;
    private int previewBoxX, previewBoxY, previewBoxW, previewBoxH;
    private PillRect footerSynergyBadge;

    private @Nullable OriginSkillCatalog.SkillEntry hoveredSkill;
    private @Nullable String statusMessage;
    private long statusUntilMs = 0L;

    public OriginArsenalScreen(@Nullable Screen parent) {
        super(Component.translatable("jcraft.origin.arsenal.title"));
        this.parent = parent;
        loadFromClientPlayer();
    }

    /**
     * Must NOT pause singleplayer server so C2S bind/chroma/profile packets apply immediately while open.
     */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void loadFromClientPlayer() {
        localLoadout.clear();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            CommonStandComponent comp = JComponentPlatformUtils.getStandComponent(player);
            for (MoveClass slot : MoveClass.values()) {
                String id = comp.getOriginSlot(slot);
                localLoadout.put(slot, id != null && !id.isEmpty() ? id : OriginSkillCatalog.getDefaultId(slot));
            }
            int glowRgb = comp.getOriginGlowRgb();
            applyRgbFields(glowRgb);
            for (int i = 0; i < OriginProfile.MAX_PROFILES; i++) {
                OriginProfile prof = comp.getOriginProfile(i);
                if (prof != null) {
                    profileDraftNames[i] = prof.name();
                }
            }
        } else {
            for (MoveClass slot : MoveClass.values()) {
                localLoadout.put(slot, OriginSkillCatalog.getDefaultId(slot));
            }
        }
        recomputeSynergies();
    }

    private void applyRgbFields(int glowRgb) {
        if (glowRgb >= 0) {
            chromaEnabled = true;
            chromaR = (glowRgb >> 16) & 0xFF;
            chromaG = (glowRgb >> 8) & 0xFF;
            chromaB = glowRgb & 0xFF;
        } else {
            chromaEnabled = false;
            int skin = getCurrentSkin();
            int fallback = switch (skin) {
                case 1 -> 0xF5243D;
                case 2 -> 0x24DBFF;
                case 3 -> 0xFFCC29;
                case 4 -> 0xDCE6FF;
                case 5 -> 0xFFD638;
                default -> 0xA828FF;
            };
            chromaR = (fallback >> 16) & 0xFF;
            chromaG = (fallback >> 8) & 0xFF;
            chromaB = fallback & 0xFF;
        }
    }

    private int getCurrentPackedRgb() {
        return ((chromaR & 0xFF) << 16) | ((chromaG & 0xFF) << 8) | (chromaB & 0xFF);
    }

    private int getCurrentSkin() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null ? JComponentPlatformUtils.getStandComponent(player).getSkin() : 0;
    }

    private void recomputeSynergies() {
        boolean freeBinds = JServerConfig.ORIGIN_FREE_BINDS.getValue();
        List<OriginSkillCatalog.SkillEntry> resolvedList = new ArrayList<>(MoveClass.values().length);
        for (MoveClass slot : MoveClass.values()) {
            OriginSkillCatalog.SkillEntry entry = OriginSkillCatalog.resolveForSlot(slot, localLoadout.get(slot), freeBinds);
            if (entry != null) {
                resolvedList.add(entry);
            }
        }
        cachedSynergies = OriginSynergy.evaluate(resolvedList);
    }

    private void ensurePreviewEntity() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        if (previewOrigin == null || previewOrigin.level() != mc.level) {
            previewOrigin = new OriginEntity(mc.level);
            previewOrigin.setUiPreview(true);
            previewOrigin.setPlaySummonAnim(false);
            previewOrigin.tickCount = 40;
            previewOrigin.setStateNoReset(OriginEntity.State.IDLE);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (previewOrigin != null) {
            previewOrigin.tickCount++;
            if (previewOrigin.isPlaySummonAnim() && previewOrigin.tickCount > 24) {
                previewOrigin.setPlaySummonAnim(false);
                previewOrigin.setStateNoReset(OriginEntity.State.IDLE);
            }
        }
        if (activeTab == TAB_STUDIO && autoSpin && !draggingPreview) {
            previewYawDeg = (previewYawDeg + 1.6f) % 360.0f;
        }
    }

    private void rebuildFiltersAndCatalog() {
        boolean freeBinds = JServerConfig.ORIGIN_FREE_BINDS.getValue();
        List<OriginSkillCatalog.SkillEntry> slotPool = OriginSkillCatalog.getSkillsForSlot(selectedSlot, freeBinds);

        Set<StandType> seen = new LinkedHashSet<>();
        for (OriginSkillCatalog.SkillEntry entry : slotPool) {
            seen.add(entry.standType());
        }
        availableStands.clear();
        availableStands.addAll(seen);
        if (standFilterIdx >= availableStands.size()) {
            standFilterIdx = -1;
        }
        StandType standFilter = (standFilterIdx >= 0 && standFilterIdx < availableStands.size())
                ? availableStands.get(standFilterIdx)
                : null;

        String q = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        filteredSkills.clear();
        for (OriginSkillCatalog.SkillEntry entry : slotPool) {
            if (standFilter != null && entry.standType() != standFilter) {
                continue;
            }
            if (selectedCategory != OriginSkillCatalog.SkillCategory.ALL && entry.category() != selectedCategory) {
                continue;
            }
            if (!q.isEmpty()) {
                String moveName = entry.templateMove().getName().getString().toLowerCase(Locale.ROOT);
                String standName = entry.standType().getData().getInfo().getName().getString().toLowerCase(Locale.ROOT);
                String moveDesc = entry.templateMove().getDescription().getString().toLowerCase(Locale.ROOT);
                if (!moveName.contains(q) && !standName.contains(q) && !moveDesc.contains(q)) {
                    continue;
                }
            }
            filteredSkills.add(entry);
        }
        scrollPixels = Mth.clamp(scrollPixels, 0, getMaxScroll());
        updateStandFilterLabels();
    }

    private void updateStandFilterLabels() {
        if (standCycleBtn != null) {
            String rawStandLabel = standFilterIdx >= 0 && standFilterIdx < availableStands.size()
                    ? availableStands.get(standFilterIdx).getData().getInfo().getName().getString()
                    : Component.translatable("jcraft.origin.arsenal.all_stands").getString();
            standCycleBtn.setMessage(Component.literal(fitText(rawStandLabel, standCycleBtn.getWidth() - 8)));
        }
        if (standResetBtn != null) {
            standResetBtn.active = (activeTab == TAB_SKILLS) && standFilterIdx >= 0;
        }
    }

    private void updateTabWidgetVisibility() {
        boolean isSkills = (activeTab == TAB_SKILLS);
        if (searchBox != null) {
            searchBox.visible = isSkills;
            searchBox.active = isSkills;
            if (!isSkills) {
                searchBox.setFocused(false);
            }
        }
        if (standPrevBtn != null) {
            standPrevBtn.visible = isSkills;
            standPrevBtn.active = isSkills;
        }
        if (standCycleBtn != null) {
            standCycleBtn.visible = isSkills;
            standCycleBtn.active = isSkills;
        }
        if (standNextBtn != null) {
            standNextBtn.visible = isSkills;
            standNextBtn.active = isSkills;
        }
        if (standResetBtn != null) {
            standResetBtn.visible = isSkills;
            standResetBtn.active = isSkills && standFilterIdx >= 0;
        }
        if (scrollUpBtn != null) {
            scrollUpBtn.visible = isSkills;
            scrollUpBtn.active = isSkills;
        }
        if (scrollDownBtn != null) {
            scrollDownBtn.visible = isSkills;
            scrollDownBtn.active = isSkills;
        }
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.panelW = Math.min(600, this.width - 10);
        this.panelH = Math.min(312, this.height - 10);
        this.panelLeft = (this.width - this.panelW) / 2;
        this.panelTop = (this.height - this.panelH) / 2;

        this.leftColX = this.panelLeft + 7;
        this.leftColW = Math.min(176, (int) (this.panelW * 0.33f));
        this.leftRowStartY = this.panelTop + 29;
        this.slotPitch = Math.max(17, Math.min(24, (this.panelH - 36) / MoveClass.values().length));
        this.slotH = this.slotPitch - 2;

        this.listX = this.leftColX + this.leftColW + 10;
        this.listW = this.panelLeft + this.panelW - 7 - this.listX;

        // Top tab bar across right panel
        this.tabBarY = this.panelTop + 20;
        this.contentTop = this.tabBarY + 17;
        this.contentBottom = this.panelTop + this.panelH - 36;

        // Tab 0 Toolbar Row 1: Search box + Stand filter cycler
        int bar1Y = this.contentTop;
        int searchW = Math.max(74, Math.min(126, this.listW - 146));
        String prevQuery = this.searchQuery;
        this.searchBox = new EditBox(this.font, this.listX, bar1Y, searchW, 14,
                Component.translatable("jcraft.origin.arsenal.search_hint"));
        this.searchBox.setMaxLength(48);
        this.searchBox.setValue(prevQuery);
        this.searchBox.setTextColor(0xFFF3E8FF);
        this.searchBox.setResponder(s -> {
            if (!s.equals(this.searchQuery)) {
                this.searchQuery = s;
                this.scrollPixels = 0;
                rebuildFiltersAndCatalog();
            }
        });
        this.addRenderableWidget(this.searchBox);

        int standBtnStart = this.listX + searchW + 4;
        int arrowW = 14;
        int clearW = 14;
        int standNameW = Math.max(36, (this.listX + this.listW) - standBtnStart - arrowW * 2 - clearW - 6);
        this.standPrevBtn = this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> cycleStandFilter(-1))
                .bounds(standBtnStart, bar1Y, arrowW, 14).build());
        this.standCycleBtn = this.addRenderableWidget(Button.builder(Component.empty(), b -> cycleStandFilter(1))
                .bounds(standBtnStart + arrowW + 2, bar1Y, standNameW, 14).build());
        this.standNextBtn = this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> cycleStandFilter(1))
                .bounds(standBtnStart + arrowW + 2 + standNameW + 2, bar1Y, arrowW, 14).build());
        this.standResetBtn = this.addRenderableWidget(Button.builder(Component.literal("✕"), b -> {
            standFilterIdx = -1;
            scrollPixels = 0;
            rebuildFiltersAndCatalog();
        }).bounds(this.listX + this.listW - clearW, bar1Y, clearW, 14).build());

        this.catBarY = bar1Y + 16;
        this.presetBarY = this.catBarY + 14;
        this.listY = this.presetBarY + 15;
        this.listBottom = this.contentBottom - 2;

        // Footer controls
        int footerY = this.panelTop + this.panelH - 19;
        this.scrollUpBtn = this.addRenderableWidget(Button.builder(Component.literal("▲"), b -> scrollBy(-SCROLL_STEP * 3))
                .bounds(this.listX, footerY, 18, 15).build());
        this.scrollDownBtn = this.addRenderableWidget(Button.builder(Component.literal("▼"), b -> scrollBy(SCROLL_STEP * 3))
                .bounds(this.listX + 20, footerY, 18, 15).build());

        int doneX = this.panelLeft + this.panelW - 60;
        int skinBtnW = Math.min(108, Math.max(68, this.listW - 152));
        int skinBtnX = doneX - skinBtnW - 4;
        this.skinCycleBtn = this.addRenderableWidget(Button.builder(Component.empty(), b -> cycleOriginSkin(1))
                .bounds(skinBtnX, footerY, skinBtnW, 15).build());
        updateSkinButtonLabel();
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(doneX, footerY, 53, 15).build());

        updateFooterBadgeBounds();

        rebuildAllPills();
        rebuildFiltersAndCatalog();
        this.scrollPixels = Mth.clamp(this.scrollPixels, 0, getMaxScroll());
        updateTabWidgetVisibility();
    }

    private void rebuildAllPills() {
        // Tab switcher pills
        tabPills.clear();
        int tabGap = 3;
        int tabW = (this.listW - tabGap * 2) / 3;
        for (int t = 0; t < 3; t++) {
            final int tabIdx = t;
            int tx = this.listX + t * (tabW + tabGap);
            tabPills.add(new PillRect(tx, this.tabBarY, tabW, 14, () -> selectTab(tabIdx)));
        }

        // Tab 0 category & preset pills
        categoryPills.clear();
        OriginSkillCatalog.SkillCategory[] cats = OriginSkillCatalog.SkillCategory.values();
        int cGap = 2;
        int cW = (this.listW - cGap * (cats.length - 1)) / cats.length;
        for (int i = 0; i < cats.length; i++) {
            OriginSkillCatalog.SkillCategory cat = cats[i];
            int px = this.listX + i * (cW + cGap);
            categoryPills.add(new PillRect(px, this.catBarY, cW, 12, () -> {
                selectedCategory = cat;
                scrollPixels = 0;
                rebuildFiltersAndCatalog();
                playUiClick();
            }));
        }

        presetPills.clear();
        List<OriginSkillCatalog.LoadoutPreset> presets = OriginSkillCatalog.getPresets();
        int count = presets.size() + 1;
        int pGap = 2;
        int pW = (this.listW - pGap * (count - 1)) / count;
        for (int i = 0; i < presets.size(); i++) {
            OriginSkillCatalog.LoadoutPreset preset = presets.get(i);
            int px = this.listX + i * (pW + pGap);
            presetPills.add(new PillRect(px, this.presetBarY, pW, 13, () -> applyPreset(preset)));
        }
        presetPills.add(new PillRect(this.listX + presets.size() * (pW + pGap), this.presetBarY, pW, 13, this::resetDefault));

        // Tab 1 (Studio 3D & Chroma) pills + sliders
        studioPills.clear();
        chromaBars.clear();
        int leftPaneW = Math.max(96, (int) (this.listW * 0.48f) - 4);
        int rightPaneX = this.listX + leftPaneW + 8;
        int rightPaneW = Math.max(88, (this.listX + this.listW) - rightPaneX);

        this.previewBoxX = this.listX;
        this.previewBoxY = this.contentTop;
        this.previewBoxW = leftPaneW;
        this.previewBoxH = Math.max(64, this.contentBottom - this.contentTop - 32);

        int ctrlY1 = this.previewBoxY + this.previewBoxH + 3;
        int ctrlY2 = ctrlY1 + 15;
        int skinNavArrowW = 16;
        int skinLabelW = Math.max(40, leftPaneW - skinNavArrowW * 2 - 4);
        studioPills.add(new PillRect(this.previewBoxX, ctrlY1, skinNavArrowW, 13, () -> cycleOriginSkin(-1)));
        studioPills.add(new PillRect(this.previewBoxX + skinNavArrowW + 2 + skinLabelW + 2, ctrlY1, skinNavArrowW, 13, () -> cycleOriginSkin(1)));

        int halfCtrlW = (leftPaneW - 3) / 2;
        studioPills.add(new PillRect(this.previewBoxX, ctrlY2, halfCtrlW, 13, () -> {
            autoSpin = !autoSpin;
            playUiClick();
        }));
        studioPills.add(new PillRect(this.previewBoxX + halfCtrlW + 3, ctrlY2, halfCtrlW, 13, () -> {
            ensurePreviewEntity();
            if (previewOrigin != null) {
                previewOrigin.tickCount = 1;
                previewOrigin.setPlaySummonAnim(true);
            }
            playUiClick();
        }));

        // Right Chroma Forge pane
        int toggleY = this.contentTop + 13;
        studioPills.add(new PillRect(rightPaneX, toggleY, rightPaneW, 14, () -> {
            chromaEnabled = !chromaEnabled;
            commitChromaRgb(chromaEnabled ? getCurrentPackedRgb() : -1);
            playUiClick();
        }));

        int sliderStartY = toggleY + 20;
        int sliderPitch = Math.max(17, Math.min(24, (this.contentBottom - sliderStartY - 46) / 3));
        int sliderW = rightPaneW - 38;
        for (int ch = 0; ch < 3; ch++) {
            chromaBars.add(new ChromaBar(ch, rightPaneX + 14, sliderStartY + ch * sliderPitch, Math.max(32, sliderW), 10));
        }

        int swatchY = sliderStartY + 3 * sliderPitch + 12;
        int swCount = CHROMA_SWATCHES.length;
        int swGap = 2;
        int swSize = Math.max(10, Math.min(18, (rightPaneW - swGap * (swCount - 1)) / swCount));
        for (int i = 0; i < swCount; i++) {
            final int swRgb = CHROMA_SWATCHES[i];
            int sx = rightPaneX + i * (swSize + swGap);
            studioPills.add(new PillRect(sx, swatchY, swSize, 13, () -> {
                applyRgbFields(swRgb);
                chromaEnabled = true;
                commitChromaRgb(swRgb);
                playBindSound();
            }));
        }

        int resetY = Math.min(this.contentBottom - 14, swatchY + 17);
        studioPills.add(new PillRect(rightPaneX, resetY, rightPaneW, 13, () -> {
            commitChromaRgb(-1);
            applyRgbFields(-1);
            playUiClick();
        }));

        // Tab 2 (Profiles & Synergies) pills
        profilePills.clear();
        int cardsTop = this.contentTop + 11;
        int totalH = Math.max(115, this.contentBottom - cardsTop);
        int profilesSectionH = Math.max(60, Math.min(96, (int) (totalH * 0.43f)));
        int cardGap = 3;
        int cardW = (this.listW - cardGap) / 2;
        int cardH = Math.max(28, (profilesSectionH - cardGap) / 2);
        for (int i = 0; i < OriginProfile.MAX_PROFILES; i++) {
            final int idx = i;
            int col = i % 2;
            int row = i / 2;
            int cx = this.listX + col * (cardW + cardGap);
            int cy = cardsTop + row * (cardH + cardGap);

            // Rename cycler [✎] on top-right
            profilePills.add(new PillRect(cx + cardW - 14, cy + 2, 12, 9, () -> {
                cycleProfileDraftName(idx);
                playUiClick();
            }));

            // Action pills on bottom of card: Load | Save | ✕
            int btnY = cy + cardH - 12;
            int clearW = 14;
            int halfActionW = Math.max(24, (cardW - clearW - 10) / 2);
            profilePills.add(new PillRect(cx + 3, btnY, halfActionW, 10, () -> loadProfile(idx)));
            profilePills.add(new PillRect(cx + 3 + halfActionW + 2, btnY, halfActionW, 10, () -> saveProfile(idx)));
            profilePills.add(new PillRect(cx + cardW - clearW - 3, btnY, clearW, 10, () -> clearProfile(idx)));
        }
    }

    private void updateFooterBadgeBounds() {
        int skinBtnX = this.skinCycleBtn != null ? this.skinCycleBtn.getX() : (this.panelLeft + this.panelW - 120);
        int badgeX = (activeTab == TAB_SKILLS) ? (this.listX + 42) : this.listX;
        int badgeW = Math.max(16, skinBtnX - badgeX - 4);
        this.footerSynergyBadge = new PillRect(badgeX, this.panelTop + this.panelH - 19, badgeW, 15,
                () -> selectTab(TAB_PROFILES));
    }

    public void selectTab(int tab) {
        int clamped = Mth.clamp(tab, TAB_SKILLS, TAB_PROFILES);
        if (this.activeTab != clamped) {
            this.activeTab = clamped;
            if (this.activeTab == TAB_STUDIO) {
                ensurePreviewEntity();
            }
            updateFooterBadgeBounds();
            updateTabWidgetVisibility();
        }
    }

    private void cycleProfileDraftName(int idx) {
        String current = profileDraftNames[idx];
        int found = -1;
        for (int i = 0; i < PROFILE_NAME_POOL.length; i++) {
            if (PROFILE_NAME_POOL[i].equals(current)) {
                found = i;
                break;
            }
        }
        profileDraftNames[idx] = PROFILE_NAME_POOL[(found + 1) % PROFILE_NAME_POOL.length];
    }

    private void commitChromaRgb(int rgb) {
        int clean = rgb < 0 ? -1 : (rgb & 0xFFFFFF);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            CommonStandComponent comp = JComponentPlatformUtils.getStandComponent(player);
            comp.setOriginGlowRgb(clean);
            if (JUtils.getStand(player) instanceof OriginEntity origin) {
                origin.setCustomGlowRgb(clean);
            }
        }
        OriginLoadoutPacket.sendSetChroma(clean);
        if (clean < 0) {
            showStatus("✓ " + Component.translatable("jcraft.origin.chroma.reset_done").getString());
        } else {
            showStatus("✦ " + Component.translatable("jcraft.origin.chroma.applied",
                    String.format(Locale.ROOT, "#%06X", clean)).getString());
        }
    }

    private void saveProfile(int slotIdx) {
        String cleanName = OriginProfile.sanitizeName(profileDraftNames[slotIdx], OriginProfile.defaultNameForSlot(slotIdx));
        profileDraftNames[slotIdx] = cleanName;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            JComponentPlatformUtils.getStandComponent(player).saveOriginProfile(slotIdx, cleanName);
        }
        OriginLoadoutPacket.sendSaveProfile(slotIdx, cleanName);
        playBindSound();
        showStatus("✓ " + Component.translatable("jcraft.origin.profile.saved", cleanName).getString());
    }

    private void loadProfile(int slotIdx) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        CommonStandComponent comp = JComponentPlatformUtils.getStandComponent(player);
        OriginProfile prof = comp.getOriginProfile(slotIdx);
        if (prof == null) {
            playUiClick();
            return;
        }
        comp.loadOriginProfile(slotIdx);
        if (JUtils.getStand(player) instanceof OriginEntity origin) {
            origin.setSkin(comp.getSkin());
            origin.rebuildLoadoutFromUser(comp);
        }
        for (MoveClass slot : MoveClass.values()) {
            String id = comp.getOriginSlot(slot);
            localLoadout.put(slot, id != null && !id.isEmpty() ? id : OriginSkillCatalog.getDefaultId(slot));
        }
        applyRgbFields(comp.getOriginGlowRgb());
        recomputeSynergies();
        updateSkinButtonLabel();
        OriginLoadoutPacket.sendLoadProfile(slotIdx);
        playBindSound();
        showStatus("✓ " + Component.translatable("jcraft.origin.profile.loaded", prof.name()).getString());
    }

    private void clearProfile(int slotIdx) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            JComponentPlatformUtils.getStandComponent(player).clearOriginProfile(slotIdx);
        }
        OriginLoadoutPacket.sendClearProfile(slotIdx);
        playUiClick();
        showStatus("✓ " + Component.translatable("jcraft.origin.profile.cleared", slotIdx + 1).getString());
    }

    private void cycleOriginSkin(int delta) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        CommonStandComponent comp = JComponentPlatformUtils.getStandComponent(player);
        var info = JStandTypeRegistry.ORIGIN.get().getData().getInfo();
        int count = Math.max(1, info.getSkinCount());
        int nextSkin = Math.floorMod(comp.getSkin() + delta, count);
        comp.setSkin(nextSkin);
        if (JUtils.getStand(player) instanceof OriginEntity origin) {
            origin.setSkin(nextSkin);
        }
        if (!chromaEnabled) {
            applyRgbFields(-1);
        }
        RadialMenuActionPacket.sendSkin(nextSkin);
        updateSkinButtonLabel();
        showStatus("✦ Skin ➜ " + getOriginSkinLabel(nextSkin));
        playUiClick();
    }

    private String getOriginSkinLabel(int skin) {
        var info = JStandTypeRegistry.ORIGIN.get().getData().getInfo();
        if (skin <= 0 || skin - 1 >= info.getSkinNames().size()) {
            return "Default";
        }
        return info.getSkinNames().get(skin - 1).getString();
    }

    private void updateSkinButtonLabel() {
        if (this.skinCycleBtn == null) return;
        String text = "Skin: " + getOriginSkinLabel(getCurrentSkin());
        this.skinCycleBtn.setMessage(Component.literal(fitText(text, this.skinCycleBtn.getWidth() - 8)));
    }

    private void cycleStandFilter(int delta) {
        if (availableStands.isEmpty()) {
            standFilterIdx = -1;
            updateStandFilterLabels();
            return;
        }
        int total = availableStands.size() + 1;
        int cur = standFilterIdx + 1;
        int next = Math.floorMod(cur + delta, total);
        standFilterIdx = next - 1;
        scrollPixels = 0;
        rebuildFiltersAndCatalog();
        playUiClick();
    }

    private void applyPreset(OriginSkillCatalog.LoadoutPreset preset) {
        boolean freeBinds = JServerConfig.ORIGIN_FREE_BINDS.getValue();
        EnumMap<MoveClass, String> nextMap = new EnumMap<>(MoveClass.class);
        for (MoveClass slot : MoveClass.values()) {
            String rawId = preset.slots().getOrDefault(slot, OriginSkillCatalog.getDefaultId(slot));
            OriginSkillCatalog.SkillEntry resolved = OriginSkillCatalog.resolveForSlot(slot, rawId, freeBinds);
            if (resolved != null) {
                localLoadout.put(slot, resolved.id());
                nextMap.put(slot, resolved.id());
            }
        }
        syncClientPlayerOptimistically(nextMap, false, null, null);
        recomputeSynergies();
        OriginLoadoutPacket.sendApplyPreset(preset.id());
        playBindSound();
        showStatus("✓ " + Component.translatable("jcraft.origin.arsenal.preset_applied",
                Component.translatable(preset.titleKey())).getString());
    }

    private void resetDefault() {
        for (MoveClass slot : MoveClass.values()) {
            localLoadout.put(slot, OriginSkillCatalog.getDefaultId(slot));
        }
        syncClientPlayerOptimistically(null, true, null, null);
        recomputeSynergies();
        OriginLoadoutPacket.sendResetDefault();
        playBindSound();
        showStatus("✓ " + Component.translatable("jcraft.origin.arsenal.reset_done").getString());
    }

    private void assignSkillToSelectedSlot(OriginSkillCatalog.SkillEntry entry) {
        localLoadout.put(selectedSlot, entry.id());
        syncClientPlayerOptimistically(null, false, selectedSlot, entry.id());
        recomputeSynergies();
        OriginLoadoutPacket.sendSetSlot(selectedSlot, entry.id());
        playBindSound();
        showStatus("✓ " + selectedSlot.getFriendlyName().getString()
                + " ➜ " + entry.standType().getData().getInfo().getName().getString()
                + " — " + entry.templateMove().getName().getString());
    }

    private static void syncClientPlayerOptimistically(
            @Nullable EnumMap<MoveClass, String> fullMap,
            boolean reset,
            @Nullable MoveClass singleSlot,
            @Nullable String singleId) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        CommonStandComponent comp = JComponentPlatformUtils.getStandComponent(player);
        if (reset) {
            comp.clearOriginLoadout();
        } else if (fullMap != null) {
            comp.setOriginLoadout(fullMap);
        } else if (singleSlot != null) {
            comp.setOriginSlot(singleSlot, singleId);
        }
        if (JUtils.getStand(player) instanceof OriginEntity origin) {
            origin.rebuildLoadoutFromUser(comp);
        }
    }

    private void showStatus(String message) {
        this.statusMessage = message;
        this.statusUntilMs = System.currentTimeMillis() + 3500L;
    }

    private static void playBindSound() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(JSoundRegistry.ORIGIN_BIND.get(), 1.0F));
    }

    private static void playUiClick() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    private int getMaxScroll() {
        int totalH = filteredSkills.size() * (ROW_H + ROW_GAP);
        int viewH = Math.max(1, listBottom - listY);
        return Math.max(0, totalH - viewH);
    }

    private boolean scrollBy(int delta) {
        if (activeTab != TAB_SKILLS) {
            return false;
        }
        int max = getMaxScroll();
        int next = Mth.clamp(scrollPixels + delta, 0, max);
        if (next != scrollPixels) {
            scrollPixels = next;
            return true;
        }
        return false;
    }

    private void updateChromaChannelFromMouse(int channel, double mouseX) {
        if (channel < 0 || channel >= chromaBars.size()) {
            return;
        }
        ChromaBar bar = chromaBars.get(channel);
        float t = (float) Mth.clamp((mouseX - bar.x()) / Math.max(1.0, bar.w()), 0.0, 1.0);
        int val = Math.round(t * 255.0f);
        switch (channel) {
            case 0 -> chromaR = val;
            case 1 -> chromaG = val;
            case 2 -> chromaB = val;
            default -> {}
        }
        chromaEnabled = true;
        chromaDirtyOnRelease = true;
        LocalPlayer player = Minecraft.getInstance().player;
        int packed = getCurrentPackedRgb();
        if (player != null) {
            JComponentPlatformUtils.getStandComponent(player).setOriginGlowRgb(packed);
            if (JUtils.getStand(player) instanceof OriginEntity origin) {
                origin.setCustomGlowRgb(packed);
            }
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parent);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && scrollBy(scrollY > 0 ? -SCROLL_STEP : SCROLL_STEP)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = event.x();
            double my = event.y();

            if (this.searchBox != null && !this.searchBox.isMouseOver(mx, my)) {
                this.searchBox.setFocused(false);
            }

            // Tab pills
            for (PillRect tabPill : tabPills) {
                if (tabPill.contains(mx, my)) {
                    tabPill.action().run();
                    playUiClick();
                    return true;
                }
            }

            // Footer synergy shortcut
            if (footerSynergyBadge != null && footerSynergyBadge.contains(mx, my)) {
                footerSynergyBadge.action().run();
                playUiClick();
                return true;
            }

            // Left 9 slot cards (active across all tabs)
            MoveClass[] slots = MoveClass.values();
            for (int i = 0; i < slots.length; i++) {
                int sy = leftRowStartY + i * slotPitch;
                if (mx >= leftColX && mx <= leftColX + leftColW && my >= sy && my <= sy + slotH) {
                    if (selectedSlot != slots[i]) {
                        selectedSlot = slots[i];
                        scrollPixels = 0;
                        rebuildFiltersAndCatalog();
                        playUiClick();
                    }
                    return true;
                }
            }

            if (activeTab == TAB_SKILLS) {
                for (PillRect pill : categoryPills) {
                    if (pill.contains(mx, my)) {
                        pill.action().run();
                        return true;
                    }
                }
                for (PillRect pill : presetPills) {
                    if (pill.contains(mx, my)) {
                        pill.action().run();
                        return true;
                    }
                }
                if (mx >= listX && mx <= listX + listW && my >= listY && my <= listBottom) {
                    int relY = (int) (my - listY + scrollPixels);
                    int idx = relY / (ROW_H + ROW_GAP);
                    int offsetInRow = relY % (ROW_H + ROW_GAP);
                    if (idx >= 0 && idx < filteredSkills.size() && offsetInRow <= ROW_H) {
                        assignSkillToSelectedSlot(filteredSkills.get(idx));
                        return true;
                    }
                    if (this.hoveredSkill != null) {
                        assignSkillToSelectedSlot(this.hoveredSkill);
                        return true;
                    }
                }
            } else if (activeTab == TAB_STUDIO) {
                if (mx >= previewBoxX && mx <= previewBoxX + previewBoxW
                        && my >= previewBoxY && my <= previewBoxY + previewBoxH) {
                    draggingPreview = true;
                    autoSpin = false;
                    return true;
                }
                for (ChromaBar bar : chromaBars) {
                    if (bar.contains(mx, my)) {
                        draggingChromaChannel = bar.channel();
                        updateChromaChannelFromMouse(draggingChromaChannel, mx);
                        return true;
                    }
                }
                for (PillRect pill : studioPills) {
                    if (pill.contains(mx, my)) {
                        pill.action().run();
                        return true;
                    }
                }
            } else if (activeTab == TAB_PROFILES) {
                for (PillRect pill : profilePills) {
                    if (pill.contains(mx, my)) {
                        pill.action().run();
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0 && activeTab == TAB_STUDIO) {
            if (draggingPreview) {
                previewYawDeg = (previewYawDeg + (float) dragX * 1.15f) % 360.0f;
                previewPitchDeg = Mth.clamp(previewPitchDeg + (float) dragY * 0.65f, -32.0f, 28.0f);
                return true;
            }
            if (draggingChromaChannel >= 0) {
                updateChromaChannelFromMouse(draggingChromaChannel, event.x());
                return true;
            }
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            draggingPreview = false;
            if (draggingChromaChannel >= 0) {
                draggingChromaChannel = -1;
                if (chromaDirtyOnRelease) {
                    chromaDirtyOnRelease = false;
                    commitChromaRgb(getCurrentPackedRgb());
                    playUiClick();
                }
                return true;
            }
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (searchBox == null || !searchBox.isFocused()) {
            if (JCraftClient.ORIGIN_ARSENAL_KEY != null
                    && JCraftClient.ORIGIN_ARSENAL_KEY.getParent().matches(event)) {
                onClose();
                return true;
            }
            int key = event.key();
            if (key == InputConstants.KEY_PAGEUP) {
                return scrollBy(-SCROLL_STEP * 4);
            }
            if (key == InputConstants.KEY_PAGEDOWN) {
                return scrollBy(SCROLL_STEP * 4);
            }
            if (key >= InputConstants.KEY_1 && key <= InputConstants.KEY_9) {
                int idx = key - InputConstants.KEY_1;
                MoveClass[] values = MoveClass.values();
                if (idx < values.length) {
                    selectedSlot = values[idx];
                    scrollPixels = 0;
                    rebuildFiltersAndCatalog();
                    playUiClick();
                    return true;
                }
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        gui.fill(0, 0, this.width, this.height, 0xAA040208);
        gui.fill(panelLeft, panelTop, panelLeft + panelW, panelTop + panelH, 0xEE0D0A18);
        gui.outline(panelLeft, panelTop, panelW, panelH, 0xFFC084FC);
        gui.fill(panelLeft + 1, panelTop + 17, panelLeft + panelW - 1, panelTop + 18, 0x779333EA);
        gui.fill(leftColX + leftColW + 4, panelTop + 20, leftColX + leftColW + 5, panelTop + panelH - 6, 0x557C3AED);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(gui, mouseX, mouseY, partialTick);

        boolean freeBinds = JServerConfig.ORIGIN_FREE_BINDS.getValue();
        String titleStr = Component.translatable("jcraft.origin.arsenal.title").getString();
        var hPose = gui.pose();
        hPose.pushMatrix();
        hPose.translate(leftColX, this.panelTop + 5.5f);
        hPose.scale(0.90f, 0.90f);
        gui.text(this.font, fitText(titleStr, (int) ((leftColW + 6) / 0.90f)), 0, 0, 0xFFE9D5FF, false);
        hPose.popMatrix();

        String modeShort = freeBinds ? "Free: ON" : "Strict";
        int modeW = Math.round(this.font.width(modeShort) * 0.78f);
        hPose.pushMatrix();
        hPose.translate(panelLeft + panelW - 8 - modeW, panelTop + 6);
        hPose.scale(0.78f, 0.78f);
        gui.text(this.font, modeShort, 0, 0, freeBinds ? 0xFF34D399 : 0xFFFBBF24, false);
        hPose.popMatrix();

        String stepHint = Component.translatable("jcraft.origin.arsenal.hint").getString();
        int hintRegionW = Math.max(40, listW - modeW - 12);
        drawScaledCenteredText(gui, stepHint, listX + hintRegionW * 0.5f, panelTop + 6.5f,
                0.70f, hintRegionW, 0xFFC4B5FD);

        // Left column: 9 active binds
        renderLeftBindColumn(gui, mouseX, mouseY, freeBinds);

        // Right header tabs
        renderTabSwitcher(gui, mouseX, mouseY);

        // Active tab body
        switch (activeTab) {
            case TAB_SKILLS -> renderSkillsTab(gui, mouseX, mouseY, freeBinds);
            case TAB_STUDIO -> renderStudioTab(gui, mouseX, mouseY, partialTick);
            case TAB_PROFILES -> renderProfilesAndSynergiesTab(gui, mouseX, mouseY);
            default -> {}
        }

        // Bottom description / toast banner + footer status
        renderFooter(gui, mouseX, mouseY, freeBinds);
    }

    private void renderLeftBindColumn(GuiGraphicsExtractor gui, int mouseX, int mouseY, boolean freeBinds) {
        gui.text(this.font, Component.translatable("jcraft.origin.arsenal.binds"),
                leftColX, this.panelTop + 20, 0xFFC084FC, false);

        MoveClass[] slots = MoveClass.values();
        for (int i = 0; i < slots.length; i++) {
            MoveClass slot = slots[i];
            int sy = leftRowStartY + i * slotPitch;
            boolean active = (slot == selectedSlot);
            boolean hovered = mouseX >= leftColX && mouseX <= leftColX + leftColW && mouseY >= sy && mouseY <= sy + slotH;

            int bg = active ? 0xEE2B124C : (hovered ? 0xDD1F1236 : 0xBB130C22);
            int border = active ? 0xFFE879F9 : (hovered ? 0xFFA855F7 : 0x556D28D9);
            gui.fill(leftColX, sy, leftColX + leftColW, sy + slotH, bg);
            gui.outline(leftColX, sy, leftColW, slotH, border);

            String equippedId = localLoadout.get(slot);
            OriginSkillCatalog.SkillEntry resolved = OriginSkillCatalog.resolveForSlot(slot, equippedId, freeBinds);
            if (resolved != null) {
                int iconY = sy + Math.max(-2, (slotH - 22) / 2);
                JCraftAbilityHud.renderIcon(gui, leftColX + 1, iconY, resolved.standId(), resolved.getIconName());
            }

            String slotTitle = slot.getFriendlyName().getString();
            String moveLabel = resolved != null ? resolved.templateMove().getName().getString() : "-";
            String standShort = resolved != null ? resolved.standType().getData().getInfo().getName().getString() : "";

            int tx = leftColX + 24;
            int maxW = Math.max(40, leftColW - 27);
            int topLineY = sy + (slotH >= 20 ? 2 : 1);
            int subLineY = sy + (slotH >= 20 ? 10 : 9);
            var sPose = gui.pose();
            sPose.pushMatrix();
            sPose.translate(tx, topLineY);
            sPose.scale(0.82f, 0.82f);
            gui.text(this.font, fitText("§d" + slotTitle + " §7• §f" + standShort, (int) (maxW / 0.82f)),
                    0, 0, 0xFFE9D5FF, false);
            sPose.popMatrix();

            sPose.pushMatrix();
            sPose.translate(tx, subLineY);
            sPose.scale(0.85f, 0.85f);
            gui.text(this.font, fitText(moveLabel, (int) (maxW / 0.85f)),
                    0, 0, active ? 0xFFFDE68A : 0xFFC4B5FD, false);
            sPose.popMatrix();
        }
    }

    private void renderTabSwitcher(GuiGraphicsExtractor gui, int mouseX, int mouseY) {
        String[] tabKeys = {
                "jcraft.origin.tab.skills",
                "jcraft.origin.tab.studio",
                "jcraft.origin.tab.profiles"
        };
        for (int t = 0; t < Math.min(tabKeys.length, tabPills.size()); t++) {
            PillRect p = tabPills.get(t);
            boolean sel = (t == activeTab);
            boolean hov = p.contains(mouseX, mouseY);
            gui.fill(p.x(), p.y(), p.x() + p.w(), p.y() + p.h(),
                    sel ? 0xEE4C1D95 : (hov ? 0xDD271344 : 0xAA160C28));
            gui.outline(p.x(), p.y(), p.w(), p.h(),
                    sel ? 0xFFF0ABFC : (hov ? 0xFFC084FC : 0x667C3AED));
            String label = Component.translatable(tabKeys[t]).getString();
            if (t == TAB_PROFILES && cachedSynergies.activeCount() > 0) {
                label = label + " (" + cachedSynergies.activeCount() + ")";
            }
            drawScaledCenteredText(gui, label, p.x() + p.w() / 2.0f, p.y() + 3.5f, 0.76f, p.w() - 6,
                    sel ? 0xFFFDE68A : 0xFFE9D5FF);
        }
    }

    private void renderSkillsTab(GuiGraphicsExtractor gui, int mouseX, int mouseY, boolean freeBinds) {
        OriginSkillCatalog.SkillCategory[] cats = OriginSkillCatalog.SkillCategory.values();
        for (int i = 0; i < Math.min(cats.length, categoryPills.size()); i++) {
            PillRect p = categoryPills.get(i);
            OriginSkillCatalog.SkillCategory cat = cats[i];
            boolean sel = (cat == selectedCategory);
            boolean hov = p.contains(mouseX, mouseY);
            gui.fill(p.x(), p.y(), p.x() + p.w(), p.y() + p.h(),
                    sel ? 0xEE3B0764 : (hov ? 0xDD25133E : 0xBB140D24));
            gui.outline(p.x(), p.y(), p.w(), p.h(),
                    sel ? 0xFFE879F9 : (hov ? 0xFFA855F7 : 0x557C3AED));
            String cLabel = Component.translatable(cat.getTranslationKey()).getString();
            drawScaledCenteredText(gui, cLabel, p.x() + p.w() / 2.0f, p.y() + 3.0f, 0.70f, p.w() - 4,
                    sel ? 0xFFFDE68A : 0xFFD8B4FE);
        }

        List<OriginSkillCatalog.LoadoutPreset> presets = OriginSkillCatalog.getPresets();
        for (int i = 0; i < presetPills.size(); i++) {
            PillRect p = presetPills.get(i);
            boolean isReset = (i >= presets.size());
            boolean hov = p.contains(mouseX, mouseY);
            gui.fill(p.x(), p.y(), p.x() + p.w(), p.y() + p.h(),
                    hov ? (isReset ? 0xDD3F1522 : 0xEE2B1648) : (isReset ? 0xBB230D16 : 0xBB18102C));
            gui.outline(p.x(), p.y(), p.w(), p.h(),
                    hov ? (isReset ? 0xFFF87171 : 0xFFC084FC) : (isReset ? 0x77EF4444 : 0x669333EA));
            String pLabel = isReset
                    ? Component.translatable("jcraft.origin.arsenal.reset").getString()
                    : Component.translatable(presets.get(i).titleKey()).getString();
            drawScaledCenteredText(gui, pLabel, p.x() + p.w() / 2.0f, p.y() + 3.5f, 0.74f, p.w() - 4,
                    isReset ? 0xFFFCA5A5 : 0xFFE9D5FF);
        }

        this.hoveredSkill = null;
        String currentEquippedId = localLoadout.get(selectedSlot);

        gui.enableScissor(listX - 1, listY, listX + listW + 1, listBottom);
        if (filteredSkills.isEmpty()) {
            gui.centeredText(this.font, Component.translatable("jcraft.origin.arsenal.no_results"),
                    listX + listW / 2, listY + 24, 0xFF9CA3AF);
        } else {
            for (int i = 0; i < filteredSkills.size(); i++) {
                OriginSkillCatalog.SkillEntry entry = filteredSkills.get(i);
                int ry = listY + i * (ROW_H + ROW_GAP) - scrollPixels;
                if (ry + ROW_H < listY || ry > listBottom) {
                    continue;
                }
                boolean equipped = entry.id().equals(currentEquippedId);
                boolean rowHover = mouseX >= listX && mouseX <= listX + listW
                        && mouseY >= Math.max(listY, ry) && mouseY <= Math.min(listBottom, ry + ROW_H);
                if (rowHover) {
                    this.hoveredSkill = entry;
                }

                int bg = equipped ? 0xDD1A2E2A : (rowHover ? 0xEE2B1648 : 0xAA150E26);
                int border = equipped ? 0xFF34D399 : (rowHover ? 0xFFE879F9 : 0x557C3AED);
                gui.fill(listX, ry, listX + listW, ry + ROW_H, bg);
                gui.outline(listX, ry, listW, ROW_H, border);

                JCraftAbilityHud.renderIcon(gui, listX + 2, ry + 1, entry.standId(), entry.getIconName());

                AbstractMove<?, ?> move = entry.templateMove();
                String moveTitle = move.getName().getString();
                if (equipped) {
                    moveTitle = "✓ " + moveTitle;
                }
                String standTitle = entry.standType().getData().getInfo().getName().getString()
                        + " [" + entry.sourceSlot().getFriendlyName().getString() + "]";

                int rightPad = 6;
                float standScale = 0.80f;
                int standW = Math.round(this.font.width(standTitle) * standScale);
                var tPose = gui.pose();
                tPose.pushMatrix();
                tPose.translate(listX + listW - standW - rightPad, ry + 3);
                tPose.scale(standScale, standScale);
                gui.text(this.font, standTitle, 0, 0, equipped ? 0xFF6EE7B7 : 0xFFC084FC, false);
                tPose.popMatrix();

                int textLeft = listX + 26;
                int titleMaxW = Math.max(50, listW - 32 - standW - rightPad);
                gui.text(this.font, fitText(moveTitle, titleMaxW), textLeft, ry + 3,
                        equipped ? 0xFFA7F3D0 : 0xFFF3E8FF, true);

                String actionTag = Component.translatable(equipped
                        ? "jcraft.origin.arsenal.equipped_btn"
                        : "jcraft.origin.arsenal.equip_btn").getString();
                float actScale = 0.74f;
                int actW = Math.round(this.font.width(actionTag) * actScale) + 6;
                int actX = listX + listW - actW - 4;
                int actY = ry + 13;
                int pillFill = equipped ? 0xCC064E3B : (rowHover ? 0xEE581C87 : 0x99261342);
                int pillBorder = equipped ? 0xFF34D399 : (rowHover ? 0xFFE879F9 : 0x889333EA);
                int pillText = equipped ? 0xFFA7F3D0 : (rowHover ? 0xFFFDE68A : 0xFFC4B5FD);
                gui.fill(actX, actY, actX + actW, actY + 10, pillFill);
                gui.outline(actX, actY, actW, 10, pillBorder);
                drawScaledCenteredText(gui, actionTag, actX + actW / 2.0f, actY + 2.0f, actScale, actW - 2, pillText);

                String dmgStr = (move instanceof AbstractSimpleAttack<?, ?> atk)
                        ? String.format(Locale.ROOT, "%.1f", atk.getDamage())
                        : "-";
                String cdStr = String.format(Locale.ROOT, "%.1f", move.getCooldown() / 20.0f);
                String catStr = Component.translatable(entry.category().getTranslationKey()).getString();
                String stats = Component.translatable("jcraft.origin.arsenal.stats",
                        dmgStr, cdStr, move.getArmor(), catStr).getString();
                int statsMaxW = Math.max(40, listW - 32 - (actW + 8));
                tPose.pushMatrix();
                tPose.translate(textLeft, ry + 15);
                tPose.scale(0.82f, 0.82f);
                gui.text(this.font, fitText(stats, (int) (statsMaxW / 0.82f)), 0, 0, 0xFFA78BFA, false);
                tPose.popMatrix();
            }
        }
        gui.disableScissor();
        gui.fill(listX, listBottom + 1, listX + listW, listBottom + 2, 0x667C3AED);

        int maxScroll = getMaxScroll();
        if (maxScroll > 0) {
            int barH = listBottom - listY;
            int thumbH = Math.max(16, (int) (barH * (barH / (double) (barH + maxScroll))));
            int thumbY = listY + (int) ((barH - thumbH) * (scrollPixels / (double) maxScroll));
            gui.fill(listX + listW - 3, listY, listX + listW - 1, listBottom, 0x337C3AED);
            gui.fill(listX + listW - 3, thumbY, listX + listW - 1, thumbY + thumbH, 0xFFC084FC);
        }
    }

    private void renderStudioTab(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        // Left: Live 3D Rotating Preview Box
        int glowAccent = chromaEnabled ? (0xFF000000 | getCurrentPackedRgb()) : 0xFFC084FC;
        gui.fill(previewBoxX, previewBoxY, previewBoxX + previewBoxW, previewBoxY + previewBoxH, 0xDD070510);
        gui.outline(previewBoxX, previewBoxY, previewBoxW, previewBoxH, glowAccent);
        // Subtle floor pedestal ellipse highlight
        int pedY = previewBoxY + previewBoxH - 12;
        gui.fill(previewBoxX + 16, pedY, previewBoxX + previewBoxW - 16, pedY + 2, (glowAccent & 0x00FFFFFF) | 0x66000000);

        ensurePreviewEntity();
        if (previewOrigin != null) {
            int skin = getCurrentSkin();
            previewOrigin.setSkin(skin);
            previewOrigin.setCustomGlowRgb(chromaEnabled ? getCurrentPackedRgb() : -1);
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                previewOrigin.setPos(player.getX(), player.getY() + 1.0, player.getZ());
            }
            previewOrigin.yBodyRot = 180.0f;
            previewOrigin.yBodyRotO = 180.0f;
            previewOrigin.setYRot(180.0f);
            previewOrigin.yRotO = 180.0f;
            previewOrigin.yHeadRot = 180.0f;
            previewOrigin.yHeadRotO = 180.0f;
            previewOrigin.setXRot(0.0f);
            previewOrigin.xRotO = 0.0f;

            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            EntityRenderState state = dispatcher.extractEntity(previewOrigin, partialTick);
            state.shadowPieces.clear();
            state.outlineColor = 0;

            float scale = Mth.clamp(Math.min(previewBoxW * 0.25f, previewBoxH * 0.28f), 26.0f, 50.0f);
            Vector3f translation = new Vector3f(0.0f, previewOrigin.getBbHeight() * 0.56f + 0.14f, 0.0f);
            Quaternionf rot = new Quaternionf()
                    .rotateZ((float) Math.PI)
                    .rotateX((float) Math.toRadians(previewPitchDeg))
                    .rotateY((float) Math.toRadians(previewYawDeg));
            Quaternionf camAngle = new Quaternionf().rotateX((float) Math.toRadians(previewPitchDeg));
            gui.entity(state, scale, translation, rot, camAngle,
                    previewBoxX + 2, previewBoxY + 2,
                    previewBoxX + previewBoxW - 2, previewBoxY + previewBoxH - 2);
            rendered3DPreview = true;
        }

        drawScaledCenteredText(gui,
                Component.translatable("jcraft.origin.studio.drag_hint").getString(),
                previewBoxX + previewBoxW / 2.0f, previewBoxY + 4, 0.70f, previewBoxW - 8, 0xCCDDD6FE);

        // Skin arrows & studio controls below preview box
        if (studioPills.size() >= 4) {
            PillRect prevBtn = studioPills.get(0);
            PillRect nextBtn = studioPills.get(1);
            drawPillButton(gui, prevBtn, "◀", mouseX, mouseY, 0xEE2B1648, 0xFFA855F7, 0xFFF3E8FF);
            drawPillButton(gui, nextBtn, "▶", mouseX, mouseY, 0xEE2B1648, 0xFFA855F7, 0xFFF3E8FF);
            int labelX = prevBtn.x() + prevBtn.w() + 2;
            int labelW = Math.max(20, nextBtn.x() - labelX - 2);
            gui.fill(labelX, prevBtn.y(), labelX + labelW, prevBtn.y() + prevBtn.h(), 0xBB140D24);
            gui.outline(labelX, prevBtn.y(), labelW, prevBtn.h(), 0x777C3AED);
            drawScaledCenteredText(gui, getOriginSkinLabel(getCurrentSkin()),
                    labelX + labelW / 2.0f, prevBtn.y() + 3.5f, 0.74f, labelW - 4, 0xFFFDE68A);

            PillRect spinBtn = studioPills.get(2);
            PillRect summonBtn = studioPills.get(3);
            String spinText = (autoSpin ? "⟳ " : "⏸ ") + Component.translatable("jcraft.origin.studio.spin").getString();
            drawPillButton(gui, spinBtn, spinText, mouseX, mouseY,
                    autoSpin ? 0xEE3B0764 : 0xBB18102C, autoSpin ? 0xFFE879F9 : 0x667C3AED,
                    autoSpin ? 0xFFFDE68A : 0xFFC4B5FD);
            drawPillButton(gui, summonBtn, "✦ " + Component.translatable("jcraft.origin.studio.summon").getString(),
                    mouseX, mouseY, 0xEE271546, 0xFFC084FC, 0xFFE9D5FF);
        }

        // Right: Chroma RGB Color Forge
        int rightPaneX = previewBoxX + previewBoxW + 8;
        int rightPaneW = Math.max(88, (this.listX + this.listW) - rightPaneX);
        var pPose = gui.pose();
        pPose.pushMatrix();
        pPose.translate(rightPaneX, this.contentTop + 1);
        pPose.scale(0.84f, 0.84f);
        gui.text(this.font, Component.translatable("jcraft.origin.chroma.title"), 0, 0, 0xFFE9D5FF, false);
        pPose.popMatrix();

        // Swatch preview + toggle pill (index 4 in studioPills)
        if (studioPills.size() >= 5) {
            PillRect togglePill = studioPills.get(4);
            int previewHex = chromaEnabled ? (0xFF000000 | getCurrentPackedRgb()) : 0xFF4B5563;
            String toggleLabel = chromaEnabled
                    ? Component.translatable("jcraft.origin.chroma.enabled").getString()
                            + " #" + String.format(Locale.ROOT, "%06X", getCurrentPackedRgb())
                    : Component.translatable("jcraft.origin.chroma.disabled").getString();
            drawPillButton(gui, togglePill, toggleLabel, mouseX, mouseY,
                    chromaEnabled ? 0xEE18302A : 0xBB160E28,
                    chromaEnabled ? 0xFF34D399 : 0x889333EA,
                    chromaEnabled ? 0xFFA7F3D0 : 0xFFD8B4FE);
            gui.fill(togglePill.x() + 3, togglePill.y() + 3, togglePill.x() + 11, togglePill.y() + 11, previewHex);
            gui.outline(togglePill.x() + 3, togglePill.y() + 3, 8, 8, 0xFFFFFFFF);
        }

        // R / G / B Gradient Bars
        String[] chLabels = {"R", "G", "B"};
        int[] chVals = {chromaR, chromaG, chromaB};
        for (int i = 0; i < chromaBars.size(); i++) {
            ChromaBar bar = chromaBars.get(i);
            gui.text(this.font, chLabels[i], rightPaneX + 2, bar.y() + 1,
                    i == 0 ? 0xFFFCA5A5 : (i == 1 ? 0xFF6EE7B7 : 0xFF93C5FD), false);

            int segs = 16;
            for (int s = 0; s < segs; s++) {
                int sx0 = bar.x() + (s * bar.w()) / segs;
                int sx1 = bar.x() + ((s + 1) * bar.w()) / segs;
                int c = Math.round(255.0f * (s + 0.5f) / segs);
                int segColor = switch (i) {
                    case 0 -> 0xFF000000 | (c << 16) | ((chromaG / 3) << 8) | (chromaB / 3);
                    case 1 -> 0xFF000000 | ((chromaR / 3) << 16) | (c << 8) | (chromaB / 3);
                    default -> 0xFF000000 | ((chromaR / 3) << 16) | ((chromaG / 3) << 8) | c;
                };
                gui.fill(sx0, bar.y() + 1, sx1, bar.y() + bar.h() - 1, segColor);
            }
            gui.outline(bar.x(), bar.y(), bar.w(), bar.h(), 0xAA9333EA);

            int knobX = bar.x() + Math.round((chVals[i] / 255.0f) * Math.max(1, bar.w() - 4));
            gui.fill(knobX, bar.y() - 1, knobX + 4, bar.y() + bar.h() + 1, 0xFFFDE68A);
            gui.outline(knobX, bar.y() - 1, 4, bar.h() + 2, 0xFF1E1B4B);

            pPose.pushMatrix();
            pPose.translate(bar.x() + bar.w() + 4, bar.y() + 1.5f);
            pPose.scale(0.80f, 0.80f);
            gui.text(this.font, String.format(Locale.ROOT, "%3d", chVals[i]), 0, 0, 0xFFE9D5FF, false);
            pPose.popMatrix();
        }

        if (!chromaBars.isEmpty()) {
            int swatchLabelY = chromaBars.get(chromaBars.size() - 1).y() + 14;
            pPose.pushMatrix();
            pPose.translate(rightPaneX, swatchLabelY);
            pPose.scale(0.76f, 0.76f);
            gui.text(this.font, Component.translatable("jcraft.origin.chroma.swatches"), 0, 0, 0xFFC4B5FD, false);
            pPose.popMatrix();
        }

        // Preset color swatches (studioPills indices 5 .. 5 + CHROMA_SWATCHES.length - 1)
        for (int i = 0; i < CHROMA_SWATCHES.length; i++) {
            int pIdx = 5 + i;
            if (pIdx >= studioPills.size()) break;
            PillRect sw = studioPills.get(pIdx);
            int rgb = CHROMA_SWATCHES[i];
            boolean sel = chromaEnabled && getCurrentPackedRgb() == rgb;
            boolean hov = sw.contains(mouseX, mouseY);
            gui.fill(sw.x(), sw.y(), sw.x() + sw.w(), sw.y() + sw.h(), 0xFF000000 | rgb);
            gui.outline(sw.x(), sw.y(), sw.w(), sw.h(), sel ? 0xFFFFFFFF : (hov ? 0xFFFDE68A : 0x883B0764));
        }

        // Reset Chroma button
        int resetIdx = 5 + CHROMA_SWATCHES.length;
        if (resetIdx < studioPills.size()) {
            drawPillButton(gui, studioPills.get(resetIdx),
                    Component.translatable("jcraft.origin.chroma.reset").getString(),
                    mouseX, mouseY, 0xBB230D16, 0x88EF4444, 0xFFFCA5A5);
        }
    }

    private void renderProfilesAndSynergiesTab(GuiGraphicsExtractor gui, int mouseX, int mouseY) {
        var pPose = gui.pose();
        pPose.pushMatrix();
        pPose.translate(this.listX, this.contentTop);
        pPose.scale(0.80f, 0.80f);
        gui.text(this.font, Component.translatable("jcraft.origin.profiles.title"), 0, 0, 0xFFE9D5FF, false);
        pPose.popMatrix();

        LocalPlayer player = Minecraft.getInstance().player;
        CommonStandComponent comp = player != null ? JComponentPlatformUtils.getStandComponent(player) : null;

        int cardsTop = this.contentTop + 11;
        int totalH = Math.max(115, this.contentBottom - cardsTop);
        int profilesSectionH = Math.max(60, Math.min(96, (int) (totalH * 0.43f)));
        int cardGap = 3;
        int cardW = (this.listW - cardGap) / 2;
        int cardH = Math.max(28, (profilesSectionH - cardGap) / 2);

        for (int i = 0; i < OriginProfile.MAX_PROFILES; i++) {
            int col = i % 2;
            int row = i / 2;
            int cx = this.listX + col * (cardW + cardGap);
            int cy = cardsTop + row * (cardH + cardGap);
            OriginProfile saved = comp != null ? comp.getOriginProfile(i) : null;
            boolean hasData = saved != null;

            gui.fill(cx, cy, cx + cardW, cy + cardH, hasData ? 0xDD17112B : 0x99100B1C);
            gui.outline(cx, cy, cardW, cardH, hasData ? 0xFFA855F7 : 0x555B21B6);

            String headerTitle = (i + 1) + ". " + (hasData ? saved.name() : profileDraftNames[i]);
            pPose.pushMatrix();
            pPose.translate(cx + 4, cy + 2.5f);
            pPose.scale(0.76f, 0.76f);
            gui.text(this.font, fitText(headerTitle, (int) ((cardW - 22) / 0.76f)), 0, 0,
                    hasData ? 0xFFFDE68A : 0xFFA78BFA, false);
            pPose.popMatrix();

            // Subline: Skin + Chroma swatch (or Empty Slot)
            int subY = cy + (cardH >= 32 ? 12 : 10);
            if (hasData) {
                int dotColor = saved.customGlowRgb() >= 0 ? (0xFF000000 | saved.customGlowRgb()) : 0xFF9333EA;
                gui.fill(cx + 4, subY, cx + 9, subY + 5, dotColor);
                gui.outline(cx + 4, subY, 5, 5, 0xCCFFFFFF);
                String meta = getOriginSkinLabel(saved.skin())
                        + (saved.customGlowRgb() >= 0 ? String.format(Locale.ROOT, " • #%06X", saved.customGlowRgb()) : "");
                pPose.pushMatrix();
                pPose.translate(cx + 12, subY);
                pPose.scale(0.68f, 0.68f);
                gui.text(this.font, fitText(meta, (int) ((cardW - 18) / 0.68f)), 0, 0, 0xFFD8B4FE, false);
                pPose.popMatrix();
            } else {
                pPose.pushMatrix();
                pPose.translate(cx + 4, subY);
                pPose.scale(0.68f, 0.68f);
                gui.text(this.font, fitText(Component.translatable("jcraft.origin.profile.empty").getString(),
                        (int) ((cardW - 10) / 0.68f)), 0, 0, 0xFF9CA3AF, false);
                pPose.popMatrix();
            }

            int pillBase = i * 4;
            if (pillBase + 3 < profilePills.size()) {
                drawPillButton(gui, profilePills.get(pillBase), "✎", mouseX, mouseY, 0xAA251342, 0x889333EA, 0xFFD8B4FE);
                drawPillButton(gui, profilePills.get(pillBase + 1),
                        Component.translatable("jcraft.origin.profile.load").getString(),
                        mouseX, mouseY, hasData ? 0xEE183830 : 0x66161420,
                        hasData ? 0xFF34D399 : 0x444B5563, hasData ? 0xFFA7F3D0 : 0xFF6B7280);
                drawPillButton(gui, profilePills.get(pillBase + 2),
                        Component.translatable("jcraft.origin.profile.save").getString(),
                        mouseX, mouseY, 0xEE2B1648, 0xFFC084FC, 0xFFE9D5FF);
                drawPillButton(gui, profilePills.get(pillBase + 3), "✕",
                        mouseX, mouseY, hasData ? 0xCC3B1320 : 0x551A121C,
                        hasData ? 0xFFF87171 : 0x444B5563, hasData ? 0xFFFCA5A5 : 0xFF6B7280);
            }
        }

        // Bottom half: Passive Synergies (guaranteed to fit above contentBottom even at 427x240)
        int actualCardsEnd = cardsTop + 2 * cardH + cardGap;
        int synHeaderY = actualCardsEnd + 3;
        pPose.pushMatrix();
        pPose.translate(this.listX, synHeaderY);
        pPose.scale(0.76f, 0.76f);
        gui.text(this.font, Component.translatable("jcraft.origin.synergies.title"), 0, 0, 0xFFE9D5FF, false);
        pPose.popMatrix();

        int synStartY = synHeaderY + 10;
        List<OriginSynergy.SynergyStatus> statuses = cachedSynergies.statuses();
        int availSynH = Math.max(55, this.contentBottom - synStartY);
        int synPitch = Math.max(11, Math.min(24, availSynH / Math.max(1, statuses.size())));
        int synH = Math.max(9, synPitch - 2);
        for (int i = 0; i < statuses.size(); i++) {
            OriginSynergy.SynergyStatus st = statuses.get(i);
            int sy = synStartY + i * synPitch;
            boolean act = st.active();
            gui.fill(this.listX, sy, this.listX + this.listW, sy + synH, act ? 0xDD132B24 : 0x99120D22);
            gui.outline(this.listX, sy, this.listW, synH, act ? 0xFF34D399 : 0x556D28D9);

            String title = Component.translatable(st.type().getTitleKey()).getString();
            String desc = Component.translatable(st.type().getDescKey()).getString();
            String progress = act
                    ? Component.translatable("jcraft.origin.synergy.active").getString() + " (" + st.count() + "/" + st.required() + ")"
                    : (st.count() + "/" + st.required());

            float rowScale = synH >= 15 ? 0.74f : 0.68f;
            float rowTextY = sy + Math.max(1.5f, (synH - 7.0f * rowScale) * 0.5f);
            if (synH >= 18) {
                rowTextY = sy + 2.5f;
            }

            int progW = Math.round(this.font.width(progress) * rowScale);
            pPose.pushMatrix();
            pPose.translate(this.listX + this.listW - progW - 5, rowTextY);
            pPose.scale(rowScale, rowScale);
            gui.text(this.font, progress, 0, 0, act ? 0xFF34D399 : 0xFFA78BFA, false);
            pPose.popMatrix();

            pPose.pushMatrix();
            pPose.translate(this.listX + 5, rowTextY);
            pPose.scale(rowScale, rowScale);
            gui.text(this.font, fitText(title, (int) (Math.max(40, (this.listW - progW - 14) * 0.42f) / rowScale)),
                    0, 0, act ? 0xFFA7F3D0 : 0xFFE9D5FF, false);
            pPose.popMatrix();

            if (synH >= 18) {
                pPose.pushMatrix();
                pPose.translate(this.listX + 5, sy + 11.0f);
                pPose.scale(0.70f, 0.70f);
                gui.text(this.font, fitText(desc, (int) ((this.listW - 12) / 0.70f)),
                        0, 0, act ? 0xFF6EE7B7 : 0xFFC4B5FD, false);
                pPose.popMatrix();
            } else {
                int descX = this.listX + Math.max(76, (int) (this.listW * 0.38f));
                int descMaxW = Math.max(30, this.listX + this.listW - progW - 10 - descX);
                pPose.pushMatrix();
                pPose.translate(descX, rowTextY);
                pPose.scale(rowScale - 0.04f, rowScale - 0.04f);
                gui.text(this.font, fitText(desc, (int) (descMaxW / (rowScale - 0.04f))),
                        0, 0, act ? 0xFF6EE7B7 : 0xFFC4B5FD, false);
                pPose.popMatrix();
            }
        }
    }

    private void renderFooter(GuiGraphicsExtractor gui, int mouseX, int mouseY, boolean freeBinds) {
        int descY = this.panelTop + this.panelH - 32;
        boolean hasToast = statusMessage != null && System.currentTimeMillis() < statusUntilMs;
        var dPose = gui.pose();
        if (hasToast) {
            dPose.pushMatrix();
            dPose.translate(listX, descY);
            dPose.scale(0.84f, 0.84f);
            gui.text(this.font, fitText(statusMessage, (int) ((listW - 4) / 0.84f)), 0, 0, 0xFF34D399, true);
            dPose.popMatrix();
        } else if (activeTab == TAB_SKILLS) {
            String currentEquippedId = localLoadout.get(selectedSlot);
            OriginSkillCatalog.SkillEntry preview = hoveredSkill != null
                    ? hoveredSkill
                    : OriginSkillCatalog.resolveForSlot(selectedSlot, currentEquippedId, freeBinds);
            if (preview != null) {
                List<FormattedCharSequence> lines = this.font.split(preview.templateMove().getDescription(), Math.max(80, listW - 6));
                if (!lines.isEmpty()) {
                    dPose.pushMatrix();
                    dPose.translate(listX, descY);
                    dPose.scale(0.82f, 0.82f);
                    gui.text(this.font, lines.get(0), 0, 0, 0xFFD8B4FE, false);
                    dPose.popMatrix();
                }
            }
        } else {
            String synLine = Component.translatable("jcraft.origin.synergies.badge", cachedSynergies.activeCount()).getString();
            dPose.pushMatrix();
            dPose.translate(listX, descY);
            dPose.scale(0.82f, 0.82f);
            gui.text(this.font, synLine, 0, 0, cachedSynergies.activeCount() > 0 ? 0xFF34D399 : 0xFFA78BFA, false);
            dPose.popMatrix();
        }

        // Bottom-left footer badge
        if (footerSynergyBadge != null) {
            String badgeText = activeTab == TAB_SKILLS
                    ? Component.translatable("jcraft.origin.arsenal.catalog", filteredSkills.size()).getString()
                    + (cachedSynergies.activeCount() > 0 ? " • ✦" + cachedSynergies.activeCount() : "")
                    : Component.translatable("jcraft.origin.synergies.badge", cachedSynergies.activeCount()).getString();
            boolean hov = footerSynergyBadge.contains(mouseX, mouseY);
            int synCount = cachedSynergies.activeCount();
            gui.fill(footerSynergyBadge.x(), footerSynergyBadge.y(),
                    footerSynergyBadge.x() + footerSynergyBadge.w(), footerSynergyBadge.y() + footerSynergyBadge.h(),
                    synCount > 0 ? (hov ? 0xEE183C32 : 0xBB102A23) : (hov ? 0xDD24133E : 0x99140D24));
            gui.outline(footerSynergyBadge.x(), footerSynergyBadge.y(), footerSynergyBadge.w(), footerSynergyBadge.h(),
                    synCount > 0 ? 0xFF34D399 : 0x667C3AED);
            drawScaledCenteredText(gui, badgeText,
                    footerSynergyBadge.x() + footerSynergyBadge.w() / 2.0f,
                    footerSynergyBadge.y() + 4.0f, 0.74f, footerSynergyBadge.w() - 6,
                    synCount > 0 ? 0xFFA7F3D0 : 0xFFC4B5FD);
        }
    }

    private void drawPillButton(GuiGraphicsExtractor gui, PillRect pill, String text,
                                int mouseX, int mouseY, int bg, int border, int textColor) {
        boolean hov = pill.contains(mouseX, mouseY);
        gui.fill(pill.x(), pill.y(), pill.x() + pill.w(), pill.y() + pill.h(), hov ? (bg | 0x222222) : bg);
        gui.outline(pill.x(), pill.y(), pill.w(), pill.h(), hov ? 0xFFFDE68A : border);
        drawScaledCenteredText(gui, text, pill.x() + pill.w() / 2.0f, pill.y() + (pill.h() - 6) * 0.5f,
                0.74f, Math.max(8, pill.w() - 4), textColor);
    }

    private void drawScaledCenteredText(GuiGraphicsExtractor gui, String text, float centerX, float y,
                                        float scale, int maxUnscaledW, int color) {
        String fitted = fitText(text, (int) (maxUnscaledW / scale));
        float textW = this.font.width(fitted) * scale;
        var pose = gui.pose();
        pose.pushMatrix();
        pose.translate(centerX - textW * 0.5f, y);
        pose.scale(scale, scale);
        gui.text(this.font, fitted, 0, 0, color, false);
        pose.popMatrix();
    }

    private String fitText(String text, int maxW) {
        if (this.font.width(text) <= maxW) {
            return text;
        }
        return this.font.plainSubstrByWidth(text, Math.max(8, maxW - 8)) + "...";
    }

    // Coordinates & helpers for automated UI verification in ClientVisualDiagnostic
    public double getLeftSlotCenterX() {
        return leftColX + leftColW * 0.5;
    }

    public double getLeftSlotCenterY(MoveClass slot) {
        return leftRowStartY + slot.ordinal() * slotPitch + slotH * 0.5;
    }

    public double getFirstNonOriginSkillCenterX() {
        return listX + listW * 0.5;
    }

    public double getSkillRowCenterY(int visibleRowIndex) {
        return listY + visibleRowIndex * (ROW_H + ROW_GAP) + ROW_H * 0.5;
    }

    public int findFirstSkillIndexFromStand(String standIdPrefix) {
        for (int i = 0; i < filteredSkills.size(); i++) {
            if (filteredSkills.get(i).id().startsWith(standIdPrefix)) {
                return i;
            }
        }
        return -1;
    }

    public MoveClass getSelectedSlot() {
        return selectedSlot;
    }

    public int getRowPitch() {
        return ROW_H + ROW_GAP;
    }

    public int getScrollPixels() {
        return scrollPixels;
    }

    public boolean hasRendered3DPreview() {
        return rendered3DPreview;
    }

    public void applyChromaForTest(int rgb) {
        applyRgbFields(rgb);
        chromaEnabled = rgb >= 0;
        commitChromaRgb(rgb);
    }

    public void saveProfileForTest(int slotIdx, String name) {
        if (slotIdx >= 0 && slotIdx < OriginProfile.MAX_PROFILES) {
            profileDraftNames[slotIdx] = OriginProfile.sanitizeName(name, OriginProfile.defaultNameForSlot(slotIdx));
            saveProfile(slotIdx);
        }
    }

    public void loadProfileForTest(int slotIdx) {
        loadProfile(slotIdx);
    }
}
