package com.chinaex123.redstone_chan_lib.api.fluid;

/**
 * 流体种类。
 * <p>
 * 决定注册时使用哪一套实现：
 * 液体像原版水一样往下流，
 * 气体则反过来往上飘、并且会在一定时间后自行消散。
 */
public enum FluidKind {

    /** 液体 */
    LIQUID,
    /** 气体 */
    GAS
}
