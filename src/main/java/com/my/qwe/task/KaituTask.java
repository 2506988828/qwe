package com.my.qwe.task;

import com.my.qwe.controller.HumanLikeController;
import com.my.qwe.http.DeviceHttpClient;
import com.my.qwe.task.config.IniConfigLoader;
import java.io.IOException;
import java.util.*;

public class KaituTask implements ITask {

    private String treasureMapImg = "藏宝图"; // 藏宝图图片路径
    private int waittime = new Random().nextInt(200) + 300; // 随机延迟
    private final int[] OCR_RECT = {412, 210, 514, 229}; // OCR识别区域
    private final String TARGET_KEYWORDS = "右键使用后显示"; // 目标关键词

    // 状态控制变量
    private boolean isStoringToWarehouse = false; // 标记是否正在执行"背包存仓库"操作

    @Override
    public void start(TaskContext context, TaskThread thread) {
        CommonActions common = new CommonActions(context, thread);
        TaskStepNotifier.notifyStep(context.getDeviceId(), "===== 开始开图任务 =====");

        boolean isWarehouseOpened = false; // 仓库是否已打开
        int remainingEmptySlots = 0; // 背包剩余空格数

        try {
            while (!thread.isStopped() && !Thread.currentThread().isInterrupted()) {
                thread.checkPause();

                // 步骤1：打开仓库
                if (!openWarehouse(context, thread, common)) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "仓库无法打开，任务终止");
                    return;
                }

                // 首次打开仓库或存完后，重新识别背包空格
                if (!isWarehouseOpened) {
                    remainingEmptySlots = common.findcangkujiemianEmptyBagIndices(context.getDeviceId()).size();
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "仓库打开，识别背包空格：" + remainingEmptySlots);
                    isWarehouseOpened = true;
                }

