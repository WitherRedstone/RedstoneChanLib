package com.chinaex123.redstone_chan_lib.util;

import java.util.Arrays;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

/**
 * 传送门提示文案工具类。
 * <p>
 * 统一管理模组提示文案的翻译键前缀，提供本地化组件与纯文本的获取方法；
 * 纯文本方法在翻译缺失时回退为键名（附带参数），避免抛出异常。
 */
public class PortalTexts {

    /** 模组提示文案的键前缀。 */
    public static final String PREFIX = "message.redstone_chan_lib.";

    /**
     * 获取本地化文本组件。
     *
     * @param key  翻译键后缀
     * @param args 格式化参数
     * @return 文本组件
     */
    public static Component component(String key, Object... args) {
        return Component.translatable(PREFIX + key, args);
    }

    /**
     * 获取本地化纯文本。
     * <p>
     * 翻译缺失时返回完整键名，若有参数则附在键名之后。
     *
     * @param key  翻译键后缀
     * @param args 格式化参数
     * @return 本地化文本
     */
    public static String text(String key, Object... args) {
        String fullKey = PREFIX + key;
        Language language = Language.getInstance();
        if (!language.has(fullKey)) {
            return args.length == 0 ? fullKey : fullKey + " " + Arrays.toString(args);
        }
        return Component.translatable(fullKey, args).getString();
    }
}
