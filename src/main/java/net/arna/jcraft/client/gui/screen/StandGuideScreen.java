package net.arna.jcraft.client.gui.screen;

import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.IAttackerType;
import net.arna.jcraft.api.attack.MoveMap;
import net.arna.jcraft.api.attack.MoveSet;
import net.arna.jcraft.api.attack.MoveSetManager;
import net.arna.jcraft.api.attack.enums.MoveClass;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.attack.moves.AbstractSimpleAttack;
import net.arna.jcraft.api.component.living.CommonStandComponent;
import net.arna.jcraft.api.component.player.CommonSpecComponent;
import net.arna.jcraft.api.spec.JSpec;
import net.arna.jcraft.api.spec.SpecData;
import net.arna.jcraft.api.spec.SpecType;
import net.arna.jcraft.api.spec.SpecTypeUtil;
import net.arna.jcraft.api.stand.StandData;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.api.stand.StandInfo;
import net.arna.jcraft.api.stand.StandType;
import net.arna.jcraft.api.stand.StandTypeUtil;
import net.arna.jcraft.client.JCraftClient;
import net.arna.jcraft.client.gui.hud.JCraftAbilityHud;
import net.arna.jcraft.common.util.CooldownType;
import net.arna.jcraft.common.util.EvolutionItemHandler;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Interactive 4-tab JCraft Gameplay & Stand Guide Screen (Moveset, Evolutions, Combat, Survival Progression).
 * Accessible from the Y Radial Wheel center medallion and the JCraft Config Screen.
 */
public final class StandGuideScreen extends Screen {
    private static final String[] TAB_KEYS = {
            "jcraft.guide.tab.moveset",
            "jcraft.guide.tab.evolutions",
            "jcraft.guide.tab.combat",
            "jcraft.guide.tab.progression"
    };

    private static final int SCROLL_STEP = 26;

    @Nullable
    private final Screen parent;
    private final List<GuideCard> cards = new ArrayList<>();

    private int activeTab = 0;
    private int scrollPixels = 0;
    private int totalContentHeight = 0;

    private int panelLeft;
    private int panelTop;
    private int panelW;
    private int panelH;
    private int bodyTop;
    private int bodyBottom;

    private record GuideCard(
            String title,
            int titleColor,
            @Nullable String badge,
            int badgeColor,
            @Nullable String stats,
            List<FormattedCharSequence> bodyLines,
            int borderColor,
            int bgColor,
            @Nullable Identifier iconOwner,
            @Nullable String iconName,
            @Nullable ItemStack itemIcon,
            int height
    ) {}