                // 步骤2：处理存背包状态
                if (isStoringToWarehouse) {
                    storeUsedTreasureToWarehouse(context, thread, common);
                    isStoringToWarehouse = false;

                    // 存完后重新识别背包空格
                    remainingEmptySlots = common.findcangkujiemianEmptyBagIndices(context.getDeviceId()).size();
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "存图后背包空格：" + remainingEmptySlots);
                    continue;
                }

                // 步骤3：检查背包是否满
                if (remainingEmptySlots <= 0) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "背包已满，强制处理");
                    isStoringToWarehouse = true;
                    processFullBag(context, thread, common);
                    continue;
                }

                // 步骤4：正常取图流程（获取实际消耗的空格数）
                int slotsConsumed = processCurrentWarehousePage(
                        context, thread, common, remainingEmptySlots
                );
                remainingEmptySlots -= slotsConsumed;
                TaskStepNotifier.notifyStep(context.getDeviceId(), "当前页取图后剩余空格：" + remainingEmptySlots);

                // 步骤5：翻页逻辑
                if (slotsConsumed == 0) { // 当前页无有效宝图
                    if (common.canPageDown()) {
                        common.pageDown();
                        TaskStepNotifier.notifyStep(context.getDeviceId(), "翻至下一页");
                        Thread.sleep(waittime + 500);
                    } else {
                        // 无下一页且背包有图→处理背包；无图→任务结束
                        if (isBagHasUnusedTreasure(context, common)) {
                            isStoringToWarehouse = true;
                            processFullBag(context, thread, common);
                        } else {
                            TaskStepNotifier.notifyStep(context.getDeviceId(), "所有藏宝图处理完毕，任务结束");
                            break;
                        }
                    }
                }
            }

            // 标记任务完成
            IniConfigLoader.setTaskConfig(context.getDeviceName(), "开图", "是否完成开图", "1");
            TaskStepNotifier.notifyStep(context.getDeviceId(), "===== 开图任务完成 =====");

        } catch (Exception e) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "任务异常终止：" + e.getMessage());
            e.printStackTrace();
        } finally {
            isStoringToWarehouse = false;
        }
    }

    /**
     * 处理当前仓库页的藏宝图，返回实际消耗的空格数
     */
    private int processCurrentWarehousePage(
            TaskContext context, TaskThread thread, CommonActions common, int remainingEmptySlots
    ) throws IOException, InterruptedException {

        List<Integer> warehouseTreasureIndices = common.findcangkuAllItemIndices(
                context.getDeviceId(), treasureMapImg, 0.8
        );
        if (warehouseTreasureIndices.isEmpty()) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "当前仓库页无藏宝图");
            return 0;
        }

        int consumedSlots = 0; // 记录实际消耗的空格数
        int totalTreasures = warehouseTreasureIndices.size();

        for (int i = 0; i < totalTreasures; i++) {
            int warehouseIndex = warehouseTreasureIndices.get(i);
            if (thread.isStopped() || isStoringToWarehouse) break;
            thread.checkPause();

            if (remainingEmptySlots <= 0) {
                TaskStepNotifier.notifyStep(context.getDeviceId(), "背包已满，准备存回仓库");
                isStoringToWarehouse = true;
                processFullBag(context, thread, common);
                return consumedSlots;
            }

            // 检查并取出宝图
            common.clickCangkuGrid(context.getDeviceId(), warehouseIndex);
            Thread.sleep(waittime);

            String ocrResult = DeviceHttpClient.ocr(context.getDeviceId(), OCR_RECT);
            if (isContainTargetChar(ocrResult)) {
                common.doubleclickCangkuGrid(context.getDeviceId(), warehouseIndex);
                TaskStepNotifier.notifyStep(context.getDeviceId(),
                        "取出仓库格子[" + warehouseIndex + "]的藏宝图（本页进度：" + (i+1) + "/" + totalTreasures + "）");
                remainingEmptySlots--;
                consumedSlots++;
                Thread.sleep(waittime);
            }
        }

        return consumedSlots;
    }

    /**
     * 处理满背包：关闭仓库→使用→存回→重新打开仓库
     */
    private void processFullBag(TaskContext context, TaskThread thread, CommonActions common) throws IOException, InterruptedException {
        // 关闭仓库→打开背包→使用所有藏宝图
        common.closeWarehouse();
        common.openBag();

        List<Integer> bagTreasureIndices = common.findAllItemIndices(context.getDeviceId(), treasureMapImg, 0.8);
        for (int bagIndex : bagTreasureIndices) {
            if (thread.isStopped()) break;
            common.doubleclickBagGrid(context.getDeviceId(), bagIndex);
            TaskStepNotifier.notifyStep(context.getDeviceId(), "使用背包格子[" + bagIndex + "]的藏宝图");
            Thread.sleep(waittime + new Random().nextInt(300));
        }

        common.closeBag();
        Thread.sleep(waittime);

        // 重新打开仓库（准备存回）
        openWarehouse(context, thread, common);
    }

    /**
     * 将使用后的藏宝图存回仓库（使用总页数限制翻页）
     */
    private void storeUsedTreasureToWarehouse(TaskContext context, TaskThread thread, CommonActions common) throws IOException, InterruptedException {
        String deviceId = context.getDeviceId();
        TaskStepNotifier.notifyStep(deviceId, "===== 开始存回使用后的藏宝图 =====");

        // 获取仓库总页数
        int totalPages = common.getWarehouseTotalPages();
        TaskStepNotifier.notifyStep(deviceId, "仓库总页数: " + totalPages);

        // 获取背包中待存的藏宝图
        List<Integer> usedTreasureIndices = common.findcangkujiemianAllItemIndices(
                deviceId, treasureMapImg, 0.8
        );

        if (usedTreasureIndices.isEmpty()) {
            TaskStepNotifier.notifyStep(deviceId, "背包中无待存的藏宝图");
            return;
        }

        TaskStepNotifier.notifyStep(deviceId, "待存藏宝图数量：" + usedTreasureIndices.size());

        // 逐个存入仓库
        for (int bagIndex : usedTreasureIndices) {
            if (thread.isStopped()) break;
            thread.checkPause();

            // 寻找仓库空格子（使用总页数作为限制）
            int warehouseIndex = -1;
            int currentPage = 1; // 当前页码

            while (warehouseIndex == -1 && currentPage <= totalPages) {
                // 在当前页查找第一个空格子
                warehouseIndex = common.findFirstEmptyCangkuIndex(deviceId);
                if (warehouseIndex != -1) {
                    break; // 找到空格，退出循环
                }

                // 当前页无空格，尝试翻页
                if (currentPage < totalPages) {
                    common.pageDown();
                    currentPage++;
                    TaskStepNotifier.notifyStep(deviceId, "翻至第" + currentPage + "页（共" + totalPages + "页）");
                    Thread.sleep(waittime + 500);
                } else {
                    // 已到达最后一页，尝试返回第一页
                    TaskStepNotifier.notifyStep(deviceId, "已达最后一页，返回第一页重新查找");
                    while (common.canPageUp()) {
                        common.pageUp();
                        Thread.sleep(waittime);
                    }
                    currentPage = 1;
                    warehouseIndex = common.findFirstEmptyCangkuIndex(deviceId);
                    if (warehouseIndex == -1) {
                        TaskStepNotifier.notifyStep(deviceId, "仓库所有页面均无空格，存图失败");
                        return;
                    }
                }
            }

            // 执行存入操作
            try {
                common.doubleclickcangkujiemianBagGrid(deviceId, bagIndex);
                Thread.sleep(waittime);

                TaskStepNotifier.notifyStep(deviceId, "存入成功：仓库格子[" + warehouseIndex + "]");
            } catch (Exception e) {
                TaskStepNotifier.notifyStep(deviceId, "存入失败：背包格子[" + bagIndex + "] → 仓库格子[" + warehouseIndex + "]，错误：" + e.getMessage());
            }
        }

        TaskStepNotifier.notifyStep(deviceId, "所有藏宝图已存入仓库");
    }

    // 其他辅助方法（保持不变）
    private boolean openWarehouse(TaskContext context, TaskThread thread, CommonActions common) throws IOException, InterruptedException {
        int retry = 0;
        while (retry < 15) {
            if (common.ifOpenCangku()) return true;
            common.openJianyeCangku();
            Thread.sleep(1000);
            retry++;
        }
        return common.ifOpenCangku();
    }

    private boolean isContainTargetChar(String ocrResult) {
        if (ocrResult == null) return false;
        for (char c : TARGET_KEYWORDS.toCharArray()) {
            if (ocrResult.contains(String.valueOf(c))) return true;
        }
        return false;
    }

    private boolean isBagHasUnusedTreasure(TaskContext context, CommonActions common) throws IOException {
        return common.findcangkujiemianFirstItemIndex(context.getDeviceId(), treasureMapImg, 0.8) > -1;
    }

    @Override
    public String getName() {
        return "开图任务";
    }
}