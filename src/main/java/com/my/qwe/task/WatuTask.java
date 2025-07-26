package com.my.qwe.task;

import com.my.qwe.http.DeviceHttpClient;
import com.my.qwe.task.config.IniConfigLoader;

import java.util.*;

public class WatuTask implements ITask {
    private int waittime = new Random().nextInt(200) + 300; // 随机延迟
    // 配置常量定义
    private static final String SECTION_WATU = "挖图";
    private static final String KEY_BAG_MAPS = "背包";  // 背包配置项的键名
    private static final String KEY_TOTALSHU = "宝图总数";  // 宝图总数的键名
    private static final String KEY_YIWASHU = "已挖图数";  // 宝图总数的键名

    // 背包宝图数据存储
    private Map<String, List<BagMapInfo>> bagMaps = new HashMap<>();

    // 配置加载器和上下文
    private IniConfigLoader configLoader;
    private TaskContext context;

    /**
     * 初始化配置加载器
     * @param context 任务上下文
     */
    public void initConfig(TaskContext context) {
        this.context = context;
        this.configLoader = new IniConfigLoader(context.getDeviceName());
        
        // 添加配置文件路径调试信息
        String deviceId = context.getDeviceId();
        TaskStepNotifier.notifyStep(deviceId, "初始化配置文件: " + context.getDeviceName() + ".ini");
    }

