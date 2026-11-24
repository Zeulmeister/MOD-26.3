package net.arna.jcraft.api.registry;

import net.arna.jcraft.JCraft;
import net.minecraft.resources.Identifier;

public interface JPacketRegistry {

    // Shortened strings for the sake of saving bandwidth.
    // Further optimization would probably just be an int to char conversion + a static registration method.
    Identifier S2C_SERVER_CHANNEL_FEEDBACK = JCraft.id("sfc");
    Identifier S2C_PLAYER_ANIMATION = JCraft.id("anim");
    Identifier S2C_SHADER_ACTIVATION = JCraft.id("s_act");
    Identifier S2C_SHADER_DEACTIVATION = JCraft.id("s_dct");
    Identifier S2C_TIME_ACCELERATION_STATE = JCraft.id("t_acl");
    Identifier S2C_EPITAPH_STATE = JCraft.id("epth");
    Identifier S2C_TIME_ERASE_PREDICTION_STATE = JCraft.id("te_prdct");
    Identifier S2C_SERVER_CONFIG = JCraft.id("s_config");
    Identifier S2C_J_EXPLOSION = JCraft.id("expl");
    Identifier S2C_COMBO_COUNTER = JCraft.id("combo");
    Identifier S2C_TIME_STOP = JCraft.id("ts");
    Identifier S2C_SPLATTER = JCraft.id("splt");
    Identifier S2C_STAND_HURT = JCraft.id("stnd_hurt");
    Identifier S2C_PREDICTION_UPDATE = JCraft.id("prdct");
    Identifier S2C_MAGNETIC_FIELD_PARTICLE = JCraft.id("mfp");
    Identifier S2C_ATTACKER_DATA = JCraft.id("atk_data");
    Identifier S2C_MANDOM_DATA = JCraft.id("mndm_data");
    Identifier S2C_STONE_MASK_CLENCH = JCraft.id("msk_clch");
    Identifier S2C_IPS_TRIGGERED = JCraft.id("ips");
    Identifier S2C_DAMAGE_NUMBER = JCraft.id("dmg_no");
    Identifier S2C_BLOCK_BREAKAGE = JCraft.id("bb");
    Identifier S2C_BOUND_SOUND = JCraft.id("bs");

    Identifier C2S_STAND_BLOCK = JCraft.id("stnd_blk");
    Identifier C2S_COOLDOWN_CANCEL = JCraft.id("cdc");
    Identifier C2S_PLAYER_INPUT = JCraft.id("plr_input");
    Identifier C2S_PLAYER_INPUT_HOLD = JCraft.id("plr_input_h");
    Identifier C2S_VARIANT = JCraft.id("vrnt");
    Identifier C2S_REMOTE_STAND_INTERACT = JCraft.id("rmt_stnd_act");
    Identifier C2S_PREDICTION_TRIGGER = JCraft.id("prdct_trig");
    Identifier C2S_MENU_CALL = JCraft.id("menu");
    Identifier C2S_GUN_AIM = JCraft.id("gun_aim");
    Identifier C2S_POSE_TAUNT = JCraft.id("pose_taunt");
    Identifier C2S_REMOTE_CONTROL_TOGGLE = JCraft.id("pilot_toggle");
    Identifier C2S_STAND_ROTATION = JCraft.id("stnd_rot");
    Identifier C2S_RADIAL_ACTION = JCraft.id("radial_act");

    Identifier S2C_COLOR_SHIFT = JCraft.id("clr_shft");
    Identifier S2C_KINETIC_TS_COUNT = JCraft.id("ts_kin");

    Identifier S2C_SYNC = JCraft.id("s2c_sync");
}
