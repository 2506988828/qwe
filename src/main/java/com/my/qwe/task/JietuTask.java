package com.my.qwe.task;

import com.my.qwe.controller.HumanLikeController;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class JietuTask implements ITask {

    private String treasureMapImg = "藏宝图"; // 藏宝图图片路径
    private int waittime = new Random().nextInt(200) + 300; // 随机延迟
    // 仓库缓存变量（稳定，可缓存）
    private List<Integer> currentWarehouseEmptySlots; // 当前仓库页的空格子列表
    private boolean isWarehouseFull; // 标记仓库是否整体已满


    @Override
    public void start(TaskContext context, TaskThread thread) {
        CommonActions commonActions = new CommonActions(context, thread);
        TaskStepNotifier.notifyStep(context.getDeviceId(), "===== 开始藏宝图存入仓库任务 =====");

        try {
            // 步骤1：打开仓库
            if (!openWarehouse(context, thread, commonActions)) {
                TaskStepNotifier.notifyStep(context.getDeviceId(), "无法打开仓库，任务终止");
                return;
            }

            // 初始化仓库缓存（仓库状态稳定，仅初始和翻页时更新）
            currentWarehouseEmptySlots = getCurrentWarehouseEmptySlots(context, commonActions);
            isWarehouseFull = false;

            // 步骤2：循环存图（背包实时检查，仓库用缓存）
            while (!thread.isStopped() && !Thread.currentThread().isInterrupted()) {
                thread.checkPause();

                // 关键：背包不缓存，每次都重新获取最新的藏宝图（处理他人实时给予的情况）
                int bagTreasureIndices = getBagTreasureIndices(context, commonActions);

                // 检查终止条件1：仓库整体已满
                if (isWarehouseFull) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "所有仓库页均已满，任务结束");
                    break;
                }

                // 检查终止条件2：背包无图（但可能后续会收到，因此短暂等待后重试）
                if (bagTreasureIndices<0) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "当前背包无藏宝图，等待3秒后重试...");
                    Thread.sleep(3000); // 等待3秒（给他人给予的时间）
                    continue; // 重新进入循环，再次检查背包（处理新给予的藏宝图）
                }

                // 当前仓库页无空格，尝试翻页并更新仓库缓存
                if (currentWarehouseEmptySlots.isEmpty()) {
                    if (!commonActions.canPageDown()) {
                        isWarehouseFull = true;
                        continue;
                    }
                    // 翻页并更新仓库空格子缓存
                    commonActions.pageDown();
                    Thread.sleep(waittime + 500);
                    currentWarehouseEmptySlots = getCurrentWarehouseEmptySlots(context, commonActions);
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "翻至下一页，当前页空格子数量：" + currentWarehouseEmptySlots.size());
                    continue;
                }

                // 执行存入操作：从实时获取的背包列表中取第一个，从仓库缓存中取第一个空格子
                int bagIndex = bagTreasureIndices; // 取最新的第一个藏宝图
                int warehouseIndex = currentWarehouseEmptySlots.remove(0); // 从缓存中取空格子
                storeTreasureToWarehouse(bagIndex, warehouseIndex, context, thread, commonActions);


            }

            // 任务结束，关闭仓库
            commonActions.closeWarehouse();
            TaskStepNotifier.notifyStep(context.getDeviceId(), "藏宝图存入仓库任务完成");

        } catch (Exception e) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "任务异常终止：" + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * 实时获取背包中所有藏宝图的索引（不缓存，每次都重新识别）
     * 处理他人实时给予的藏宝图
     */
    private int getBagTreasureIndices(TaskContext context, CommonActions common) throws IOException {
        // 直接调用实时识别方法，不做缓存
         int indices = common.findcangkujiemianFirstItemIndex(context.getDeviceId(), treasureMapImg, 0.8);
        // 日志：实时反馈背包数量（便于监控他人给予情况）
        TaskStepNotifier.notifyStep(context.getDeviceId(), "实时检测到背包藏宝图数量：" + indices);
        return indices;
    }

    /**
     * 缓存当前仓库页的空格子（仓库稳定，仅翻页时更新）
     */
    private List<Integer> getCurrentWarehouseEmptySlots(TaskContext context, CommonActions common) throws IOException {
        List<Integer> emptySlots = common.findEmptyCangkuIndices(context.getDeviceId());
        TaskStepNotifier.notifyStep(context.getDeviceId(), "仓库当前页空格子数量：" + emptySlots.size());
        return emptySlots;
    }

    /**
     * 打开仓库（复用CommonActions）
     */
    private boolean openWarehouse(TaskContext context, TaskThread thread, CommonActions common) throws IOException, InterruptedException {
        int retry = 0;
        while (retry < 3) {
            if (common.ifOpenCangku()) {
                return true;
            }
            common.openJianyeCangku();
            Thread.sleep(1000);
            retry++;
        }
        return common.ifOpenCangku();
    }

    /**
     * 存入单个藏宝图（使用实时背包索引和缓存的仓库空格子）
     */
    private void storeTreasureToWarehouse(int bagIndex, int warehouseIndex, TaskContext context, TaskThread thread, CommonActions common) throws IOException, InterruptedException {
        // 双击背包中的藏宝图（实时索引）
        common.doubleclickcangkujiemianBagGrid(context.getDeviceId(), bagIndex);
        Thread.sleep(waittime);


    }

    /**
     * 上一页操作（补充实现）
     */
    private void pageUp(TaskContext context, TaskThread thread) throws IOException, InterruptedException {
        HumanLikeController human = new HumanLikeController(thread);
        human.click(context.getDeviceId(), 180, 357, 2, 2); // 上一页坐标
        Thread.sleep(waittime);
    }

    @Override
    public String getName() {
        return "接图";
    }
}

