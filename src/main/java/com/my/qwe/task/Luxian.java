package com.my.qwe.task;

import com.my.qwe.controller.HumanLikeController;
import com.my.qwe.http.DeviceHttpClient;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Luxian {

    private final TaskContext context;
    private final TaskThread taskThread;
    private final Map<String, SceneHandler> sceneHandlerMap = new HashMap<>();
    private int waittime = (new java.util.Random().nextInt(201) + 300);
    private MovementStateDetector movementDetector;

    // 定义需要多路线的场景及对应的子路线名
    private static final Map<String, String[]> MULTI_ROUTE_SCENES = new HashMap<>();
    static {
        MULTI_ROUTE_SCENES.put("大唐国境", new String[]{"大唐国境1", "大唐国境2"});
        MULTI_ROUTE_SCENES.put("大唐境外", new String[]{"大唐境外1", "大唐境外2"});
    }
    // 暂存目标坐标（用于路线判断）
    private int[] targetPosition;

    public Luxian(TaskContext context, TaskThread taskThread) {
        this.context = context;
        this.taskThread = taskThread;
        this.movementDetector = new MovementStateDetector(context, new CommonActions(context, taskThread), new GameStateDetector(context, new DeviceHttpClient()));
        initSceneHandlers();
    }

    private void waitForPosition(int x, int y, int timeoutSeconds) throws IOException, InterruptedException {
        long start = System.currentTimeMillis();
        CommonActions commonActions = new CommonActions(context, taskThread);
        while ((System.currentTimeMillis() - start) < timeoutSeconds * 1000L) {
            int[] current = commonActions.ocrZuobiao();
            if (current[0] == x && current[1] == y) {
                TaskStepNotifier.notifyStep(context.getDeviceId(), "已到达目的地(" + x + "," + y + ")");
                return;
            }
            MovementStateDetector.MovementState state = movementDetector.detectMovementState(x, y);
            switch (state) {
                case MOVING_TO_DESTINATION:
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "角色正在正常前往目的地(" + x + "," + y + ")");
                    break;
                case STATIONARY:
                case ABNORMAL_MOVEMENT:
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "检测到异常，尝试重新激活");
                    boolean handled = movementDetector.handleAbnormalMovement(x, y, 3);
                    if (!handled) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(), "多次尝试后仍未能修正移动异常，跳过");
                        return;
                    }
                    break;
                case IN_BATTLE:
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "角色正在战斗中，等待战斗结束");
                    while (movementDetector.detectMovementState(x, y) == MovementStateDetector.MovementState.IN_BATTLE) {
                        Thread.sleep(1000);
                    }
                    break;
                case UNKNOWN:
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "无法识别移动状态，跳过");
                    return;
            }
            Thread.sleep(5000);
        }
        TaskStepNotifier.notifyStep(context.getDeviceId(), "等待超时，未能到达目的地(" + x + "," + y + ")");
    }

    private void initSceneHandlers() {
        CommonActions commonActions = new CommonActions(context, taskThread);
        HumanLikeController human = new HumanLikeController(taskThread);
        sceneHandlerMap.put("建邺城", new SceneHandler() {
            @Override
            public void enterScene() throws Exception {
                String diqu = commonActions.ocrShibieDiqu();
                if (!diqu.equals("建邺城")) {
                    commonActions.userFeixingfuToMudidi("建邺城");
                }
            }
        });
        sceneHandlerMap.put("东海湾", new SceneHandler() {
            @Override
            public void enterScene() throws Exception {
                if (taskThread.isStopped() || Thread.currentThread().isInterrupted()) return;
                taskThread.checkPause();
                TaskStepNotifier.notifyStep(context.getDeviceId(), "准备去东海湾");

                String diqu = commonActions.ocrShibieDiqu();
                if (!diqu.equals("傲来国")) {
                    commonActions.userFeixingfuToMudidi("傲来国");
                }
                int[] dangqianzuobiao = commonActions.ocrZuobiao();
                if (dangqianzuobiao[0] != 164 || dangqianzuobiao[1] != 15) {
                    commonActions.clickInputPos("164,15");
                    waitForPosition(164, 15, 60);
                }
                Thread.sleep(waittime);
                human.click(context.getDeviceId(), 429, 181, 4, 10);
                Thread.sleep(waittime);
                // 点击船夫并检查选项，最多重试3次
                int maxRetry = 4;
                int retryCount = 0;
                while (retryCount < maxRetry) {
                    Thread.sleep(1000);
                    if (taskThread.isStopped() || Thread.currentThread().isInterrupted()) return;
                    taskThread.checkPause();
                    String dangqiandiqu = commonActions.ocrShibieDiqu();
                    if ("东海湾".equals(dangqiandiqu)) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(), "已在东海湾，退出导航流程");
                        return;
                    }
                    int[] ifpos = DeviceHttpClient.findMultiColor(
                            context.getDeviceId(), 40, 185, 560, 210,
                            "415a6c",
                            "11|0|496473,10|8|9eacb5,0|7|ccdfe6,7|5|f1f5f8,7|0|4c6675,9|9|758791,6|8|eff7fe,2|2|dde0e4,10|9|748791,1|12|dfeef4,11|5|babbc4,1|8|aabac7",
                            0.6, 0
                    );
                    if (ifpos[0] > 0) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(), "找到确认选项，点击确认");
                        human.click(context.getDeviceId(), 627, 198, 30, 12);
                        Thread.sleep(waittime);
                        continue;
                    }
                    retryCount++;
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "未找到确认选项，重试点击船夫（第" + retryCount + "次）");
                }
                TaskStepNotifier.notifyStep(context.getDeviceId(), "警告：多次尝试后仍未能到达东海湾");
            }
        });
        sceneHandlerMap.put("傲来国", new SceneHandler() {
            @Override
            public void enterScene() {
                String diqu = commonActions.ocrShibieDiqu();
                if (!diqu.equals("傲来国")){
                    // 先使用飞行符到傲来国
                    commonActions.userFeixingfuToMudidi("傲来国");}
            }
        });
        sceneHandlerMap.put("大唐国境", new SceneHandler() {
            @Override
            public void enterScene() throws Exception {
                int[]targetPos = getTargetPosition();
                if (targetPos == null || targetPos.length < 2) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "无法获取坐标，默认使用路线1");
                    sceneHandlerMap.get("大唐国境1").enterScene();
                    return;
                }
                int x = targetPos[0];
                int y = targetPos[1];
                TaskStepNotifier.notifyStep(context.getDeviceId(), "根据坐标(" + x + "," + y + ")选择路线");
                // 根据坐标判断路线（可修改条件）
                if (x < 310 ) { // 条件1：走路线1
                    sceneHandlerMap.get("大唐国境1").enterScene();
                } else { // 条件2：走路线2
                    sceneHandlerMap.get("大唐国境2").enterScene();
                }
            }
        });

        sceneHandlerMap.put("大唐境外", new SceneHandler() {
            @Override
            public void enterScene() throws Exception {
                int[]targetPos = getTargetPosition();
                if (targetPos == null || targetPos.length < 2) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(), "无法获取坐标，默认使用路线1");
                    sceneHandlerMap.get("大唐境外1").enterScene();
                    return;
                }
                int x = targetPos[0];
                int y = targetPos[1];
                TaskStepNotifier.notifyStep(context.getDeviceId(), "根据坐标(" + x + "," + y + ")选择路线");
                // 根据坐标判断路线（可修改条件）
                if (x < 320 ) { // 条件1：走路线1
                    sceneHandlerMap.get("大唐境外1").enterScene();
                } else { // 条件2：走路线2
                    sceneHandlerMap.get("大唐境外2").enterScene();
                }
            }
        });

        sceneHandlerMap.put("女儿村", new SceneHandler() {
            @Override
            public void enterScene() throws InterruptedException, IOException {

                String diqu = commonActions.ocrShibieDiqu();
                if (!diqu.equals("女儿村")){
                    if (!diqu.equals("傲来国")){
                        // 先使用飞行符到傲来国
                        commonActions.userFeixingfuToMudidi("傲来国");}

                    int []dangqianzuobiao = commonActions.ocrZuobiao();

                    if (dangqianzuobiao[0]!=9 || dangqianzuobiao[1]!=141){
                    commonActions.clickInputPos("9,141");}//打开地图输入坐标后关闭地图
                    waitForPosition(9, 141, 60);
                    while (dangqianzuobiao[0]!=9 || dangqianzuobiao[1]!=141) {
                        if (taskThread.isStopped()||Thread.currentThread().isInterrupted()) return; ;
                        taskThread.checkPause();
                        dangqianzuobiao = commonActions.ocrZuobiao();
                        Thread.sleep(2000);
                    }
                    human.clickImg(context.getDeviceId(),"传送按钮",8,8);
                }
            }
        });
        sceneHandlerMap.put("花果山", new SceneHandler() {
            @Override
            public void enterScene() throws InterruptedException, IOException {
                String diqu = commonActions.ocrShibieDiqu();
                if (!diqu.equals("花果山")){
                    if (!diqu.equals("傲来国")){
                        // 先使用飞行符到傲来国
                        commonActions.userFeixingfuToMudidi("傲来国");}

                    int []dangqianzuobiao = commonActions.ocrZuobiao();

                    if (dangqianzuobiao[0]!=212 || dangqianzuobiao[1]!=141){
                        commonActions.clickInputPos("212,141");}//打开地图输入坐标后关闭地图
                    waitForPosition(212,141,180);
                    while (dangqianzuobiao[0]!=212 || dangqianzuobiao[1]!=141) {
                        if (taskThread.isStopped()||Thread.currentThread().isInterrupted()) return; ;
                        taskThread.checkPause();
                        dangqianzuobiao = commonActions.ocrZuobiao();
                        Thread.sleep(2000);
                    }
                    human.clickImg(context.getDeviceId(),"传送按钮",8,8);
                }
            }
        });
        sceneHandlerMap.put("北俱芦洲", new SceneHandler() {
            @Override
            public void enterScene() throws Exception {
                String diqu = commonActions.ocrShibieDiqu();
                int []dangqianzuobiao = commonActions.ocrZuobiao();
                if (!diqu.equals("花果山")){
                    toScene("花果山","32,98");
                }
                diqu= commonActions.ocrShibieDiqu();
                if (diqu.equals("花果山")&& (dangqianzuobiao[0]!=32 || dangqianzuobiao[1]!=98)){
                    commonActions.clickInputPos("32,98");//到花果山土地NPC旁边
                    waitForPosition(32,98,180);
                    while (dangqianzuobiao[0]!=32 || dangqianzuobiao[1]!=98) {
                        if (taskThread.isStopped()||Thread.currentThread().isInterrupted()) return; ;
                        taskThread.checkPause();
                        dangqianzuobiao = commonActions.ocrZuobiao();
                        Thread.sleep(2000);
                    }

                }
                human.click(context.getDeviceId(),305,195,10,10);//点击花果山土地NPC
                Thread.sleep(1000);
                int[]jianceZiShi = DeviceHttpClient.findMultiColor(context.getDeviceId(),542,184,560,203,"415a6c","2|5|f4f1fb,1|3|e0eaef,10|7|d3d7e1,4|0|4b6575",0.8,0);

                while (jianceZiShi[0]<0){
                    if (taskThread.isStopped()||Thread.currentThread().isInterrupted()) return; ;
                    taskThread.checkPause();
                    jianceZiShi = DeviceHttpClient.findMultiColor(context.getDeviceId(),542,184,560,203,"415a6c","2|5|f4f1fb,1|3|e0eaef,10|7|d3d7e1,4|0|4b6575",0.8,0);
                    Thread.sleep(1000);
                }
                    human.click(context.getDeviceId(),624,198,30,15);//点击是的，我要去选项
                    Thread.sleep(1000);
            }
        });
        sceneHandlerMap.put("长寿郊外", new SceneHandler() {
            @Override
            public void enterScene() throws InterruptedException, IOException {
                String diqu = commonActions.ocrShibieDiqu();
                if (!diqu.equals("长寿郊外")){
                    if (!diqu.equals("长寿村")){
                        // 先使用飞行符到傲来国
                        commonActions.userFeixingfuToMudidi("长寿村");}

                    int []dangqianzuobiao = commonActions.ocrZuobiao();

                    if (dangqianzuobiao[0]!=145 || dangqianzuobiao[1]!=6){
                        commonActions.clickInputPos("145,6");}//打开地图输入坐标后关闭地图
                    waitForPosition(145,6,180);
                    while (dangqianzuobiao[0]!=145 || dangqianzuobiao[1]!=6) {
                        if (taskThread.isStopped()||Thread.currentThread().isInterrupted()) return; ;
                        taskThread.checkPause();
                        dangqianzuobiao = commonActions.ocrZuobiao();
                        Thread.sleep(2000);
                    }
                    human.clickImg(context.getDeviceId(),"传送按钮",8,8);
                }
            }
        });
        sceneHandlerMap.put("狮驼岭", new SceneHandler() {
            @Override
            public void enterScene() throws Exception {
                DeviceHttpClient deviceHttpClient = new DeviceHttpClient();
                GameStateDetector detector = new GameStateDetector(context,deviceHttpClient);
                String diqu = commonActions.ocrShibieDiqu();
                if (diqu.equals("狮驼岭")){
                    TaskStepNotifier.notifyStep(context.getDeviceId(),"当前已经在狮驼岭");
                    return;
                }
                if (!diqu.equals("大唐境外")){
                    toScene("大唐境外","8,49");
                }
                while (!detector.isTeleportButtonVisible()){
                    if (taskThread.isStopped()||Thread.currentThread().isInterrupted()) return; ;
                    taskThread.checkPause();

                    Thread.sleep(2000);
                }

                human.clickImg(context.getDeviceId(), "传送按钮", 8, 8);


            }
        });
        sceneHandlerMap.put("墨家村", new SceneHandler() {
            @Override
            public void enterScene() throws Exception {
                DeviceHttpClient deviceHttpClient = new DeviceHttpClient();
                GameStateDetector detector = new GameStateDetector(context,deviceHttpClient);
                String diqu = commonActions.ocrShibieDiqu();
                if (diqu.equals("墨家村")){
                    TaskStepNotifier.notifyStep(context.getDeviceId(),"当前已经在墨家村");
                    return;
                }
                if (!diqu.equals("大唐境外")){
                    toScene("大唐境外","238,108");
                }
                int [] currentpos = commonActions.ocrZuobiao();
                while (currentpos[0]!=238 || currentpos[1]!=108){
                    if (taskThread.isStopped() || Thread.currentThread().isInterrupted()) return;
                    taskThread.checkPause();
                    TaskStepNotifier.notifyStep(context.getDeviceId(),currentpos[0]+","+currentpos[1]+"未到达目的地");
                    currentpos = commonActions.ocrZuobiao();
                    Thread.sleep(2000);
                }
                human.click(context.getDeviceId(),363,101,3,5);
                Thread.sleep(1000);
                int []songwodaomojiacun = DeviceHttpClient.findMultiColor(context.getDeviceId(),540,130,580,160,"6a818d","16|4|eceef3,9|1|778894,9|6|94acb8,10|5|aeb9c8,6|5|e9f4f4,4|6|a4acb6,14|0|b8c3d0,23|11|eaeef5",0.8,0);
                if (songwodaomojiacun[0]<0){TaskStepNotifier.notifyStep(context.getDeviceId(),"未出现送我到莫家村的选项，无其他处理，请完善代码");}
                human.click(context.getDeviceId(),624,147,30,10);


            }
        });
        sceneHandlerMap.put("朱紫国", new SceneHandler() {
            @Override
            public void enterScene() throws InterruptedException, IOException {
                String diqu = commonActions.ocrShibieDiqu();
                if (!diqu.equals("朱紫国")){
                    // 先使用飞行符到傲来国
                    commonActions.userFeixingfuToMudidi("朱紫国");
                }
            }
        });
        sceneHandlerMap.put("麒麟山", new SceneHandler() {
            @Override
            public void enterScene() throws InterruptedException, IOException {
                String diqu = commonActions.ocrShibieDiqu();
                if (!diqu.equals("麒麟山")){
                    if (!diqu.equals("朱紫国")){
                        // 先使用飞行符到傲来国
                        commonActions.userFeixingfuToMudidi("朱紫国");}

                    int []dangqianzuobiao = commonActions.ocrZuobiao();

                    if (dangqianzuobiao[0]!=2 || dangqianzuobiao[1]!=111){/// /2，111为朱紫国到麒麟山的传送点
                        commonActions.clickInputPos("2,111");}//打开地图输入坐标后关闭地图
                    waitForPosition(2,111,180);
                    while (dangqianzuobiao[0]!=2 || dangqianzuobiao[1]!=111) {
                        if (taskThread.isStopped()||Thread.currentThread().isInterrupted()) return; ;
                        taskThread.checkPause();
                        dangqianzuobiao = commonActions.ocrZuobiao();
                        Thread.sleep(2000);
                    }
                    human.clickImg(context.getDeviceId(),"传送按钮",8,8);
                }
            }
        });
        sceneHandlerMap.put("大唐境外1", new SceneHandler() {
            //此方法用于到大唐境外x坐标小于340的情况
            @Override
            public void enterScene() throws InterruptedException, IOException {
                String diqu = commonActions.ocrShibieDiqu();
                if (!diqu.equals("大唐境外")){
                    if (!diqu.equals("朱紫国")){
                        // 先使用飞行符到傲来国
                        commonActions.userFeixingfuToMudidi("朱紫国");}

                    int []dangqianzuobiao = commonActions.ocrZuobiao();

                    if (dangqianzuobiao[0]!=8 || dangqianzuobiao[1]!=3){/// /8，3为朱紫国到大唐境外的传送点
                        commonActions.clickInputPos("8,3");}//打开地图输入坐标后关闭地图
                    waitForPosition(8,3,180);
                    while (dangqianzuobiao[0]!=8 || dangqianzuobiao[1]!=3) {
                        if (taskThread.isStopped()||Thread.currentThread().isInterrupted()) return; ;
                        taskThread.checkPause();
                        dangqianzuobiao = commonActions.ocrZuobiao();
                        Thread.sleep(2000);
                    }
                    human.clickImg(context.getDeviceId(),"传送按钮",8,8);
                }
            }
        });
        sceneHandlerMap.put("大唐境外2", new SceneHandler() {
            //此方法用于到大唐境外x坐标大于320的情况
            @Override
            public void enterScene() throws Exception {
                String diqu = commonActions.ocrShibieDiqu();
                if (!diqu.equals("大唐境外")){
                    toScene("大唐国境","10,78");
                }
                int[] pos =commonActions.ocrZuobiao();
                while (pos[0]!=10 || pos[1]!=78){
                    if (taskThread.isStopped() || Thread.currentThread().isInterrupted()) return;
                    taskThread.checkPause();
                    Thread.sleep(2000);
                    pos =commonActions.ocrZuobiao();
                }
                TaskStepNotifier.notifyStep(context.getDeviceId(),
                        "点击大唐境外传送点");

                // 步骤4: 点击传送按钮（带重试）
                for (int attempt = 1; attempt <= 3; attempt++) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(),
                            "点击传送按钮（尝试 " + attempt + "/3）");

                    human.clickImg(context.getDeviceId(), "传送按钮", 8, 8);

                    // 验证是否成功到达
                    String newArea = commonActions.ocrShibieDiqu();
                    if (newArea.contains("大唐境外")) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(),
                                "成功到达大唐境外");
                        return;
                    }

                    if (attempt < 3) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(),
                                "传送失败，重试中...");
                    }
                }

            }
        });
        sceneHandlerMap.put("大唐国境2", new SceneHandler() {
            @Override
            public void enterScene() throws Exception {
                DeviceHttpClient httpClient = new DeviceHttpClient();
                if (taskThread.isStopped() || Thread.currentThread().isInterrupted()) return;
                taskThread.checkPause();

                // 初始化检测器
                GameStateDetector detector = new GameStateDetector(context, httpClient);

                // 检测当前状态
                String currentArea = commonActions.ocrShibieDiqu();
                double currentHp = detector.getPlayerHpPercent();

                TaskStepNotifier.notifyStep(context.getDeviceId(),
                        "当前地区: " + currentArea + ", 血量: " + String.format("%.0f%%", currentHp * 100));

                // 如果已经在大唐国境，直接退出
                if (currentArea.contains("大唐国境")) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(),
                            "已在大唐国境，无需导航");
                    return;
                }

                TaskStepNotifier.notifyStep(context.getDeviceId(),
                        "准备前往大唐国境...");
                commonActions.openBag();
                // 步骤1: 查找并使用红色飞行棋（带重试）
                for (int attempt = 1; attempt <= 3; attempt++) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(),
                            "查找红色飞行棋（尝试 " + attempt + "/3）");

                    int[] feixingqiPos = DeviceHttpClient.findImage(context.getDeviceId(),"红色飞行棋",0.8);

                    if (feixingqiPos[0] > 0) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(),
                                "找到红色飞行棋，双击使用");

                        human.doubleclick(context.getDeviceId(),
                        feixingqiPos[0], feixingqiPos[1], 5,5);

                        break;
                    }

                    if (attempt == 3) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(),
                                "错误：多次尝试未找到红色飞行棋，终止导航");
                        return;
                    }

                    TaskStepNotifier.notifyStep(context.getDeviceId(),
                            "未找到，重试中...");
                    Thread.sleep(1000);
                }


                // 步骤2: 点击大唐国境传送点
                TaskStepNotifier.notifyStep(context.getDeviceId(),
                        "点击大唐国境传送点");

                human.click(context.getDeviceId(), 109, 362, 5, 5);
