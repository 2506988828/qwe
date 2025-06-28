package com.my.qwe.ui;

import com.my.qwe.http.DeviceHttpClient;
import com.my.qwe.model.DeviceInfo;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.List;

public class MainUI extends JFrame {
    private JPanel deviceContainer;


    public MainUI() {
        setTitle("多设备控制中心");
        setSize(1000, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        deviceContainer = new JPanel();
        deviceContainer.setLayout(new GridLayout(0, 2, 10, 10));
        add(new JScrollPane(deviceContainer), BorderLayout.CENTER);

        loadDevices(); // 关键方法
    }

    private void loadDevices() {
        try {
            List<DeviceInfo> deviceList = DeviceHttpClient.getDeviceList();

            deviceList.sort(Comparator.comparing(
                    d -> d.name == null ? "" : d.name,
                    String.CASE_INSENSITIVE_ORDER));
            for (DeviceInfo info : deviceList) {
                DevicePanel panel = new DevicePanel(info);
                deviceContainer.add(panel);
                deviceContainer.revalidate();
                deviceContainer.repaint();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "加载设备失败: " + e.getMessage());
        }
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            MainUI ui = new MainUI();
            ui.setVisible(true);
        });
    }
}
