package com.chinaex123.redstone_chan_lib.api.portal;

import com.chinaex123.redstone_chan_lib.util.PortalTexts;
import net.minecraft.core.Direction;

import java.util.Locale;

/**
 * 传送门摆放类型。
 * <p>
 * 表示传送门平面允许的朝向：竖门（法线为水平轴）、
 * 横门（法线为竖直轴）或两者皆可，并提供按平面法线轴的判定与字符串解析。
 */
public enum PortalOrientation {

    /** 竖门与横门都接受 */
    ANY,

    /** 只接受竖着站的传送门 */
    VERTICAL,

    /** 只接受平铺在地面的传送门 */
    HORIZONTAL;

    /**
     * 判断是否接受指定法线轴的传送门平面。
     * <p>
     * ANY 接受全部；VERTICAL 只接受非竖直轴（竖门）；
     * HORIZONTAL 只接受竖直轴（横门）。
     *
     * @param axis 平面法线轴
     * @return 接受时返回 true
     */
    public boolean allows(Direction.Axis axis) {
        return switch (this) {
            case ANY -> true;
            case VERTICAL -> axis != Direction.Axis.Y;
            case HORIZONTAL -> axis == Direction.Axis.Y;
        };
    }

    /**
     * 从字符串解析摆放类型。
     * <p>
     * 匹配不区分大小写，支持若干同义写法；无法识别时抛出异常。
     *
     * @param value 字符串定义
     * @return 摆放类型
     */
    public static PortalOrientation parse(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "any", "all", "both" -> ANY;
            case "vertical", "upright", "standing" -> VERTICAL;
            case "horizontal", "flat", "lying" -> HORIZONTAL;
            default -> throw new IllegalArgumentException(PortalTexts.text("invalid_orientation", value));
        };
    }
}
