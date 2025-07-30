package com.my.qwe.task;

import com.my.qwe.controller.HumanLikeController;
import com.my.qwe.http.DeviceHttpClient;
import com.my.qwe.task.config.IniConfigLoader;
import org.bytedeco.opencv.opencv_core.Device;

import java.io.IOException;
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
        // 添加任务开始的日志
        TaskStepNotifier.notifyStep(context.getDeviceId(), "挖图任务开始执行");

        initConfig(context);
        Luxian luxian = new Luxian(context, thread);
        DeviceHttpClient httpClient = new DeviceHttpClient();
        GameStateDetector gameStateDetector = new GameStateDetector(context, httpClient);
        CommonActions commonActions = new CommonActions(context, thread);
        MovementStateDetector movementDetector = new MovementStateDetector(context, commonActions, gameStateDetector);
        Properties watuProps = configLoader.getSection(SECTION_WATU);
        int kaituwancheng = Integer.parseInt(watuProps.getProperty("开图完成"));
        int dutuwancheng = Integer.parseInt(watuProps.getProperty("读图完成"));

        BagMapInfo bagMapInfo;
        int gezishu, x, y;
        String scence, pos;

        KaituTask kaituTask = new KaituTask();
        DutuTask dutuTask = new DutuTask();

        // 关键修复：在执行前检查线程状态
        if (thread.isStopped() || Thread.currentThread().isInterrupted()) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "任务在开始阶段被停止");
            return;
        }

        if (kaituwancheng == 0) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "执行开图任务");
            kaituTask.start(context, thread);
            // 检查是否被停止
            if (thread.isStopped() || Thread.currentThread().isInterrupted()) {
                TaskStepNotifier.notifyStep(context.getDeviceId(), "开图任务被停止");
                return;
            }
            configLoader.setProperty(SECTION_WATU, "开图完成", "1");
            configLoader.save();
        }

        // 修复：使用 TaskThread.sleep 而不是 Thread.sleep
        TaskThread.sleep(new Random().nextInt(400) + 300);

        if (thread.isStopped() || Thread.currentThread().isInterrupted()) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "任务在读图前被停止");
            return;
        }

        if (dutuwancheng == 0) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "执行读图任务");
            dutuTask.start(context, thread);
            if (thread.isStopped() || Thread.currentThread().isInterrupted()) {
                TaskStepNotifier.notifyStep(context.getDeviceId(), "读图任务被停止");
                return;
            }
        }

        TaskThread.sleep(new Random().nextInt(400) + 300);

        QutuTask qutuTask = new QutuTask();
        qutuTask.initConfig(context);

        watuProps = configLoader.getSection(SECTION_WATU);
        int baotuzongshu = Integer.parseInt(watuProps.getProperty(KEY_TOTALSHU) == null ? String.valueOf(0) : watuProps.getProperty(KEY_TOTALSHU));
        int yiwashu = Integer.parseInt(watuProps.getProperty(KEY_YIWASHU) == null ? String.valueOf(0) : watuProps.getProperty(KEY_YIWASHU));
        int shutieLevel = Integer.parseInt(watuProps.getProperty("书铁等级"));

        if (baotuzongshu == 0 && yiwashu == 0) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "宝图总数和已挖数都为0，跳过挖图");
        }

        // 主循环 - 增强停止检查
        while ((qutuTask.loadFirstTreasureMap() != null || loadFirstBagTreasureMap() != null)
                && (!thread.isStopped() && !Thread.currentThread().isInterrupted())) {

            // 循环开始时立即检查
            if (thread.isStopped() || Thread.currentThread().isInterrupted()) {
                TaskStepNotifier.notifyStep(context.getDeviceId(), "挖图主循环被停止");
                break;
            }
            thread.checkPause();

            //判断摄妖香到期时间，并使用
            commonActions.useXiangyaoxiang();

            //处理背包物资
            dealBagwuzi(shutieLevel, thread);

            // 再次检查停止状态
            if (thread.isStopped() || Thread.currentThread().isInterrupted()) {
                TaskStepNotifier.notifyStep(context.getDeviceId(), "处理背包物资后任务被停止");
                break;
            }

            bagMapInfo = loadFirstBagTreasureMap();
            if (bagMapInfo == null) {
                TaskStepNotifier.notifyStep(context.getDeviceId(), "背包中没有宝图，执行取图任务");
                qutuTask.start(context, thread);
                // 检查取图任务是否被停止
                if (thread.isStopped() || Thread.currentThread().isInterrupted()) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "取图任务被停止");
                    break;
                }
                // 强制重新加载配置文件，确保获取最新数据
                watuProps = configLoader.getSectionReload(SECTION_WATU);
                TaskStepNotifier.notifyStep(context.getDeviceId(), "重新加载配置文件，获取最新数据");
            }

            bagMapInfo = loadFirstBagTreasureMap();
            while (bagMapInfo != null) {
                if (thread.isStopped() || Thread.currentThread().isInterrupted()) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "挖图循环被停止");
                    return;
                }

                gezishu = bagMapInfo.getBagSlot();
                scence = bagMapInfo.getScene();
                x = bagMapInfo.getX();
                y = bagMapInfo.getY();
                pos = x + "," + y;

                TaskStepNotifier.notifyStep(context.getDeviceId(),
                        String.format("开始挖图：格子%d，场景%s，坐标(%d,%d)", gezishu, scence, x, y));

                luxian.toScene(scence, pos);

                // 检查是否在路线执行过程中被停止
                if (thread.isStopped() || Thread.currentThread().isInterrupted()) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "路线执行过程中被停止");
                    return;
                }

                commonActions.openBag();
                TaskThread.sleep(new Random().nextInt(200) + 300);
                commonActions.doubleclickBagGrid(context.getDeviceId(), gezishu - 1);//使用藏宝图
                watuProps = configLoader.getSection(SECTION_WATU);
                TaskThread.sleep(new Random().nextInt(200) + 300);

                removeBagMapBySlot(bagMapInfo);

                TaskThread.sleep(new Random().nextInt(200) + 300);
                commonActions.closeBag();
                TaskThread.sleep(new Random().nextInt(200) + 300);

                if (gameStateDetector.isChuxianpiaofuzi()){
                    //截图，裁剪，
                   String screenshotbase64= DeviceHttpClient.getScreenshotBase64(context.getDeviceId());
                   String imgbase64=commonActions.cropImage(screenshotbase64,1,1,1,1);
                    commonActions.piaofuzi(imgbase64);
                }

                // 战斗循环 - 增强停止检查
                while (gameStateDetector.isInBattle()) {
                    if (thread.isStopped() || Thread.currentThread().isInterrupted()) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(), "战斗过程中被停止");
                        return;
                    }

                    TaskStepNotifier.notifyStep(context.getDeviceId(), "正在战斗中...");
                    int[] poszhandou = DeviceHttpClient.findImages(context.getDeviceId(), "自动战斗", "自动战斗", "取消自动", "自动战斗", 0.8);

                    if (poszhandou[0] > 0) {
                        HumanLikeController humanLikeController = new HumanLikeController(thread);
                        humanLikeController.click(context.getDeviceId(), poszhandou[0], poszhandou[1], 5, 5);
                    }
                    TaskThread.sleep(new Random().nextInt(200) + 300);
                }

                // 血量检查和恢复
                if (gameStateDetector.isPlayerHpLow()) {
                    commonActions.huifuHP();
                }
                if (gameStateDetector.isPlayerMpLow()) {
                    commonActions.huifuMP();
                }
                if (gameStateDetector.isPetHpLow()) {
                    commonActions.huifuPetHP();
                }
                if (gameStateDetector.isPetMpLow()) {
                    commonActions.huifuPetMP();
                }

                bagMapInfo = loadFirstBagTreasureMap();
                yiwashu = 1 + Integer.parseInt(watuProps.getProperty(KEY_YIWASHU));
                configLoader.setProperty(SECTION_WATU, "已挖图数", String.valueOf(yiwashu));
                configLoader.save();
                TaskThread.sleep(new Random().nextInt(200) + 300);
                // 强制重新加载配置文件，确保获取最新数据
                watuProps = configLoader.getSectionReload(SECTION_WATU);
            }
        }

        TaskStepNotifier.notifyStep(context.getDeviceId(), "挖图任务完成");
    }

    private void dealBagwuzi(int shutieLevel, TaskThread thread) {
        GameStateDetector detector = new GameStateDetector(context, new DeviceHttpClient());
        CommonActions commonActions = new CommonActions(context, thread);
        HumanLikeController humanLikeController = new HumanLikeController(thread);
        try {
            // 增强停止检查
            if (thread != null && (thread.isStopped() || thread.isInterrupted())) {
                TaskStepNotifier.notifyStep(context.getDeviceId(), "处理背包物资被停止");
                return;
            }

            if (!detector.isBagOpen()) {
                commonActions.openBag();
            }
            List<Integer> gezi = commonActions.findBagItemIndex(context.getDeviceId(), "书", 0.8);
            if (gezi == null || gezi.isEmpty()) {
                TaskStepNotifier.notifyStep(context.getDeviceId(), "没有书");
            } else {
                for (int index : gezi) {
                    if (thread != null && (thread.isStopped() || thread.isInterrupted())) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(), "线程已停止或暂停，终止点击");
                        break;
                    }
                    commonActions.clickBagGrid(context.getDeviceId(), index);
                    TaskThread.sleep(new Random().nextInt(400) + 500);
                    int[] imgdengji = DeviceHttpClient.findImage(context.getDeviceId(), "等级", 0.8);
                    int[] rect = {imgdengji[0] + 15, imgdengji[1] - 8, imgdengji[0] + 43, imgdengji[1] + 9};
                    String ocrshutieLevel = DeviceHttpClient.ocr(context.getDeviceId(), rect);
                    if (Integer.parseInt(ocrshutieLevel) <= shutieLevel) {
                        //执行丢弃
                        int[] posdiuqi = DeviceHttpClient.findImage(context.getDeviceId(), "丢弃", 0.8);
                        if (posdiuqi[0] > 0) {
                            humanLikeController.click(context.getDeviceId(), posdiuqi[0], posdiuqi[1], 5, 5);
                            TaskThread.sleep(new Random().nextInt(100) + 500);
                            int[] shi = DeviceHttpClient.findImage(context.getDeviceId(), "是", 0.8);
                            if (shi[0] > 0) {
                                humanLikeController.click(context.getDeviceId(), shi[0], shi[1], 10, 5);
                                TaskThread.sleep(new Random().nextInt(100) + 500);
                            }
                        }
                        TaskThread.sleep(new Random().nextInt(400) + 300);
                    }
                }
            }

            List<Integer> gezitie = commonActions.findBagItemIndex(context.getDeviceId(), "铁", 0.8);
            if (gezitie == null || gezitie.isEmpty()) {
                TaskStepNotifier.notifyStep(context.getDeviceId(), "没有铁");
            } else {
                for (int index : gezitie) {
                    if (thread != null && (thread.isStopped() || thread.isInterrupted())) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(), "线程已停止或暂停，终止点击");
                        break;
                    }
                    commonActions.clickBagGrid(context.getDeviceId(), index);
                    TaskThread.sleep(new Random().nextInt(400) + 500);
                    int[] imgdengji = DeviceHttpClient.findImage(context.getDeviceId(), "等级", 0.8);
                    int[] rect = {imgdengji[0] + 15, imgdengji[1] - 8, imgdengji[0] + 43, imgdengji[1] + 9};
                    String ocrshutieLevel = DeviceHttpClient.ocr(context.getDeviceId(), rect);
                    if (Integer.parseInt(ocrshutieLevel) <= shutieLevel) {
                        //执行丢弃
                        int[] posdiuqi = DeviceHttpClient.findImage(context.getDeviceId(), "丢弃", 0.8);
                        if (posdiuqi[0] > 0) {
                            humanLikeController.click(context.getDeviceId(), posdiuqi[0], posdiuqi[1], 5, 5);
                            TaskThread.sleep(new Random().nextInt(100) + 500);
                            int[] shi = DeviceHttpClient.findImage(context.getDeviceId(), "是", 0.8);
                            if (shi[0] > 0) {
                                humanLikeController.click(context.getDeviceId(), shi[0], shi[1], 10, 5);
                                TaskThread.sleep(new Random().nextInt(100) + 500);
                            }
                        }
                        TaskThread.sleep(new Random().nextInt(400) + 300);
                    }
                    else commonActions.closeBag();
                }
            }
            if (detector.isBagOpen()){commonActions.closeBag();}

            commonActions.openJianyeCangku();
            //打开仓库以后存物资
            commonActions.transferBagItemsToWarehouse();

        } catch (Exception e) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "处理背包物资异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return "挖图任务";
    }

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