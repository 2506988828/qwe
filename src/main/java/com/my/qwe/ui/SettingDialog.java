package com.my.qwe.ui;

import javax.swing.*;
import java.awt.*;

public class SettingDialog extends JDialog {

    private final String deviceId;

    public SettingDialog(String deviceId) {
        this.deviceId = deviceId;
        setTitle(deviceId + " 设置");
        setModal(true);
        setSize(400, 300);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel("这里放置设备 " + deviceId + " 的配置界面");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(label, BorderLayout.CENTER);
        add(panel);
    }
}

