package com.my.qwe.ui;

import com.my.qwe.core.DeviceManager;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class MainUI extends JFrame {



    private final JPanel deviceContainer;
    private final Map<String, DevicePanel> devicePanels = new HashMap<>();

    public MainUI() {
        setTitle("设备任务管理器");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        deviceContainer = new JPanel();
        deviceContainer.setLayout(new BoxLayout(deviceContainer, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(deviceContainer);
        add(scrollPane, BorderLayout.CENTER);

        // 示例添加设备
        addDevice("DEVICE-001");
        addDevice("DEVICE-002");
    }

    public void addDevice(String deviceId) {
        if (devicePanels.containsKey(deviceId)) return;

        DevicePanel panel = new DevicePanel(deviceId);
        devicePanels.put(deviceId, panel);
        deviceContainer.add(panel);
        deviceContainer.revalidate();
        deviceContainer.repaint();
    }

    public DevicePanel getDevicePanel(String deviceId) {
        return devicePanels.get(deviceId);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainUI ui = new MainUI();
            ui.setVisible(true);
        });

    }
}