    /**
     * 加载配置文件中背包的藏宝图信息，并返回第一个有效藏宝图
     * @return 第一个藏宝图信息，无则返回null
     */
    public BagMapInfo loadFirstBagTreasureMap() {
        String deviceId = context.getDeviceId();
        TaskStepNotifier.notifyStep(deviceId, "开始加载配置文件中背包的藏宝图信息...");

        try {
            // 1. 读取[挖图]区块配置（强制重新加载）
            Properties watuProps = configLoader.getSectionReload(SECTION_WATU);

            if (watuProps.isEmpty()) {
                TaskStepNotifier.notifyStep(deviceId, "配置文件中[挖图]区块为空");
                return null;
            }

            // 2. 直接获取"背包"配置项（不再需要遍历所有key）

            String bagConfigStr = watuProps.getProperty(KEY_BAG_MAPS, "");
            if (bagConfigStr.trim().isEmpty()) {
                TaskStepNotifier.notifyStep(deviceId, "配置文件中[背包]信息为空");
                return null;
            }

            // 3. 解析背包的宝图信息（格式：格子1,场景1,X1,Y1|格子2,场景2,X2,Y2...）
            bagMaps.clear();
            List<BagMapInfo> bagMapList = new ArrayList<>();
            String[] mapEntries = bagConfigStr.split("\\|");

            for (String entry : mapEntries) {
                entry = entry.trim();
                if (entry.isEmpty()) continue;

                // 背包配置格式应为：背包格子数,场景,坐标X,坐标Y（共4个部分）
                String[] parts = entry.split(",", 4);
                if (parts.length != 4) {
                    TaskStepNotifier.notifyStep(deviceId, "无效的背包宝图配置格式: " + entry + "（应为：格子数,场景,X,Y）");
                    continue;
                }

                try {
                    // 解析宝图信息：背包格子数,场景,坐标X,坐标Y
                    int bagSlot = Integer.parseInt(parts[0].trim());  // 修正：第一个部分就是背包格子数
                    String mapScene = parts[1].trim();
                    int x = Integer.parseInt(parts[2].trim());
                    int y = Integer.parseInt(parts[3].trim());

                    // 创建宝图信息对象
                    BagMapInfo mapInfo = new BagMapInfo(
                            bagSlot, mapScene, x, y
                    );
                    bagMapList.add(mapInfo);

                    // 4. 找到第一个有效宝图后立即返回
                    TaskStepNotifier.notifyStep(deviceId,
                            "成功加载背包中第一个藏宝图: 格子" + bagSlot + "(" + mapScene + "," + x + "," + y + ")");
                    // 将解析结果存入内存（如果需要）
                    bagMaps.put(KEY_BAG_MAPS, bagMapList);
                    return mapInfo;

                } catch (NumberFormatException e) {
                    TaskStepNotifier.notifyStep(deviceId, "背包宝图配置数字格式错误: " + entry);
                }
            }

            // 5. 没有找到任何有效宝图
            TaskStepNotifier.notifyStep(deviceId, "配置文件中背包内未找到任何有效藏宝图");
            return null;

        } catch (Exception e) {
            TaskStepNotifier.notifyStep(deviceId, "加载背包藏宝图信息失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 背包藏宝图信息类
     */
    public static class BagMapInfo {
        private int bagSlot;    // 背包格子数
        private String scene;   // 场景
        private int x;          // X坐标
        private int y;          // Y坐标

        public BagMapInfo(int bagSlot, String scene, int x, int y) {
            this.bagSlot = bagSlot;
            this.scene = scene;
            this.x = x;
            this.y = y;
        }

        // Getter方法
        public int getBagSlot() { return bagSlot; }
        public String getScene() { return scene; }
        public int getX() { return x; }
        public int getY() { return y; }
    }

    // 其他接口方法...
    @Override
    public void start(TaskContext context, TaskThread thread) throws Exception {
        initConfig(context);
        Luxian luxian = new Luxian(context,thread);
        DeviceHttpClient httpClient = new DeviceHttpClient();
        GameStateDetector gameStateDetector = new GameStateDetector(context,httpClient);
        CommonActions commonActions = new CommonActions(context,thread );
        MovementStateDetector movementDetector = new MovementStateDetector(context, commonActions, gameStateDetector);
        Properties watuProps = configLoader.getSection(SECTION_WATU);
        int kaituwancheng = Integer.parseInt(watuProps.getProperty("开图完成"));
        int dutuwancheng = Integer.parseInt(watuProps.getProperty("读图完成"));

        BagMapInfo bagMapInfo;
        int gezishu,x,y;
        String scence,pos;
        int[] jiancezuobiao;
        KaituTask kaituTask = new KaituTask();
        DutuTask dutuTask = new DutuTask();


        if (kaituwancheng == 0) {
            kaituTask.start(context, thread);
            configLoader.setProperty(SECTION_WATU, "开图完成", "1");
            configLoader.save();
        }
        TaskThread.sleep(new Random().nextInt(400) + 300);
        if (dutuwancheng == 0) {
            dutuTask.start(context, thread);
        }

        TaskThread.sleep(new Random().nextInt(400) + 300);

        QutuTask qutuTask = new QutuTask();
        qutuTask.initConfig(context);

        watuProps = configLoader.getSection(SECTION_WATU);
        int baotuzongshu = Integer.parseInt(watuProps.getProperty(KEY_TOTALSHU)==null? String.valueOf(0) :watuProps.getProperty(KEY_TOTALSHU));
        int yiwashu = Integer.parseInt(watuProps.getProperty(KEY_YIWASHU)==null? String.valueOf(0) :watuProps.getProperty(KEY_YIWASHU));
        if (baotuzongshu == 0 && yiwashu == 0) {}


        while ((qutuTask.loadFirstTreasureMap() != null ||loadFirstBagTreasureMap() != null)
                && (!thread.isStopped() && !Thread.currentThread().isInterrupted()) ) {
            if (thread.isStopped() || Thread.currentThread().isInterrupted()) break;
            thread.checkPause();

            bagMapInfo = loadFirstBagTreasureMap();
            if (bagMapInfo==null) {
                qutuTask.start(context, thread);
                // 强制重新加载配置文件，确保获取最新数据
                watuProps = configLoader.getSectionReload(SECTION_WATU);
                TaskStepNotifier.notifyStep(context.getDeviceId(), "重新加载配置文件，获取最新数据");
            }
            bagMapInfo = loadFirstBagTreasureMap();
            while (bagMapInfo != null) {
                if (thread.isStopped() || Thread.currentThread().isInterrupted()) return;

                gezishu =  bagMapInfo.getBagSlot();
                scence= bagMapInfo.getScene();
                x= bagMapInfo.getX();
                y = bagMapInfo.getY();
                pos  = x+","+y;
                luxian.toScene(scence,pos);
                
                // 使用新的移动状态检测功能
                TaskStepNotifier.notifyStep(context.getDeviceId(), "开始前往目的地: (" + x + "," + y + ")");
                
                // 等待角色到达目的地，使用移动状态检测
                boolean reachedDestination = movementDetector.waitForDestination(x, y, 60); // 60秒超时
                
                if (!reachedDestination) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "等待到达目的地超时，尝试处理移动异常");
                    boolean handled = movementDetector.handleAbnormalMovement(x, y, 3); // 最多重试3次
                    
                    if (!handled) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(), "处理移动异常失败，跳过当前宝图");
                        continue;
                    }
                }
                
                TaskStepNotifier.notifyStep(context.getDeviceId(), "已到达目的地: (" + x + "," + y + ")");
                commonActions.openBag();
                Thread.sleep(new Random().nextInt(200) + 300);
                commonActions.doubleclickBagGrid(context.getDeviceId(),gezishu-1);
                watuProps = configLoader.getSection(SECTION_WATU);
                Thread.sleep(new Random().nextInt(200) + 300);

                removeBagMapBySlot(bagMapInfo);
                System.out.println();


                Thread.sleep(new Random().nextInt(200) + 300);
                commonActions.closeBag();

                Thread.sleep(new Random().nextInt(200) + 300);

                while (gameStateDetector.isInBattle()){//判断是否进入战斗
                    TaskStepNotifier.notifyStep(context.getDeviceId(),"正在战斗中...");
                    Thread.sleep(new Random().nextInt(200) + 300);

                }




                bagMapInfo = loadFirstBagTreasureMap();
                    yiwashu = 1+Integer.parseInt(watuProps.getProperty(KEY_YIWASHU));
                    Thread.sleep(new Random().nextInt(200) + 300);
                // 强制重新加载配置文件，确保获取最新数据
                watuProps = configLoader.getSectionReload(SECTION_WATU);
            }
        }
    }

    @Override
    public String getName() { return "挖图任务"; }


    /**
     * 根据BagMapInfo中的背包格子数，移除配置文件中对应格子的藏宝图信息
     * @param targetMap 包含目标背包格子数的宝图信息对象
     * @return 是否移除成功
     */
    public boolean removeBagMapBySlot(BagMapInfo targetMap) {
        // 参数校验
        if (targetMap == null) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "目标宝图信息为空，无法移除");
            return false;
        }

        int targetSlot = targetMap.getBagSlot();
        if (targetSlot < 1) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "无效的背包格子数: " + targetSlot);
            return false;
        }

        String deviceId = context.getDeviceId();
        TaskStepNotifier.notifyStep(deviceId, "准备移除背包格子" + targetSlot + "的藏宝图信息");

        try {
            // 1. 读取[挖图]区块配置
            Properties watuProps = configLoader.getSection(SECTION_WATU);
            if (watuProps.isEmpty()) {
                TaskStepNotifier.notifyStep(deviceId, "配置文件中[挖图]区块为空，无法移除");
                return false;
            }

            // 2. 获取背包配置信息
            String bagConfigStr = watuProps.getProperty(KEY_BAG_MAPS, "");
            if (bagConfigStr.trim().isEmpty()) {
                TaskStepNotifier.notifyStep(deviceId, "背包中没有任何藏宝图信息，无需移除");
                return false;
            }

            // 3. 拆分背包中的所有宝图（格式：格子1,场景1,X1,Y1|格子2,场景2,X2,Y2...）
            String[] mapEntries = bagConfigStr.split("\\|");
            List<String> remainingEntries = new ArrayList<>();
            boolean found = false;

            // 4. 遍历所有宝图，筛选出非目标格子的条目
            for (String entry : mapEntries) {
                entry = entry.trim();
                if (entry.isEmpty()) continue;

                // 解析条目获取格子数（格式：格子数,场景,X,Y）
                String[] parts = entry.split(",", 2);  // 只需要分割出第一个部分（格子数）
                if (parts.length < 1) {
                    remainingEntries.add(entry);  // 保留格式错误的条目
                    continue;
                }

                try {
                    int slot = Integer.parseInt(parts[0].trim());
                    // 比对是否为目标格子
                    if (slot == targetSlot) {
                        // 找到目标格子，不添加到剩余列表
                        found = true;
                        TaskStepNotifier.notifyStep(deviceId, "已找到并移除目标格子的宝图: " + entry);
                    } else {
                        // 非目标格子，保留
                        remainingEntries.add(entry);
                    }
                } catch (NumberFormatException e) {
                    // 格子数格式错误，保留原条目
                    remainingEntries.add(entry);
                    TaskStepNotifier.notifyStep(deviceId, "宝图配置格式错误，保留原条目: " + entry);
                }
            }

            // 5. 判断是否找到并移除了目标宝图
            if (!found) {
                TaskStepNotifier.notifyStep(deviceId, "未在背包中找到格子" + targetSlot + "的宝图信息");
                return false;
            }

            // 6. 构建新的背包配置字符串
            String newBagConfig = String.join("|", remainingEntries);
            TaskStepNotifier.notifyStep(deviceId,
                    "移除后剩余的背包宝图: " + (newBagConfig.isEmpty() ? "无" : newBagConfig));

            // 7. 更新并保存配置文件
            configLoader.setProperty(SECTION_WATU, KEY_BAG_MAPS, newBagConfig);
            configLoader.save();

            TaskStepNotifier.notifyStep(deviceId, "成功移除背包格子" + targetSlot + "的藏宝图信息");
            return true;

        } catch (Exception e) {
            TaskStepNotifier.notifyStep(deviceId, "移除背包宝图失败: " + e.getMessage());
            return false;
        }
    }
}
