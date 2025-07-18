package com.my.qwe.ui;

import com.my.qwe.core.DeviceManager;
import com.my.qwe.task.*;
import com.my.qwe.model.DeviceInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class DevicePanel extends JPanel {
    private final DeviceInfo deviceInfo;

    private JLabel taskLabel;
    private JLabel stepLabel;
    private JLabel threadLabel;

    private JPopupMenu menu;
    private JComboBox<String> taskSelector;
    private JButton startButton;

    public DevicePanel(DeviceInfo info) {
        this.deviceInfo = info;
        String isonline = (info.state==1)?"在线":"离线";
        setBorder(BorderFactory.createTitledBorder(info.name + " (" + info.deviceId + ")"+isonline));
        setLayout(new BorderLayout());

        // 任务选择 + 开始按钮放入顶部面板
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        taskSelector = new JComboBox<>(new String[]{"接图", "挖图", "打图", "师门", "转图","开图","读图"});
        startButton = new JButton("开始");
        startButton.addActionListener(e -> startSelectedTask());

        topPanel.add(new JLabel("任务: "));
        topPanel.add(taskSelector);
        topPanel.add(startButton);

        add(topPanel, BorderLayout.NORTH);

        // 状态面板放中间，3行显示任务、步骤、线程状态
        taskLabel = new JLabel("任务: -");
        stepLabel = new JLabel("步骤: -");
        threadLabel = new JLabel("线程: -");

        JPanel statusPanel = new JPanel(new GridLayout(3, 1));
        statusPanel.add(taskLabel);
        statusPanel.add(stepLabel);
        statusPanel.add(threadLabel);

        add(statusPanel, BorderLayout.CENTER);

        // 添加右键菜单
        initPopupMenu();
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) menu.show(e.getComponent(), e.getX(), e.getY());
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) menu.show(e.getComponent(), e.getX(), e.getY());
            }
        });

        // 监听任务步骤通知
        TaskStepNotifier.registerListener(deviceInfo.deviceId, message -> {
            SwingUtilities.invokeLater(() -> {
                stepLabel.setText("步骤: " + message);
            });
        });

        // 监听线程状态通知
        TaskThreadStatusNotifier.register(deviceInfo.deviceId, status -> {
            SwingUtilities.invokeLater(() -> {
                threadLabel.setText("线程: " + status);
            });
        });
    }

    private void startSelectedTask() {
        if (deviceInfo == null || deviceInfo.deviceId == null) {
            JOptionPane.showMessageDialog(this, "设备ID无效，无法启动任务");
            return;
        }

        String selected = (String) taskSelector.getSelectedItem();
        ITask task;
        switch (selected) {
            case "挖图": task = new WatuTask(); break;
            case "打图": task = new DatuTask(); break;
            case "师门": task = new ShimenTask(); break;
            case "转图": task = new ZhuantuTask(); break;
            case "接图": task = new JietuTask(); break;
            case "开图": task = new KaituTask(); break;
            case "读图": task = new DutuTask(); break;
            default:
                JOptionPane.showMessageDialog(this, "未知任务类型: " + selected);
                return;
        }

       // Map<String, Map<String, String>> config = TaskConfigLoader.loadConfig(deviceInfo.name, selected.toLowerCase());
        TaskContext context = new TaskContext(deviceInfo.deviceId, deviceInfo.name);

        DeviceManager.startTask(deviceInfo.deviceId, task, context);

        taskLabel.setText("任务: " + selected);
        stepLabel.setText("步骤: 等待中...");
        threadLabel.setText("线程: 运行中");
    }

    private void startTask() {
        startSelectedTask();
    }

    private void initPopupMenu() {
        menu = new JPopupMenu();
        JMenuItem startItem = new JMenuItem("开始任务");
        JMenuItem pauseItem = new JMenuItem("暂停任务");
        JMenuItem resumeItem = new JMenuItem("继续任务");
        JMenuItem stopItem = new JMenuItem("停止任务");
        JMenuItem settingItem = new JMenuItem("设置");

        startItem.addActionListener(e -> startTask());
        pauseItem.addActionListener(e -> pauseTask());
        resumeItem.addActionListener(e -> resumeTask());
        stopItem.addActionListener(e -> stopTask());
        settingItem.addActionListener(e -> openSettings());

        menu.add(startItem);
        menu.add(pauseItem);
        menu.add(resumeItem);
        menu.add(stopItem);
        menu.addSeparator();
        menu.add(settingItem);
    }

    private void pauseTask() {
        DeviceManager.pauseTask(deviceInfo.deviceId);
        threadLabel.setText("线程: 暂停中");
    }

    private void resumeTask() {
        DeviceManager.resumeTask(deviceInfo.deviceId);
        threadLabel.setText("线程: 运行中");
    }

    private void stopTask() {
        DeviceManager.stopTask(deviceInfo.deviceId);
        threadLabel.setText("线程: 已停止");
        stepLabel.setText("步骤: -");
        taskLabel.setText("任务: -");
    }

    private void openSettings() {
        TaskSettingDialog dialog = new TaskSettingDialog(SwingUtilities.getWindowAncestor(this), deviceInfo.name);
        dialog.setVisible(true);
    }
}
