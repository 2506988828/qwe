package com.my.qwe.task;

import com.my.qwe.controller.HumanLikeController;
import com.my.qwe.http.DeviceHttpClient;
import com.my.qwe.task.TaskContext;
import com.my.qwe.util.BagGridUtil;
import org.bytedeco.opencv.opencv_core.Device;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonActions {
    private final TaskContext context;
    private final TaskThread taskThread;
    private int waittime=(new java.util.Random().nextInt(201) + 300);

    /**
     * 藏宝图识别相关区域定义（根据实际游戏界面调整坐标）
     */
// 地图名称识别区域（例如："麒麟山"、"北俱芦洲"所在位置）
    private static final int[] MAP_NAME_AREA = {408, 190, 500, 210}; // [x1, y1, x2, y2]
    // X坐标识别区域（例如：坐标中"32"的X值位置）
    private static final int[] COORD_X_AREA = {450, 213, 480, 228};
    // Y坐标识别区域（例如：坐标中"32"的Y值位置）
    private static final int[] COORD_Y_AREA = {500, 213, 530, 228};
    // 藏宝图整体坐标区域（之前定义的baotuzuobiaoquyu，可保留备用）
    private static final int[] baotuzuobiaoquyu = {408, 213, 537, 228};



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
     * 识别当前所在的坐标（支持无括号格式，如192,133）
     */
    public int[] ocrZuobiao() {
        int[] diququyu = {52, 31, 124, 50}; // 坐标识别区域
        try {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "识别所在坐标（无括号格式）");

            String diqu = "";
            while (true) {
                // 检查任务状态，终止时返回无效坐标
                if (taskThread.isStopped() || Thread.currentThread().isInterrupted()) {
                    return new int[]{-1, -1};
                }
                taskThread.checkPause();

                // 调用OCR接口获取坐标文本
                diqu = DeviceHttpClient.ocr(context.getDeviceId(), diququyu);
                if (diqu == null || diqu.trim().isEmpty()) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "坐标识别为空，重试...");
                    Thread.sleep(500); // 识别为空时短暂延迟再重试
                    continue;
                }

                // 清理格式：移除所有可能的括号（包括中文括号），只保留数字和逗号
                diqu = diqu.trim()
                        .replace("(", "")   // 移除英文左括号
                        .replace(")", "")   // 移除英文右括号
                        .replace("（", "")  // 移除中文左括号（新增）
                        .replace("）", "")  // 移除中文右括号（新增）
                        .replace("。", ",")  // 替换中文句号为逗号
                        .replace(".", ",");  // 替换英文句号为逗号

                // 关键：仅判断是否包含逗号，且逗号前后有数字
                int commaIndex = diqu.indexOf(',');
                if (commaIndex > 0 && commaIndex < diqu.length() - 1) {
                    // 提取逗号前后的数字
                    String xStr = diqu.substring(0, commaIndex).trim();
                    String yStr = diqu.substring(commaIndex + 1).trim();

                    // 验证数字格式（仅包含数字）
                    if (xStr.matches("\\d+") && yStr.matches("\\d+")) {
                        int x = Integer.parseInt(xStr);
                        int y = Integer.parseInt(yStr);
                        TaskStepNotifier.notifyStep(context.getDeviceId(), "识别坐标成功：(" + x + "," + y + ")");
                        return new int[]{x, y};
                    }
                }

                // 格式不正确，重试
                TaskStepNotifier.notifyStep(context.getDeviceId(), "坐标格式不正确（" + diqu + "），重试...");
                Thread.sleep(500);
            }
        } catch (IOException e) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "OCR接口调用失败：" + e.getMessage());
            return new int[]{-1, -1};
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 保留中断状态
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

    //打开背包
    public void openBag(){
        TaskStepNotifier.notifyStep(context.getDeviceId(),"打开背包...");
        HumanLikeController human = new HumanLikeController(taskThread);
        try {
            while (true) {
                if (taskThread.isStopped()||Thread.currentThread().isInterrupted()) return ;
                taskThread.checkPause();
            int[]jiancebeibao = DeviceHttpClient.findMultiColor(context.getDeviceId(),649,365,665,382,"d0a357","6|12|b35d20,2|14|cc3021,6|4|df1020,1|8|ac6714,1|11|f8cf5d,15|10|9d561a,13|13|990116,9|16|b5741f",0.8,0);
            int[]jiancebeibaoshifoudakai = DeviceHttpClient.findMultiColor(context.getDeviceId(),316,27,333,46,"2d3374","5|0|2d3472,0|12|383c85,12|11|c3bee2,13|14|9698b8,11|1|6465a6,6|4|9fa3c6,1|6|d2dcef,12|0|292c73",0.8,0);
                Thread.sleep(new java.util.Random().nextInt(200) + 100);
                if (jiancebeibao[0]>0) {
                Thread.sleep(new java.util.Random().nextInt(200) + 100);
                human.click(context.getDeviceId(),659,378,5,5);
                    Thread.sleep(new java.util.Random().nextInt(200) + 100);
                }

            else if (jiancebeibao[0]<0) {
                    if (jiancebeibaoshifoudakai[0] > 0) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(),"背包已打开");
                        Thread.sleep(new java.util.Random().nextInt(200) + 100);
                        break;
                    }
                TaskStepNotifier.notifyStep(context.getDeviceId(),"未识别到道具按钮");}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
     * 遍历背包格子查找图片
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
     * 遍历仓库界面背包格子查找图片
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
                        "b8add9", // 空格子的特征色
                        "4|19|baacd9,25|19|baacd9,16|14|baacd9,11|6|baacd9,0|27|baacd9,16|29|b9add9,24|12|baacd9,20|1|baacd9,28|23|baacd9,10|21|baacd9,29|27|baacd9,8|4|baacd9,9|29|b9add9,18|27|baacd9,10|24|baacd9,23|19|baacd9,5|23|baacd9,24|2|baacd9,9|1|baacd9,24|29|b9add9,8|12|baacd9,31|14|b9add9,30|23|baacd9,2|2|baacd9,9|20|baacd9,24|24|baacd9,2|0|b8add9,26|4|baacd9", // 空格子的多点校验（复用原参数）
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

    //遍历仓库界面的背包空格子数
    public List<Integer> findcangkujiemianEmptyBagIndices(String deviceId) {
        List<Integer> matchedIndices = new ArrayList<>();
        List<int[]> grids = BagGridUtil.cangkujiemianBagGrids();

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




    public void clickGrids(String deviceId, int gezi) {

    }

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

    //双击仓库的第gridIndex个格子
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

    public void bagToCangku(String filename){
        int i=0;
        i=findcangkujiemianBagItemIndex(context.getDeviceId(),filename,0.8);
        doubleclickcangkujiemianBagGrid(context.getDeviceId(),i);
    }

    public void cangkuToBag(String filename){
        int i=-1;
        i=findCangkuItemIndex(context.getDeviceId(),filename,0.8);
        if (i>-1) {
        doubleclickCangkuGrid(context.getDeviceId(),i);}
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

    //打开地图后输入坐标，确定后关闭地图，调用时形参传递“243,11”
    public void clickInputPos(String str){
        HumanLikeController human = new HumanLikeController(taskThread);

        while(true){
            try {
                if (taskThread.isStopped()||Thread.currentThread().isInterrupted()) return; ;
                taskThread.checkPause();
                human.click(context.getDeviceId(),65,31,20,5);//打开地图
                Thread.sleep(500);
                int[]jiancedakaiditu = DeviceHttpClient.findMultiColor(context.getDeviceId(),360,21,725,231,"7aa7c8","16|7|1a425a,5|2|69aac0,6|14|16334e,17|16|223e5d,3|6|264960,4|18|1e314a,9|14|1d3a59,17|3|1e425b,21|21|38638c,10|19|2d5a85,17|9|456e95,0|4|5d8fae,4|10|386285,18|22|3278ac,21|23|2e5278",0.8,0);
                if(jiancedakaiditu[0]>0){
                    Thread.sleep(500);
                    break;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }

        try {
            Thread.sleep(500);
            int[]posX= DeviceHttpClient.findMultiColor(context.getDeviceId(),355,0,2000,2000,"1a2429","17|8|1a252a,6|10|e6ecf0,9|14|151d21,5|5|192328,5|15|1e262b,1|13|172227",0.8,0);

            human.click(context.getDeviceId(),posX[0]+60,posX[1]+20,0,0 );
            Thread.sleep(new java.util.Random().nextInt(1001) + 400);
            Thread.sleep(new java.util.Random().nextInt(1001) + 400);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        for (int i = 0; i < str.length(); i++) {
            try {
                if(str.charAt(i)!=','){
                char ch = str.charAt(i);
                human.clickImg(context.getDeviceId(),"坐标"+ch);
                }
                else if(str.charAt(i)==','){
                    human.clickImg(context.getDeviceId(),"确定");
                }
                Thread.sleep(new java.util.Random().nextInt(101) + 200);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        human.clickImg(context.getDeviceId(),"确定");

        try {
            Thread.sleep(500);
            human.click(context.getDeviceId(),700,57,5,5);//关闭地图
        } catch (Exception e) {
            throw new RuntimeException(e);
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
        if(!diqu.equals("建邺城") ){
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
                zizuo = DeviceHttpClient.findMultiColor(context.getDeviceId(),0,0,735,401,"3e5568","3|11|c3d2de,2|7|9dacbd,5|13|3b5a68,2|10|6e858d,6|15|315565,1|0|3f5568,2|8|dee9f2,10|15|b2b6c1,16|12|2e5562",0.8,0);
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
    void closeBag() {
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
     boolean canPageDown() throws IOException {
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

     void closeWarehouse() throws IOException {
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
    private String recognizeMapArea(TaskContext context) throws IOException {
        String deviceId = context.getDeviceId();

        // 定义各地区的多点找色参数（根据实际游戏调整）
        Map<String, MapAreaColorConfig> areaColorConfigs = new HashMap<>();

        // 添加"花果山"地区的识别参数
        areaColorConfigs.put("花果山", new MapAreaColorConfig(
                "141e27",  // 基准色
                "41|2|171927,39|12|24d122,11|14|0c1f1d,24|9|15ff10,5|5|103c21,12|14|032a0e," +
                        "31|7|47bf47,14|8|15172c,32|5|20e21e,15|10|1fd517,6|10|22cf16,12|5|04150c," +
                        "28|13|1f4c28,32|1|131727,36|8|33663d,43|12|078d0b,40|11|103c1a,15|2|15bc17," +
                        "18|2|2fb032,32|13|165f1c",  // 偏移色
                0.8  // 相似度阈值
        ));

        // 添加更多地区...
        areaColorConfigs.put("建邺城", new MapAreaColorConfig(
                "151f28","3|9|215628,3|6|213d2c,26|1|21542d,19|13|0db515,8|14|33430b,26|13|052e12,1|0|161e27,32|3|36e328,40|0|0c2715,39|5|2ec939,16|1|15192b,25|8|2ae227,27|10|1b1c27,38|8|1ae01a,7|4|36943c,38|0|3c3b29,2|2|10141c,22|9|09ca08,35|8|58b153",0.8
        ));
        areaColorConfigs.put("东海湾", new MapAreaColorConfig(
                "171e27","8|3|14fe0b,23|14|0b0e1a,12|5|111018,41|3|11d10d,14|5|250532,8|5|17371a,21|4|11db12,12|12|121b17,25|3|0ca40d,24|9|1fe115,29|1|1a3d27,14|3|26cb1f,41|13|2edb2c,39|13|32dc31,20|3|21b91d,28|2|2cf828,5|11|38de39,3|10|121b20,15|8|0e650e",0.8
        ));
        areaColorConfigs.put("江南野外", new MapAreaColorConfig(
                "171d26","18|12|3bde38,44|13|102b24,25|6|1ee21e,47|3|23f01a,29|1|123723,2|9|122027,51|4|16d714,15|12|2acf27,40|3|0f4413,16|7|041119,2|14|122022,17|8|277c38,56|7|27bd2a,49|7|17d81b,56|3|23162f,4|0|0c1c1d,0|3|161e26,31|2|4abf51,5|8|121c23,19|6|085611,46|7|047506,53|7|2dff2a,24|0|264111,3|7|130d1d,25|10|24e425,15|2|19d01a",0.8
        ));
        areaColorConfigs.put("朱紫国", new MapAreaColorConfig(
                "161d28","38|12|10ac0e,44|6|074a08,32|5|20d11d,4|10|12301e,21|0|17300f,16|5|0f1412,26|3|259825,26|9|1ccd21,3|6|25602e,24|7|359937,44|10|043206,36|6|31c42f,14|12|2d7d2a,35|8|152b23,36|14|173b2b,19|0|1a2d1c,3|7|215323,20|11|13e913,33|10|26b42c,13|2|0c1f20",0.8
        ));
        areaColorConfigs.put("五庄观", new MapAreaColorConfig(
                "171f27","44|2|290b3a,12|12|2df629,17|11|29b32b,4|0|0d141f,26|11|0d3415,18|12|13ba11,3|7|141926,36|1|1e292c,2|4|141c2a,21|1|0c2118,2|5|1b1830,38|4|018d02,22|7|2fcc2d,15|11|10431c,0|9|171f27,13|0|313912,28|4|0e1c17,0|5|151c25,5|3|125a1b,3|12|367f37",0.8
        ));
        areaColorConfigs.put("北俱芦洲", new MapAreaColorConfig(
                "161e26","26|5|14ac15,26|3|14d615,1|13|161e28,43|9|092a18,41|13|161e25,44|3|071e14,9|9|14381d,40|9|1d8320,3|7|131a26,27|12|3fc53e,43|3|164d22,34|9|178416,2|7|131b22,35|5|3de33a,40|2|20fd17,28|13|238526,12|0|314713,5|2|181822",0.8
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





        // 遍历所有地区配置，进行多点找色识别
        for (Map.Entry<String, MapAreaColorConfig> entry : areaColorConfigs.entrySet()) {
            String areaName = entry.getKey();
            MapAreaColorConfig config = entry.getValue();

            int[] pos = DeviceHttpClient.findMultiColor(
                    deviceId,
                    400, 180, 550, 220,  // 藏宝图地区名称所在区域（根据实际游戏调整）
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
    private static class MapAreaColorConfig {
        String baseColor;       // 基准色
        String offsetColors;    // 偏移色字符串
        double similarity;      // 相似度阈值

        public MapAreaColorConfig(String baseColor, String offsetColors, double similarity) {
            this.baseColor = baseColor;
            this.offsetColors = offsetColors;
            this.similarity = similarity;
        }
    }





}