    public StandGuideScreen(@Nullable Screen parent) {
        super(Component.translatable("jcraft.guide.title"));
        this.parent = parent;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.panelW = Math.min(580, this.width - 10);
        this.panelH = Math.min(264, this.height - 10);
        this.panelLeft = (this.width - this.panelW) / 2;
        this.panelTop = (this.height - this.panelH) / 2;

        int tabCount = TAB_KEYS.length;
        int tabGap = 4;
        int tabW = (this.panelW - 16 - tabGap * (tabCount - 1)) / tabCount;
        int tabY = this.panelTop + 18;
        for (int t = 0; t < tabCount; t++) {
            final int idx = t;
            String label = Component.translatable(TAB_KEYS[t]).getString();
            if (idx == activeTab) {
                label = "§e" + label;
            }
            this.addRenderableWidget(Button.builder(Component.literal(label), b -> switchTab(idx))
                    .bounds(this.panelLeft + 8 + t * (tabW + tabGap), tabY, tabW, 16)
                    .build());
        }

        this.bodyTop = this.panelTop + 38;
        int footerY = this.panelTop + this.panelH - 22;
        this.bodyBottom = footerY - 4;

        rebuildCards();
        int maxScroll = getMaxScroll();
        this.scrollPixels = Mth.clamp(this.scrollPixels, 0, maxScroll);

        Button upBtn = Button.builder(Component.literal("▲"), b -> {
            scrollPixels = Math.max(0, scrollPixels - SCROLL_STEP * 3);
            this.init();
        }).bounds(this.panelLeft + 8, footerY, 26, 18).build();
        upBtn.active = scrollPixels > 0;
        this.addRenderableWidget(upBtn);

        Button downBtn = Button.builder(Component.literal("▼"), b -> {
            scrollPixels = Math.min(getMaxScroll(), scrollPixels + SCROLL_STEP * 3);
            this.init();
        }).bounds(this.panelLeft + 36, footerY, 26, 18).build();
        downBtn.active = scrollPixels < maxScroll;
        this.addRenderableWidget(downBtn);

        int btnW = 108;
        int centerStart = this.panelLeft + (this.panelW - (btnW * 2 + 6)) / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("jcraft.guide.open_config"),
                        b -> Minecraft.getInstance().gui.setScreen(new JCraftConfigScreen(this)))
                .bounds(centerStart, footerY, btnW, 18)
                .build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(centerStart + btnW + 6, footerY, btnW, 18)
                .build());
    }

    private void switchTab(int tab) {
        this.activeTab = Mth.clamp(tab, 0, TAB_KEYS.length - 1);
        this.scrollPixels = 0;
        this.init();
    }

    private int getViewportHeight() {
        return Math.max(40, this.bodyBottom - this.bodyTop);
    }

    private int getMaxScroll() {
        return Math.max(0, this.totalContentHeight - getViewportHeight());
    }

    private void rebuildCards() {
        this.cards.clear();
        switch (this.activeTab) {
            case 0 -> buildMovesetCards();
            case 1 -> buildEvolutionCards();
            case 2 -> buildCombatCards();
            case 3 -> buildProgressionCards();
        }
        int h = 0;
        for (int i = 0; i < cards.size(); i++) {
            h += cards.get(i).height();
            if (i + 1 < cards.size()) {
                h += 4;
            }
        }
        this.totalContentHeight = h;
    }

    private void buildMovesetCards() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        CommonStandComponent standComp = JComponentPlatformUtils.getStandComponent(player);
        StandType standType = standComp.getType();
        StandEntity<?, ?> activeStand = JUtils.getStand(player);

        CommonSpecComponent specComp = JComponentPlatformUtils.getSpecData(player);
        SpecType specType = specComp.getType();
        JSpec<?, ?> activeSpec = JUtils.getSpec(player);

        boolean hasStand = standType != null && !StandTypeUtil.isNone(standType);
        boolean hasSpec = specType != null && !SpecTypeUtil.isNone(specType);

        if (!hasStand && !hasSpec) {
            addTextCard(
                    Component.translatable("jcraft.guide.no_stand.title").getString(),
                    0xFFFDE047,
                    Component.translatable("jcraft.guide.no_stand.badge").getString(),
                    0xFF38BDF8,
                    null,
                    Component.translatable("jcraft.guide.no_stand.body"),
                    0xFFFACC15,
                    0xAA1F1633,
                    null,
                    null,
                    new ItemStack(Items.NETHER_STAR)
            );
            return;
        }

        if (hasStand) {
            StandData data = standType.getData();
            StandInfo info = data.getInfo();
            int modeOrd = activeStand != null ? activeStand.getModeOrdinal() : 0;
            String descKey = String.format("%s%s.info.desc", info.getNameKey(), modeOrd == 0 ? "" : Integer.toString(modeOrd));

            StringBuilder summary = new StringBuilder();
            if (Language.getInstance().has(descKey)) {
                summary.append(Component.translatable(descKey).getString());
            }
            for (int p = 1; p <= info.getProCount(); p++) {
                String k = String.format("%s.info.pro%d", info.getNameKey(), p);
                if (Language.getInstance().has(k)) {
                    if (!summary.isEmpty()) summary.append("\n");
                    summary.append("§b+ ").append(Component.translatable(k).getString());
                }
            }
            for (int c = 1; c <= info.getConCount(); c++) {
                String k = String.format("%s.info.con%d", info.getNameKey(), c);
                if (Language.getInstance().has(k)) {
                    if (!summary.isEmpty()) summary.append("\n");
                    summary.append("§c- ").append(Component.translatable(k).getString());
                }
            }
            if (info.getFreeSpace().getContents() != PlainTextContents.EMPTY) {
                String fs = info.getFreeSpace().getString().trim();
                if (!fs.isEmpty()) {
                    if (!summary.isEmpty()) summary.append("\n");
                    summary.append("§7").append(fs);
                }
            }

            String summonKey = JCraftClient.generateName(JCraftClient.STAND_SUMMON_KEY.getParent(), false);
            String stateBadge = activeStand != null
                    ? Component.translatable("jcraft.guide.stand.active").getString()
                    : Component.translatable("jcraft.guide.stand.summon_with", summonKey).getString();

            addTextCard(
                    "★ " + info.getName().getString(),
                    0xFFFDE047,
                    stateBadge,
                    activeStand != null ? 0xFF34D399 : 0xFF93C5FD,
                    null,
                    Component.literal(summary.isEmpty()
                            ? Component.translatable("jcraft.guide.stand.ready_desc").getString()
                            : summary.toString()),
                    0xFFD4AF37,
                    0xCC211638,
                    standType.getId(),
                    "ult",
                    null
            );

            MoveMap<?, ?> standMoves = activeStand != null ? activeStand.getMoveMap() : resolveDefaultMoveMap(standType);
            if (standMoves != null) {
                appendMoveEntries(standMoves, standType.getId(), false);
            }
        }

        if (hasSpec) {
            SpecData sData = specType.getData();
            StringBuilder specSummary = new StringBuilder();
            String d = sData.getDescription().getString().trim();
            if (!d.isEmpty()) {
                specSummary.append(d);
            }
            String det = sData.getDetails().getString().trim();
            if (!det.isEmpty()) {
                if (!specSummary.isEmpty()) specSummary.append("\n");
                specSummary.append("§7").append(det);
            }
            addTextCard(
                    "✦ Spec: " + sData.getName().getString(),
                    0xFF67E8F9,
                    Component.translatable("jcraft.guide.spec.badge").getString(),
                    0xFF38BDF8,
                    null,
                    Component.literal(specSummary.toString()),
                    0xFF0EA5E9,
                    0xBB142033,
                    specType.getId(),
                    "special1",
                    null
            );

            MoveMap<?, ?> specMoves = activeSpec != null ? activeSpec.getMoveMap() : resolveDefaultMoveMap(specType);
            if (specMoves != null) {
                appendMoveEntries(specMoves, specType.getId(), true);
            }
        }
    }

    @Nullable
    private static MoveMap<?, ?> resolveDefaultMoveMap(IAttackerType type) {
        MoveSet<?, ?> def = MoveSetManager.get(type, "default");
        if (def != null) {
            return def.getMoveMap();
        }
        Map<String, ? extends MoveSet<?, ?>> all = MoveSetManager.get(type);
        for (MoveSet<?, ?> ms : all.values()) {
            return ms.getMoveMap();
        }
        return null;
    }

    private void appendMoveEntries(MoveMap<?, ?> moveMap, Identifier iconOwner, boolean isSpec) {
        for (MoveClass mc : MoveClass.values()) {
            for (MoveMap.Entry<?, ?> entry : moveMap.getEntries(mc)) {
                addMoveCard(entry, iconOwner, isSpec, null, 0xFFE9D5FF);
                if (entry.getCrouchingVariant() != null) {
                    addMoveCard(entry.getCrouchingVariant(), iconOwner, isSpec,
                            Component.translatable("jcraft.guide.variant.crouch").getString(), 0xFF38BDF8);
                }
                if (entry.getAerialVariant() != null) {
                    addMoveCard(entry.getAerialVariant(), iconOwner, isSpec,
                            Component.translatable("jcraft.guide.variant.aerial").getString(), 0xFFFBBF24);
                }
                if (entry.getFollowup() != null) {
                    addMoveCard(entry.getFollowup(), iconOwner, isSpec,
                            Component.translatable("jcraft.guide.variant.followup").getString(), 0xFFC084FC);
                }
            }
        }
    }

    private void addMoveCard(MoveMap.Entry<?, ?> entry, Identifier iconOwner, boolean isSpec,
                             @Nullable String variantLabel, int variantColor) {
        AbstractMove<?, ?> move = entry.getMove();
        CooldownType cdType = entry.getCooldownType() != null
                ? entry.getCooldownType()
                : entry.getMoveClass().getDefaultCooldownType();
        String keyStr = JCraftAbilityHud.cooldownTypeToKeybind(cdType, false);
        String classLabel = entry.getMoveClass().getFriendlyName().getString();
        String moveName = move.getName().getString().trim();
        if (moveName.isEmpty()) {
            moveName = classLabel;
        }

        String prefix = variantLabel != null ? "↳ " : "";
        String title = prefix + moveName + " §7(" + classLabel + ")";
        String badge = variantLabel != null ? ("[" + keyStr + " + " + variantLabel + "]") : ("[" + keyStr + "]");

        StringBuilder stats = new StringBuilder();
        if (move.getCooldown() > 0) {
            stats.append(String.format(Locale.ROOT, "CD %.1fs", move.getCooldown() / 20.0f));
        }
        if (move instanceof AbstractSimpleAttack<?, ?> atk && atk.getDamage() > 0f) {
            if (!stats.isEmpty()) stats.append(" • ");
            stats.append(String.format(Locale.ROOT, "DMG %.1f", atk.getDamage()));
            if (atk.getStun() > 0) {
                stats.append(String.format(Locale.ROOT, " • STUN %dt", atk.getStun()));
            }
        }

        String iconName = moveClassToIconName(entry.getMoveClass());
        ItemStack tossFallback = entry.getMoveClass() == MoveClass.TOSS ? new ItemStack(Items.IRON_SWORD) : null;

        String rawDesc = move.getDescription().getString().trim();
        Component descComp = rawDesc.isEmpty()
                ? Component.translatable("jcraft.guide.move.no_desc")
                : move.getDescription();

        addTextCard(
                title,
                variantLabel != null ? variantColor : (isSpec ? 0xFFA5F3FC : 0xFFFDE68A),
                badge,
                variantLabel != null ? variantColor : 0xFF34D399,
                stats.isEmpty() ? null : stats.toString(),
                descComp,
                variantLabel != null ? 0x886366F1 : 0x779333EA,
                variantLabel != null ? 0x77131122 : 0x991A132B,
                tossFallback == null ? iconOwner : null,
                tossFallback == null ? iconName : null,
                tossFallback
        );
    }

    private static String moveClassToIconName(MoveClass mc) {
        return switch (mc) {
            case LIGHT -> "light";
            case HEAVY -> "heavy";
            case BARRAGE -> "barrage";
            case SPECIAL1 -> "special1";
            case SPECIAL2 -> "special2";
            case SPECIAL3 -> "special3";
            case ULTIMATE -> "ult";
            case UTILITY, TOSS -> "utility";
        };
    }

    private void buildEvolutionCards() {
        LocalPlayer player = Minecraft.getInstance().player;
        StandType currentStand = player != null ? JComponentPlatformUtils.getStandComponent(player).getType() : null;

        addTextCard(
                Component.translatable("jcraft.guide.evo.header.title").getString(),
                0xFFFDE047,
                null,
                0,
                null,
                Component.translatable("jcraft.guide.evo.header.body"),
                0xFF9333EA,
                0xAA1D1430,
                null,
                null,
                null
        );

        List<EvolutionItemHandler.Evolution> evos = new ArrayList<>(EvolutionItemHandler.getEvolutions());
        evos.sort(Comparator.comparing((EvolutionItemHandler.Evolution e) -> e.stand() != currentStand));

        for (EvolutionItemHandler.Evolution evo : evos) {
            boolean matchesCurrent = currentStand != null && !StandTypeUtil.isNone(currentStand) && evo.stand() == currentStand;
            String fromName = evo.stand() != null
                    ? evo.stand().getData().getInfo().getName().getString()
                    : Component.translatable("jcraft.guide.evo.any_stand").getString();
            String toName = evo.target().getData().getInfo().getName().getString();
            ItemStack itemStack = new ItemStack(evo.item());
            String itemName = itemStack.getHoverName().getString();

            String title = fromName + "  §7➜  §e" + toName;
            String badge = matchesCurrent
                    ? Component.translatable("jcraft.guide.evo.ready_badge").getString()
                    : itemName;
            Component body = Component.translatable("jcraft.guide.evo.step", fromName, itemName, toName);

            addTextCard(
                    title,
                    matchesCurrent ? 0xFFFDE047 : 0xFFF3F4F6,
                    badge,
                    matchesCurrent ? 0xFF34D399 : 0xFFC4B5FD,
                    null,
                    body,
                    matchesCurrent ? 0xFFFACC15 : 0x669333EA,
                    matchesCurrent ? 0xCC2C1D11 : 0x88181228,
                    null,
                    null,
                    itemStack
            );
        }
    }

    private void buildCombatCards() {
        Minecraft mc = Minecraft.getInstance();
        String summon = JCraftClient.generateName(JCraftClient.STAND_SUMMON_KEY.getParent(), false);
        String useKey = JCraftClient.generateName(mc.options.keyUse, false);
        String dash = JCraftClient.generateName(JCraftClient.DASH_KEY.getParent(), false);
        String cdc = JCraftClient.generateName(JCraftClient.COOLDOWN_CANCEL_KEY.getParent(), false);
        String pilot = JCraftClient.generateName(JCraftClient.PILOT_MODE_KEY.getParent(), false);
        String zoom = JCraftClient.generateName(JCraftClient.PRECISION_VISION_KEY.getParent(), false);
        String toss = JCraftClient.generateName(mc.options.keyPickItem, false);
        String sneak = JCraftClient.generateName(mc.options.keyShift, false);
        String jump = JCraftClient.generateName(mc.options.keyJump, false);

        addGuideTopic("jcraft.guide.combat.summon.title", "[" + summon + "]",
                Component.translatable("jcraft.guide.combat.summon.body", summon), 0xFFFDE047);
        addGuideTopic("jcraft.guide.combat.guard.title", "[" + useKey + "]",
                Component.translatable("jcraft.guide.combat.guard.body", useKey), 0xFF60A5FA);
        addGuideTopic("jcraft.guide.combat.variants.title", "[" + sneak + " / " + jump + "]",
                Component.translatable("jcraft.guide.combat.variants.body", sneak, jump), 0xFF38BDF8);
        addGuideTopic("jcraft.guide.combat.escape.title", "[" + dash + " / " + cdc + "]",
                Component.translatable("jcraft.guide.combat.escape.body", dash, cdc), 0xFF34D399);
        addGuideTopic("jcraft.guide.combat.pilot.title", "[" + pilot + " / " + zoom + "]",
                Component.translatable("jcraft.guide.combat.pilot.body", pilot, zoom), 0xFFC084FC);
        addGuideTopic("jcraft.guide.combat.toss.title", "[" + toss + "]",
                Component.translatable("jcraft.guide.combat.toss.body", toss), 0xFFFBBF24);
        addGuideTopic("jcraft.guide.combat.timestop.title", "ZA WARUDO",
                Component.translatable("jcraft.guide.combat.timestop.body"), 0xFFF472B6);
    }

    private void buildProgressionCards() {
        addGuideTopic("jcraft.guide.prog.arrows.title", "★ Stand",
                Component.translatable("jcraft.guide.prog.arrows.body"), 0xFFFDE047);
        addGuideTopic("jcraft.guide.prog.discs.title", "◈ Discs",
                Component.translatable("jcraft.guide.prog.discs.body"), 0xFF38BDF8);
        addGuideTopic("jcraft.guide.prog.specs.title", "✦ 5 Specs",
                Component.translatable("jcraft.guide.prog.specs.body"), 0xFF34D399);
        addGuideTopic("jcraft.guide.prog.npcs.title", "🌍 World",
                Component.translatable("jcraft.guide.prog.npcs.body"), 0xFFC084FC);
    }

    private void addGuideTopic(String titleKey, String badge, Component body, int accentColor) {
        addTextCard(
                Component.translatable(titleKey).getString(),
                accentColor,
                badge,
                accentColor,
                null,
                body,
                0x779333EA,
                0x99181228,
                JCraft.id("universal"),
                "dash",
                null
        );
    }

    private void addTextCard(
            String title,
            int titleColor,
            @Nullable String badge,
            int badgeColor,
            @Nullable String stats,
            Component body,
            int borderColor,
            int bgColor,
            @Nullable Identifier iconOwner,
            @Nullable String iconName,
            @Nullable ItemStack itemIcon
    ) {
        boolean hasIcon = (iconOwner != null && iconName != null) || itemIcon != null;
        int contentLeftPad = hasIcon ? 28 : 8;
        int wrapW = Math.max(120, (this.panelW - 22) - contentLeftPad - 8);
        List<FormattedCharSequence> lines = this.font.split(body, wrapW);
        int textBlockH = 14 + lines.size() * 10 + (stats != null ? 10 : 0);
        int cardH = Math.max(hasIcon ? 28 : 24, textBlockH + 6);

        this.cards.add(new GuideCard(
                title, titleColor, badge, badgeColor, stats, lines,
                borderColor, bgColor, iconOwner, iconName, itemIcon, cardH
        ));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = getMaxScroll();
        if (max > 0 && scrollY != 0) {
            int next = Mth.clamp(scrollPixels + (scrollY > 0 ? -SCROLL_STEP : SCROLL_STEP), 0, max);
            if (next != scrollPixels) {
                scrollPixels = next;
                this.init();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        gui.fill(0, 0, this.width, this.height, 0x9905030B);
        gui.fill(panelLeft, panelTop, panelLeft + panelW, panelTop + panelH, 0xEE141020);
        gui.outline(panelLeft, panelTop, panelW, panelH, 0xFFD4AF37);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(gui, mouseX, mouseY, partialTick);

        gui.centeredText(this.font, Component.translatable("jcraft.guide.title"),
                this.panelLeft + this.panelW / 2, this.panelTop + 6, 0xFFFDE68A);

        int cardLeft = this.panelLeft + 8;
        int cardRight = this.panelLeft + this.panelW - 12;
        int cardW = cardRight - cardLeft;

        gui.enableScissor(this.panelLeft + 6, this.bodyTop, this.panelLeft + this.panelW - 6, this.bodyBottom);

        int curY = this.bodyTop - this.scrollPixels;
        for (GuideCard card : this.cards) {
            int cardBottom = curY + card.height();
            if (cardBottom >= this.bodyTop && curY <= this.bodyBottom) {
                gui.fill(cardLeft, curY, cardRight, cardBottom, card.bgColor());
                gui.outline(cardLeft, curY, cardW, card.height(), card.borderColor());

                boolean hasIcon = (card.iconOwner() != null && card.iconName() != null) || card.itemIcon() != null;
                if (card.itemIcon() != null) {
                    gui.fakeItem(card.itemIcon(), cardLeft + 5, curY + 5);
                } else if (card.iconOwner() != null && card.iconName() != null) {
                    JCraftAbilityHud.renderIcon(gui, cardLeft + 3, curY + 3, card.iconOwner(), card.iconName());
                }

                int textX = cardLeft + (hasIcon ? 28 : 8);
                int badgeW = 0;
                if (card.badge() != null && !card.badge().isEmpty()) {
                    badgeW = this.font.width(card.badge()) + 6;
                    gui.text(this.font, card.badge(), cardRight - badgeW, curY + 4, card.badgeColor(), false);
                }

                int maxTitleW = Math.max(60, (cardRight - badgeW - 6) - textX);
                String fittedTitle = this.font.width(card.title()) > maxTitleW
                        ? this.font.plainSubstrByWidth(card.title(), maxTitleW - 8) + "..."
                        : card.title();
                gui.text(this.font, fittedTitle, textX, curY + 4, card.titleColor(), true);

                int lineY = curY + 15;
                if (card.stats() != null) {
                    gui.text(this.font, card.stats(), textX, lineY, 0xFF93C5FD, false);
                    lineY += 10;
                }
                for (FormattedCharSequence line : card.bodyLines()) {
                    gui.text(this.font, line, textX, lineY, 0xFFE5E7EB, false);
                    lineY += 10;
                }
            }
            curY += card.height() + 4;
        }

        gui.disableScissor();

        int maxScroll = getMaxScroll();
        if (maxScroll > 0) {
            int barX = this.panelLeft + this.panelW - 6;
            int trackH = this.bodyBottom - this.bodyTop;
            gui.fill(barX, this.bodyTop, barX + 3, this.bodyBottom, 0x44FFFFFF);
            int thumbH = Math.max(16, (int) ((getViewportHeight() / (double) this.totalContentHeight) * trackH));
            int thumbY = this.bodyTop + (int) ((this.scrollPixels / (double) maxScroll) * (trackH - thumbH));
            gui.fill(barX, thumbY, barX + 3, thumbY + thumbH, 0xFFFACC15);
        }

        int footerTextY = this.panelTop + this.panelH - 17;
        String hint = Component.translatable("jcraft.guide.scroll_hint").getString();
        gui.text(this.font, hint, this.panelLeft + this.panelW - this.font.width(hint) - 10, footerTextY, 0xFFC4B5FD, false);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(this.parent);
    }
}
