package com.my.qwe.ui;

import com.my.qwe.core.DeviceManager;
import com.my.qwe.task.*;
import com.my.qwe.model.DeviceInfo;
import com.my.qwe.task.config.TaskConfigLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

public class DevicePanel extends JPanel {
    private final DeviceInfo deviceInfo;
    private final JLabel statusLabel;
    private JPopupMenu menu;
    private JComboBox<String> taskSelector;
    private JButton startButton;


    public DevicePanel(DeviceInfo info) {
        this.deviceInfo = info;
        setBorder(BorderFactory.createTitledBorder(info.deviceName + " (" + info.deviceId + ")"));
        setLayout(new BorderLayout());

        // 任务选择 + 开始按钮放入顶部面板
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        taskSelector = new JComboBox<>(new String[]{"挖图", "打图", "师门","分图"});
        startButton = new JButton("开始");
        startButton.addActionListener(e -> startSelectedTask());

        topPanel.add(new JLabel("任务: "));
        topPanel.add(taskSelector);
        topPanel.add(startButton);

        add(topPanel, BorderLayout.NORTH);

        // 状态标签放中间
        statusLabel = new JLabel("状态: 未启动");
        add(statusLabel, BorderLayout.CENTER);

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
        TaskStepNotifier.registerListener(deviceInfo.deviceId, message -> {
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("状态: " + message);
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
            case "挖图":
                task = new BaotuTask();
                break;
            case "打图":
                task = new DatuTask();
                break;
            case "师门":
                task = new ShimenTask();
                break;
            case "分图":
                task = new FentuTask();
                break;
            default:
                JOptionPane.showMessageDialog(this, "未知任务类型: " + selected);
                return;
        }

        Map<String, Map<String, String>> config = TaskConfigLoader.loadConfig(deviceInfo.name, selected.toLowerCase());
        TaskContext context = new TaskContext(deviceInfo.deviceId, config,deviceInfo.name);

        DeviceManager.startTask(deviceInfo.deviceId, task, context);
        statusLabel.setText("状态: 执行中（" + selected + "）");
    }

    private void startTask() {
        // 改成调用上面的方法
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
        statusLabel.setText("状态: 暂停中");
    }

    private void resumeTask() {
        DeviceManager.resumeTask(deviceInfo.deviceId);
        statusLabel.setText("状态: 恢复执行");
    }

    private void stopTask() {
        DeviceManager.stopTask(deviceInfo.deviceId);
        statusLabel.setText("状态: 已停止");
    }

    private void openSettings() {
        TaskSettingDialog dialog = new TaskSettingDialog(SwingUtilities.getWindowAncestor(this), deviceInfo.name);
        dialog.setVisible(true);
    }
}
