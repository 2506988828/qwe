package com.my.qwe.manager;


import com.my.qwe.controller.HumanLikeController;
import com.my.qwe.task.TaskContext;

public class InventoryManager {
    private final TaskContext context;
    private final HumanLikeController human;
    private static final int INVENTORY_SIZE = 40; // 背包格子数

    public InventoryManager(TaskContext context) {
        this.context = context;
        this.human = new HumanLikeController(context.getTaskThread());
    }

    // 打开背包
    public void openInventory() throws Exception {
        // 点击背包按钮（根据实际游戏界面调整坐标）
        human.click(context.getDeviceId(), 800, 50, 5, 5);
        Thread.sleep(500);
    }

    // 查找空背包格子
    public int findEmptySlot() throws Exception {
        openInventory();
        // 遍历背包格子，使用图像识别判断是否为空
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (isSlotEmpty(i)) {
                return i;
            }
        }
        return -1; // 没有空槽
    }

    // 判断格子是否为空
    private boolean isSlotEmpty(int slotIndex) {
        // 使用图像识别判断格子是否为空（需实现）
        // 示例：return DeviceHttpClient.findColor(context.getDeviceId(), emptySlotColor, x, y, width, height) > 0;
        return false; // 占位实现
    }

    // 查找特定物品的格子位置
    public int findItemSlot(String itemName) throws Exception {
        openInventory();
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (getItemName(i).equals(itemName)) {
                return i;
            }
        }
        return -1; // 未找到
    }

    // 获取格子中的物品名称
    private String getItemName(int slotIndex) {
        // 使用图像识别获取物品名称（需实现）
        return ""; // 占位实现
    }

    // 使用指定格子的物品
    public boolean useItem(int slotIndex) throws Exception {
        openInventory();
        // 计算格子在屏幕上的坐标（根据实际背包布局调整）
        int x = 100 + (slotIndex % 8) * 60;
        int y = 150 + (slotIndex / 8) * 60;

        human.click(context.getDeviceId(), x, y, 5, 5);
        Thread.sleep(500);
        return true;
    }

    // 丢弃指定格子的物品
    public void discardItem(int slotIndex) throws Exception {
        openInventory();
        // 右键点击物品（准备丢弃）
        int x = 100 + (slotIndex % 8) * 60;
        int y = 150 + (slotIndex / 8) * 60;
        human.rightClick(context.getDeviceId(), x, y, 5, 5);
        Thread.sleep(300);

        // 点击"丢弃"按钮（根据实际游戏界面调整坐标）
        human.click(context.getDeviceId(), x + 50, y + 30, 5, 5);
        Thread.sleep(300);

        // 确认丢弃
        human.click(context.getDeviceId(), 400, 300, 5, 5); // 确认按钮坐标
        Thread.sleep(500);
    }

    // 查找多个物品的位置
    public List<Integer> findItemsByNames(List<String> itemNames) throws Exception {
        List<Integer> result = new ArrayList<>();
        openInventory();

        for (int i = 0; i < INVENTORY_SIZE; i++) {
            String name = getItemName(i);
            if (itemNames.contains(name)) {
                result.add(i);
            }
        }
        return result;
    }

    // 检查是否有空余格子
    public boolean hasEmptySlot() throws Exception {
        return findEmptySlot() >= 0;
    }
}