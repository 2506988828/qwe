package com.my.qwe.ui;

import com.my.qwe.http.DeviceHttpClient;
import com.my.qwe.model.DeviceInfo;
import com.my.qwe.util.DefaultConfigGenerator;

import javax.swing.*;
import java.awt.*;
import java.util.Comparator;
import java.util.List;

public class MainUI extends JFrame {
    private JPanel deviceContainer;
    private final List<String> taskTypes = List.of("挖图", "打图", "师门", "分图");

    public MainUI() {
        setTitle("多设备控制中心");
        setSize(1000, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 主容器布局
        setLayout(new BorderLayout());

        // 顶部按钮区域
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton generateConfigBtn = new JButton("一键生成配置文件");
        generateConfigBtn.addActionListener(e -> generateAllDeviceConfigs());
        topPanel.add(generateConfigBtn);
        add(topPanel, BorderLayout.NORTH);

        // 设备面板
        deviceContainer = new JPanel();
        deviceContainer.setLayout(new GridLayout(0, 2, 10, 10));
        add(new JScrollPane(deviceContainer), BorderLayout.CENTER);

        // 加载设备
        loadDevices();
    }

    /**
     * 遍历所有设备并生成默认配置文件
     */
    private void generateAllDeviceConfigs() {
        try {
            List<DeviceInfo> deviceList = DeviceHttpClient.getDeviceList();
            for (DeviceInfo device : deviceList) {
                DefaultConfigGenerator.generate(device.name, taskTypes);
            }
            JOptionPane.showMessageDialog(this, "所有配置文件已生成！");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "生成配置失败: " + e.getMessage());
        }
    }

    /**
     * 加载所有设备并渲染面板
     */
    private void loadDevices() {
        try {
            List<DeviceInfo> deviceList = DeviceHttpClient.getDeviceList();

            deviceList.sort(Comparator.comparing(
                    d -> d.name == null ? "" : d.name,
                    String.CASE_INSENSITIVE_ORDER));

            for (DeviceInfo info : deviceList) {
                DevicePanel panel = new DevicePanel(info);
                deviceContainer.add(panel);
            }

            deviceContainer.revalidate();
            deviceContainer.repaint();
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
