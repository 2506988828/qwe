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
    private List<Integer> currentWarehouseEmptySlots; // 仓库空格子缓存


    @Override
    public void start(TaskContext context, TaskThread thread) {
        CommonActions common = new CommonActions(context, thread);
        TaskStepNotifier.notifyStep(context.getDeviceId(), "===== 开始开图任务 =====");

        try {
            // 主循环：处理仓库藏宝图→使用背包→存回仓库
            while (!thread.isStopped() && !Thread.currentThread().isInterrupted()) {
                thread.checkPause();

                // 步骤1：打开仓库
                if (!openWarehouse(context, thread, common)) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "仓库无法打开，任务终止");
                    return;
                }

                // 步骤2：如果正在存背包，跳过取图流程，专注存完
                if (isStoringToWarehouse) {
                    storeUsedTreasureToWarehouse(context, thread, common);
                    // 存完后重置状态，重新获取背包空格并开始取图
                    isStoringToWarehouse = false;
                    continue;
                }

                // 步骤3：获取背包空格（仅在取图阶段初始获取）
                List<Integer> emptyBagSlots = common.findcangkujiemianEmptyBagIndices(context.getDeviceId());
                int remainingEmptySlots = emptyBagSlots.size();
                TaskStepNotifier.notifyStep(context.getDeviceId(), "背包空格子数量：" + remainingEmptySlots);

                if (remainingEmptySlots <= 0) {
                    // 背包满，进入"存背包"流程（设置状态标记）
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "背包已满，开始使用并存回仓库");
                    isStoringToWarehouse = true;
                    processFullBag(context, thread, common);
                    continue;
                }

                // 步骤4：遍历当前仓库页取图（仅在非存背包阶段执行）
                boolean currentPageHasValid = processCurrentWarehousePage(
                        context, thread, common, remainingEmptySlots
                );

                // 步骤5：当前页无有效藏宝图则翻页
                if (!currentPageHasValid) {
                    if (common.canPageDown()) {
                        common.pageDown();
                        TaskStepNotifier.notifyStep(context.getDeviceId(), "当前页无有效藏宝图，翻至下一页");
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
        }
    }


    /**
     * 处理当前仓库页的藏宝图（仅在非存背包阶段执行）
     */
    private boolean processCurrentWarehousePage(
            TaskContext context, TaskThread thread, CommonActions common, int remainingEmptySlots
    ) throws IOException, InterruptedException {

        List<Integer> warehouseTreasureIndices = common.findcangkuAllItemIndices(
                context.getDeviceId(), treasureMapImg, 0.8
        );
        if (warehouseTreasureIndices.isEmpty()) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "当前仓库页无藏宝图");
            return false;
        }

        boolean currentPageHasValid = false;

        for (int warehouseIndex : warehouseTreasureIndices) {
            if (thread.isStopped() || isStoringToWarehouse) break; // 存背包阶段直接跳出
            thread.checkPause();

            // 背包满则进入存背包流程
            if (remainingEmptySlots <= 0) {
                TaskStepNotifier.notifyStep(context.getDeviceId(), "背包已满，准备存回仓库");
                isStoringToWarehouse = true;
                processFullBag(context, thread, common);
                remainingEmptySlots = common.findcangkujiemianEmptyBagIndices(context.getDeviceId()).size();
                if (remainingEmptySlots <= 0) break;
            }

            // 单击检查仓库藏宝图
            common.clickCangkuGrid(context.getDeviceId(), warehouseIndex);
            Thread.sleep(waittime);

            // OCR识别并判断是否取出
            String ocrResult = DeviceHttpClient.ocr(context.getDeviceId(), OCR_RECT);
            if (isContainTargetChar(ocrResult)) {
                common.doubleclickCangkuGrid(context.getDeviceId(), warehouseIndex);
                TaskStepNotifier.notifyStep(context.getDeviceId(), "取出仓库格子[" + warehouseIndex + "]的藏宝图");
                remainingEmptySlots--;
                currentPageHasValid = true;
                Thread.sleep(waittime);
            }
        }

        return currentPageHasValid;
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
     * 将使用后的藏宝图存回仓库（仅在此阶段执行，不处理取图）
     */
    private void storeUsedTreasureToWarehouse(TaskContext context, TaskThread thread, CommonActions common) throws IOException, InterruptedException {
        // 1. 获取背包中待存的藏宝图列表
        List<Integer> usedTreasureIndices = common.findcangkujiemianAllItemIndices(
                context.getDeviceId(), treasureMapImg, 0.8
        );
        if (usedTreasureIndices.isEmpty()) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "背包无使用后的藏宝图可存");
            return;
        }

        // 2. 逐个存入仓库（每次找一个空格子）
        for (int bagIndex : usedTreasureIndices) {
            if (thread.isStopped()) break;
            thread.checkPause();

            // 3. 找第一个空格子（找不到则翻页）
            int warehouseIndex = -1;
            while (true) {
                // 尝试在当前页找第一个空格子
                warehouseIndex = common.findFirstEmptyCangkuIndex(context.getDeviceId());
                if (warehouseIndex != -1) {
                    // 找到空格子，跳出循环
                    break;
                }

                // 当前页无空格子，检查是否可翻页
                if (common.canPageDown()) {
                    common.pageDown();
                    Thread.sleep(waittime + 500); // 翻页延迟
                } else {
                    // 无下一页且无空格子，存图失败
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "仓库无空格子，无法存回");
                    return;
                }
            }

            // 4. 执行存入操作（双击背包格子取出，单击仓库格子存入）
            common.doubleclickcangkujiemianBagGrid(context.getDeviceId(), bagIndex); // 从背包取出
            TaskStepNotifier.notifyStep(context.getDeviceId(), "存回仓库格子[" + warehouseIndex + "]");
            Thread.sleep(waittime);
        }

        TaskStepNotifier.notifyStep(context.getDeviceId(), "背包藏宝图全部存回仓库");
    }


    // 其他辅助方法（保持不变）
    private boolean openWarehouse(TaskContext context, TaskThread thread, CommonActions common) throws IOException, InterruptedException {
        int retry = 0;
        while (retry < 3) {
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