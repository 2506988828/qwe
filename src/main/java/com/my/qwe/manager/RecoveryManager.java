package com.my.qwe.manager;

import com.my.qwe.context.TaskContext;
import com.my.qwe.utils.HumanLikeController;

public class RecoveryManager {
    private final TaskContext context;
    private final HumanLikeController human;
    private final InventoryManager inventory;

    public RecoveryManager(TaskContext context) {
        this.context = context;
        this.human = new HumanLikeController(context.getTaskThread());
        this.inventory = new InventoryManager(context);
    }

    // 恢复角色生命值
    public void recoverHp() throws Exception {
        TaskStepNotifier.notifyStep(context.getDeviceId(), "角色血量不足，开始恢复...");

        // 优先使用药品恢复
        int hpPotionSlot = inventory.findItemSlot("金创药");
        if (hpPotionSlot >= 0) {
            inventory.useItem(hpPotionSlot);
            Thread.sleep(1000);
            return;
        }

        // 如果没有药品，使用技能恢复
        if (canUseHealSkill()) {
            useHealSkill();
            Thread.sleep(1000);
            return;
        }

        // 如果都没有，返回安全区
        navigateToSafeArea();
    }

    // 恢复角色魔法值
    public void recoverMp() throws Exception {
        TaskStepNotifier.notifyStep(context.getDeviceId(), "角色魔法值不足，开始恢复...");

        // 优先使用药品恢复
        int mpPotionSlot = inventory.findItemSlot("魔法香烛");
        if (mpPotionSlot >= 0) {
            inventory.useItem(mpPotionSlot);
            Thread.sleep(1000);
            return;
        }

        // 如果没有药品，返回安全区
        navigateToSafeArea();
    }

    // 恢复召唤兽生命值
    public void recoverPetHp(PetStatus pet) throws Exception {
        TaskStepNotifier.notifyStep(context.getDeviceId(), "召唤兽血量不足，开始恢复...");

        // 使用召唤兽药品（需根据实际游戏调整）
        int petPotionSlot = inventory.findItemSlot("金柳露");
        if (petPotionSlot >= 0) {
            inventory.useItem(petPotionSlot);
            Thread.sleep(1000);

            // 选择要治疗的召唤兽
            selectPet(pet);
            return;
        }

        // 如果没有药品，返回安全区
        navigateToSafeArea();
    }

    // 使用恢复道具
    public void useItemForRecovery(String itemName) throws Exception {
        int slot = inventory.findItemSlot(itemName);
        if (slot >= 0) {
            inventory.useItem(slot);
            Thread.sleep(1000);
        } else {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "警告：没有找到恢复道具：" + itemName);
        }
    }

    // 检查当前状态
    public void checkStatus() throws Exception {
        CharacterStatus status = getCurrentStatus();

        // 检查角色血量
        if (status.needHpRecovery(50)) {
            recoverHp();
        }

        // 检查角色魔法值
        if (status.needMpRecovery(30)) {
            recoverMp();
        }

        // 检查召唤兽血量
        for (PetStatus pet : status.getPets()) {
            if (pet.needHpRecovery(60)) {
                recoverPetHp(pet);
            }
        }
    }

    // 获取当前状态（需实现）
    private CharacterStatus getCurrentStatus() {
        // 使用图像识别或其他方式获取当前状态
        return new CharacterStatus(); // 占位实现
    }

    // 选择要操作的召唤兽
    private void selectPet(PetStatus pet) throws Exception {
        // 根据召唤兽索引选择（需根据实际游戏调整）
        int petIndex = 0; // 假设获取到的pet对象包含索引信息
        human.click(context.getDeviceId(), 200 + petIndex * 40, 500, 5, 5);
        Thread.sleep(500);
    }

    // 判断是否可以使用治疗技能
    private boolean canUseHealSkill() {
        // 判断是否有治疗技能且魔法值足够（需实现）
        return false; // 占位实现
    }

    // 使用治疗技能
    private void useHealSkill() throws Exception {
        // 点击治疗技能按钮（需根据实际游戏调整）
        human.click(context.getDeviceId(), 300, 400, 5, 5);
        Thread.sleep(1000);
    }

    // 前往安全区恢复
    private void navigateToSafeArea() throws Exception {
        TaskStepNotifier.notifyStep(context.getDeviceId(), "前往安全区恢复...");
        RouteManager routeManager = new RouteManager(context);
        routeManager.navigateToScene("长安");
        routeManager.gotoCoordinates(240, 120); // 长安药店坐标
        Thread.sleep(3000);

        // 与药店老板对话恢复
        human.click(context.getDeviceId(), 240, 120, 5, 5);
        Thread.sleep(1000);
        human.clickImg(context.getDeviceId(), "治疗", 5, 5);
        Thread.sleep(2000);
    }
}