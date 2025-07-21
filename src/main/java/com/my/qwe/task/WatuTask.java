package com.my.qwe.task;

import java.util.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class WatuTask implements ITask {
    private TaskContext context;
    private TaskThread taskThread;
    private Configuration config;           // 配置管理器
    private InventoryManager inventory;     // 背包管理器
    private WarehouseManager warehouse;     // 仓库管理器
    private RouteManager routeManager;      // 路线管理器
    private BattleManager battleManager;    // 战斗管理器
    private RecoveryManager recoveryManager; // 恢复管理器

    private List<TreasureMap> treasureMaps; // 当前背包中的藏宝图
    private LocalDateTime incenseExpireTime; // 摄妖香过期时间

    // 常量配置
    private static final int MAX_TREASURE_MAPS = 15;  // 背包最大藏宝图数量
    private static final int HP_RECOVERY_THRESHOLD = 50;  // 血量恢复阈值(%)
    private static final int MP_RECOVERY_THRESHOLD = 30;  // 魔法值恢复阈值(%)
    private static final int PET_HP_RECOVERY_THRESHOLD = 60;  // 召唤兽血量恢复阈值(%)
    private static final List<String> KEEP_ITEMS = Arrays.asList(
            "飞行符", "摄妖香", "红碗", "蓝碗", "飞行棋"
    ); // 需要保留的物品列表

    @Override
    public void start(TaskContext context, TaskThread thread) {
        this.context = context;
        this.taskThread = thread;
        this.config = new Configuration(context);
        this.inventory = new InventoryManager(context);
        this.warehouse = new WarehouseManager(context);
        this.routeManager = new RouteManager(context);
        this.battleManager = new BattleManager(context);
        this.recoveryManager = new RecoveryManager(context);
        this.treasureMaps = new ArrayList<>();

        try {
            executeDiggingTask();
        } catch (Exception e) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "挖图任务异常：" + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return "挖图";
    }

    // 执行挖图任务主循环
    private void executeDiggingTask() throws Exception {
        TaskStepNotifier.notifyStep(context.getDeviceId(), "开始执行挖图任务...");

        while (!taskThread.isStopped()) {
            // 步骤1：从仓库取出藏宝图
            if (!fetchTreasureMapsFromWarehouse()) {
                TaskStepNotifier.notifyStep(context.getDeviceId(), "仓库中没有符合条件的藏宝图，任务结束");
                return;
            }

            // 更新配置文件
            updateConfigWithMaps();

            // 使用摄妖香（第一次取图后）
            if (incenseExpireTime == null) {
                useIncense();
            }

            // 步骤2：前往场景并挖图
            for (TreasureMap map : new ArrayList<>(treasureMaps)) {
                if (taskThread.isStopped()) return;

                // 检查状态并恢复
                checkAndRecoverStatus();

                // 检查摄妖香是否过期
                if (isIncenseExpired()) {
                    useIncense();
                }

                // 前往指定场景和坐标
                gotoMapLocation(map);

                // 使用藏宝图挖图
                if (useTreasureMap(map)) {
                    // 记录已挖图数
                    config.incrementDugCount();

                    // 处理可能的战斗
                    handlePossibleBattle();

                    // 移除已使用的地图
                    treasureMaps.remove(map);
                    inventory.removeItem(map.getGridIndex());

                    // 更新配置
                    updateConfigWithMaps();
                }
            }

            // 步骤3：清理背包
            cleanUpInventory();

            // 步骤4：返回仓库存储物品
            returnToWarehouseAndStoreItems();
        }
    }

    // 从仓库取出藏宝图
    private boolean fetchTreasureMapsFromWarehouse() throws Exception {
        TaskStepNotifier.notifyStep(context.getDeviceId(), "准备从仓库取出藏宝图...");
        // 返回仓库
        routeManager.returnToWarehouse();

        // 打开仓库
        warehouse.openWarehouse();

        // 查找符合条件的藏宝图
        List<Integer> mapLocations = warehouse.findTreasureMaps(config.getMapFilter());
        if (mapLocations.isEmpty()) {
            return false;
        }

        // 取出藏宝图到背包
        int count = 0;
        for (int location : mapLocations) {
            if (count >= MAX_TREASURE_MAPS || !inventory.hasEmptySlot()) {
                break;
            }

            int targetSlot = inventory.findEmptySlot();
            warehouse.takeItem(location, targetSlot);

            // 记录藏宝图信息
            TreasureMap map = new TreasureMap(
                    targetSlot,
                    config.getMapScene(location),
                    config.getMapX(location),
                    config.getMapY(location),
                    config.getMapType(location)
            );
            treasureMaps.add(map);
            count++;
        }

        TaskStepNotifier.notifyStep(context.getDeviceId(), "已从仓库取出" + count + "张藏宝图");
        return true;
    }

    // 更新配置文件中的背包藏宝图信息
    private void updateConfigWithMaps() {
        StringBuilder mapInfo = new StringBuilder();
        for (TreasureMap map : treasureMaps) {
            mapInfo.append(map.getGridIndex())
                    .append(":")
                    .append(map.getSceneName())
                    .append(",")
                    .append(map.getX())
                    .append(",")
                    .append(map.getY())
                    .append(";");
        }
        config.setProperty("挖图.背包", mapInfo.toString());
        config.save();
    }

    // 前往藏宝图位置
    private void gotoMapLocation(TreasureMap map) throws Exception {
        TaskStepNotifier.notifyStep(context.getDeviceId(), "前往藏宝图位置：" + map.getSceneName() + "(" + map.getX() + "," + map.getY() + ")");

        // 前往场景
        routeManager.navigateToScene(map.getSceneName());

        // 使用坐标输入方法前往具体坐标
        String coordinates = map.getX() + "," + map.getY();
        clickInputPos(coordinates);
    }

    // 使用摄妖香
    private void useIncense() throws Exception {
        TaskStepNotifier.notifyStep(context.getDeviceId(), "使用摄妖香...");
        int incenseSlot = inventory.findItemSlot("摄妖香");
        if (incenseSlot >= 0) {
            inventory.useItem(incenseSlot);
            // 记录摄妖香过期时间（28分钟后）
            incenseExpireTime = LocalDateTime.now().plus(28, ChronoUnit.MINUTES);
            TaskStepNotifier.notifyStep(context.getDeviceId(), "摄妖香已使用，下次使用时间：" + incenseExpireTime);
        } else {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "警告：背包中没有摄妖香！");
        }
    }

    // 检查摄妖香是否过期
    private boolean isIncenseExpired() {
        return incenseExpireTime != null && LocalDateTime.now().isAfter(incenseExpireTime);
    }

    // 使用藏宝图
    private boolean useTreasureMap(TreasureMap map) throws Exception {
        TaskStepNotifier.notifyStep(context.getDeviceId(), "准备使用背包格子" + map.getGridIndex() + "的藏宝图");

        // 打开背包
        openBag();

        // 使用指定格子的藏宝图
        boolean result = inventory.useItem(map.getGridIndex());

        Thread.sleep(1000); // 等待挖图动画
        return result;
    }

    // 处理可能的战斗
    private void handlePossibleBattle() throws Exception {
        if (battleManager.isInBattle()) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "进入战斗，开始战斗处理...");
            battleManager.autoFight();

            // 战斗结束后检查状态
            checkAndRecoverStatus();
        }
    }

    // 检查并恢复状态
    private void checkAndRecoverStatus() throws Exception {
        CharacterStatus status = getCurrentStatus();

        // 检查角色血量
        if (status.needHpRecovery(HP_RECOVERY_THRESHOLD)) {
            recoveryManager.recoverHp();
        }

        // 检查角色魔法值
        if (status.needMpRecovery(MP_RECOVERY_THRESHOLD)) {
            recoveryManager.recoverMp();
        }

        // 检查召唤兽血量
        for (PetStatus pet : status.getPets()) {
            if (pet.needHpRecovery(PET_HP_RECOVERY_THRESHOLD)) {
                recoveryManager.recoverPetHp(pet);
            }
        }
    }

    // 获取当前角色和召唤兽状态
    private CharacterStatus getCurrentStatus() {
        // 此方法需要具体实现，获取角色和召唤兽的当前状态
        return new CharacterStatus(); // 占位实现
    }

    // 清理背包（扔掉低级书和铁）
    private void cleanUpInventory() throws Exception {
        TaskStepNotifier.notifyStep(context.getDeviceId(), "开始清理背包...");
        openBag();

        // 查找并扔掉低级书和铁
        List<Integer> itemsToDiscard = inventory.findItemsByNames(
                Arrays.asList("低级技能书", "铁", "铜", "银")
        );

        for (int slot : itemsToDiscard) {
            inventory.discardItem(slot);
            Thread.sleep(300); // 避免操作过快
        }

        TaskStepNotifier.notifyStep(context.getDeviceId(), "背包清理完成，已丢弃" + itemsToDiscard.size() + "件物品");
    }

    // 返回仓库并存储物品
    private void returnToWarehouseAndStoreItems() throws Exception {
        TaskStepNotifier.notifyStep(context.getDeviceId(), "返回仓库并存储物品...");

        // 返回仓库
        routeManager.returnToWarehouse();

        // 打开仓库
        warehouse.openWarehouse();

        // 存储背包中的物品（除了保留物品）
        List<Integer> itemsToStore = inventory.findItemsExcept(KEEP_ITEMS);
        for (int slot : itemsToStore) {
            String itemName = inventory.getItemName(slot);
            if (!KEEP_ITEMS.contains(itemName)) {
                warehouse.storeItem(slot);
                Thread.sleep(300); // 避免操作过快
            }
        }

        TaskStepNotifier.notifyStep(context.getDeviceId(), "已将" + itemsToStore.size() + "件物品存入仓库");
    }

    // 打开背包
    private void openBag() throws Exception {
        // 实现打开背包的逻辑
        // 例如：human.clickImg(context.getDeviceId(), "背包", 5, 5);
        // 这里使用之前的坐标输入方法
        clickInputPos("0,0"); // 假设0,0是打开背包的操作，需根据实际情况修改
        Thread.sleep(500);
    }

    // 输入坐标前往指定位置（复用之前实现）
    private void clickInputPos(String str) {
        // 这里直接复用之前实现的坐标输入方法
        // 由于代码较长，此处省略具体实现，可直接复制之前的clickInputPos方法
    }

    // 藏宝图数据类
    private static class TreasureMap {
        private int gridIndex;       // 背包格子索引
        private String sceneName;    // 场景名称
        private int x;               // X坐标
        private int y;               // Y坐标
        private MapType type;        // 地图类型

        public TreasureMap(int gridIndex, String sceneName, int x, int y, MapType type) {
            this.gridIndex = gridIndex;
            this.sceneName = sceneName;
            this.x = x;
            this.y = y;
            this.type = type;
        }

        // getter方法
        public int getGridIndex() { return gridIndex; }
        public String getSceneName() { return sceneName; }
        public int getX() { return x; }
        public int getY() { return y; }
        public MapType getType() { return type; }

        // 枚举：藏宝图类型
        public enum MapType {
            COMMON, // 普通藏宝图
            HIGH_LEVEL // 高级藏宝图
        }
    }

    // 角色状态类
    private static class CharacterStatus {
        private int hp;            // 角色当前血量
        private int maxHp;         // 角色最大血量
        private int mp;            // 角色当前魔法值
        private int maxMp;         // 角色最大魔法值
        private List<PetStatus> pets; // 召唤兽状态列表

        public CharacterStatus() {
            this.pets = new ArrayList<>();
        }

        // 是否需要加血
        public boolean needHpRecovery(int thresholdPercent) {
            return (double)hp / maxHp < thresholdPercent / 100.0;
        }

        // 是否需要加蓝
        public boolean needMpRecovery(int thresholdPercent) {
            return (double)mp / maxMp < thresholdPercent / 100.0;
        }

        // getter和setter方法
        public int getHp() { return hp; }
        public void setHp(int hp) { this.hp = hp; }
        public int getMaxHp() { return maxHp; }
        public void setMaxHp(int maxHp) { this.maxHp = maxHp; }
        public int getMp() { return mp; }
        public void setMp(int mp) { this.mp = mp; }
        public int getMaxMp() { return maxMp; }
        public void setMaxMp(int maxMp) { this.maxMp = maxMp; }
        public List<PetStatus> getPets() { return pets; }
        public void setPets(List<PetStatus> pets) { this.pets = pets; }
    }

    // 召唤兽状态类
    private static class PetStatus {
        private int hp;
        private int maxHp;
        private boolean alive;

        public boolean needHpRecovery(int thresholdPercent) {
            return alive && (double)hp / maxHp < thresholdPercent / 100.0;
        }

        // getter和setter方法
        public int getHp() { return hp; }
        public void setHp(int hp) { this.hp = hp; }
        public int getMaxHp() { return maxHp; }
        public void setMaxHp(int maxHp) { this.maxHp = maxHp; }
        public boolean isAlive() { return alive; }
        public void setAlive(boolean alive) { this.alive = alive; }
    }
}