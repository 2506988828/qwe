package com.my.qwe.task;

import com.my.qwe.http.DeviceHttpClient;

import java.io.IOException;

/**
 * 游戏状态检测工具类
 * 提供背包、战斗、召唤兽等状态的检测方法
 */
public class GameStateDetector  {
    private final TaskContext context;
    private final DeviceHttpClient httpClient;

    // 常量定义（可根据实际游戏调整）
    private static final String BAG_COLOR = "585c9c";
    private static final String BAG_COLOR_OFFSETS = "16|0|545b9f,14|15|43447e,13|5|2d3574,13|17|4f5690,2|14|c9cbe7,14|6|555e90,0|9|353881,12|12|daddf0,2|16|c9c8e6";
    private static final String TELEPORT_COLOR = "5e959f";
    private static final String TELEPORT_COLOR_OFFSETS = "3|38|1e525a,27|5|43848d,6|34|75a9b6,2|18|2f656e,36|10|327a87,24|16|98bdcd,1|5|31818c,30|27|265a66,18|1|428f98,28|9|367a81,15|12|b9dcea,20|20|5e8a9a,31|28|215561,3|22|2b666f,24|27|75a3b6,9|6|87bac9,29|8|367e85,8|22|29606a,21|4|3d7280,33|27|87b8c9,36|4|317a84,10|12|b8dbec,22|16|65919c,4|6|33818c,20|6|9ec4d4,14|38|1b535d,36|33|255c64,18|33|245763,27|34|588690,33|19|326b77,7|11|3d6e78,35|2|41848e,20|34|1f545d,33|17|2b6570,31|29|245865,29|37|1d525f,11|0|388692,1|28|447a8b,33|38|296a73,19|19|a1cede,4|28|285d68,12|30|598d9c,17|28|85b9ce";
    private static final String BATTLE_COLOR = "363d17";
    private static final String BATTLE_COLOR_OFFSETS = "0|5|303524,6|13|4f563e,7|5|bbc3b8,9|10|19200c,6|11|464a3e";
    private static final String HP_LOW_COLOR = "低血量特征色";
    private static final String MP_LOW_COLOR = "低魔法值特征色";
    private static final String PET_LOYALTY_LOW_COLOR = "召唤兽低忠诚特征色";
    // 常量定义（可根据实际游戏调整）
    private static final int PLAYER_HP_BAR_X1 = 300; // 人物血条左边界
    private static final int PLAYER_HP_BAR_Y1 = 550;
    private static final int PLAYER_HP_BAR_X2 = 400; // 人物血条右边界
    private static final int PLAYER_HP_BAR_Y2 = 570;

    private static final int PLAYER_MP_BAR_X1 = 420; // 人物蓝条左边界
    private static final int PLAYER_MP_BAR_Y1 = 550;
    private static final int PLAYER_MP_BAR_X2 = 520; // 人物蓝条右边界
    private static final int PLAYER_MP_BAR_Y2 = 570;

    private static final int PET_HP_BAR_X1 = 300; // 召唤兽血条左边界
    private static final int PET_HP_BAR_Y1 = 480;
    private static final int PET_HP_BAR_X2 = 400; // 召唤兽血条右边界
    private static final int PET_HP_BAR_Y2 = 500;

    private static final double HP_LOW_THRESHOLD = 0.8; // 血量低于30%视为低血量
    private static final double MP_LOW_THRESHOLD = 0.8; // 魔法值低于20%视为低魔法值

    public GameStateDetector(TaskContext context, DeviceHttpClient httpClient) {
        this.context = context;
        this.httpClient = httpClient;
    }

    /**
     * 检测背包是否打开
     */
    public boolean isBagOpen() throws IOException {
        int[] bagPos = httpClient.findMultiColor(
                context.getDeviceId(), 349, 26, 366, 44,
                BAG_COLOR, BAG_COLOR_OFFSETS, 0.8, 0);

        return bagPos[0] > 0;
    }

