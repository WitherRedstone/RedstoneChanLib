/**
 * 红石 LIB 传送门系统对外公开的 API。
 * <p>
 * 入口一览：
 * <ul>
 *     <li>{@link com.chinaex123.redstone_chan_lib.api.portal.PortalDefinition}：传送门定义，
 *         描述外围方块、激活方式、颜色、名称、目标维度与尺寸限制，用 {@code builder(...)} 链式构造</li>
 *     <li>{@link com.chinaex123.redstone_chan_lib.api.portal.PortalCatalyst}：激活方式，
 *         支持空手、物品、物品标签、流体、流体标签五种</li>
 *     <li>{@link com.chinaex123.redstone_chan_lib.api.portal.PortalDefinitions}：定义注册表，
 *         负责 Java 注册、数据包合并与查询</li>
 *     <li>{@link com.chinaex123.redstone_chan_lib.api.portal.PortalRitual}：仪式配置，
 *         描述暮色森林式的激活方式：地面留出流体池、外圈摆框架、往流体里丢物品</li>
 *     <li>{@link com.chinaex123.redstone_chan_lib.api.portal.PortalShape}：形状判定，
 *         从某个框架方块出发把整座门圈出来，竖直的门与平铺在地面的横门都支持</li>
 *     <li>{@link com.chinaex123.redstone_chan_lib.api.portal.PortalOrientation}：摆放类型，
 *         可选“竖直”“平铺”或“两种都行”，定义里限制门是立着还是躺着</li>
 *     <li>{@link com.chinaex123.redstone_chan_lib.api.portal.PortalStructureHelper}：摆放与拆除，
 *         负责把内部填成传送门方块、按定义建一座新门、拆掉整座门</li>
 *     <li>{@link com.chinaex123.redstone_chan_lib.api.portal.PortalPlacement}：跨维度定位，
 *         负责坐标换算、在目标维度寻找同款门、找不到时建一座</li>
 *     <li>{@link com.chinaex123.redstone_chan_lib.api.portal.PortalTeleport}：传送执行，
 *         一步完成“算目的地 + 移动玩家”</li>
 *     <li>{@link com.chinaex123.redstone_chan_lib.api.portal.PortalActivation}：激活流程，
 *         右键框架点亮传送门的完整逻辑，模组自己的右键事件就是调用它</li>
 *     <li>{@link com.chinaex123.redstone_chan_lib.api.portal.PortalRitualActivation}：仪式激活流程，
 *         掉落物落进流体池后打雷点亮，模组自己的掉落物检测就是调用它</li>
 *     <li>{@link com.chinaex123.redstone_chan_lib.api.portal.PortalEffects}：特效与音效，
 *         点亮时与到达时的表现</li>
 *     <li>{@link com.chinaex123.redstone_chan_lib.util.PortalTexts}：提示文案，
 *         把报错与日志交给语言文件（{@code message.redstone_chan_lib.*}），按玩家语言显示</li>
 * </ul>
 * <p>
 * 数据包与 Java 定义共用同一个 id 空间，数据包定义会覆盖同 id 的内置定义。
 */
package com.chinaex123.redstone_chan_lib.api.portal;