// 步骤3: 关闭背包（带验证）
                if (detector.isBagOpen()) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(),
                            "关闭背包");
                    human.click(context.getDeviceId(), 614, 34, 10, 10);

                    // 验证是否成功关闭
                    if (detector.isBagOpen()) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(),
                                "警告：背包未成功关闭，继续执行");
                    }
                }
                // 验证传送界面是否打开
                if (!detector.isTeleportButtonVisible()) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(),
                            "警告：传送界面未正常打开，等待");

                    // 二次验证
                    if (!detector.isTeleportButtonVisible()) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(),
                                "错误：传送界面仍未打开，终止导航");
                        return;
                    }
                }



                // 步骤4: 点击传送按钮（带重试）
                for (int attempt = 1; attempt <= 3; attempt++) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(),
                            "点击传送按钮（尝试 " + attempt + "/3）");

                    human.clickImg(context.getDeviceId(), "传送按钮", 8, 8);

                    // 验证是否成功到达
                    String newArea = commonActions.ocrShibieDiqu();
                    if (newArea.contains("大唐国境")) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(),
                                "成功到达大唐国境");
                        return;
                    }

                    if (attempt < 3) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(),
                                "传送失败，重试中...");
                    }
                }

                TaskStepNotifier.notifyStep(context.getDeviceId(),
                        "错误：多次尝试后仍未能到达大唐国境");
            }
        });

        sceneHandlerMap.put("大唐国境1", new SceneHandler() {
            @Override
            public void enterScene() throws Exception {
                DeviceHttpClient httpClient = new DeviceHttpClient();
                if (taskThread.isStopped() || Thread.currentThread().isInterrupted()) return;
                taskThread.checkPause();

                // 初始化检测器
                GameStateDetector detector = new GameStateDetector(context, httpClient);

                // 检测当前状态
                String currentArea = commonActions.ocrShibieDiqu();
                double currentHp = detector.getPlayerHpPercent();

                TaskStepNotifier.notifyStep(context.getDeviceId(),
                        "当前地区: " + currentArea + ", 血量: " + String.format("%.0f%%", currentHp * 100));

                // 如果已经在大唐国境，直接退出
                if (currentArea.contains("大唐国境")) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(),
                            "已在大唐国境，无需导航");
                    return;
                }

                TaskStepNotifier.notifyStep(context.getDeviceId(),
                        "准备前往大唐国境...");
                commonActions.openBag();
                // 步骤1: 查找并使用红色飞行棋（带重试）
                for (int attempt = 1; attempt <= 3; attempt++) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(),
                            "查找红色飞行棋（尝试 " + attempt + "/3）");

                    int[] feixingqiPos = DeviceHttpClient.findImage(context.getDeviceId(),"红色飞行棋",0.8);

                    if (feixingqiPos[0] > 0) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(),
                                "找到红色飞行棋，双击使用");

                        human.doubleclick(context.getDeviceId(),
                                feixingqiPos[0], feixingqiPos[1], 5,5);

                        break;
                    }

                    if (attempt == 3) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(),
                                "错误：多次尝试未找到红色飞行棋，终止导航");
                        return;
                    }

                    TaskStepNotifier.notifyStep(context.getDeviceId(),
                            "未找到，重试中...");
                    Thread.sleep(1000);
                }


                // 步骤2: 点击大唐国境传送点（带验证）
                TaskStepNotifier.notifyStep(context.getDeviceId(),
                        "点击大唐国境传送点");
                human.click(context.getDeviceId(), 363, 324, 5, 5);
                commonActions.closeBag();


                int[] pos= DeviceHttpClient.findImages(context.getDeviceId(),"国境驿站1","国境驿站2","国境驿站3","国境驿站4",0.7);

                while (pos[0] <0){
                    if (taskThread.isStopped() || Thread.currentThread().isInterrupted()) return;
                    taskThread.checkPause();
                    pos= DeviceHttpClient.findImages(context.getDeviceId(),"国境驿站1","国境驿站2","国境驿站3","国境驿站4",0.7);
                }


                human.click(context.getDeviceId(), pos[0], pos[1], 1, 1);

                // 检查是否出现"是的，我要去"选项
                int[] ifpos = DeviceHttpClient.findMultiColor(
                        context.getDeviceId(), 40, 185, 560, 210,
                        "415a6c",
                        "11|0|496473,10|8|9eacb5,0|7|ccdfe6,7|5|f1f5f8,7|0|4c6675,9|9|758791,6|8|eff7fe,2|2|dde0e4,10|9|748791,1|12|dfeef4,11|5|babbc4,1|8|aabac7",
                        0.6, 0
                );

                System.out.println(ifpos[0]);

                Thread.sleep(waittime);

                while (ifpos[0] < 0){
                    if (taskThread.isStopped() || Thread.currentThread().isInterrupted()) return;
                    taskThread.checkPause();
                    ifpos = DeviceHttpClient.findMultiColor(
                            context.getDeviceId(), 40, 185, 560, 210,
                            "415a6c",
                            "11|0|496473,10|8|9eacb5,0|7|ccdfe6,7|5|f1f5f8,7|0|4c6675,9|9|758791,6|8|eff7fe,2|2|dde0e4,10|9|748791,1|12|dfeef4,11|5|babbc4,1|8|aabac7",
                            0.6, 0
                    );
                    Thread.sleep(waittime);
                }
                // 如果找到选项，点击确认
                if (ifpos[0] > 0) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(),
                            "找到确认选项，点击确认");
                    human.click(context.getDeviceId(), 627, 198, 30, 12);
                }




                // 步骤3: 关闭背包（带验证）
                if (detector.isBagOpen()) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(),
                            "关闭背包");
                    human.click(context.getDeviceId(), 614, 34, 10, 10);

                    // 验证是否成功关闭
                    if (detector.isBagOpen()) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(),
                                "警告：背包未成功关闭，继续执行");
                    }
                }
                // 验证传送界面是否打开
                if (!detector.isTeleportButtonVisible()) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(),
                            "警告：传送界面未正常打开，等待");

                    // 二次验证
                    if (!detector.isTeleportButtonVisible()) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(),
                                "错误：传送界面仍未打开，终止导航");
                        return;
                    }
                }



                // 步骤4: 点击传送按钮（带重试）
                for (int attempt = 1; attempt <= 3; attempt++) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(),
                            "点击传送按钮（尝试 " + attempt + "/3）");

                    human.clickImg(context.getDeviceId(), "传送按钮", 8, 8);

                    // 验证是否成功到达
                    String newArea = commonActions.ocrShibieDiqu();
                    if (newArea.contains("大唐国境")) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(),
                                "成功到达大唐国境");
                        return;
                    }

                    if (attempt < 3) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(),
                                "传送失败，重试中...");
                    }
                }

                TaskStepNotifier.notifyStep(context.getDeviceId(),
                        "错误：多次尝试后仍未能到达大唐国境");
            }
        });

        sceneHandlerMap.put("五庄观", new SceneHandler() {
            @Override
            public void enterScene() throws Exception {
                DeviceHttpClient httpClient = new DeviceHttpClient();
                GameStateDetector detector = new GameStateDetector(context,httpClient);
                if (taskThread.isStopped() || Thread.currentThread().isInterrupted()) return;
                taskThread.checkPause();
                String currentArea = commonActions.ocrShibieDiqu();

                if (currentArea.equals("五庄观")){
                    TaskStepNotifier.notifyStep(context.getDeviceId(),
                            "已在五庄观，无需导航");
                    return;
                }
                toScene("大唐境外","631,74");
                int[]currentpos =commonActions.ocrZuobiao();
                while (currentpos[0]!=631 || currentpos[1]!=74){
                    currentpos =commonActions.ocrZuobiao();
                    Thread.sleep(2000);

                }
                // 步骤4: 点击传送按钮（带重试）
                for (int attempt = 1; attempt <= 3; attempt++) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(),
                            "点击传送按钮（尝试 " + attempt + "/3）");

                    human.clickImg(context.getDeviceId(), "传送按钮", 8, 8);

                    // 验证是否成功到达
                    String newArea = commonActions.ocrShibieDiqu();
                    if (newArea.contains("五庄观")) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(),
                                "成功到达五庄观");
                        return;
                    }

                    if (attempt < 3) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(),
                                "传送失败，重试中...");
                    }
                }


            }
        });
        sceneHandlerMap.put("普陀山", new SceneHandler() {
            @Override
            public void enterScene() throws Exception {

                DeviceHttpClient httpClient = new DeviceHttpClient();
                GameStateDetector detector = new GameStateDetector(context,httpClient);
                if (taskThread.isStopped() || Thread.currentThread().isInterrupted()) return;
                taskThread.checkPause();
                String currentArea = commonActions.ocrShibieDiqu();
                int[]currentpos =commonActions.ocrZuobiao();
                if (currentArea.equals("普陀山")){
                    TaskStepNotifier.notifyStep(context.getDeviceId(),
                            "已在普陀山，无需导航");
                    return;
                }
                toScene("大唐国境","215,65");

                while (currentpos[0]!=215 || currentpos[1]!=65){
                    if (taskThread.isStopped() || Thread.currentThread().isInterrupted()) return;
                    taskThread.checkPause();
                    Thread.sleep(2000);
                    currentpos =commonActions.ocrZuobiao();
                }
                human.click(context.getDeviceId(), 468, 262, 3, 6);

                Thread.sleep(2500);
                if (detector.isshidewoyaoqu()) {
                    human.click(context.getDeviceId(),624,198,30,15);
                }
            }
        });
        sceneHandlerMap.put("江南野外", new SceneHandler() {
            @Override
            public void enterScene() throws Exception {
                DeviceHttpClient httpClient = new DeviceHttpClient();
                if (taskThread.isStopped() || Thread.currentThread().isInterrupted()) return;
                taskThread.checkPause();

                // 初始化检测器
                GameStateDetector detector = new GameStateDetector(context, httpClient);

                // 检测当前状态
                String currentArea = commonActions.ocrShibieDiqu();
                double currentHp = detector.getPlayerHpPercent();

                TaskStepNotifier.notifyStep(context.getDeviceId(),
                        "当前地区: " + currentArea + ", 血量: " + String.format("%.0f%%", currentHp * 100));

                // 如果已经在大唐国境，直接退出
                if (currentArea.contains("江南野外")) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(),
                            "已在江南野外，无需导航");
                    return;
                }

                TaskStepNotifier.notifyStep(context.getDeviceId(),
                        "准备前往江南野外...");
                commonActions.openBag();
                // 步骤1: 查找并使用红色飞行棋（带重试）
                for (int attempt = 1; attempt <= 3; attempt++) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(),
                            "查找红色飞行棋（尝试 " + attempt + "/3）");

                    int[] feixingqiPos = DeviceHttpClient.findImage(context.getDeviceId(),"红色飞行棋",0.8);

                    if (feixingqiPos[0] > 0) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(),
                                "找到红色飞行棋，双击使用");

                        human.doubleclick(context.getDeviceId(),
                                feixingqiPos[0], feixingqiPos[1], 5,5);

                        break;
                    }

                    if (attempt == 3) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(),
                                "错误：多次尝试未找到红色飞行棋，终止导航");
                        return;
                    }

                    TaskStepNotifier.notifyStep(context.getDeviceId(),
                            "未找到，重试中...");
                    Thread.sleep(1000);
                }


                // 步骤2: 点击江南野外传送点（带验证）
                TaskStepNotifier.notifyStep(context.getDeviceId(),
                        "点击江南野外传送点");

                human.click(context.getDeviceId(), 622, 364, 5, 5);
                // 步骤3: 关闭背包（带验证）
                if (detector.isBagOpen()) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(),
                            "关闭背包");
                    human.click(context.getDeviceId(), 614, 34, 10, 10);

                    // 验证是否成功关闭
                    if (detector.isBagOpen()) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(),
                                "警告：背包未成功关闭，继续执行");
                    }
                }
                // 验证传送界面是否打开
                if (!detector.isTeleportButtonVisible()) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(),
                            "警告：传送界面未正常打开，等待");

                    // 二次验证
                    if (!detector.isTeleportButtonVisible()) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(),
                                "错误：传送界面仍未打开，终止导航");
                        return;
                    }
                }



                // 步骤4: 点击传送按钮（带重试）
                for (int attempt = 1; attempt <= 3; attempt++) {
                    TaskStepNotifier.notifyStep(context.getDeviceId(),
                            "点击传送按钮（尝试 " + attempt + "/3）");

                    human.clickImg(context.getDeviceId(), "传送按钮", 8, 8);

                    // 验证是否成功到达
                    String newArea = commonActions.ocrShibieDiqu();
                    if (newArea.contains("江南野外")) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(),
                                "成功到达江南野外");
                        return;
                    }

                    if (attempt < 3) {
                        TaskStepNotifier.notifyStep(context.getDeviceId(),
                                "传送失败，重试中...");
                    }
                }

                TaskStepNotifier.notifyStep(context.getDeviceId(),
                        "错误：多次尝试后仍未能到达江南野外");
            }
        });
    }

    // 获取暂存的目标坐标
    private int[] getTargetPosition() {
        return targetPosition;
    }

    /**
     * 方法1：只去场景（不需要坐标）
     */
    public void toScene(String changjing) throws Exception {
        SceneHandler handler = sceneHandlerMap.get(changjing);
        if (handler == null) {
            System.out.println("未知场景：" + changjing);
            return;
        }
        handler.enterScene();
    }

    /**
     * 方法2：去场景并移动到坐标（如果场景支持）
     */
    public void toScene(String changjing, String mubiaozuobiao) throws Exception {
        try {
            if (mubiaozuobiao != null && !mubiaozuobiao.isEmpty()) {
                String[] posStr = mubiaozuobiao.split(",");
                if (posStr.length == 2) {
                    int x = Integer.parseInt(posStr[0].trim());
                    int y = Integer.parseInt(posStr[1].trim());
                    targetPosition = new int[]{x, y};
                }
            }
        } catch (Exception e) {
            TaskStepNotifier.notifyStep(context.getDeviceId(), "解析目标坐标失败：" + e.getMessage());
            targetPosition = null;
        }
        SceneHandler handler = sceneHandlerMap.get(changjing);
        if (handler == null) {
            System.out.println("未知场景：" + changjing);
            return;
        }
        handler.enterScene();
        if (targetPosition != null) {
            CommonActions commonActions = new CommonActions(context, taskThread);
            commonActions.clickInputPos(mubiaozuobiao);
            waitForPosition(targetPosition[0], targetPosition[1], 60);
            targetPosition = null;
        }
    }

    // ---------------------- 接口定义 ----------------------
    private interface SceneHandler {
        void enterScene() throws Exception;
    }
}