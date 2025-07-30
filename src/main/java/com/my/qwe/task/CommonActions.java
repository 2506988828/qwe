package com.my.qwe.task;

import com.my.qwe.controller.HumanLikeController;
import com.my.qwe.http.DeviceHttpClient;
import com.my.qwe.task.config.IniConfigLoader;
import com.my.qwe.util.BagGridUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CommonActions {
    private final TaskContext context;
    private final TaskThread taskThread;
    private int waittime=(new java.util.Random().nextInt(201) + 300);
    // 坐标识别区域配置（仅包含起点坐标和高度，根据地区字数区分）
    // 格式：[x起点, y起点, 高度]，需根据实际游戏界面调整！
    private static final int[] AREA_3CHAR = {460, 212, 15}; // 3字地区：x起点, y起点, 高度
    private static final int[] AREA_4CHAR = {480, 212, 15}; // 4字地区：x起点, y起点, 高度

    // 右括号“)”的多点找色配置（用于动态计算宽度）
    // 格式：基准色,偏移点1|颜色1,偏移点2|颜色2...（需根据实际游戏调整）
    private static final String BRACKET_BASE_COLOR = "171f27"; // 右括号基准色
    private static final String BRACKET_OFFSET_COLORS = "2|2|171f27,4|0|171f27,0|4|171f27"; // 右括号特征点

    public CommonActions(TaskContext context, TaskThread taskThread) {
        this.context = context;
        this.taskThread = taskThread;
    }

    /**
     * 判断当前是否在游戏主界面（基于多点找色）
     */
    public String isMainGameScreen() {
        HumanLikeController human = new HumanLikeController(taskThread);


        try {
            int retry = 0;
            while(true) {
                if (taskThread.isStopped()||Thread.currentThread().isInterrupted()) return "任务已终止";
                taskThread.checkPause();
                int x1 = 683;
                int y1 = 360;
                int x2 = 718;
                int y2 = 398;
                int[] resp = DeviceHttpClient.findMultiColor(context.getDeviceId(), x1, y1, x2, y2, "213741", "8|9|97bec7,17|8|678289,3|7|83b0bf,14|20|98b2b8,17|5|bbd7dc,17|18|dcf2f8,16|11|3c535a,5|20|567380,4|1|405458,10|18|42575b,10|11|aecfdb", 0.8, 0);
                int[] resp2 = DeviceHttpClient.findMultiColor(context.getDeviceId(), x1, y1, x2, y2, "1f343d","4|16|213643,8|12|9ec2d0,8|13|93bac8,11|3|cee7e9,2|13|93b1bb,16|20|4a676e,6|3|879b9f,6|6|839a9c,13|17|4b676d,1|10|accfd9,2|6|1d3138",0.8, 0);
                if (resp[0] > 0) {
                    return "游戏画面正常";
                }
                else if (resp2[0] > 0 && resp[0] < 0) {

                    human.click(context.getDeviceId(),700 ,380,10,10 );
                    Thread.sleep(1000);

                }
                else if (resp2[0] < 0 && resp[0] < 0) { TaskStepNotifier.notifyStep(context.getDeviceId(),"未检测到游戏画面");}
                retry++;
                Thread.sleep(1000);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * 识别当前的地图是什么地区
     * */
    public String ocrShibieDiqu(){
        TaskStepNotifier.notifyStep(context.getDeviceId(),"识别所在地图...");
        if (taskThread.isStopped()||Thread.currentThread().isInterrupted()) return "任务已终止";
        taskThread.checkPause();
        int[] diququyu={53,12,124,30};
        try {
            String diqu =DeviceHttpClient.ocr(context.getDeviceId(),diququyu);
            return diqu;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *恢复人物气血
     * */
    public void huifuHP() throws IOException, InterruptedException {
        HumanLikeController human = new HumanLikeController(taskThread);
        human.click(context.getDeviceId(),715,15,9,9);
        Thread.sleep(new java.util.Random().nextInt(201) + 700);
        int[]posHP =DeviceHttpClient.findImage(context.getDeviceId(),"补充气血",0.8);
        if (posHP[0] > 0) {
        human.click(context.getDeviceId(),posHP[0], posHP[1],15,9);}
        Thread.sleep(new java.util.Random().nextInt(201) + 700);
        /*human.click(context.getDeviceId(),715,15,9,9);
        Thread.sleep(new java.util.Random().nextInt(201) + 700);
        int[]posMP =DeviceHttpClient.findImage(context.getDeviceId(),"补充魔法",0.8);
        human.click(context.getDeviceId(),posMP[0], posMP[1],15,9);
        Thread.sleep(new java.util.Random().nextInt(201) + 700);*/
    }
    /**
     *恢复人物MP
     * */
    public void huifuMP() throws IOException, InterruptedException {
        HumanLikeController human = new HumanLikeController(taskThread);
        human.click(context.getDeviceId(),715,15,9,9);
        Thread.sleep(new java.util.Random().nextInt(201) + 700);
        int[]posMP =DeviceHttpClient.findImage(context.getDeviceId(),"补充魔法",0.8);
        if (posMP[0] > 0 ) {
        human.click(context.getDeviceId(),posMP[0], posMP[1],15,9);}
        Thread.sleep(new java.util.Random().nextInt(201) + 700);
    }
    /**
     *恢复宠物气血
     * */
    public void huifuPetHP() throws IOException, InterruptedException {
        HumanLikeController human = new HumanLikeController(taskThread);
        human.click(context.getDeviceId(),621,15,9,9);
        Thread.sleep(new java.util.Random().nextInt(201) + 700);
        int[]posHP =DeviceHttpClient.findImage(context.getDeviceId(),"补充气血",0.8);
        if (posHP[0] > 0){
        human.click(context.getDeviceId(),posHP[0], posHP[1],15,9);}
        Thread.sleep(new java.util.Random().nextInt(201) + 700);
        /*human.click(context.getDeviceId(),715,15,9,9);
        Thread.sleep(new java.util.Random().nextInt(201) + 700);
        int[]posMP =DeviceHttpClient.findImage(context.getDeviceId(),"补充魔法",0.8);
        human.click(context.getDeviceId(),posMP[0], posMP[1],15,9);
        Thread.sleep(new java.util.Random().nextInt(201) + 700);*/
    }
    /**
     *恢复宠物MP
     * */
    public void huifuPetMP() throws IOException, InterruptedException {
        HumanLikeController human = new HumanLikeController(taskThread);
        human.click(context.getDeviceId(),621,15,9,9);
        Thread.sleep(new java.util.Random().nextInt(201) + 700);
        int[]posMP =DeviceHttpClient.findImage(context.getDeviceId(),"补充魔法",0.8);
        if (posMP[0] > 0){
        human.click(context.getDeviceId(),posMP[0], posMP[1],15,9);}
        Thread.sleep(new java.util.Random().nextInt(201) + 700);
    }



    /**
     * 使用摄妖香
     * */
    public void useXiangyaoxiang(){
        GameStateDetector detector = new GameStateDetector(context,new DeviceHttpClient());
        HumanLikeController human = new HumanLikeController(taskThread);
        IniConfigLoader configLoader= new IniConfigLoader(context.getDeviceName());;

        Properties quanjuProps = configLoader.getSection("全局变量");
        long xiangdaoqi = Long.parseLong(quanjuProps.getProperty("摄妖香"));
        if (xiangdaoqi< System.currentTimeMillis() ) {
            try {
                if (!detector.isBagOpen()){
                    openBag();
                }
                int[]posxiang = DeviceHttpClient.findImage(context.getDeviceId(),"摄妖香",0.8);
                if (posxiang[0] > 0){
                human.doubleclick(context.getDeviceId(),posxiang[0], posxiang[1],10,9);
                configLoader.setProperty("全局变量","摄妖香", String.valueOf(System.currentTimeMillis()+25*60*1000));
                configLoader.save();}
                Thread.sleep(new Random().nextInt(200) + 500);
                closeBag();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }




    /**
     * 识别当前所在的坐标（支持无括号格式，如192,133）
     * 每次识别将区域x坐标减1，连续两次识别结果相同时返回
     */
    public int[] ocrZuobiao() {
        // 初始识别区域 [x, y, width, height]，后续每次x减1
        int baseX = 52;
        int baseY = 37;
        int width = 124;
        int height = 50;

        int[] previousResult = null; // 缓存上一次识别的有效结果
        int currentX = baseX; // 当前识别区域的x坐标
        int retryCount = 0; // 识别计数器
        final int MAX_RETRY = 8; // 最大识别次数，防止无限循环

        try {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "开始多次识别坐标");

            String diqu = "";
            while (retryCount < MAX_RETRY) {
                retryCount++;
                // 检查任务状态，终止时返回无效坐标
                if (taskThread.isStopped() || Thread.currentThread().isInterrupted()) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "任务终止，停止坐标识别");
                    return new int[]{-1, -1};
                }
                taskThread.checkPause();

                // 当前识别区域（x坐标随次数递减）
                int[] diququyu = {currentX, baseY, width, height};
                /*TaskStepNotifier.notifyStep(context.getDeviceId(),
                        "第" + retryCount + "次识别，区域：[" + currentX + "," + baseY + "," + width + "," + height + "]");*/

                // 调用OCR接口获取坐标文本
                diqu = DeviceHttpClient.ocr(context.getDeviceId(), diququyu);
                if (diqu == null || diqu.trim().isEmpty()) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "第" + retryCount + "次识别为空，重试...");
                    currentX=currentX-2; // 即使识别为空，也调整区域x坐标
                    baseY--;
                    Thread.sleep(500);
                    continue;
                }

                // 清理格式：移除所有括号和无关字符，统一使用逗号分隔
                diqu = diqu.trim()
                        .replaceAll("[()（）]", "") // 移除所有类型的括号
                        .replaceAll("[。.]", ",")    // 替换句号为逗号
                        .replaceAll("\\s+", "");    // 移除所有空格

                // 解析坐标
                int commaIndex = diqu.indexOf(',');
                if (commaIndex <= 0 || commaIndex >= diqu.length() - 1) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "第" + retryCount + "次格式错误（" + diqu + "），重试...");
                    currentX=currentX-2; // 格式错误也调整区域x坐标
                    baseY--;
                    Thread.sleep(500);
                    continue;
                }

                // 提取并验证数字
                String xStr = diqu.substring(0, commaIndex);
                String yStr = diqu.substring(commaIndex + 1);
                if (!xStr.matches("\\d+") || !yStr.matches("\\d+")) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "第" + retryCount + "次包含非数字（" + diqu + "），重试...");
                    currentX=currentX-2; // 非数字也调整区域x坐标
                    baseY--;
                    Thread.sleep(500);
                    continue;
                }

                // 转换为整数坐标
                int x = Integer.parseInt(xStr);
                int y = Integer.parseInt(yStr);
                //TaskStepNotifier.notifyStep(context.getDeviceId(), "第" + retryCount + "次识别结果：(" + x + "," + y + ")");

                // 验证条件：本次结果与上次结果完全相同
                if (previousResult != null) {
                    if (x == previousResult[0] && y == previousResult[1]) {
                        //TaskStepNotifier.notifyStep(context.getDeviceId(), "连续两次识别结果一致，返回：(" + x + "," + y + ")");
                        return new int[]{x, y};
                    } else {
                        //TaskStepNotifier.notifyStep(context.getDeviceId(), "与上次结果不一致（上次：" + previousResult[0] + "," + previousResult[1] + "），继续识别...");
                    }
                }

                // 缓存当前结果，准备下一次识别
                previousResult = new int[]{x, y};
                currentX--; // 核心：下一次识别的x坐标减1
                Thread.sleep(500);
            }

            // 达到最大重试次数仍未满足条件，返回最后一次有效结果或无效值
            if (previousResult != null) {
               // TaskStepNotifier.notifyStep(context.getDeviceId(), "达到最大识别次数，返回最后一次结果：(" + previousResult[0] + "," + previousResult[1] + ")");
                return previousResult;
            } else {
                //TaskStepNotifier.notifyStep(context.getDeviceId(), "多次识别失败，返回无效坐标");
                return new int[]{-1, -1};
            }

        } catch (IOException e) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "OCR接口调用失败：" + e.getMessage());
            return new int[]{-1, -1};
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 保留中断状态
            TaskStepNotifier.notifyStep(context.getDeviceId(), "识别被中断");
            return new int[]{-1, -1};
        }
    }




    /**
     * 识别当前仓库页数
     * */
    public int[] ocrCangkuyeshu() {
        int[] diququyu = {124, 344, 179, 369};
        try {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "识别当前仓库页数");

            String diqu = "";
            while (true) {
                if (taskThread.isStopped()||Thread.currentThread().isInterrupted()) return new int[]{-1,-1} ;
                taskThread.checkPause();
                diqu = DeviceHttpClient.ocr(context.getDeviceId(), diququyu);
                // 清理格式
                diqu = diqu.replace("（", "(")
                        .replace("）", ")")
                        .replace("。", ",")
                        .replace(".", ",");

                int fenge = diqu.indexOf('/');
                String[] parts = diqu.split("/");

            /*if (left < 0 || comma < 0 || right < 0 || comma <= left || right <= comma) {
                throw new RuntimeException("坐标格式不正确，原始OCR结果：" + diqu);
            }*/
                if (fenge >= 0) {

                    int a = Integer.parseInt(parts[0]);  // 提取第一个部分（"1"）并转为整数
                    int b = Integer.parseInt(parts[1]);  // 提取第二个部分（"45"）并转为整数
                    return new int[]{a, b};
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("OCR接口调用失败: " + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new RuntimeException("坐标不是有效数字格式", e);
        }
    }

    /**
     * 设置隐藏按钮，收缩任务框、对话框
     * */
    public void resetJiemian(){
        TaskStepNotifier.notifyStep(context.getDeviceId(),"设置隐藏界面按钮...");
        HumanLikeController human = new HumanLikeController(taskThread);
        if (taskThread.isStopped()||Thread.currentThread().isInterrupted()) return ;
        taskThread.checkPause();
        try {
            //点击设置隐藏界面的按钮
            human.click(context.getDeviceId(),24,142,10,10);
            Thread.sleep(1000);
            int[]jianchadianji = DeviceHttpClient.findMultiColor(context.getDeviceId(),29,359,47,377,"324a4d","0|3|334a50,8|2|405058,17|12|33464b,10|3|a6b4bb,6|8|849196,13|15|708189,13|1|334950,16|16|2f3e46,13|7|3f4e52",0.8,0);
            int[]yincangwanjia = DeviceHttpClient.findMultiColor(context.getDeviceId(),17,185,40,205,"438393","11|12|74abb7,2|1|437d8f,19|0|478894,15|11|2a6477,12|3|4b8698,13|0|c1e2e8,20|1|4f919b,5|18|b2e7f0,22|14|56848d,13|4|4e8c9e,12|13|79a9b2,7|3|102f3c,5|1|d1f4f8",0.8,0);
            int[]yincangtanwei = DeviceHttpClient.findMultiColor(context.getDeviceId(),17,242,40,262,"4a7e89","18|3|daf4fa,18|6|d1f0f2,11|8|437b87,3|13|c1ebec,4|3|d4f2f9,14|16|5c8e9a,21|13|c4f4f4,10|3|d5f2f7,9|0|d6f5fb,9|6|376471,21|10|cff1f4,14|9|307685,3|8|ceeff7",0.8,0);
            int[]yincangjiemian= DeviceHttpClient.findMultiColor(context.getDeviceId(),10,329,47,338,"809da7","17|0|caeafa,14|0|c4deec,36|6|2b626d,14|1|c0e9ed,18|1|bee4ef,7|2|a7d9e7,35|5|91c3c7,11|7|99ccd7,30|5|a6dce3",0.8,0);

            if (jianchadianji[0]>0){
                if(yincangwanjia[1]<0){
                    human.click(context.getDeviceId(),29,199,15,15);
                    Thread.sleep(new java.util.Random().nextInt(201) + 300);
                }

                if(yincangtanwei[0]<0){
                    human.click(context.getDeviceId(),29,251,15,15);
                    Thread.sleep(new java.util.Random().nextInt(201) + 400);
                }
                if (yincangjiemian[0]>0){
                    human.click(context.getDeviceId(),29,312,15,15);
                    Thread.sleep(new java.util.Random().nextInt(201) + 300);
                }
            }
            //点击返回按钮
            human.click(context.getDeviceId(),30,365,10,10);
            Thread.sleep(new java.util.Random().nextInt(2000) + 300);
            int[]jianchaduihua = DeviceHttpClient.findMultiColor(context.getDeviceId(),0,0,2000,2000,"082318","2|4|082019,24|13|abeaf4,6|22|96dbe1,5|25|54808b,12|6|b7ecf7,9|2|284743,27|4|021a1b,27|12|00151f,2|11|86929c,14|13|37616c,18|2|8dacb3,12|25|0d211e,7|15|a5e8ee,11|6|b7ecf7,6|3|9cbdbd,2|19|101f1c,9|11|8bb7c4,7|4|c5edf6,1|20|17231c,13|17|9eeaee,15|12|315663,18|21|6b9da9",0.8,0);
            int[]jiancharenwu =  DeviceHttpClient.findMultiColor(context.getDeviceId(),586,144,602,155,"586261","13|5|99c2cf,14|2|6d8996,11|0|b2d0d9,2|10|596163,11|3|a4c7d4",0.8,0);
            if(jianchaduihua[0]>0){
                human.click(context.getDeviceId(),202,297,0,0);
                Thread.sleep(new java.util.Random().nextInt(301) + 300);
            }
            if(jiancharenwu[0]>0){
                human.click(context.getDeviceId(),589,149,2,4);
                Thread.sleep(new java.util.Random().nextInt(401) + 300);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 打开背包（优化版）
     * 增加超时控制、减少重复操作、优化判断逻辑
     */
    public void openBag() {
        TaskStepNotifier.notifyStep(context.getDeviceId(), "尝试打开背包...");
        HumanLikeController human = new HumanLikeController(taskThread);
        int maxAttempts = 5;      // 最大尝试次数
        int attemptDelay = 500;   // 每次尝试间隔(ms)
        int timeoutSeconds = 10;  // 超时时间(秒)

        try {
            long startTime = System.currentTimeMillis();

            while (true) {
                // 检查任务状态
                if (taskThread.isStopped() || Thread.currentThread().isInterrupted()) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "任务已停止，终止打开背包");
                    return;
                }
                taskThread.checkPause();

                // 超时检查
                if ((System.currentTimeMillis() - startTime) / 1000 > timeoutSeconds) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "打开背包超时，已尝试" + maxAttempts + "次");
                    break;
                }

                // 检查背包是否已打开
                boolean isBagOpen = isBagOpen();
                if (isBagOpen) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "背包已打开");
                    break;
                }

                // 查找背包按钮
                int[] bagButtonPos = findBagButton();
                if (bagButtonPos[0] > 0) {
                    // 找到背包按钮，点击它
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "找到背包按钮，点击打开");
                    human.click(context.getDeviceId(), bagButtonPos[0], bagButtonPos[1], 5, 5);
                    Thread.sleep(attemptDelay); // 等待界面响应
                } else {
                    // 未找到背包按钮
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "未识别到背包按钮，重试中...");
                    Thread.sleep(1000);
                }
            }
        } catch (Exception e) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "打开背包时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 检查背包是否已打开
     */
    private boolean isBagOpen() throws IOException {
        int[] result = DeviceHttpClient.findMultiColor(
                context.getDeviceId(), 316, 27, 333, 46,
                "2d3374", "5|0|2d3472,0|12|383c85,12|11|c3bee2,13|14|9698b8,11|1|6465a6,6|4|9fa3c6,1|6|d2dcef,12|0|292c73",
                0.8, 0
        );
        return result[0] > 0;
    }

    /**
     * 查找背包按钮位置
     */
    private int[] findBagButton() throws IOException {
        return DeviceHttpClient.findMultiColor(
                context.getDeviceId(), 649, 365, 665, 382,
                "d0a357", "6|12|b35d20,2|14|cc3021,6|4|df1020,1|8|ac6714,1|11|f8cf5d,15|10|9d561a,13|13|990116,9|16|b5741f",
                0.8, 0
        );
    }

    /**
     * 遍历背包格子查找图片
     * @param deviceId 设备ID
     * @param imagePath 图片路径（目标道具）
     * @param similarity 相似度阈值
     * @return 找到的坐标或 null
     */
    public int findItemIndex(String deviceId, String imagePath, double similarity) {
        List<int[]> grids = BagGridUtil.generateBagGrids();
        for (int i = 0; i < grids.size(); i++) {
            try {
                int[] rect = grids.get(i);
                int[] pos = DeviceHttpClient.findImage(deviceId, rect, imagePath, similarity);
                if (pos != null && pos[0] > 0 && pos[1] > 0) {
                    return i; // 找到了，返回格子编号
                }
            } catch (IOException ignored) {}
        }
        return -1;
    }

    /**
     * 遍历背包格子查找图片所在格子数
     * @param deviceId 设备ID
     * @param imagePath 图片路径（目标道具）
     * @param similarity 相似度阈值
     * @return 找到的坐标或 null
     */
    public int findCangkuItemIndex(String deviceId, String imagePath, double similarity) {
        List<int[]> grids = BagGridUtil.generateCangkuGrids();
        for (int i = 0; i < grids.size(); i++) {
            try {
                int[] rect = grids.get(i);
                int[] pos = DeviceHttpClient.findImage(deviceId, rect, imagePath, similarity);
                if (pos != null && pos[0] > 0 && pos[1] > 0) {
                    return i; // 找到了，返回格子编号
                }
            } catch (IOException ignored) {}
        }
        return -1;
    }

    //查找仓库中是否存在这个图片的物品
    public int findifCangkuItem(String deviceId, String imagePath, double similarity) {
        int[]rect ={77,115,341,328};
        try {
            int[]a =DeviceHttpClient.findImage(deviceId, rect, imagePath, similarity);
            if( a[0]>0 ){
                return 1;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return -1;
    }

    /**
     * 遍历仓库界面的背包格子查找这个图片的物品所在格子数
     * @param deviceId 设备ID
     * @param imagePath 图片路径（目标道具）
     * @param similarity 相似度阈值
     * @return 找到的坐标或 null
     */
    public int findcangkujiemianBagItemIndex(String deviceId, String imagePath, double similarity) {
        List<int[]> grids = BagGridUtil.cangkujiemianBagGrids();
        for (int i = 0; i < grids.size(); i++) {
            try {
                int[] rect = grids.get(i);
                int[] pos = DeviceHttpClient.findImage(deviceId, rect, imagePath, similarity);
                if (pos != null && pos[0] > 0 && pos[1] > 0) {
                    return i; // 找到了，返回格子编号
                }
            } catch (IOException ignored) {}
        }
        return -1;
    }

    /**
     * 遍历背包界面的背包格子查找这个图片的物品所在格子数
     * @param deviceId 设备ID
     * @param imagePath 图片路径（目标道具）
     * @param similarity 相似度阈值
     * @return 找到的坐标或 null
     */
        //遍历给予界面的背包格子，寻找符合图片的格子
        public List<Integer> findBagItemIndex(String deviceId, String imagePath, double similarity) {
            List<Integer> matchedIndices = new ArrayList<>();
            List<int[]> grids = BagGridUtil.generateBagGrids();

            for (int i = 0; i < grids.size(); i++) {
                int[] rect = grids.get(i);
                try {
                    int[] pos = DeviceHttpClient.findImage(deviceId, rect, imagePath, similarity);
                    if (pos != null && pos[0] > 0 && pos[1] > 0) {
                        matchedIndices.add(i); // 找到则加入编号列表
                    }
                } catch (IOException ignored) {
                    // 忽略查找失败
                }
            }


            return matchedIndices;
        }


    //遍历给予界面的背包格子，寻找符合图片的格子
    public List<Integer> findJiyujiemianAllItemIndices(String deviceId, String imagePath, double similarity) {
        List<Integer> matchedIndices = new ArrayList<>();
        List<int[]> grids = BagGridUtil.jiyujiemianBagGrids();

        for (int i = 0; i < grids.size(); i++) {
            int[] rect = grids.get(i);
            try {
                int[] pos = DeviceHttpClient.findImage(deviceId, rect, imagePath, similarity);
                if (pos != null && pos[0] > 0 && pos[1] > 0) {
                    matchedIndices.add(i); // 找到则加入编号列表
                }
            } catch (IOException ignored) {
                // 忽略查找失败
            }
        }


        return matchedIndices;
    }

    //遍历给予界面的背包格子，寻找符合图片的格子
    public List<Integer> findcangkujiemianAllItemIndices(String deviceId, String imagePath, double similarity) {
        List<Integer> matchedIndices = new ArrayList<>();
        List<int[]> grids = BagGridUtil.cangkujiemianBagGrids();

        for (int i = 0; i < grids.size(); i++) {
            int[] rect = grids.get(i);
            try {
                int[] pos = DeviceHttpClient.findImage(deviceId, rect, imagePath, similarity);
                if (pos != null && pos[0] > 0 && pos[1] > 0) {
                    matchedIndices.add(i); // 找到则加入编号列表
                }
            } catch (IOException ignored) {
                // 忽略查找失败
            }
        }


        return matchedIndices;
    }

    /**
     * 遍历给予界面的背包格子，找到第一个匹配图片的格子立即返回
     * @param deviceId 设备ID
     * @param imagePath 要查找的图片路径
     * @param similarity 相似度阈值
     * @return 第一个匹配的格子索引，未找到则返回-1
     */
    public int findcangkujiemianFirstItemIndex(String deviceId, String imagePath, double similarity) {
        List<int[]> grids = BagGridUtil.cangkujiemianBagGrids();

        for (int i = 0; i < grids.size(); i++) {
            int[] rect = grids.get(i);
            try {
                int[] pos = DeviceHttpClient.findImage(deviceId, rect, imagePath, similarity);
                if (pos != null && pos[0] > 0 && pos[1] > 0) {
                    return i; // 找到第一个匹配项后立即返回
                }
            } catch (IOException ignored) {
                // 忽略查找失败，继续检查下一个格子
            }
        }

        return -1; // 未找到匹配项
    }


    /**
     * 遍历背包格子，寻找符合图片的格子
     * @param deviceId 设备ID
     * @param imagePath 图片路径
     * @param similarity 相似度阈值
     * @return 匹配到的格子索引列表
     */
    public List<Integer> findAllItemIndices(String deviceId, String imagePath, double similarity) {
        List<Integer> matchedIndices = new ArrayList<>();
        List<int[]> grids = BagGridUtil.generateBagGrids();

        for (int i = 0; i < grids.size(); i++) {
            int[] rect = grids.get(i);
            try {
                // 直接使用findImage的返回值判断，无需捕获异常
                int[] pos = DeviceHttpClient.findImage(deviceId, rect, imagePath, similarity);
                if (pos[0] != -1) {
                    matchedIndices.add(i); // 找到则加入编号列表
                }
            } catch (IOException e) {
                // 仅处理真正的网络异常或接口错误
                TaskStepNotifier.notifyStep(context.getDeviceId(),
                        "查找格子 " + i + " 时发生异常：" + e.getMessage());
            }
        }
        return matchedIndices;
    }

    //在仓库界面查找仓库中所有符合图片的格子数
    public List<Integer> findcangkuAllItemIndices(String deviceId, String imagePath, double similarity) {
        List<Integer> matchedIndices = new ArrayList<>();
        List<int[]> grids = BagGridUtil.generateCangkuGrids();

        for (int i = 0; i < grids.size(); i++) {
            int[] rect = grids.get(i);
            try {
                // 直接使用findImage的返回值判断，无需捕获异常
                int[] pos = DeviceHttpClient.findImage(deviceId, rect, imagePath, similarity);
                if (pos[0] != -1 ) {
                    matchedIndices.add(i); // 找到则加入编号列表
                }
            } catch (IOException e) {
                // 仅处理真正的网络异常或接口错误
                TaskStepNotifier.notifyStep(context.getDeviceId(),
                        "查找格子 " + i + " 时发生异常：" + e.getMessage());
            }
        }
        return matchedIndices;
    }




    //遍历背包格子，寻找空闲格子
    public List<Integer> findEmptyBagIndices(String deviceId) {
        List<Integer> matchedIndices = new ArrayList<>();
        List<int[]> grids = BagGridUtil.generateBagGrids();

        for (int i = 0; i < grids.size(); i++) {
            int[] rect = grids.get(i);
            try {
                int[] pos = DeviceHttpClient.findMultiColor(deviceId, rect[0],rect[1],rect[2],rect[3], "b8add9","4|19|baacd9,25|19|baacd9,16|14|baacd9,11|6|baacd9,0|27|baacd9,16|29|b9add9,24|12|baacd9,20|1|baacd9,28|23|baacd9,10|21|baacd9,29|27|baacd9,8|4|baacd9,9|29|b9add9,18|27|baacd9,10|24|baacd9,23|19|baacd9,5|23|baacd9,24|2|baacd9,9|1|baacd9,24|29|b9add9,8|12|baacd9,31|14|b9add9,30|23|baacd9,2|2|baacd9,9|20|baacd9,24|24|baacd9,2|0|b8add9,26|4|baacd9",0.8,0);
                if (pos != null && pos[0] > 0 && pos[1] > 0) {
                    matchedIndices.add(i); // 找到则加入编号列表
                }
            } catch (IOException ignored) {
                // 忽略查找失败
            }
        }


        return matchedIndices;
    }

    /**
     * 查找仓库中第一个空格子的索引（找到一个就返回，不遍历所有）
     * @param deviceId 设备ID
     * @return 第一个空格子的索引，未找到返回-1
     */
    public int findFirstEmptyCangkuIndex(String deviceId) {
        List<int[]> grids = BagGridUtil.generateCangkuGrids(); // 获取仓库所有格子的坐标

        for (int i = 0; i < grids.size(); i++) { // 遍历格子
            int[] rect = grids.get(i); // 当前格子的坐标范围
            try {
                // 调用多点找色判断当前格子是否为空（复用原空格子识别逻辑）
                int[] pos = DeviceHttpClient.findMultiColor(
                        deviceId,
                        rect[0], rect[1], rect[2], rect[3],
                        "b1a6dd", // 空格子的特征色
                        "27|19|c6b6e9,28|3|c6b6e9,19|26|c6b6e9,13|12|c6b6e9,6|16|c6b6e9,11|31|c6b6e9,1|1|c5b6ea,7|10|c6b6e9,32|16|c6b6e9,27|21|c6b6e9,32|14|c6b6e9,22|2|c6b6e9,24|31|c6b6e9,25|29|c6b6e9,32|20|c6b6e9,2|12|c5b6ea,4|10|c6b6e9,13|8|c6b6e9,16|17|c6b6e9,4|13|c6b6e9,1|4|c5b7ec,15|20|c6b6e9,6|24|c6b6e9,21|10|c6b6e9,13|29|c6b6e9,31|21|c6b6e9,19|30|c6b6e9,24|0|b6a8de,9|7|c6b5eb,12|14|c6b6e9,14|21|c6b6e9,7|20|c6b6e9,6|22|c6b6e9", // 空格子的多点校验（复用原参数）
                        0.8, 0
                );
                // 如果找到空格子（pos[0]>0），立即返回当前格子索引i
                if (pos != null && pos[0] > 0 && pos[1] > 0) {
                    return i;
                }
            } catch (IOException ignored) {
                // 忽略单个格子的识别失败，继续检查下一个
            }
        }
        return -1; // 所有格子都不是空格子
    }

    //遍历仓库格子，寻找空闲格子，返回空闲格子数次
    public List<Integer> findEmptyCangkuIndices(String deviceId) {
        List<Integer> matchedIndices = new ArrayList<>();
        List<int[]> grids = BagGridUtil.generateCangkuGrids();

        for (int i = 0; i < grids.size(); i++) {
            int[] rect = grids.get(i);
            try {
                int[] pos = DeviceHttpClient.findMultiColor(deviceId, rect[0],rect[1],rect[2],rect[3], "b1a6dd","27|19|c6b6e9,28|3|c6b6e9,19|26|c6b6e9,13|12|c6b6e9,6|16|c6b6e9,11|31|c6b6e9,1|1|c5b6ea,7|10|c6b6e9,32|16|c6b6e9,27|21|c6b6e9,32|14|c6b6e9,22|2|c6b6e9,24|31|c6b6e9,25|29|c6b6e9,32|20|c6b6e9,2|12|c5b6ea,4|10|c6b6e9,13|8|c6b6e9,16|17|c6b6e9,4|13|c6b6e9,1|4|c5b7ec,15|20|c6b6e9,6|24|c6b6e9,21|10|c6b6e9,13|29|c6b6e9,31|21|c6b6e9,19|30|c6b6e9,24|0|b6a8de,9|7|c6b5eb,12|14|c6b6e9,14|21|c6b6e9,7|20|c6b6e9,6|22|c6b6e9",0.8,0);
                if (pos != null && pos[0] > 0 && pos[1] > 0) {
                    matchedIndices.add(i); // 找到则加入编号列表
                }
            } catch (IOException ignored) {
                // 忽略查找失败
            }
        }
        return matchedIndices;
    }

    //遍历仓库界面的背包空格子数
    public List<Integer> findcangkujiemianEmptyBagIndices(String deviceId) {
        List<Integer> matchedIndices = new ArrayList<>();
        List<int[]> grids = BagGridUtil.cangkujiemianBagGrids();

        for (int i = 0; i < grids.size(); i++) {
            int[] rect = grids.get(i);
            try {
                int[] pos = DeviceHttpClient.findMultiColor(deviceId, rect[0],rect[1],rect[2],rect[3], "b1a6dd","27|19|c6b6e9,28|3|c6b6e9,19|26|c6b6e9,13|12|c6b6e9,6|16|c6b6e9,11|31|c6b6e9,1|1|c5b6ea,7|10|c6b6e9,32|16|c6b6e9,27|21|c6b6e9,32|14|c6b6e9,22|2|c6b6e9,24|31|c6b6e9,25|29|c6b6e9,32|20|c6b6e9,2|12|c5b6ea,4|10|c6b6e9,13|8|c6b6e9,16|17|c6b6e9,4|13|c6b6e9,1|4|c5b7ec,15|20|c6b6e9,6|24|c6b6e9,21|10|c6b6e9,13|29|c6b6e9,31|21|c6b6e9,19|30|c6b6e9,24|0|b6a8de,9|7|c6b5eb,12|14|c6b6e9,14|21|c6b6e9,7|20|c6b6e9,6|22|c6b6e9",0.8,0);
                if (pos != null && pos[0] > 0 && pos[1] > 0) {
                    matchedIndices.add(i); // 找到则加入编号列表
                }
            } catch (IOException ignored) {
                // 忽略查找失败
            }
        }
        return matchedIndices;
    }


    //查找所有背包的空格子数
    public List<Integer> findBagEmptyIndices(String deviceId, String imagePath, double similarity) {
        List<Integer> matchedIndices = new ArrayList<>();
        List<int[]> grids = BagGridUtil.generateEmptyBagGrids();

        for (int i = 0; i < grids.size(); i++) {
            int[] rect = grids.get(i);
            try {
                int[] pos = DeviceHttpClient.findImage(deviceId, rect, imagePath, similarity);
                if (pos != null && pos[0] > 0 && pos[1] > 0) {
                    matchedIndices.add(i); // 找到则加入编号列表
                }
            } catch (IOException ignored) {
                // 忽略查找失败
            }
        }


        return matchedIndices;
    }



    //遍历背包格子，挨个双击格子
    public void clickAllMatchedGrids(String deviceId, List<Integer> indices) {
        List<int[]> grids = BagGridUtil.generateBagGrids();
        HumanLikeController human = new HumanLikeController(taskThread);

        for (int index : indices) {
            if (index < 0 || index >= grids.size()) continue;
            int[] rect = grids.get(index);
            int centerX = (rect[0] + rect[2]) / 2;
            int centerY = (rect[1] + rect[3]) / 2;


            try {
                human.doubleclick(deviceId, centerX, centerY, 10, 10);
                Thread.sleep( new java.util.Random().nextInt(200) + 300); // 每次点击间隔，避免过快
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    //双击仓库的第gridIndex个格子
    public void doubleclickCangkuGrid(String deviceId, int gridIndex) {
        List<int[]> grids = BagGridUtil.generateCangkuGrids();
        if (gridIndex < 0 || gridIndex >= grids.size()) return;

        int[] rect = grids.get(gridIndex);
        int centerX = (rect[0] + rect[2]) / 2;
        int centerY = (rect[1] + rect[3]) / 2;

        HumanLikeController human = new HumanLikeController(taskThread);
        try {
            human.doubleclick(deviceId, centerX, centerY, 10, 10); // 偏移点击
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //单击仓库的第gridIndex个格子
    public void clickCangkuGrid(String deviceId, int gridIndex) {
        List<int[]> grids = BagGridUtil.generateCangkuGrids();
        if (gridIndex < 0 || gridIndex >= grids.size()) return;

        int[] rect = grids.get(gridIndex);
        int centerX = (rect[0] + rect[2]) / 2;
        int centerY = (rect[1] + rect[3]) / 2;

        HumanLikeController human = new HumanLikeController(taskThread);
        try {
            human.click(deviceId, centerX, centerY, 10, 10); // 偏移点击
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //单击仓库的第gridIndex个格子
    public void clickBagGrid(String deviceId, int gridIndex) {
        List<int[]> grids = BagGridUtil.generateBagGrids();
        if (gridIndex < 0 || gridIndex >= grids.size()) return;

        int[] rect = grids.get(gridIndex);
        int centerX = (rect[0] + rect[2]) / 2;
        int centerY = (rect[1] + rect[3]) / 2;

        HumanLikeController human = new HumanLikeController(taskThread);
        try {
            human.click(deviceId, centerX, centerY, 10, 10); // 偏移点击
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //双击背包的第gridIndex个格子
    public void doubleclickBagGrid(String deviceId, int gridIndex) {
        List<int[]> grids = BagGridUtil.generateBagGrids();
        if (gridIndex < 0 || gridIndex >= grids.size()) return;

        int[] rect = grids.get(gridIndex);
        int centerX = (rect[0] + rect[2]) / 2;
        int centerY = (rect[1] + rect[3]) / 2;

        HumanLikeController human = new HumanLikeController(taskThread);
        try {
            human.doubleclick(deviceId, centerX, centerY, 10, 10); // 偏移点击
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //点击给予界面的的第gridIndex个格子
    public void clickJiyuJiemianBagGrid(String deviceId, int gridIndex) {
        List<int[]> grids = BagGridUtil.jiyujiemianBagGrids();
        if (gridIndex < 0 || gridIndex >= grids.size()) return;

        int[] rect = grids.get(gridIndex);
        int centerX = (rect[0] + rect[2]) / 2;
        int centerY = (rect[1] + rect[3]) / 2;

        HumanLikeController human = new HumanLikeController(taskThread);
        try {
            human.click(deviceId, centerX, centerY, 8, 8); // 偏移点击
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //识别仓库当前页数和总页数
    public int[] getDangqianyeAndQuanbuye(){
        return  new int[]{0,0};
    }

    //仓库向下翻一页
    public void pageDown(){
        HumanLikeController human = new HumanLikeController(taskThread);
        try {
            human.click(context.getDeviceId(), 204,357,2,2);
            Thread.sleep(waittime);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 仓库向上翻一页
     */
    public void pageUp() {
        HumanLikeController human = new HumanLikeController(taskThread);
        try {
            human.click(context.getDeviceId(), 95, 357, 2, 2); // 上一页按钮坐标
            Thread.sleep(waittime);
            TaskStepNotifier.notifyStep(context.getDeviceId(), "向上翻页成功");
        } catch (Exception e) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "向上翻页失败: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 判断是否可以向上翻页
     */
    public boolean canPageUp() {
        try {
            int[] yeshu = ocrCangkuyeshu();
            return yeshu[0] > 1; // 当前页大于1时可上翻
        } catch (Exception e) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "判断是否可上翻失败: " + e.getMessage());
            return false;
        }
    }

    //获得仓库的当前页数
    public int getWarehouseCurrentPages() {
        int[] yeshu = ocrCangkuyeshu();
        int currentPage = yeshu[0];
        int totalPages = yeshu[1];
        return currentPage;
    }

    //获得仓库的总页数
    public int getWarehouseTotalPages() {
        int[] yeshu = ocrCangkuyeshu();
        int currentPage = yeshu[0];
        int totalPages = yeshu[1];
        return totalPages;
    }

    /**
     * 跳转到指定仓库页（支持上下双向翻页）
     */
    public void gotoPage(int targetPage) {
        try {
            int[] yeshu = ocrCangkuyeshu();
            int currentPage = yeshu[0];
            int totalPages = yeshu[1];

            if (targetPage < 1 || targetPage > totalPages) {
                TaskStepNotifier.notifyStep(context.getDeviceId(), "目标页超出范围: " + targetPage);
                return;
            }

            if (currentPage == targetPage) {
                TaskStepNotifier.notifyStep(context.getDeviceId(), "已在第" + targetPage + "页");
                return;
            }

            // 根据目标页与当前页的关系选择翻页方向
            while (ocrCangkuyeshu()[0] != targetPage) {
                int current = ocrCangkuyeshu()[0];
                if (current < targetPage) {
                    if (canPageDown()) {
                        pageDown(); // 下翻
                    } else {
                        TaskStepNotifier.notifyStep(context.getDeviceId(), "无法下翻，已到最后一页");
                        break;
                    }
                } else {
                    if (canPageUp()) {
                        pageUp(); // 上翻
                    } else {
                        TaskStepNotifier.notifyStep(context.getDeviceId(), "无法上翻，已到第一页");
                        break;
                    }
                }
            }

            TaskStepNotifier.notifyStep(context.getDeviceId(), "成功跳转到第" + targetPage + "页");
        } catch (Exception e) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "跳转页面异常: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }




    //双击仓库界面的背包的第gridIndex个格子
    public void doubleclickcangkujiemianBagGrid(String deviceId, int gridIndex) {
        List<int[]> grids = BagGridUtil.cangkujiemianBagGrids();
        if (gridIndex < 0 || gridIndex >= grids.size()) return;

        int[] rect = grids.get(gridIndex);
        int centerX = (rect[0] + rect[2]) / 2;
        int centerY = (rect[1] + rect[3]) / 2;

        HumanLikeController human = new HumanLikeController(taskThread);
        try {
            human.doubleclick(deviceId, centerX, centerY, 10, 10); // 偏移点击
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 打开地图后输入坐标，确定后关闭地图（优化版：基于基准图片1推算其他位置）
     * 适配布局：
     * 第一行：1 2 3 删除
     * 第二行：4 5 6 0
     * 第三行：7 8 9 确定
     * 每个按钮大小：48×48像素，间距：10像素
     * @param str 坐标字符串，格式如"243,11"
     */
    public void clickInputPos(String str) {
        HumanLikeController human = new HumanLikeController(taskThread);
        // 空白区域坐标（避免鼠标遮挡）
        final int BLANK_X = 100;
        final int BLANK_Y = 500;
        // 打开地图最大尝试次数
        final int MAX_MAP_OPEN_ATTEMPTS = 5;
        int mapOpenAttempts = 0;
        // 按钮尺寸和间距参数
        final int BUTTON_WIDTH = 48;  // 按钮宽度
        final int BUTTON_HEIGHT = 48; // 按钮高度
        final int GRID_SPACING = 10;  // 按钮间距

        // 基于按钮尺寸和间距计算的相对位置偏移量
        final int ROW_SPACING = BUTTON_HEIGHT + GRID_SPACING; // 行间距
        final int COL_SPACING = BUTTON_WIDTH + GRID_SPACING;  // 列间距

        try {
            // 步骤1：打开地图（带超时控制）
            TaskStepNotifier.notifyStep(context.getDeviceId(), "打开地图...");
            while (true) {
                if (taskThread.isStopped() || Thread.currentThread().isInterrupted()) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "任务中断，终止操作");
                    return;
                }
                taskThread.checkPause();
                human.click(context.getDeviceId(), 86, 32, 20, 10); // 打开地图
                Thread.sleep(500);

                // 检测地图是否打开
                int[] mapCheck = DeviceHttpClient.findMultiColor(
                        context.getDeviceId(), 1, 1, 2000, 2000,
                        "aeadbb",
                        "7|12|b3b7cb,18|8|b3b7cb,3|19|b3b7c9,28|7|b5b6c9,15|14|b3b7cb,10|11|b3b7cb,3|11|b2b8cb,15|3|b6b6c9,7|21|b4b7cb,16|11|b3b7cb,28|6|b5b6c9,13|20|b3b7cb,1|5|b3b7cb,7|8|b3b7cb,11|22|b4b7cb,5|10|b2b8cb,7|17|b3b7cb,2|1|b7b5c9,5|7|b3b7cc,21|10|b3b7cb,3|21|b4b7c9",
                        0.9, 0
                );
                if (mapCheck[0] > 0) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "地图已打开");
                    break;
                }
                if (++mapOpenAttempts >= MAX_MAP_OPEN_ATTEMPTS) {
                    throw new RuntimeException("打开地图失败，超过最大尝试次数");
                }
                Thread.sleep(500);
            }

            // 步骤2：激活坐标输入区域
            TaskStepNotifier.notifyStep(context.getDeviceId(), "激活坐标输入区域...");
            int[] inputArea = DeviceHttpClient.findMultiColor(
                    context.getDeviceId(), 1, 1, 2000, 2000,
                    "aeadbb",
                    "7|12|b3b7cb,18|8|b3b7cb,3|19|b3b7c9,28|7|b5b6c9,15|14|b3b7cb,10|11|b3b7cb,3|11|b2b8cb,15|3|b6b6c9,7|21|b4b7cb,16|11|b3b7cb,28|6|b5b6c9,13|20|b3b7cb,1|5|b3b7cb,7|8|b3b7cb,11|22|b4b7cb,5|10|b2b8cb,7|17|b3b7cb,2|1|b7b5c9,5|7|b3b7cc,21|10|b3b7cb,3|21|b4b7c9",
                    0.9, 0
            );
            if (inputArea[0] <= 0) {
                throw new RuntimeException("未找到坐标输入区域");
            }
            int a = new java.util.Random().nextInt(10) + 10;
            int b = new java.util.Random().nextInt(10) + 10;
            human.click(context.getDeviceId(), inputArea[0] + a, inputArea[1] + b, 0, 0);
            Thread.sleep(new java.util.Random().nextInt(2000) + 400);

            // 步骤3：找到基准图片"1"的位置（以此推算其他数字）
            TaskStepNotifier.notifyStep(context.getDeviceId(), "查找数字1的位置...");
            int[] num1Pos = DeviceHttpClient.findImage(context.getDeviceId(),"坐标1",0.8);
            if (num1Pos[0] <= 0) {
                throw new RuntimeException("未找到数字1的位置，无法推算其他数字");
            }
            TaskStepNotifier.notifyStep(context.getDeviceId(), "找到数字1位置：(" + num1Pos[0] + "," + num1Pos[1] + ")");

            // 计算按钮中心偏移量（从按钮左上角到中心的偏移）
            final int CENTER_OFFSET_X = BUTTON_WIDTH / 2;
            final int CENTER_OFFSET_Y = BUTTON_HEIGHT / 2;

            // 以数字1的左上角为基准点，计算各按钮中心坐标
            int baseX = num1Pos[0] - CENTER_OFFSET_X;  // 数字1按钮的左上角x坐标
            int baseY = num1Pos[1] - CENTER_OFFSET_Y;  // 数字1按钮的左上角y坐标

            // 步骤4：输入坐标（通过推算的坐标点击）
            TaskStepNotifier.notifyStep(context.getDeviceId(), "开始输入坐标：" + str);
            for (int i = 0; i < str.length(); i++) {
                if (taskThread.isStopped() || Thread.currentThread().isInterrupted()) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "任务中断，停止输入");
                    return;
                }
                taskThread.checkPause();

                char ch = str.charAt(i);
                int targetX = 0;
                int targetY = 0;

                // 根据字符推算目标按钮中心坐标（按实际键盘布局调整）
                switch (ch) {
                    case '1':
                        targetX = baseX + CENTER_OFFSET_X;
                        targetY = baseY + CENTER_OFFSET_Y;
                        break;
                    case '2':
                        targetX = baseX + COL_SPACING + CENTER_OFFSET_X;  // 1的右侧
                        targetY = baseY + CENTER_OFFSET_Y;
                        break;
                    case '3':
                        targetX = baseX + 2 * COL_SPACING + CENTER_OFFSET_X;  // 2的右侧
                        targetY = baseY + CENTER_OFFSET_Y;
                        break;
                    case '4':
                        targetX = baseX + CENTER_OFFSET_X;
                        targetY = baseY + ROW_SPACING + CENTER_OFFSET_Y;  // 1的下方
                        break;
                    case '5':
                        targetX = baseX + COL_SPACING + CENTER_OFFSET_X;
                        targetY = baseY + ROW_SPACING + CENTER_OFFSET_Y;  // 4的右侧
                        break;
                    case '6':
                        targetX = baseX + 2 * COL_SPACING + CENTER_OFFSET_X;
                        targetY = baseY + ROW_SPACING + CENTER_OFFSET_Y;  // 5的右侧
                        break;
                    case '7':
                        targetX = baseX + CENTER_OFFSET_X;
                        targetY = baseY + 2 * ROW_SPACING + CENTER_OFFSET_Y;  // 4的下方
                        break;
                    case '8':
                        targetX = baseX + COL_SPACING + CENTER_OFFSET_X;
                        targetY = baseY + 2 * ROW_SPACING + CENTER_OFFSET_Y;  // 7的右侧
                        break;
                    case '9':
                        targetX = baseX + 2 * COL_SPACING + CENTER_OFFSET_X;
                        targetY = baseY + 2 * ROW_SPACING + CENTER_OFFSET_Y;  // 8的右侧
                        break;
                    case '0':
                        targetX = baseX + 3 * COL_SPACING + CENTER_OFFSET_X;  // 第二行第4个
                        targetY = baseY + ROW_SPACING + CENTER_OFFSET_Y;
                        break;
                    case ',':  // 逗号对应"确定"按钮
                        targetX = baseX + 3 * COL_SPACING + CENTER_OFFSET_X;  // 第三行第4个
                        targetY = baseY + 2 * ROW_SPACING + CENTER_OFFSET_Y;
                        break;
                    default:
                        throw new RuntimeException("不支持的字符：" + ch);
                }

                Thread.sleep(100);
                human.click(context.getDeviceId(), targetX, targetY, 5, 5);  // 增加点击范围容错
                TaskStepNotifier.notifyStep(context.getDeviceId(), "输入字符：" + ch );
                Thread.sleep(new java.util.Random().nextInt(20) + 20);
            }

            // 步骤5：最终确认并关闭地图
            int confirmX = baseX + 3 * COL_SPACING + CENTER_OFFSET_X;  // "确定"按钮位置
            int confirmY = baseY + 2 * ROW_SPACING + CENTER_OFFSET_Y;
            human.click(context.getDeviceId(), confirmX, confirmY, 5, 5);  // 点击确定
            Thread.sleep(500);
            human.clickImg(context.getDeviceId(), "关闭地图", 7, 7);
            TaskStepNotifier.notifyStep(context.getDeviceId(), "坐标输入完成，地图已关闭");

        } catch (Exception e) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "坐标输入失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    //使用飞行符
    public void useFeixingfu(){
        HumanLikeController human = new HumanLikeController(taskThread);
        try {
            Thread.sleep(new java.util.Random().nextInt(101) + 200);
            openBag();
            Thread.sleep(new java.util.Random().nextInt(101) + 200);
            while (true){
                if (taskThread.isStopped()||Thread.currentThread().isInterrupted()) return;
                taskThread.checkPause();
                int feixingfugezi=findItemIndex(context.getDeviceId(),"飞行符",0.8);
                if(feixingfugezi > -1){
                    doubleclickBagGrid(context.getDeviceId(),feixingfugezi);
                    Thread.sleep(new java.util.Random().nextInt(101) + 200);
                    break;
                /*int[] cen = BagGridUtil.getGridCenter(feixingfugezi);
                human.doubleclick(context.getDeviceId(),cen[0],cen[1],5,5);*/
                }
                TaskStepNotifier.notifyStep(context.getDeviceId(),"没有找到飞行符.bmp");
                Thread.sleep(new java.util.Random().nextInt(101) + 200);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //使用飞行符飞到哪个地方
    public void userFeixingfuToMudidi(String Mudidi){
        HumanLikeController human = new HumanLikeController(taskThread);
        try {
            useFeixingfu();
            Thread.sleep( new java.util.Random().nextInt(301) + 200);
            int[]jiancefeixingdakaijiemian = DeviceHttpClient.findImage(context.getDeviceId(),"飞行符打开",0.8);

            if (jiancefeixingdakaijiemian[0]>0){
                switch (Mudidi){
                    case "建邺城":human.click(context.getDeviceId(),468,253,4,4);
                        break;
                    case "长安城":human.click(context.getDeviceId(),457,193,20,20);
                        break;
                    case "长寿村":human.click(context.getDeviceId(),294,122,4,4);
                        break;
                    case "西凉女国":human.click(context.getDeviceId(),288,163,4,4);
                        break;
                    case "宝象国":human.click(context.getDeviceId(),281,224,4,4);
                        break;
                    case "朱紫国":human.click(context.getDeviceId(),332,275,4,4);
                        break;
                    case "傲来国":human.click(context.getDeviceId(),584,297,10,10);
                        break;
                }
            }
            Thread.sleep(waittime);
            human.click(context.getDeviceId(),614,32,5,5);
            Thread.sleep(waittime);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


    //打开长安仓库，从当前已经在长安城开始

    public void openChanganCangku(){

    }


    //打开建邺仓库，从当前已经在建邺城开始
    public void openJianyeCangku(){
        String diqu = ocrShibieDiqu();
        CommonActions commonActions = new CommonActions(context,taskThread);
        if(!diqu.equals("建邺城") ){
            commonActions.userFeixingfuToMudidi("建邺城");
        }
        HumanLikeController human = new HumanLikeController(taskThread);
        String str="56,33";
        /*int[]pos = new int[2];
                    pos=common.ocrZuobiao();
                    System.out.println(pos[0]+","+pos[1]);*/
        int[]pos = new int[2];
        pos=ocrZuobiao();

        if (pos[0] != 56 || pos[1] != 33){
            clickInputPos(str);//打开地图，输入坐标，确定
            while (true) {
                if (taskThread.isStopped() || Thread.currentThread().isInterrupted()) return;
                taskThread.checkPause();
                try {
                    Thread.sleep(2000);
                    pos = ocrZuobiao();
                    if (pos[0] == 56 && pos[1] == 33) {
                        Thread.sleep(2000);
                        break;
                    } else Thread.sleep(500);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        try {
            human.click(context.getDeviceId(),337,204,2,2);//点击仓库管理员
            Thread.sleep(500);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        int[]zizuo = {1,1};
        int[]zijuese = {1,1};
        while (zizuo[0]>0 || zijuese[0]>0){
            try {
                //判断点击仓库管理员后有没有出现打开仓库选项
                zizuo = DeviceHttpClient.findImage(context.getDeviceId(),"打开仓库按钮",0.8);
                //判断点击仓库管理员后是否有其他选项
                System.out.println(zizuo[0]);
                zijuese = DeviceHttpClient.findMultiColor(context.getDeviceId(),0,0,735,401,"101c1c","22|6|dbdde1,16|4|ced1d6,9|6|56575a,4|0|516161,11|10|d5d6de,23|10|2c2d35,6|5|727175,5|14|182028,18|9|dee1e5,24|5|a6a4a9,19|0|39494c,21|2|cacbd0,18|11|4e5056",0.8,0);
                if (zijuese[0]<0 && zizuo[0]>0) {human.click(context.getDeviceId(),623,251,60,7);//点击我要进行仓库操作
                    Thread.sleep(waittime);
                    break;
                }
                else if (zijuese[0]>0) {
                    int[] poscangku= null;
                    TaskStepNotifier.notifyStep(context.getDeviceId(),"寻找仓库管理员选项坐标");
                    poscangku = DeviceHttpClient.findMultiColor(context.getDeviceId(), 0, 0, 735, 401, "3c5469", "2|13|275362,10|11|baccd4,23|4|b8c2c8,18|6|acadb9,26|3|91a4ae,10|3|abb1be,10|9|c5d8e2,29|12|9ea5b5,26|10|6e7f87,16|9|849ba5,14|9|355268,8|9|27505d,27|11|9eb5bc", 0.8, 0);
                    TaskStepNotifier.notifyStep(context.getDeviceId(),"点击仓库管理员选项");
                    human.click(context.getDeviceId(), poscangku[0], poscangku[1], 0, 0);//点击仓库管理员选项
                    Thread.sleep(waittime);
                    human.click(context.getDeviceId(), 623, 251, 70, 15);//点击我要进行仓库操作
                    Thread.sleep(waittime);
                    break;
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }

        }

    }

    public boolean ifOpenCangku(){
        try {
            int[]jianceopen = DeviceHttpClient.findMultiColor(context.getDeviceId(),338,25,365,45,"3e4188","8|3|30367c,6|6|686daf,1|8|dde2ed,26|15|475487,19|8|f2ecf8,12|8|595e8f,13|7|43468f,10|12|959cd5,15|0|3e418a,23|10|d2d1e2,8|0|3c4387,20|8|9591b5,7|0|3b448b,11|18|3e4793,19|19|404388,12|3|31347a",0.8,0);
            if (jianceopen[0]<0) {
                return false;
            }
            else if (jianceopen[0]>0) {
                return  true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return false;
    }



    /// ////////////////////////////////////////////////////////////////


    /**
     * 藏宝图转移主流程：按以下逻辑循环直至仓库无图
     * 1. 打开背包检查数量，≥5则开始给予
     * 2. 首次给予：输入好友ID→操作菜单→给予→重复至无图
     * 3. 仓库取图→直接操作好友菜单给予→重复
     */
    public void transferTreasureMaps(String treasureMapImg, String friendId) {
        HumanLikeController human = new HumanLikeController(taskThread);
        boolean hasTreasureInWarehouse = true;
        boolean firstGive = true; // 标记首次给予

        try {
            // 步骤1：初始检查背包藏宝图数量
            openBag();
            Thread.sleep(waittime);
            int initialBagCount = getBagTreasureCount(treasureMapImg);
            closeBag();
            Thread.sleep(waittime);
            TaskStepNotifier.notifyStep(context.getDeviceId(), "初始背包藏宝图数量：" + initialBagCount);

            // 若数量≥5，开始给予流程
            if (initialBagCount < 3) {
                TaskStepNotifier.notifyStep(context.getDeviceId(), "藏宝图不足5个，直接从仓库取图");
            } else {
                TaskStepNotifier.notifyStep(context.getDeviceId(), "藏宝图≥5个，开始给予好友...");
                // 首次给予完整流程
                boolean hasTreasure = giveUntilEmpty(treasureMapImg, friendId, firstGive);
                firstGive = false; // 首次给予完成

                // 若给予后仍有剩余（如单次给予不足），继续给予
                if (hasTreasure) {
                    giveUntilEmpty(treasureMapImg, friendId, firstGive);
                }
            }

            // 步骤2：循环从仓库取图并给予
            while (hasTreasureInWarehouse && !taskThread.isStopped()) {
                taskThread.checkPause();

                // 步骤2.1：从仓库取图
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "背包为空，从仓库取图...");
                    Thread.sleep(waittime);
                    // 打开仓库
                    if (!ifOpenCangku()) {
                        openJianyeCangku();
                        Thread.sleep(waittime);
                    }

                    // 取图前检查仓库是否有图
                    int treasureGrid = findifCangkuItem(context.getDeviceId(), treasureMapImg, 0.8);
                    if (treasureGrid < 0 && !canPageDown()) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(), "仓库所有页数无藏宝图，任务结束");
                        hasTreasureInWarehouse = false;
                        break;
                    }

                    // 取满背包空格
                    List<Integer> emptySlots = findcangkujiemianEmptyBagIndices(context.getDeviceId());
                    if (emptySlots.isEmpty()) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(), "背包已满，先给予再取图");
                        giveUntilEmpty(treasureMapImg, friendId, firstGive);
                        emptySlots = findcangkujiemianEmptyBagIndices(context.getDeviceId());
                    }

                    // 从仓库取图
                    int takenCount = takeFromWarehouseToBag(treasureMapImg, emptySlots.size());
                    if (takenCount <= 0) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(), "仓库无图可取，任务结束");
                        hasTreasureInWarehouse = false;
                        break;
                    }
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "从仓库取到" + takenCount + "个藏宝图");
                    closeWarehouse(); // 取完图关闭仓库
                    Thread.sleep(waittime);


                // 步骤2.2：取图后直接通过好友菜单给予（利用已打开的好友界面）
                TaskStepNotifier.notifyStep(context.getDeviceId(), "开始给予好友...");
                boolean hasRemaining = giveUntilEmpty(treasureMapImg, friendId, firstGive);
                firstGive = false; // 首次给予后标记为false

                // 若给予后仍有剩余（如给予失败），强制处理
                if (hasRemaining) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "仍有剩余藏宝图，继续给予");
                    giveUntilEmpty(treasureMapImg, friendId, firstGive);
                }
            }

            // 结束：
            TaskStepNotifier.notifyStep(context.getDeviceId(), "所有藏宝图转移完成");
        } catch (Exception e) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "转移失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 循环给予好友直至某次给予时无藏宝图
     * @param firstGive 是否首次给予（控制是否输入好友ID）
     * @return 给予结束后是否仍有剩余（true=有剩余，false=无）
     */
    private boolean giveUntilEmpty(String treasureMapImg, String friendId, boolean firstGive) throws InterruptedException, IOException {
        HumanLikeController human = new HumanLikeController(taskThread);


        // 首次给予：打开好友界面→输入ID
        if (firstGive) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "首次给予：打开好友界面并输入ID");
            human.click(context.getDeviceId(), 700, 325, 10, 10); // 打开好友界面
            Thread.sleep(waittime);
            human.click(context.getDeviceId(), 630, 90, 50, 6); // 点击ID输入框
            Thread.sleep(300);
            human.sendkey(context.getDeviceId(), friendId); // 输入好友ID
            Thread.sleep(waittime);
        }

        // 点击好友操作菜单




        // 循环给予：每次点击好友操作菜单→给予→直至无图
        while (true) {
            if (taskThread.isStopped()) return false;
            taskThread.checkPause();
            TaskStepNotifier.notifyStep(context.getDeviceId(), "点击好友操作菜单");
            human.click(context.getDeviceId(), 704, 141, 5, 5); // 好友操作菜单按钮
            Thread.sleep(waittime);
            // 点击给予按钮
            human.click(context.getDeviceId(), 538, 244, 20, 10); // 给予按钮
            Thread.sleep(waittime);
            // 检查给予界面是否有藏宝图（无则退出循环）
            List<Integer> treasureIndices = findJiyujiemianAllItemIndices(context.getDeviceId(), treasureMapImg, 0.8);
            if (treasureIndices.isEmpty()) {
                TaskStepNotifier.notifyStep(context.getDeviceId(), "给予界面无藏宝图，结束本次给予");
                // 关闭给予界面（保留好友界面）
                human.click(context.getDeviceId(), 717, 26, 5, 5); // 关闭给予弹窗
                Thread.sleep(waittime);
                return false; // 无剩余
            }



            // 选择最多3个藏宝图给予
            int giveCount = Math.min(3, treasureIndices.size());
            TaskStepNotifier.notifyStep(context.getDeviceId(), "选择" + giveCount + "个藏宝图给予");
            for (int i = 0; i < giveCount; i++) {
                clickJiyuJiemianBagGrid(context.getDeviceId(), treasureIndices.get(i));
                Thread.sleep(waittime);
            }

            // 确定给予
            human.click(context.getDeviceId(), 578, 381, 20, 10); // 确定按钮
            Thread.sleep(waittime); // 等待给予完成


        }
    }

    /**
     * 从仓库取藏宝图（带翻页逻辑）
     */
    private int takeFromWarehouseToBag(String treasureMapImg, int takeCount) throws InterruptedException, IOException {
        if (!ifOpenCangku()) {
            openJianyeCangku();
            Thread.sleep(1000);
        }

        int takenCount = 0;
        int maxTake = takeCount;
        while (takenCount < maxTake) {
            if (taskThread.isStopped()) break;
            taskThread.checkPause();

            // 查找仓库中的藏宝图
            int treasureGrid = findifCangkuItem(context.getDeviceId(), treasureMapImg, 0.8);
            if (treasureGrid < 0) {
                if (canPageDown()) { // 翻页继续找
                    pageDown();
                    Thread.sleep(500);
                    continue;
                } else {
                    break; // 无更多图
                }
            }

            List<Integer> baotuweizhi= findcangkuAllItemIndices(context.getDeviceId(), treasureMapImg, 0.8);

            for (int i=0; i<baotuweizhi.size(); i++) {
                // 取图
                doubleclickCangkuGrid(context.getDeviceId(), baotuweizhi.get(i));
                takenCount++;
                Thread.sleep(300 + new Random().nextInt(200));
            }


        }

        return takenCount;
    }

    // 保留必要的辅助方法，移除多余逻辑
    private boolean ifFriendInterfaceOpen() throws IOException {
        int[] pos = DeviceHttpClient.findMultiColor(context.getDeviceId(), 612,45,649,65,"2e4552","1|11|3b4f60,16|3|89969e",0.8,0);
        return pos != null && pos[0] > 0;
    }

    private void closeFriendInterface() throws IOException {
        HumanLikeController human = new HumanLikeController(taskThread);
        human.click(context.getDeviceId(), 717, 26, 5, 5); // 关闭按钮
    }
    // 关闭背包
    public void closeBag() {
        HumanLikeController human = new HumanLikeController(taskThread);
        try {
            human.click(context.getDeviceId(), 614,35,4,4);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取给予界面中的藏宝图数量
     */
    private int getTreasureCountInGiveInterface(String treasureMapImg) throws IOException {
        int count =findJiyujiemianAllItemIndices(context.getDeviceId(), treasureMapImg, 0.8).size();

        return count;
    }

    // 以下为需要补充实现的辅助方法（根据UI实际情况）
    public boolean canPageDown() throws IOException {
        // 检查是否有"下一页"按钮
        int[]yeshu =ocrCangkuyeshu();

        return yeshu[0]<yeshu[1];
    }

    private int getBagMaxCapacity() {
        // 背包最大容量（例如固定为20格，或通过格子总数计算）
        return BagGridUtil.generateBagGrids().size();
    }

    private int getBagTreasureCount(String treasureMapImg) {
        // 获取背包中藏宝图数量
        return findAllItemIndices(context.getDeviceId(), treasureMapImg, 0.8).size();
    }

     public void closeWarehouse() throws IOException {
        // 点击仓库关闭按钮（坐标需根据实际UI调整）
        HumanLikeController human = new HumanLikeController(taskThread);
        human.click(context.getDeviceId(), 617, 32, 5, 5); // 坐标
    }

    // 以下为好友操作的辅助方法（需根据UI实际情况实现）
    private void openFriendInterface() throws IOException {
        HumanLikeController human = new HumanLikeController(taskThread);
        human.click(context.getDeviceId(), 700, 325, 10, 10); // 好友按钮坐标
    }

    private void inputFriendId(String friendId) throws IOException, InterruptedException {
        // 模拟输入好友ID（例如点击输入框→输入文字）
        HumanLikeController human = new HumanLikeController(taskThread);
        human.click(context.getDeviceId(), 630, 90, 50, 6); // 输入框坐标
        Thread.sleep(300);
        // 若支持直接输入文字，可调用输入API；否则模拟按键点击
        human.sendkey(context.getDeviceId(), friendId);
    }

    private void clickFriendMenu() throws IOException {
        // 点击好友菜单按钮
        HumanLikeController human = new HumanLikeController(taskThread);
        human.click(context.getDeviceId(), 704, 141, 5, 5);
    }

    private void clickGiveButton() throws IOException {
        // 点击"给予"按钮
        HumanLikeController human = new HumanLikeController(taskThread);
        human.click(context.getDeviceId(), 538, 244, 20, 10);
    }
    private void clickquedingGiveButton() throws IOException {
        // 点击"给予"按钮
        HumanLikeController human = new HumanLikeController(taskThread);
        human.click(context.getDeviceId(), 578, 381, 20, 10);
    }

     /// ///////////////////////////////

    /**
     * 识别藏宝图地区（使用多点找色）
     * @param context 任务上下文
     * @return 识别到的地区名称，未识别到则返回空字符串
     */
    String recognizeMapArea(TaskContext context) throws IOException {
        String deviceId = context.getDeviceId();

        // 定义各地区的多点找色参数（根据实际游戏调整）
        Map<String, MapAreaColorConfig> areaColorConfigs = new HashMap<>();

        // 添加"花果山"地区的识别参数
        areaColorConfigs.put("花果山", new MapAreaColorConfig(
                "161e26","6|3|38b735,14|12|3bd034,17|6|0e1520,33|2|131a26,2|11|1a1c28,11|11|028404,44|2|131f27,20|7|109011,35|8|080814,17|7|0e291d,16|11|034209,25|4|35ca3b,2|6|111d27,16|7|121321,42|8|65c661,18|11|19661c,25|6|38c03e,31|7|41be48",0.8
        ));

        // 添加更多地区...
        areaColorConfigs.put("建邺城", new MapAreaColorConfig(
                "171f27","34|7|242c2c,19|2|5ac45a,34|12|47db47,37|9|676270,10|4|2aec2a,5|4|3ece3b,31|6|1b2e23,13|12|12a113,18|12|2a972e,44|9|102b1d,0|9|171f27,32|8|31e234,43|12|16c316,33|4|13dc12,3|10|0e1422,25|6|1ceb12,28|2|32f335,4|13|229d24",0.8
        ));
        areaColorConfigs.put("东海湾", new MapAreaColorConfig(
                "161e26","1|6|141a25,1|10|181b25,25|7|1df916,9|1|08520c,44|11|021609,18|2|33cd38,10|3|22ce24,31|6|1a6021,11|1|191729,41|10|1af017,13|10|55be5d,1|11|161d28,0|4|161e26,22|8|10ee0e,32|0|1a2722,44|3|1d0e33,32|9|2dbb35,17|9|141a28",0.8
        ));
        areaColorConfigs.put("江南野外", new MapAreaColorConfig(
                "161e26","33|11|219d28,10|10|272936,7|6|0f121f,18|7|31e131,29|7|07dc06,33|1|18ad21,23|8|27ff10,28|6|5ab25b,12|1|0f371d,18|9|2de42f,21|3|0da306,14|3|24782c,24|8|12f008,31|4|4dc448,5|5|087309,44|9|051b11,4|13|278f2f,17|8|33783c",0.8
        ));
        areaColorConfigs.put("朱紫国", new MapAreaColorConfig(
                "161e26","10|1|19991e,27|9|36fa32,23|1|242335,30|9|110d21,41|4|1d881b,12|4|30972c,15|3|0f980e,11|1|0b1f21,10|2|17c611,35|10|22d720,31|1|1f4232,18|4|28ce27,9|11|31d72b,24|9|11da13,29|2|08360f,4|9|181c28,11|4|21a11e,29|10|11b40c",0.8
        ));
        areaColorConfigs.put("五庄观", new MapAreaColorConfig(
                "151c26","33|10|2eeb27,40|13|1f6325,41|0|1e2919,11|13|0e7011,4|7|113a1a,16|2|12181d,22|4|0b0d1a,2|4|161e23,2|13|182327,24|7|15fe0e,28|5|1a1629,23|6|3e9945,25|8|09840b,44|0|0f1e30,25|6|05a100,24|6|15fa0a,39|5|1ae621,37|10|262e31",0.8
        ));
        areaColorConfigs.put("北俱芦洲", new MapAreaColorConfig(
                "171c27","26|10|24f823,17|10|14101f,32|4|11451a,3|13|0a2d1b,0|3|161d28,36|7|0c301d,5|6|0a0e17,23|9|18b819,26|12|25663a,29|10|1ed517,38|2|1efa19,15|3|10cf0e,11|7|20c01f,15|11|1aeb19,17|12|12131c,4|5|298228,6|2|130b20,11|2|1cbb1d",0.8
        ));
        areaColorConfigs.put("傲来国", new MapAreaColorConfig(
                "161e26","15|13|299d35,41|12|17af11,35|12|0ead0f,11|2|15571d,13|4|0c8f10,7|13|088009,21|11|084b09,41|9|0c7611,25|2|18a315,18|2|218231,43|2|07a310,41|4|29842c,3|3|170b27,5|1|449f51,36|10|26e923,0|4|151d25,26|0|37391b,33|5|073017",0.8
        ));
        areaColorConfigs.put("狮驼岭", new MapAreaColorConfig(
                "161e26","28|12|41e33f,30|11|003c02,0|3|151c27,38|7|19971d,16|3|0d201a,31|3|3d8d4a,12|6|1cd81e,15|1|1f491d,5|1|0b7514,17|12|1f1226,2|6|141c1f,33|6|1ff919,22|8|28e226,25|0|323e18,5|7|41d641,44|2|161e27,6|13|0e2e15,35|7|21cb28",0.8
        ));
        areaColorConfigs.put("大唐境外", new MapAreaColorConfig(
                "171e28","12|0|162127,25|1|134924,12|3|145a1c,31|9|211a2b,33|12|136a13,5|2|131b23,8|6|323740,16|0|131c2b,20|2|24f71b,1|2|161e26,4|11|206c22,1|3|151d27,38|4|0fe812,40|11|0db60e,26|4|31c534,5|10|195229,11|8|2dd92d,42|10|0d6f0f",0.8
        ));
        areaColorConfigs.put("女儿村", new MapAreaColorConfig(
                "161d28","41|2|34ba33,8|10|2a5938,8|13|012b0e,9|8|0e2117,33|0|213d1c,36|0|141b26,41|5|29c826,8|8|1b5e1b,28|10|36763b,22|10|2c083f,1|6|141b25,13|3|0e6f13,3|5|0b2a13,15|3|0b6d13,19|10|236b27,30|12|01290a,44|5|141e24,19|13|045d08",0.8
        ));
        areaColorConfigs.put("墨家村", new MapAreaColorConfig(
                "161e26","4|6|31aa35,0|6|151d25,29|13|0d501a,13|6|11ff08,10|10|12ef10,28|0|0b1612,16|7|06280e,8|0|082319,19|6|32a932,14|4|0ad50d,28|6|1da120,15|3|027807,31|3|31833e,34|6|1ffe13,4|0|0f1d23,20|4|30c832,30|4|014602,23|11|343e42",0.8
        ));
        areaColorConfigs.put("麒麟山", new MapAreaColorConfig(
                "171e28","16|6|0e0228,16|5|130225,25|7|0b6112,39|11|153e27,34|5|320f3d,25|0|282e2e,11|2|2ef124,42|3|497e50,23|13|1cb61b,18|3|23f421,18|7|2ff42e,34|13|137211,15|6|079904,1|5|151b28,28|4|1be51c,8|6|10f306,16|3|093410,3|12|33b232",0.8
        ));
        areaColorConfigs.put("长寿郊外", new MapAreaColorConfig(
                "161d27","39|2|31f133,30|2|08201a,6|7|2ffb26,8|6|27e22d,25|4|25dc25,4|1|0f1628,23|9|1fae1d,2|1|1a1c1f,11|8|079706,40|13|065709,23|7|106b10,32|12|34ea29,35|0|141b2a,34|7|085c0c,42|12|218620,23|0|0a2021,44|3|0b1a13,24|7|0f7410",0.8
        ));
        areaColorConfigs.put("大唐国境", new MapAreaColorConfig(
                "161d28","17|1|0a171d,10|9|264a34,6|11|1cdd18,27|6|1ef71b,38|0|2b471b,12|5|195f18,32|6|1ed116,17|13|1e6e29,44|6|001c06,12|12|172324,42|2|3ced3c,2|0|141d1e,37|2|25a727,33|11|229928,31|7|45c24d,35|5|002701,15|11|05620a,24|11|072d0b",0.8
        ));
        areaColorConfigs.put("普陀山", new MapAreaColorConfig(
                "161e26","17|7|309c3a,11|9|259523,41|9|311c40,20|5|31bf30,9|6|27bb2a,34|7|2a0c3d,29|1|0f0720,13|8|21f71b,34|8|290d40,5|6|3de436,3|1|141b29,23|7|34ff2e,10|5|2d8430,21|10|0dc107,29|8|101126,23|13|144618,41|1|0f1820,39|5|220933",0.8
        ));


        // 遍历所有地区配置，进行多点找色识别
        for (Map.Entry<String, MapAreaColorConfig> entry : areaColorConfigs.entrySet()) {
            String areaName = entry.getKey();
            MapAreaColorConfig config = entry.getValue();

            int[] pos = DeviceHttpClient.findMultiColor(
                    deviceId,
                    412, 213, 457, 227,  // 藏宝图地区名称所在区域（根据实际游戏调整）
                    config.baseColor,
                    config.offsetColors,
                    config.similarity,
                    0
            );

            if (pos[0] > 0 && pos[1] > 0) {
                TaskStepNotifier.notifyStep(deviceId, "识别到藏宝图地区：" + areaName);
                return areaName;
            }
        }

        return "";  // 未识别到任何地区
    }




    /**
     * 地图地区的多点找色配置类
     */
    static class MapAreaColorConfig {
        String baseColor;       // 基准色
        String offsetColors;    // 偏移色字符串
        double similarity;      // 相似度阈值

        public MapAreaColorConfig(String baseColor, String offsetColors, double similarity) {
            this.baseColor = baseColor;
            this.offsetColors = offsetColors;
            this.similarity = similarity;
        }
    }

    /**
     * 识别游戏界面中的坐标（带重试机制）
     * @param context 任务上下文
     * @param thread 任务线程
     * @return 坐标数组 [x, y]，识别失败返回null
     */
    int[] recognizeCoordinates(TaskContext context, TaskThread thread,int[]rect) throws Exception {

        String deviceId = context.getDeviceId();
        int[] result = null;

        for (int attempt = 1; attempt <= 1; attempt++) {
            // 检查任务是否被终止
            if (thread.isStopped()) {
                TaskStepNotifier.notifyStep(deviceId, "任务已终止，停止坐标识别");
                return null;
            }

            String ocrResult = DeviceHttpClient.ocrEx(deviceId, rect);
            TaskStepNotifier.notifyStep(deviceId, "OCR识别结果（第" + attempt + "次）: " + ocrResult);

            // 提取坐标
            result = extractCoordinatesFromText(ocrResult);
            if (result != null) {
                TaskStepNotifier.notifyStep(deviceId, "坐标识别成功: (" + result[0] + ", " + result[1] + ")");
                return result;
            }

            // 识别失败，等待重试
            TaskStepNotifier.notifyStep(deviceId, "坐标识别失败，尝试第" + (attempt + 1) + "次...");
            Thread.sleep(waittime);
        }

        TaskStepNotifier.notifyStep(deviceId, "坐标识别失败，已达到最大重试次数");
        return null;
    }

    /**
     * 从文本中提取坐标值（支持格式："46.20"、"46,20"、"46 20"等）
     * @param text OCR识别的文本
     * @return 包含X和Y坐标的数组 [x, y]，提取失败返回null
     */
    private int[] extractCoordinatesFromText(String text) {
        if (text == null || text.isEmpty()) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "OCR识别文本为空，无法提取坐标");
            return null;
        }

        // 仅移除非数字、非点号、非逗号、非空格的字符（保留潜在分隔符）
        String cleanedText = text.replaceAll("[^0-9.,\\s]", "");
        TaskStepNotifier.notifyStep(context.getDeviceId(), "清洗后坐标文本: " + cleanedText);

        // 尝试按多种分隔符分割（点号、逗号、空格）
        String[] parts = cleanedText.split("[.,\\s]+"); // 匹配 . 或 , 或 空格（连续多个也视为一个分隔符）

        // 需提取到至少两个有效数字
        if (parts.length >= 2) {
            try {
                // 过滤空字符串（可能因连续分隔符产生）
                List<String> validParts = new ArrayList<>();
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        validParts.add(part);
                    }
                }

                if (validParts.size() >= 2) {
                    int x = Integer.parseInt(validParts.get(0));
                    int y = Integer.parseInt(validParts.get(1));
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "提取坐标成功: (" + x + "," + y + ")");
                    return new int[]{x, y};
                }
            } catch (NumberFormatException e) {
                TaskStepNotifier.notifyStep(context.getDeviceId(),
                        "坐标转换失败: " + e.getMessage() + ", 原始文本: " + text);
                return null;
            }
        }

        TaskStepNotifier.notifyStep(context.getDeviceId(), "无法从文本中提取有效坐标: " + text);
        return null;
    }

    /**
     * 根据地区字数获取起点坐标和高度
     */
    private int[] getBaseAreaByAreaLength(String area) {
        int length = area.length();
        if (length == 3) {
            return AREA_3CHAR; // 3字地区
        } else if (length == 4) {
            return AREA_4CHAR; // 4字地区
        } else {
            return null; // 不支持其他字数
        }
    }

    /**
     * 识别坐标结尾的右括号“)”，返回其x坐标（用于动态计算宽度）
     */
    private int findRightBracketX(String deviceId, int xStart, int yStart, int height) throws Exception {
        // 搜索范围：从x起点向右扩展最大可能宽度（如200像素，足够覆盖最长坐标）
        int searchMaxX = xStart + 200;
        int searchY1 = yStart;
        int searchY2 = yStart + height;

        // 调用多点找色识别右括号“)”
        int[] bracketPos = DeviceHttpClient.findMultiColor(
                deviceId,
                xStart, searchY1, searchMaxX, searchY2, // 搜索范围
                BRACKET_BASE_COLOR, // 右括号基准色
                BRACKET_OFFSET_COLORS, // 右括号特征点
                0.8, // 相似度阈值
                0
        );

        if (bracketPos[0] > 0 && bracketPos[1] > 0) {
            TaskStepNotifier.notifyStep(deviceId, "找到坐标结尾符号，x=" + bracketPos[0]);
            return bracketPos[0]; // 返回右括号的x坐标
        }
        return -1; // 未找到
    }

    /**
     * 在动态计算的区域内识别坐标（x, y）
     */
    private int[] recognizeCoordinatesInArea(String deviceId, int[] fullArea) throws Exception {
        // 解析区域参数
        int xStart = fullArea[0];
        int yStart = fullArea[1];
        int width = fullArea[2];
        int height = fullArea[3];

        // 1. 识别逗号“,”拆分x和y
        int[] commaPos = findComma(deviceId, xStart, yStart, width, height);
        if (commaPos == null) {
            TaskStepNotifier.notifyStep(deviceId, "未找到坐标分隔符“,”");
            return null;
        }

        // 2. 识别x坐标（逗号左侧）
        String xStr = recognizeNumber(deviceId, xStart, yStart, commaPos[0] - xStart, height);
        if (xStr == null) {
            TaskStepNotifier.notifyStep(deviceId, "x坐标识别失败");
            return null;
        }

        // 3. 识别y坐标（逗号右侧，到右括号为止）
        String yStr = recognizeNumber(deviceId, commaPos[0] + 2, yStart, width - (commaPos[0] - xStart) - 2, height);
        if (yStr == null) {
            TaskStepNotifier.notifyStep(deviceId, "y坐标识别失败");
            return null;
        }

        // 4. 转换为数字
        try {
            int x = Integer.parseInt(xStr);
            int y = Integer.parseInt(yStr);
            return new int[]{x, y};
        } catch (NumberFormatException e) {
            TaskStepNotifier.notifyStep(deviceId, "坐标转换失败：" + xStr + "," + yStr);
            return null;
        }
    }

    /**
     * 识别逗号“,”（分隔x和y）
     */
    private int[] findComma(String deviceId, int xStart, int yStart, int width, int height) throws Exception {
        // 逗号“,”的多点找色配置（需根据实际游戏调整）
        String commaBaseColor = "171f27";
        String commaOffsets = "3|2|171f27,5|2|171f27";

        return DeviceHttpClient.findMultiColor(
                deviceId,
                xStart, yStart, xStart + width, yStart + height,
                commaBaseColor,
                commaOffsets,
                0.8,
                0
        );
    }

    /**
     * 识别数字（x或y坐标）
     * @param xStart 数字区域x起点
     * @param yStart 数字区域y起点
     * @param width 数字区域宽度（动态）
     * @param height 数字区域高度（固定）
     */
    private String recognizeNumber(String deviceId, int xStart, int yStart, int width, int height) throws Exception {
        // 数字0-9的多点找色配置（需根据实际游戏补充完整）
        Map<String, String> digitConfigs = new HashMap<>();
        digitConfigs.put("0", "38b735|2|1|38b735,5|1|38b735,2|5|38b735,5|5|38b735"); // 数字0特征
        digitConfigs.put("1", "3bd034|8|3|3bd034,8|1|3bd034,8|5|3bd034"); // 数字1特征
        digitConfigs.put("2", "35ca3b|2|1|35ca3b,8|1|35ca3b,5|3|35ca3b,2|5|35ca3b,8|5|35ca3b"); // 数字2特征
        // ... 补充3-9的数字配置

        StringBuilder number = new StringBuilder();
        int digitWidth = 10; // 单个数字宽度（固定，根据实际调整）

        // 从左到右识别每个数字
        for (int x = xStart; x < xStart + width; x += digitWidth) {
            boolean matched = false;
            // 尝试匹配每个数字
            for (Map.Entry<String, String> entry : digitConfigs.entrySet()) {
                String digit = entry.getKey();
                String[] config = entry.getValue().split("\\|");
                String baseColor = config[0];
                String offsets = String.join("|", java.util.Arrays.copyOfRange(config, 1, config.length));

                // 识别单个数字
                int[] pos = DeviceHttpClient.findMultiColor(
                        deviceId,
                        x, yStart, x + digitWidth, yStart + height,
                        baseColor,
                        offsets,
                        0.8,
                        0
                );

                if (pos[0] > 0) {
                    number.append(digit);
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                break; // 未匹配到数字，结束识别
            }
        }

        return number.length() > 0 ? number.toString() : null;
    }



    /**
     * 识别仓库页面中背包除了指定物品外的其他物品，并将其转移到仓库空格子中
     * 如果当前页没有空格子则向下翻页继续寻找
     */
    public void transferBagItemsToWarehouse() {
        TaskStepNotifier.notifyStep(context.getDeviceId(), "开始转移背包物品到仓库...");

        // 需要排除的物品列表
        String[] excludedItems = {"飞行棋", "飞行符", "红碗", "蓝碗", "摄妖香"};

        try {
            // 确保仓库界面已打开
            if (!ifOpenCangku()) {
                TaskStepNotifier.notifyStep(context.getDeviceId(), "仓库未打开，尝试打开仓库...");
                openJianyeCangku();
                Thread.sleep(waittime);
            }

            while (true) {
                if (taskThread.isStopped() || Thread.currentThread().isInterrupted()) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "任务已终止");
                    return;
                }
                taskThread.checkPause();

                // 1. 获取仓库界面背包中所有非空格子
                List<Integer> allBagItems = getAllNonEmptyBagItems();

                // 2. 筛选出非排除物品的格子
                List<Integer> transferableItems = filterExcludedItems(allBagItems, excludedItems);

                if (transferableItems.isEmpty()) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "背包中没有可转移的物品，转移完成");
                    break;
                }

                TaskStepNotifier.notifyStep(context.getDeviceId(),
                        "发现 " + transferableItems.size() + " 个可转移的物品格子");

                // 3. 查找当前页仓库空格子
                List<Integer> emptyWarehouseSlots = findEmptyCangkuIndices(context.getDeviceId());

                if (emptyWarehouseSlots.isEmpty()) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "当前页仓库无空格子，尝试翻页...");

                    // 检查是否可以翻页
                    if (canPageDown()) {
                        pageDown();
                        Thread.sleep(waittime);
                        continue; // 翻页后重新检查
                    } else {
                        TaskStepNotifier.notifyStep(context.getDeviceId(), "已到最后一页且无空格子，转移终止");
                        break;
                    }
                }

                // 4. 执行转移操作（一次转移一个物品）
                int transferCount = Math.min(transferableItems.size(), emptyWarehouseSlots.size());
                TaskStepNotifier.notifyStep(context.getDeviceId(),
                        "开始转移 " + transferCount + " 个物品到仓库");

                for (int i = 0; i < transferCount; i++) {
                    if (taskThread.isStopped() || Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    taskThread.checkPause();

                    int bagGridIndex = transferableItems.get(i);
                    TaskStepNotifier.notifyStep(context.getDeviceId(),
                            "转移第 " + (i + 1) + " 个物品，背包格子索引: " + bagGridIndex);

                    // 双击背包格子来转移物品
                    doubleclickcangkujiemianBagGrid(context.getDeviceId(), bagGridIndex);
                    Thread.sleep(new Random().nextInt(300) + 500); // 随机延迟避免操作过快
                }

                // 转移完成后短暂等待，然后重新检查
                Thread.sleep(waittime);
            }

            TaskStepNotifier.notifyStep(context.getDeviceId(), "背包物品转移到仓库完成");

        } catch (Exception e) {
            TaskStepNotifier.notifyStep(context.getDeviceId(),
                    "转移背包物品时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取仓库界面背包中所有非空格子的索引
     * @return 非空格子索引列表
     */
    private List<Integer> getAllNonEmptyBagItems() {
        List<Integer> nonEmptyItems = new ArrayList<>();
        List<Integer> emptySlots = findcangkujiemianEmptyBagIndices(context.getDeviceId());

        // 获取背包总格子数
        int totalBagSlots = BagGridUtil.cangkujiemianBagGrids().size();

        // 找出所有非空格子（总格子数减去空格子）
        for (int i = 0; i < totalBagSlots; i++) {
            if (!emptySlots.contains(i)) {
                nonEmptyItems.add(i);
            }
        }

        TaskStepNotifier.notifyStep(context.getDeviceId(),
                "背包总格子: " + totalBagSlots + ", 空格子: " + emptySlots.size() +
                        ", 非空格子: " + nonEmptyItems.size());

        return nonEmptyItems;
    }

    /**
     * 从非空格子中筛选出非排除物品的格子
     * @param nonEmptyItems 所有非空格子索引
     * @param excludedItems 需要排除的物品名称数组
     * @return 可转移的格子索引列表
     */
    private List<Integer> filterExcludedItems(List<Integer> nonEmptyItems, String[] excludedItems) {
        List<Integer> transferableItems = new ArrayList<>();

        for (int gridIndex : nonEmptyItems) {
            boolean isExcluded = false;

            // 检查当前格子是否包含排除的物品
            for (String excludedItem : excludedItems) {
                try {
                    // 使用现有的查找方法检查特定物品
                    int foundIndex = findcangkujiemianFirstItemIndex(
                            context.getDeviceId(), excludedItem, 0.8);

                    if (foundIndex == gridIndex) {
                        isExcluded = true;
                        TaskStepNotifier.notifyStep(context.getDeviceId(),
                                "格子 " + gridIndex + " 包含排除物品: " + excludedItem);
                        break;
                    }
                } catch (Exception e) {
                    // 如果识别失败，继续检查其他物品
                    continue;
                }
            }

            // 如果不是排除物品，加入可转移列表
            if (!isExcluded) {
                transferableItems.add(gridIndex);
            }
        }

        TaskStepNotifier.notifyStep(context.getDeviceId(),
                "筛选结果: " + nonEmptyItems.size() + " 个非空格子中，" +
                        transferableItems.size() + " 个可转移");

        return transferableItems;
    }

    /**
     * 优化版本：使用更精确的物品识别方法
     * 通过逐个检查每个格子来确定是否为排除物品
     */
    private List<Integer> filterExcludedItemsAdvanced(List<Integer> nonEmptyItems, String[] excludedItems) {
        List<Integer> transferableItems = new ArrayList<>();

        for (int gridIndex : nonEmptyItems) {
            boolean isExcluded = false;

            // 对每个排除物品进行检查
            for (String excludedItem : excludedItems) {
                try {
                    // 查找该排除物品的所有格子位置
                    List<Integer> excludedItemIndices = findcangkujiemianAllItemIndices(
                            context.getDeviceId(), excludedItem, 0.8);

                    // 如果当前格子在排除物品的位置列表中
                    if (excludedItemIndices.contains(gridIndex)) {
                        isExcluded = true;
                        TaskStepNotifier.notifyStep(context.getDeviceId(),
                                "格子 " + gridIndex + " 识别为排除物品: " + excludedItem);
                        break;
                    }
                } catch (Exception e) {
                    // 识别异常时跳过该物品检查
                    TaskStepNotifier.notifyStep(context.getDeviceId(),
                            "检查物品 " + excludedItem + " 时发生异常: " + e.getMessage());
                }
            }

            // 不是排除物品则加入可转移列表
            if (!isExcluded) {
                transferableItems.add(gridIndex);
            }
        }

        return transferableItems;
    }



    /**
     * 处理漂浮字弹窗验证   1033四小人返回258,118  1071成语返回29,150,不|90,154,惶|145,151,惶|195,160,安|=惶惶不安   1081漂浮字258,118|22,33    成语需循环处理
     * @param base64Image 图片的base64字符串
     * @return 解析后的int数组，错误时返回null
     */
    public void piaofuzi(String base64Image) {
        // 配置信息
        String miyao = "123456";
        int port = 1081;
        int maxRetryCount = 3; // 最大重试次数
        int retryIntervalMs = 1000; // 重试间隔(毫秒)
        HumanLikeController human = new HumanLikeController(taskThread);

        int retryCount = 0;
        boolean isSuccess = false;

        // 循环重试直到成功或达到最大次数
        while (retryCount < maxRetryCount && !isSuccess) {
            System.out.println("\n===== 第" + (retryCount + 1) + "次尝试 =====");

            try {
                // 1. 生成时间戳
                long timestamp = System.currentTimeMillis() / 1000;
                String timeStr = String.valueOf(timestamp);
                System.out.println("时间戳: " + timeStr);

                // 2. 计算MD5签名
                String signStr = miyao + timeStr;
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] messageDigest = md.digest(signStr.getBytes(StandardCharsets.UTF_8));
                StringBuilder hexString = new StringBuilder();
                for (byte b : messageDigest) {
                    String hex = Integer.toHexString(0xFF & b);
                    if (hex.length() == 1) {
                        hexString.append('0');
                    }
                    hexString.append(hex);
                }
                String sign = hexString.toString();
                System.out.println("签名: " + sign);

                // 3. 构建请求URL
                String urlString = String.format("http://127.0.0.1:%d/ocr?time=%s&sign=%s",
                        port,
                        URLEncoder.encode(timeStr, StandardCharsets.UTF_8.name()),
                        URLEncoder.encode(sign, StandardCharsets.UTF_8.name()));

                // 4. 创建HTTP连接
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "text/plain");

                // 5. 发送数据
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = base64Image.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // 6. 读取响应
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }

                        String responseContent = response.toString();
                        System.out.println("响应内容: " + responseContent);

                        // 7. 解析结果
                        String[] parts = responseContent.split("\\|");
                        if (parts.length < 1) {
                            System.err.println("响应格式错误，未找到分隔符|");
                            break;
                        } else {
                            String[] numberStrs = parts[0].split(",");
                            int[] coordinates = new int[numberStrs.length];
                            boolean valid = true;

                            // 验证并转换为int数组
                            for (int i = 0; i < numberStrs.length; i++) {
                                try {
                                    coordinates[i] = Integer.parseInt(numberStrs[i].trim());
                                    // 简单验证：坐标应为正数
                                    if (coordinates[i] <= 0) {
                                        valid = false;
                                        break;
                                    }
                                } catch (NumberFormatException e) {
                                    valid = false;
                                    break;
                                }
                            }

                            if (valid && coordinates.length >= 2) {
                                // 假设坐标是x,y格式，取前两个值作为点击坐标
                                int x = coordinates[0];
                                int y = coordinates[1];
                                System.out.println("获取有效坐标，准备点击: (" + x + ", " + y + ")");


                                DeviceHttpClient.click(context.getDeviceId(),"left",x,y);
                                isSuccess = true;
                                System.out.println("点击操作已执行");
                            } else {
                                System.err.println("坐标无效或格式错误，需要至少2个正整数坐标");
                            }
                        }
                    }
                } else {
                    System.err.println("请求失败，状态码: " + responseCode);
                }
            } catch (NoSuchAlgorithmException e) {
                System.err.println("MD5算法错误: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("网络请求错误: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("发生未知错误: " + e.getMessage());
            }

            // 准备重试
            if (!isSuccess) {
                retryCount++;
                if (retryCount < maxRetryCount) {
                    System.out.println("将在" + retryIntervalMs + "毫秒后重试...");
                    try {
                        Thread.sleep(retryIntervalMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }

        if (!isSuccess) {
            System.err.println("已达到最大重试次数(" + maxRetryCount + ")，无法执行点击操作");
        }
    }

    /**
     * sixiaoren方法，处理响应结果为123,321或-1（无|分隔符）的情况
     * @param base64Image 图片的base64字符串
     */
    public void sixiaoren(String base64Image) {
        // 配置信息
        String miyao = "123456";
        int port = 1033;
        int maxRetryCount = 3; // 最大重试次数
        int retryIntervalMs = 1000; // 重试间隔(毫秒)
        HumanLikeController human = new HumanLikeController(taskThread);

        int retryCount = 0;
        boolean isSuccess = false;

        // 循环重试直到成功或达到最大次数
        while (retryCount < maxRetryCount && !isSuccess) {
            System.out.println("\n===== 第" + (retryCount + 1) + "次尝试 =====");

            try {
                // 1. 生成时间戳
                long timestamp = System.currentTimeMillis() / 1000;
                String timeStr = String.valueOf(timestamp);
                System.out.println("时间戳: " + timeStr);

                // 2. 计算MD5签名
                String signStr = miyao + timeStr;
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] messageDigest = md.digest(signStr.getBytes(StandardCharsets.UTF_8));
                StringBuilder hexString = new StringBuilder();
                for (byte b : messageDigest) {
                    String hex = Integer.toHexString(0xFF & b);
                    if (hex.length() == 1) {
                        hexString.append('0');
                    }
                    hexString.append(hex);
                }
                String sign = hexString.toString();
                System.out.println("签名: " + sign);

                // 3. 构建请求URL
                String urlString = String.format("http://127.0.0.1:%d/ocr?time=%s&sign=%s",
                        port,
                        URLEncoder.encode(timeStr, StandardCharsets.UTF_8.name()),
                        URLEncoder.encode(sign, StandardCharsets.UTF_8.name()));

                // 4. 创建HTTP连接
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "text/plain");

                // 5. 发送数据
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = base64Image.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // 6. 读取响应
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }

                        String responseContent = response.toString();
                        System.out.println("响应内容: " + responseContent);

                        // 7. 解析结果（无|分隔符，处理123,321或-1格式）
                        // 检查是否为-1（表示无结果）
                        if ("-1".equals(responseContent)) {
                            System.err.println("响应结果为-1，无有效坐标");
                        } else {
                            String[] numberStrs = responseContent.split(",");
                            int[] coordinates = new int[numberStrs.length];
                            boolean valid = true;

                            // 验证并转换为int数组
                            for (int i = 0; i < numberStrs.length; i++) {
                                try {
                                    coordinates[i] = Integer.parseInt(numberStrs[i].trim());
                                    // 简单验证：坐标应为正数（-1已单独处理）
                                    if (coordinates[i] <= 0) {
                                        valid = false;
                                        break;
                                    }
                                } catch (NumberFormatException e) {
                                    valid = false;
                                    break;
                                }
                            }

                            if (valid && coordinates.length >= 2) {
                                // 取前两个值作为点击坐标
                                int x = coordinates[0];
                                int y = coordinates[1];
                                System.out.println("获取有效坐标，准备点击: (" + x + ", " + y + ")");

                                // 调用HumanlikerController的click方法进行点击


                                isSuccess = true;
                                System.out.println("点击操作已执行");
                            } else {
                                System.err.println("坐标无效或格式错误，需要至少2个正整数坐标");
                            }
                        }
                    }
                } else {
                    System.err.println("请求失败，状态码: " + responseCode);
                }
            } catch (NoSuchAlgorithmException e) {
                System.err.println("MD5算法错误: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("网络请求错误: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("发生未知错误: " + e.getMessage());
            }

            // 准备重试
            if (!isSuccess) {
                retryCount++;
                if (retryCount < maxRetryCount) {
                    System.out.println("将在" + retryIntervalMs + "毫秒后重试...");
                    try {
                        Thread.sleep(retryIntervalMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }

        if (!isSuccess) {
            System.err.println("已达到最大重试次数(" + maxRetryCount + ")，无法执行点击操作");
        }
    }

    /**
     * sizichengyu方法：处理四字成语的依次点击（单个方法实现）
     * 响应格式示例：29,150,不|90,154,惶|145,151,惶|195,160,安|=惶惶不安
     * @param base64Image 图片的base64字符串
     */
    public void sizichengyu(String base64Image) {
        // 配置信息
        String miyao = "123456";
        int port = 1071;
        int maxRetryCount = 3;
        int retryIntervalMs = 1000;
        int clickDelayMs = 500; // 点击间隔时间

        System.out.println("开始处理四字成语点击...");
        String idiom = null;

        // 首次请求获取成语信息
        for (int initRetry = 0; initRetry < maxRetryCount; initRetry++) {
            try {
                // 生成时间戳和签名
                long timestamp = System.currentTimeMillis() / 1000;
                String timeStr = String.valueOf(timestamp);
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] digest = md.digest((miyao + timeStr).getBytes(StandardCharsets.UTF_8));
                StringBuilder hexSign = new StringBuilder();
                for (byte b : digest) {
                    hexSign.append(String.format("%02x", b));
                }
                String sign = hexSign.toString();

                // 构建请求URL
                String urlString = String.format("http://127.0.0.1:%d/ocr?time=%s&sign=%s",
                        port,
                        URLEncoder.encode(timeStr, StandardCharsets.UTF_8.name()),
                        URLEncoder.encode(sign, StandardCharsets.UTF_8.name()));

                // 发送请求
                HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "text/plain");
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(base64Image.getBytes(StandardCharsets.UTF_8));
                }

                // 处理响应
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    System.err.println("首次请求失败，状态码: " + connection.getResponseCode());
                    continue;
                }

                // 读取响应内容
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line.trim());
                    }
                }
                String responseContent = response.toString();
                System.out.println("首次响应内容: " + responseContent);

                // 提取成语
                int equalIndex = responseContent.indexOf('=');
                if (equalIndex == -1) {
                    System.err.println("未找到成语分隔符'='");
                    continue;
                }
                idiom = responseContent.substring(equalIndex + 1).trim();
                if (idiom.length() == 4) {
                    System.out.println("获取到四字成语: " + idiom);
                    break;
                } else {
                    System.err.println("获取的成语不是四字: " + idiom);
                }
            } catch (Exception e) {
                System.err.println("首次请求错误: " + e.getMessage());
            }

            if (initRetry < maxRetryCount - 1) {
                System.out.println("首次请求重试...");
                try { Thread.sleep(retryIntervalMs); } catch (InterruptedException ie) { return; }
            }
        }

        // 检查是否获取到有效成语
        if (idiom == null || idiom.length() != 4) {
            System.err.println("无法获取有效的四字成语，终止操作");
            return;
        }

        // 依次点击每个字
        for (int i = 0; i < 4; i++) {
            char currentChar = idiom.charAt(i);
            System.out.println("\n===== 处理第" + (i + 1) + "个字: " + currentChar + " =====");
            boolean clicked = false;

            // 重试获取当前字的坐标
            for (int retry = 0; retry < maxRetryCount; retry++) {
                try {
                    // 生成时间戳和签名
                    long timestamp = System.currentTimeMillis() / 1000;
                    String timeStr = String.valueOf(timestamp);
                    MessageDigest md = MessageDigest.getInstance("MD5");
                    byte[] digest = md.digest((miyao + timeStr).getBytes(StandardCharsets.UTF_8));
                    StringBuilder hexSign = new StringBuilder();
                    for (byte b : digest) {
                        hexSign.append(String.format("%02x", b));
                    }
                    String sign = hexSign.toString();

                    // 构建请求URL
                    String urlString = String.format("http://127.0.0.1:%d/ocr?time=%s&sign=%s",
                            port,
                            URLEncoder.encode(timeStr, StandardCharsets.UTF_8.name()),
                            URLEncoder.encode(sign, StandardCharsets.UTF_8.name()));

                    // 发送请求
                    HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "text/plain");
                    try (OutputStream os = connection.getOutputStream()) {
                        os.write(base64Image.getBytes(StandardCharsets.UTF_8));
                    }

                    // 处理响应
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        System.err.println("请求失败，状态码: " + connection.getResponseCode());
                        continue;
                    }

                    // 读取响应内容
                    StringBuilder response = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            response.append(line.trim());
                        }
                    }
                    String responseContent = response.toString();
                    System.out.println("响应内容: " + responseContent);

                    // 解析坐标和字符映射
                    int equalIndex = responseContent.indexOf('=');
                    if (equalIndex == -1) {
                        System.err.println("未找到分隔符'='");
                        continue;
                    }
                    String coordinatePart = responseContent.substring(0, equalIndex).trim();
                    String[] records = coordinatePart.split("\\|");

                    Map<Character, int[]> charMap = new HashMap<>();
                    for (String record : records) {
                        String[] parts = record.split(",");
                        if (parts.length >= 3) {
                            try {
                                int x = Integer.parseInt(parts[0].trim());
                                int y = Integer.parseInt(parts[1].trim());
                                String charStr = parts[2].trim();
                                if (charStr.length() == 1) {
                                    charMap.put(charStr.charAt(0), new int[]{x, y});
                                }
                            } catch (NumberFormatException e) {
                                System.err.println("坐标解析错误: " + record);
                            }
                        }
                    }

                    // 查找当前字的坐标并点击
                    if (charMap.containsKey(currentChar)) {
                        int[] coords = charMap.get(currentChar);
                        System.out.println("找到坐标: (" + coords[0] + ", " + coords[1] + ")");
                        DeviceHttpClient.click(context.getDeviceId(),"left",coords[0], coords[1]);
                        clicked = true;
                        System.out.println("第" + (i + 1) + "个字点击完成");
                        break;
                    } else {
                        System.err.println("未找到字'" + currentChar + "'的坐标");
                    }
                } catch (Exception e) {
                    System.err.println("请求错误: " + e.getMessage());
                }

                if (retry < maxRetryCount - 1) {
                    System.out.println("重试获取坐标...");
                    try { Thread.sleep(retryIntervalMs); } catch (InterruptedException ie) { return; }
                }
            }

            // 如果当前字点击失败，终止后续操作
            if (!clicked) {
                System.err.println("无法点击第" + (i + 1) + "个字，终止操作");
                return;
            }

            // 点击间隔
            try {
                Thread.sleep(clickDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        System.out.println("\n所有四字成语点击操作完成");
    }


    /**
     * 裁剪Base64图片，保存到固定路径(D:\myapp\screenshot+时间戳.png)，并返回纯Base64内容
     * @param base64Str 原始图片的Base64编码（可带前缀）
     * @param x 裁剪区域左上角x坐标
     * @param y 裁剪区域左上角y坐标
     * @param width 裁剪宽度
     * @param height 裁剪高度
     * @return 裁剪后的纯Base64字符串（无任何前缀），失败返回null
     */
    public String cropImage(String base64Str, int x, int y, int width, int height) {
        // 固定输出路径和格式
        String outputPath = "D:\\myapp\\screenshot\\" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".png";
        String format = "png";

        try {
            // 1. 处理原始Base64（移除前缀）
            String pureBase64 = base64Str.contains(",") ? base64Str.split(",")[1] : base64Str;

            // 2. Base64解码为图片
            byte[] imageBytes = Base64.getDecoder().decode(pureBase64);
            BufferedImage originalImage;
            try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
                originalImage = ImageIO.read(bais);
            }

            if (originalImage == null) {
                throw new IOException("无法解析图片，格式不支持");
            }

            // 3. 验证裁剪坐标
            int imgWidth = originalImage.getWidth();
            int imgHeight = originalImage.getHeight();
            if (x < 0 || y < 0 || width <= 0 || height <= 0
                    || x + width > imgWidth || y + height > imgHeight) {
                throw new IllegalArgumentException(
                        String.format("裁剪坐标无效！图片尺寸: (%d,%d), 裁剪区域: (%d,%d,%d,%d)",
                                imgWidth, imgHeight, x, y, width, height)
                );
            }

            // 4. 执行裁剪
            BufferedImage croppedImage = originalImage.getSubimage(x, y, width, height);

            // 5. 保存裁剪后的图片文件到固定路径
            File saveFile = new File(outputPath);
            // 创建父目录（如果不存在）
            if (saveFile.getParentFile() != null) {
                saveFile.getParentFile().mkdirs(); // 自动创建D:\myapp目录（如果不存在）
            }
            ImageIO.write(croppedImage, format, saveFile);
            System.out.println("裁剪图片已保存至：" + saveFile.getAbsolutePath());

            // 6. 将裁剪后的图片转换为纯Base64（无前缀）
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(croppedImage, format, baos);
                baos.flush();
                return Base64.getEncoder().encodeToString(baos.toByteArray());
            }

        } catch (IllegalArgumentException e) {
            System.err.println("坐标错误：" + e.getMessage());
        } catch (IOException e) {
            System.err.println("图片处理错误：" + e.getMessage());
        } catch (Exception e) {
            System.err.println("裁剪失败：" + e.getMessage());
        }
        return null;
    }


}
