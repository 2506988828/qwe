package com.my.qwe.task;

import com.my.qwe.http.DeviceHttpClient;
import com.my.qwe.task.config.IniConfigLoader;
import java.util.*;

public class DutuTask implements ITask {
    private String treasureMapImg = "藏宝图";
    private int waittime = new Random().nextInt(200) + 300;

    @Override
    public void start(TaskContext context, TaskThread thread) {
        CommonActions common = new CommonActions(context, thread);
        TaskStepNotifier.notifyStep(context.getDeviceId(), "===== 开始读图任务 =====");

        try {
            if (!common.ifOpenCangku()){
                common.openJianyeCangku();
            }
            readAndSaveWarehouseTreasures(context, thread, common);
            TaskThread.sleep(new Random().nextInt(400) + 300);
            if (common.ifOpenCangku()){
                common.closeWarehouse();
            }

            cangbaotupaixu(context,thread);


            TaskStepNotifier.notifyStep(context.getDeviceId(), "===== 读图任务完成 =====");
        } catch (Exception e) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "任务异常终止：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 重新排序配置文件中的藏宝图
     * */
    /**
     * 重新排序配置文件中的藏宝图
     * 按xy坐标分为4个区域（两行两列），按1234顺序重新排序
     */
     public void cangbaotupaixu(TaskContext context,TaskThread thread) {
        if (thread.isStopped()) return;
        TaskStepNotifier.notifyStep(null, "开始对藏宝图进行分区排序");

        try {
            // 获取设备名称（从线程上下文或配置中获取，这里假设你的TaskThread有getDeviceName方法）
            String deviceName = context.getDeviceName(); // 根据实际情况调整获取方式

            // 加载现有配置
            Map<String, Object> existingData = loadExistingDigMapConfig(deviceName);
            Map<String, String> baseInfo = (Map<String, String>) existingData.get("baseInfo");
            Map<String, List<TreasureInfo>> warehouseMap = (Map<String, List<TreasureInfo>>) existingData.get("warehouse");

            // 对每个区域的宝图进行排序
            for (Map.Entry<String, List<TreasureInfo>> entry : warehouseMap.entrySet()) {
                String area = entry.getKey();
                List<TreasureInfo> treasures = entry.getValue();

                if (treasures.size() <= 1) {
                    // 单个宝图无需排序
                    continue;
                }

                // 计算当前区域所有宝图的x、y坐标极值
                int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
                int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

                for (TreasureInfo treasure : treasures) {
                    minX = Math.min(minX, treasure.getX());
                    maxX = Math.max(maxX, treasure.getX());
                    minY = Math.min(minY, treasure.getY());
                    maxY = Math.max(maxY, treasure.getY());
                }

                // 计算中间值（分区边界）
                int midX = (minX + maxX) / 2;
                int midY = (minY + maxY) / 2;

                TaskStepNotifier.notifyStep(null,
                        area + " 坐标范围：X[" + minX + "-" + maxX + "], Y[" + minY + "-" + maxY +
                                "]，中间值：(" + midX + "," + midY + ")");

                // 按区域编号排序（1-4）
                Collections.sort(treasures, (t1, t2) -> {
                    int region1 = getRegion(t1.getX(), t1.getY(), midX, midY);
                    int region2 = getRegion(t2.getX(), t2.getY(), midX, midY);

                    // 先按区域编号排序
                    if (region1 != region2) {
                        return Integer.compare(region1, region2);
                    }

                    // 同一区域内按x坐标排序，x相同则按y坐标
                    if (t1.getX() != t2.getX()) {
                        return Integer.compare(t1.getX(), t2.getX());
                    }
                    return Integer.compare(t1.getY(), t2.getY());
                });

                TaskStepNotifier.notifyStep(null,
                        area + " 宝图排序完成，共" + treasures.size() + "个，分为4个区域");
            }

            // 保存排序后的配置
            saveDigMapConfig(deviceName, baseInfo, warehouseMap);
            TaskStepNotifier.notifyStep(null, "藏宝图排序完成并已保存配置");

        } catch (Exception e) {
            TaskStepNotifier.notifyStep(null, "藏宝图排序失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 确定坐标所在的区域（1-4）
     * 1: 左上区域（x <= midX 且 y <= midY）
     * 2: 右上区域（x > midX 且 y <= midY）
     * 3: 左下区域（x <= midX 且 y > midY）
     * 4: 右下区域（x > midX 且 y > midY）
     */
    private int getRegion(int x, int y, int midX, int midY) {
        if (x <= midX) {
            return y <= midY ? 1 : 3;
        } else {
            return y <= midY ? 2 : 4;
        }
    }

    /**
     * 读取并保存仓库宝图（改进坐标识别逻辑）
     */
    private void readAndSaveWarehouseTreasures(TaskContext context, TaskThread thread, CommonActions common) throws Exception {
        String deviceId = context.getDeviceId();
        String deviceName = context.getDeviceName();
        TaskStepNotifier.notifyStep(deviceId, "开始读取仓库中的藏宝图");

        if (!openWarehouse(context, thread, common)) {
            TaskStepNotifier.notifyStep(deviceId, "无法打开仓库，终止任务");
            return;
        }

        Map<String, Object> existingData = loadExistingDigMapConfig(deviceName);
        Map<String, String> baseInfo = (Map<String, String>) existingData.get("baseInfo");
        Map<String, List<TreasureInfo>> warehouseMap = (Map<String, List<TreasureInfo>>) existingData.get("warehouse");

        int currentTotal = parseTotalTreasures(baseInfo.get("宝图总数"));
        int totalPages = common.getWarehouseTotalPages();
        int newCount = 0;
        int duplicateCount = 0;
        int failedCount = 0;

        for (int page = 1; page <= totalPages; page++) {
            TaskStepNotifier.notifyStep(deviceId, "处理第 " + page + " 页（共" + totalPages + "页）");

            List<Integer> treasureIndices = common.findcangkuAllItemIndices(deviceId, treasureMapImg, 0.8);
            if (treasureIndices.isEmpty()) {
                if (page < totalPages) common.pageDown();
                continue;
            }

            for (int index : treasureIndices) {
                if (thread.isStopped()) return;
                thread.checkPause();

                // 格子索引从1开始
                int gridIndex = index + 1;

                // 单击宝图
                common.clickCangkuGrid(deviceId, index);
                Thread.sleep(new Random().nextInt(200) + 300);

                // 第一步：识别地区
                String area = common.recognizeMapArea(context);
                if (!isValidArea(area)) {
                    failedCount++;
                    TaskStepNotifier.notifyStep(deviceId,
                            "格子" + gridIndex + "地区识别失败，跳过当前宝图");
                    continue;
                }

                // 地区识别成功，等待500-800毫秒
                int waitTime = 1000;
                /*TaskStepNotifier.notifyStep(deviceId,
                        "格子" + gridIndex + "地区识别成功：" + area + "，等待" + waitTime + "毫秒后继续");
                Thread.sleep(waitTime);*/

                // 第二步：多次识别坐标并验证
                CoordinateResult coordinateResult = recognizeCoordinatesWithRetry(context, common, gridIndex,thread);
                if (!coordinateResult.isValid()) {
                    failedCount++;
                    TaskStepNotifier.notifyStep(deviceId,
                            "格子" + gridIndex + "坐标识别失败（多次尝试不一致），跳过当前宝图");
                    continue;
                }

                // 创建宝图信息
                TreasureInfo treasure = new TreasureInfo(area, coordinateResult.getX(), coordinateResult.getY());
                treasure.setPage(page);
                treasure.setWarehouseIndex(gridIndex);

                // 检查重复
                if (isDuplicate(warehouseMap, treasure)) {
                    duplicateCount++;
                    TaskStepNotifier.notifyStep(deviceId,
                            "警告：" + treasure.getArea() + "(" + treasure.getX() + "," + treasure.getY() +
                                    ") 已存在（第" + page + "页，格子" + gridIndex + ")");
                    continue;
                }

                // 新增宝图
                currentTotal++;
                newCount++;
                warehouseMap.computeIfAbsent(treasure.getArea(), k -> new ArrayList<>()).add(treasure);
                baseInfo.put("宝图总数", String.valueOf(currentTotal));

                // 保存配置
                saveDigMapConfig(deviceName, baseInfo, warehouseMap);
                TaskStepNotifier.notifyStep(deviceId,
                        "识别成功（新增第" + currentTotal + "张）：" +
                                treasure.getArea() + "(" + treasure.getX() + "," + treasure.getY() +
                                ")，位于第" + page + "页，格子" + gridIndex);
            }

            if (page < totalPages) common.pageDown();
        }
        IniConfigLoader config = new IniConfigLoader(deviceName);
        config.setProperty("挖图","读图完成","1");
        config.save();
        TaskStepNotifier.notifyStep(deviceId,
                "读图完成：新增" + newCount + "个，跳过" + duplicateCount +
                        "个重复项，失败" + failedCount + "个，最终总数：" + currentTotal);
    }

    /**
     * 多次识别坐标并验证结果一致性（动态调整识别区域）
     */
    private CoordinateResult recognizeCoordinatesWithRetry(TaskContext context, CommonActions common, int gridIndex, TaskThread thread) throws Exception {
        String deviceId = context.getDeviceId();
        int maxAttempts = 4; // 最多识别4次
        int[] lastValidCoord = null; // 上一次有效的坐标结果
        int[] currentRect = {457, 215, 534, 227}; // 初始识别区域

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            int a=new Random().nextInt(3)+1;
            // 本次识别使用当前区域
            TaskStepNotifier.notifyStep(deviceId,
                    "格子" + gridIndex + "第" + attempt + "次识别，区域：" +
                            Arrays.toString(currentRect));

            // 调用带区域参数的识别方法
            int[] coordinates = common.recognizeCoordinates(context, thread, currentRect);

            // 校验坐标有效性
            if (!isValidCoordinates(coordinates)) {
                TaskStepNotifier.notifyStep(deviceId,
                        "格子" + gridIndex + "第" + attempt + "次坐标无效");
            } else {
                // 第一次有效结果，暂存
                if (lastValidCoord == null) {
                    lastValidCoord = coordinates;
                    TaskStepNotifier.notifyStep(deviceId,
                            "格子" + gridIndex + "第" + attempt + "次有效结果：" +
                                    coordinates[0] + "," + coordinates[1]);
                } else {
                    // 对比当前结果与上一次
                    if (Arrays.equals(coordinates, lastValidCoord)) {
                        TaskStepNotifier.notifyStep(deviceId,
                                "格子" + gridIndex + "连续两次结果一致：" +
                                        coordinates[0] + "," + coordinates[1] + "，返回结果");
                        return new CoordinateResult(true, coordinates[0], coordinates[1]);
                    } else {
                        TaskStepNotifier.notifyStep(deviceId,
                                "格子" + gridIndex + "结果不一致（上次：" +
                                        lastValidCoord[0] + "," + lastValidCoord[1] +
                                        "，本次：" + coordinates[0] + "," + coordinates[1] + "）");
                        lastValidCoord = coordinates; // 更新上次结果
                    }
                }
            }

            // 准备下一次的识别区域（后两位+2）
            if (attempt < maxAttempts) {
                currentRect = new int[]{
                        currentRect[0]-a,
                        currentRect[1]-new Random().nextInt(3),
                        currentRect[2] + a, // 宽度+2
                        currentRect[3] + new Random().nextInt(3)  // 高度+2
                };
                Thread.sleep(new Random().nextInt(200) + 300); // 等待后继续
            }
        }

        // 达到最大次数仍未得到一致结果
        TaskStepNotifier.notifyStep(deviceId,
                "格子" + gridIndex + "达到最大尝试次数（" + maxAttempts + "次），未获一致结果");
        return new CoordinateResult(false, 0, 0);
    }

    /**
     * 校验地区有效性
     */
    private boolean isValidArea(String area) {
        return area != null && !area.trim().isEmpty() && !area.equals("未知区域");
    }

    /**
     * 校验坐标有效性
     */
    private boolean isValidCoordinates(int[] coordinates) {
        return coordinates != null && coordinates.length == 2
                && coordinates[0] > 0 && coordinates[0] < 1000
                && coordinates[1] > 0 && coordinates[1] < 1000;
    }

    /**
     * 解析宝图总数
     */
    private int parseTotalTreasures(String totalStr) {
        try {
            return Integer.parseInt(totalStr);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 加载现有[挖图]配置
     */
    private Map<String, Object> loadExistingDigMapConfig(String deviceName) {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> baseInfo = new HashMap<>();
        Map<String, List<TreasureInfo>> warehouseMap = new HashMap<>();

        try {
            IniConfigLoader config = new IniConfigLoader(deviceName);
            Properties props = config.getSection("挖图");

            if (props != null) {
                for (String key : props.stringPropertyNames()) {
                    String value = props.getProperty(key);

                    if (key.equals("描述") || key.equals("宝图总数") || key.equals("已挖图数")) {
                        baseInfo.put(key, value);
                    } else if (key.equals("背包")) {
                        baseInfo.put(key, value);
                    } else {
                        String[] entries = value.split("\\|");
                        for (String entry : entries) {
                            String[] parts = entry.split(",");
                            if (parts.length >= 5) {
                                try {
                                    int page = Integer.parseInt(parts[0]);
                                    int index = Integer.parseInt(parts[1]);
                                    String area = parts[2];
                                    int x = Integer.parseInt(parts[3]);
                                    int y = Integer.parseInt(parts[4]);

                                    TreasureInfo info = new TreasureInfo(area, x, y);
                                    info.setPage(page);
                                    info.setWarehouseIndex(index);
                                    warehouseMap.computeIfAbsent(area, k -> new ArrayList<>()).add(info);
                                } catch (NumberFormatException e) {
                                    TaskStepNotifier.notifyStep(null, "解析失败：" + entry);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            TaskStepNotifier.notifyStep(null, "加载配置失败：" + e.getMessage());
        }

        // 设置默认值
        if (!baseInfo.containsKey("描述")) baseInfo.put("描述", "无其他配置");
        if (!baseInfo.containsKey("宝图总数")) baseInfo.put("宝图总数", "0");
        if (!baseInfo.containsKey("已挖图数")) baseInfo.put("已挖图数", "0");
        if (!baseInfo.containsKey("背包")) baseInfo.put("背包", "");

        result.put("baseInfo", baseInfo);
        result.put("warehouse", warehouseMap);
        return result;
    }

    /**
     * 保存数据到[挖图]区块
     */
    private void saveDigMapConfig(String deviceName, Map<String, String> baseInfo,
                                  Map<String, List<TreasureInfo>> warehouseMap) {
        try {
            IniConfigLoader config = new IniConfigLoader(deviceName);

            // 写入基础信息
            config.setProperty("挖图", "描述", baseInfo.get("描述"));
            config.setProperty("挖图", "宝图总数", String.valueOf(calculateTotalTreasures(warehouseMap)));
            config.setProperty("挖图", "已挖图数", baseInfo.get("已挖图数"));
            config.setProperty("挖图", "背包", baseInfo.get("背包"));

            // 写入仓库宝图
            for (Map.Entry<String, List<TreasureInfo>> entry : warehouseMap.entrySet()) {
                String area = entry.getKey();
                StringBuilder sb = new StringBuilder();

                for (TreasureInfo t : entry.getValue()) {
                    if (sb.length() > 0) sb.append("|");
                    sb.append(t.getPage()).append(",")
                            .append(t.getWarehouseIndex()).append(",")
                            .append(t.getArea()).append(",")
                            .append(t.getX()).append(",")
                            .append(t.getY());
                }

                config.setProperty("挖图", area, sb.toString());
            }

            config.save();
        } catch (Exception e) {
            TaskStepNotifier.notifyStep(null, "保存配置失败：" + e.getMessage());
        }
    }

    /**
     * 计算宝图总数
     */
    private int calculateTotalTreasures(Map<String, List<TreasureInfo>> warehouseMap) {
        return warehouseMap.values().stream().mapToInt(List::size).sum();
    }

    /**
     * 检查仓库宝图是否重复
     */
    private boolean isDuplicate(Map<String, List<TreasureInfo>> warehouseMap, TreasureInfo newTreasure) {
        List<TreasureInfo> areaTreasures = warehouseMap.get(newTreasure.getArea());
        if (areaTreasures == null) return false;

        for (TreasureInfo existing : areaTreasures) {
            if (existing.getX() == newTreasure.getX()
                    && existing.getY() == newTreasure.getY()
                    && existing.getPage() == newTreasure.getPage()
                    && existing.getWarehouseIndex() == newTreasure.getWarehouseIndex()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 打开仓库
     */
    private boolean openWarehouse(TaskContext context, TaskThread thread, CommonActions common) throws Exception {
        String deviceId = context.getDeviceId();
        int retry = 0;

        while (retry < 15) {
            if (common.ifOpenCangku()) return true;
            common.openJianyeCangku();
            Thread.sleep(1000);
            retry++;
        }

        return common.ifOpenCangku();
    }

    @Override
    public String getName() {
        return "读图";
    }

    /**
     * 宝图信息模型
     */
    private static class TreasureInfo {
        private final String area;
        private final int x;
        private final int y;
        private int page;
        private int warehouseIndex;

        public TreasureInfo(String area, int x, int y) {
            this.area = area;
            this.x = x;
            this.y = y;
        }

        public String getArea() { return area; }
        public int getX() { return x; }
        public int getY() { return y; }
        public int getPage() { return page; }
        public int getWarehouseIndex() { return warehouseIndex; }
        public void setPage(int page) { this.page = page; }
        public void setWarehouseIndex(int index) { this.warehouseIndex = index; }

        @Override
        public String toString() {
            return area + "(" + x + "," + y + ")";
        }
    }
}

/**
 * 坐标识别结果类
 */
 class CoordinateResult {
    private final boolean valid;
    private final int x;
    private final int y;

    public CoordinateResult(boolean valid, int x, int y) {
        this.valid = valid;
        this.x = x;
        this.y = y;
    }

    public boolean isValid() { return valid; }
    public int getX() { return x; }
    public int getY() { return y; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CoordinateResult that = (CoordinateResult) o;
        return x == that.x && y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}