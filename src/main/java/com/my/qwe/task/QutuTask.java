package com.my.qwe.task;

import com.my.qwe.task.config.IniConfigLoader;

import java.util.*;
import java.util.Properties;

/**
 * 取图任务：加载配置文件中的藏宝图信息
 */
public class QutuTask implements ITask {
    // 配置常量定义
    private static final String SECTION_WATU = "挖图";
    private static final String KEY_BAG_MAPS = "背包";  // 背包变量名

    // 配置数据存储
    private Map<String, List<WarehouseMapInfo>> warehouseMaps = new HashMap<>();

    // 配置加载器
    private IniConfigLoader configLoader;
    private TaskContext context;

    /**
     * 初始化配置加载器
     * @param context 任务上下文
     */
    public void initConfig(TaskContext context) {
        this.context = context;
        this.configLoader = new IniConfigLoader(context.getDeviceName());
    }

    /**
     * 加载配置文件中的藏宝图信息，并返回第一个有效藏宝图
     * @return 第一个藏宝图信息，无则返回null
     */
    public WarehouseMapInfo loadFirstTreasureMap() {
        String deviceId = context.getDeviceId();
        TaskStepNotifier.notifyStep(deviceId, "开始加载配置文件中的藏宝图信息...");

        try {
            // 1. 读取[挖图]区块配置
            Properties watuProps = configLoader.getSection(SECTION_WATU);
            if (watuProps.isEmpty()) {
                TaskStepNotifier.notifyStep(deviceId, "配置文件中[挖图]区块为空");
                return null;
            }

            // 2. 解析仓库中的宝图信息
            warehouseMaps.clear();
            for (Object keyObj : watuProps.keySet()) {
                String scene = (String) keyObj;

                // 跳过系统字段
                if (scene.equals("描述") ||
                        scene.equals("宝图总数") ||
                        scene.equals("已挖图数") ||
                        scene.equals("背包")) {
                    continue;
                }

                String mapStr = watuProps.getProperty(scene, "");
                if (mapStr.trim().isEmpty()) {
                    continue;
                }

                // 3. 解析当前场景的宝图信息
                String[] mapEntries = mapStr.split("\\|");
                List<WarehouseMapInfo> sceneMaps = new ArrayList<>();

                for (String entry : mapEntries) {
                    entry = entry.trim();
                    if (entry.isEmpty()) continue;

                    String[] parts = entry.split(",", 5);
                    if (parts.length != 5) {
                        TaskStepNotifier.notifyStep(deviceId, "无效的宝图配置格式: " + entry);
                        continue;
                    }

                    try {
                        // 解析宝图信息：仓库页,仓库格子,场景,坐标X,坐标Y
                        int warehousePage = Integer.parseInt(parts[0].trim());
                        int warehouseSlot = Integer.parseInt(parts[1].trim());
                        String mapScene = parts[2].trim();
                        int x = Integer.parseInt(parts[3].trim());
                        int y = Integer.parseInt(parts[4].trim());

                        // 创建宝图信息对象
                        WarehouseMapInfo mapInfo = new WarehouseMapInfo(
                                warehousePage, warehouseSlot, mapScene, x, y
                        );
                        sceneMaps.add(mapInfo);

                        // 4. 找到第一个有效宝图后立即返回
                        TaskStepNotifier.notifyStep(deviceId,
                                "成功加载第一个藏宝图: " + scene + " 第" + warehousePage + "页格子" + warehouseSlot);
                        return mapInfo;

                    } catch (NumberFormatException e) {
                        TaskStepNotifier.notifyStep(deviceId, "宝图配置数字格式错误: " + entry);
                    }
                }

                if (!sceneMaps.isEmpty()) {
                    warehouseMaps.put(scene, sceneMaps);
                }
            }

            // 5. 没有找到任何宝图
            TaskStepNotifier.notifyStep(deviceId, "配置文件中未找到任何藏宝图");
            return null;

        } catch (Exception e) {
            TaskStepNotifier.notifyStep(deviceId, "加载藏宝图信息失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 仓库藏宝图信息类
     */
    public static class WarehouseMapInfo {
        private int warehousePage;  // 仓库页码
        private int warehouseSlot;  // 仓库格子
        private String scene;       // 场景
        private int x;              // X坐标
        private int y;              // Y坐标

        public WarehouseMapInfo(int warehousePage, int warehouseSlot, String scene, int x, int y) {
            this.warehousePage = warehousePage;
            this.warehouseSlot = warehouseSlot;
            this.scene = scene;
            this.x = x;
            this.y = y;
        }

        // Getter方法
        public int getWarehousePage() { return warehousePage; }
        public int getWarehouseSlot() { return warehouseSlot; }
        public String getScene() { return scene; }
        public int getX() { return x; }
        public int getY() { return y; }
    }

    // 其他接口方法（仅为完整实现）
    @Override
    public void start(TaskContext context, TaskThread thread) throws Exception {
        CommonActions commonActions = new CommonActions(context,thread);
        // 任务启动逻辑
        initConfig(context);
        //打开仓库
        //commonActions.openJianyeCangku();

        //识别背包空格子数
        List<Integer> konggezishu= commonActions.findcangkujiemianEmptyBagIndices(context.getDeviceId());

        if (konggezishu.size() != 0) {//背包有空位的时候才执行
            WarehouseMapInfo cangkucangbaotu= loadFirstTreasureMap();
            if (cangkucangbaotu != null) {
                commonActions.gotoPage(cangkucangbaotu.warehousePage);
                commonActions.doubleclickCangkuGrid(context.getDeviceId(),cangkucangbaotu.warehouseSlot-1);//因为doubleclickCangkuGrid的参数是从0开始没做处理
                if (removeWarehouseMapConfig(cangkucangbaotu)){
                    addToBagConfig(konggezishu.get(0)+1,cangkucangbaotu);
                    konggezishu.remove(0);
                }
            }
        }



    }

    @Override
    public String getName() {
        return "取图任务";
    }
    /**
     * 从配置文件中移除指定的藏宝图信息
     * @param targetMap 要移除的藏宝图信息
     * @return 是否成功移除
     */
    public boolean removeWarehouseMapConfig(WarehouseMapInfo targetMap) {
        if (targetMap == null) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "要移除的藏宝图信息为空");
            return false;
        }

        String deviceId = context.getDeviceId();
        TaskStepNotifier.notifyStep(deviceId,
                "准备移除仓库第" + targetMap.getWarehousePage() + "页格子" + targetMap.getWarehouseSlot() + "的宝图配置");

        try {
            // 1. 读取[挖图]区块的所有配置
            Properties watuProps = configLoader.getSection(SECTION_WATU);
            if (watuProps.isEmpty()) {
                TaskStepNotifier.notifyStep(deviceId, "配置文件中[挖图]区块为空，无法移除");
                return false;
            }

            // 2. 遍历所有场景查找目标宝图
            for (Object keyObj : watuProps.keySet()) {
                String scene = (String) keyObj;

                // 跳过系统字段
                if (scene.equals("描述") ||
                        scene.equals("宝图总数") ||
                        scene.equals("已挖图数") ||
                        scene.equals("背包")) {
                    continue;
                }

                String mapStr = watuProps.getProperty(scene, "");
                if (mapStr.trim().isEmpty()) {
                    continue;
                }

                // 3. 拆分当前场景的所有宝图
                String[] mapEntries = mapStr.split("\\|");
                List<String> remainingEntries = new ArrayList<>();
                boolean found = false;

                for (String entry : mapEntries) {
                    entry = entry.trim();
                    if (entry.isEmpty()) continue;

                    // 4. 解析单个宝图配置
                    String[] parts = entry.split(",", 5);
                    if (parts.length != 5) {
                        // 保留无效格式的条目
                        remainingEntries.add(entry);
                        continue;
                    }

                    try {
                        // 解析宝图信息用于比对
                        int warehousePage = Integer.parseInt(parts[0].trim());
                        int warehouseSlot = Integer.parseInt(parts[1].trim());
                        String mapScene = parts[2].trim();
                        int x = Integer.parseInt(parts[3].trim());
                        int y = Integer.parseInt(parts[4].trim());

                        // 5. 比对是否为目标宝图（仓库位置和坐标完全匹配）
                        if (warehousePage == targetMap.getWarehousePage() &&
                                warehouseSlot == targetMap.getWarehouseSlot() &&
                                mapScene.equals(targetMap.getScene()) &&
                                x == targetMap.getX() &&
                                y == targetMap.getY()) {
                            // 找到目标宝图，不添加到剩余列表中
                            found = true;
                            TaskStepNotifier.notifyStep(deviceId, "找到并移除目标宝图配置: " + entry);
                        } else {
                            // 非目标宝图，保留
                            remainingEntries.add(entry);
                        }
                    } catch (NumberFormatException e) {
                        // 格式错误的条目保留
                        remainingEntries.add(entry);
                        TaskStepNotifier.notifyStep(deviceId, "宝图配置格式错误，保留原条目: " + entry);
                    }
                }

                // 6. 如果找到目标宝图，更新配置
                if (found) {
                    // 构建新的配置字符串
                    String newMapStr = String.join("|", remainingEntries);

                    // 更新配置文件
                    configLoader.setProperty(SECTION_WATU, scene, newMapStr);
                    configLoader.save();


                    TaskStepNotifier.notifyStep(deviceId, "成功移除目标宝图配置，剩余" + remainingEntries.size() + "个宝图");
                    return true;
                }
            }

            // 7. 未找到目标宝图
            TaskStepNotifier.notifyStep(deviceId, "未在配置文件中找到目标宝图");
            return false;

        } catch (Exception e) {
            TaskStepNotifier.notifyStep(deviceId, "移除宝图配置失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 往配置文件的背包变量中添加宝图信息
     * @param bagSlot 背包格子数（1开始的整数）
     * @param mapInfo 要添加的宝图信息（包含场景、X坐标、Y坐标）
     * @return 是否添加成功
     */
    public boolean addToBagConfig(int bagSlot, WarehouseMapInfo mapInfo) {
        // 参数校验
        if (bagSlot < 1) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "无效的背包格子数: " + bagSlot + "（必须大于0）");
            return false;
        }

        if (mapInfo == null) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "宝图信息不能为空");
            return false;
        }

        if (mapInfo.getScene() == null || mapInfo.getScene().trim().isEmpty()) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "宝图场景信息不能为空");
            return false;
        }

        String deviceId = context.getDeviceId();
        TaskStepNotifier.notifyStep(deviceId,
                "准备添加宝图到背包格子" + bagSlot + ": " +
                        mapInfo.getScene() + "(" + mapInfo.getX() + "," + mapInfo.getY() + ")");

        try {
            // 1. 读取现有背包配置
            Properties watuProps = configLoader.getSection(SECTION_WATU);
            String existingBagConfig = watuProps.getProperty(KEY_BAG_MAPS, "");
            TaskStepNotifier.notifyStep(deviceId, "当前背包配置: " + (existingBagConfig.isEmpty() ? "空" : existingBagConfig));

            // 2. 解析现有配置为列表
            List<String> bagEntries = new ArrayList<>();
            if (!existingBagConfig.isEmpty()) {
                String[] entries = existingBagConfig.split("\\|");
                for (String entry : entries) {
                    if (entry.trim().isEmpty()) continue;
                    bagEntries.add(entry.trim());
                }
            }

            // 3. 检查是否已有该格子的宝图（避免重复）
            String targetSlotPrefix = bagSlot + ",";
            boolean slotExists = false;
            for (int i = 0; i < bagEntries.size(); i++) {
                if (bagEntries.get(i).startsWith(targetSlotPrefix)) {
                    // 替换已有格子的宝图信息
                    bagEntries.set(i, buildBagEntry(bagSlot, mapInfo));
                    slotExists = true;
                    TaskStepNotifier.notifyStep(deviceId, "背包格子" + bagSlot + "已有宝图，进行替换");
                    break;
                }
            }

            // 4. 如果格子不存在，则新增
            if (!slotExists) {
                bagEntries.add(buildBagEntry(bagSlot, mapInfo));
                TaskStepNotifier.notifyStep(deviceId, "新增宝图到背包格子" + bagSlot);
            }

            // 5. 构建新的配置字符串
            String newBagConfig = String.join("|", bagEntries);
            TaskStepNotifier.notifyStep(deviceId, "更新后的背包配置: " + newBagConfig);

            // 6. 保存到配置文件
            configLoader.setProperty(SECTION_WATU, KEY_BAG_MAPS, newBagConfig);
            configLoader.save();

            TaskStepNotifier.notifyStep(deviceId, "宝图信息已成功添加到背包配置");
            return true;

        } catch (Exception e) {
            TaskStepNotifier.notifyStep(deviceId, "添加宝图到背包配置失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 构建符合格式的背包配置条目
     * 格式：背包格子数,场景,X坐标,Y坐标
     */
    private String buildBagEntry(int bagSlot, WarehouseMapInfo mapInfo) {
        return String.format("%d,%s,%d,%d",
                bagSlot,
                mapInfo.getScene(),
                mapInfo.getX(),
                mapInfo.getY());
    }

}