    /**
     * 检测点击传送NPC以后是否出现“是的，我要去”的选项
     * */
    public boolean isshidewoyaoqu() throws IOException {
        int[] ifpos = DeviceHttpClient.findMultiColor(
                context.getDeviceId(), 40, 185, 560, 210,
                "415a6c",
                "11|0|496473,10|8|9eacb5,0|7|ccdfe6,7|5|f1f5f8,7|0|4c6675,9|9|758791,6|8|eff7fe,2|2|dde0e4,10|9|748791,1|12|dfeef4,11|5|babbc4,1|8|aabac7",
                0.6, 0
        );
        return ifpos[0] > 0;
    }
    /**
     * 检测传送按钮是否出现
     */
    public boolean isTeleportButtonVisible() throws IOException {
        int[] teleportPos = httpClient.findMultiColor(
                context.getDeviceId(), 525, 230, 578, 289,
                TELEPORT_COLOR, TELEPORT_COLOR_OFFSETS, 0.8, 0);

        return teleportPos[0] > 0;
    }

    /**
     * 检测是否处于战斗中
     */
    public boolean isInBattle() throws IOException {
        int[]rect = {15,185,45,205};
        int[] battlePos = httpClient.findImage(
                context.getDeviceId(), rect, "战斗界面",0.8);

        return battlePos[0] > 0;
    }

    /**
     * 检测人物血量是否低于阈值（基于血条长度）
     */
    public boolean isPlayerHpLow() throws IOException {
        double hpPercent = getPlayerHpPercent();
        TaskStepNotifier.notifyStep(context.getDeviceId(),
                "人物血量: " + String.format("%.1f%%", hpPercent * 100));

        return hpPercent < HP_LOW_THRESHOLD;
    }


    /**
     * 检测人物血量百分比（基于指定的多点找色方法）
     * 起始坐标：694,3-698,7，每向右4像素代表10%血量
     */
    public double getPlayerHpPercent() throws IOException {
        int startX = 694;
        int y1 = 3;
        int y2 = 7;
        int xStep = 4;        // 每次x增加4像素
        double percentPerStep = 10.0;  // 每4像素代表10%血量
        int maxSteps = 10;    // 最多检测10次（覆盖100%血量）

        String colorPattern = "a84f4f";  // 血条颜色特征
        double similarity = 0.7;         // 相似度阈值
        int detectedSteps = 0;
        // 循环检测每个区域
        for (int i = 0; i < maxSteps; i++) {
            int currentX1 = startX + (i * xStep);
            int currentX2 = currentX1 + 4;  // 保持宽度为4像素

            int[] result = httpClient.findMultiColor(
                    context.getDeviceId(),
                    currentX1, y1, currentX2, y2,
                    colorPattern, "", similarity, 0
            );

            if (result[0] > 0) {
                detectedSteps++;  // 找到匹配区域，计数+1
            } else {
                break;  // 未找到匹配，认为血量已结束
            }
        }
        double percent = detectedSteps * percentPerStep;
        TaskStepNotifier.notifyStep(context.getDeviceId(),
                "检测到人物血量: " + String.format("%.0f%%", percent));

        return percent / 100.0;  // 转换为0-1比例
    }







    /**
     * 检测召唤兽忠诚度是否需要补充
     */
    public boolean isPetLoyaltyLow() throws IOException {
        int[] loyaltyPos = httpClient.findMultiColor(
                context.getDeviceId(), 420, 480, 520, 500,
                PET_LOYALTY_LOW_COLOR, "特征点", 0.8, 0);

        return loyaltyPos[0] > 0;
    }



    /**
     * 等待战斗结束（带超时控制）
     * @param timeoutSeconds 最大等待秒数
     * @return 战斗是否结束
     */
    public boolean waitForBattleEnd(int timeoutSeconds) throws IOException {
        long startTime = System.currentTimeMillis();

        while ((System.currentTimeMillis() - startTime) / 1000 < timeoutSeconds) {
            if (!isInBattle()) {
                return true; // 战斗结束
            }

            try {
                Thread.sleep(1000); // 每秒检查一次
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false; // 超时
    }
}