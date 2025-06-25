package com.my.qwe.ui;

import com.my.qwe.core.DeviceManager;
import com.my.qwe.core.TaskEventBus;
import com.my.qwe.core.TaskStepEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class DevicePanel extends JPanel {

    private final String deviceId;
    private final JLabel statusLabel;

    public DevicePanel(String deviceId) {
        this.deviceId = deviceId;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(deviceId));

        statusLabel = new JLabel("未启动");
        add(statusLabel, BorderLayout.CENTER);

        // 注册任务步骤事件监听，更新状态
        TaskEventBus.register(event -> {
            if (event.getDeviceId().equals(deviceId)) {
                SwingUtilities.invokeLater(() ->
                        statusLabel.setText(event.getStepDescription()));
            }
        });

        // 右键菜单
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem startItem = new JMenuItem("开始");
        JMenuItem pauseItem = new JMenuItem("暂停");
        JMenuItem resumeItem = new JMenuItem("继续");
        JMenuItem stopItem = new JMenuItem("停止");
        JMenuItem settingItem = new JMenuItem("设置");

        popupMenu.add(startItem);
        popupMenu.add(pauseItem);
        popupMenu.add(resumeItem);
        popupMenu.add(stopItem);
        popupMenu.addSeparator();
        popupMenu.add(settingItem);

        startItem.addActionListener(e -> {
            if (!DeviceManager.isRunning(deviceId)) {
                // 这里你可以换成对应任务，比如BaotuTask，配置可扩展
                DeviceManager.startTask(deviceId, new com.my.qwe.task.BaotuTask(),
                        new com.my.qwe.task.TaskContext(deviceId, null));
            }
        });

        pauseItem.addActionListener(e -> DeviceManager.pauseTask(deviceId));
        resumeItem.addActionListener(e -> DeviceManager.resumeTask(deviceId));
        stopItem.addActionListener(e -> DeviceManager.stopTask(deviceId));
        settingItem.addActionListener(e -> {
            SettingDialog dialog = new SettingDialog(deviceId);
            dialog.setVisible(true);
        });

        this.setComponentPopupMenu(popupMenu);

        // 鼠标右键触发菜单
        this.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }

            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }
}
