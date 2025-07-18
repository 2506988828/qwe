package com.my.qwe.ui;

import org.ini4j.Wini;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TaskSettingDialog extends JDialog {
    private final String deviceId;
    private final JTextArea configArea;
    private final JComboBox<String> taskTypeBox;

    public TaskSettingDialog(Window parent, String deviceId) {
        super(parent, "设备设置 - " + deviceId, Dialog.ModalityType.APPLICATION_MODAL);
        this.deviceId = deviceId;

        setLayout(new BorderLayout());
        setSize(600, 500);
        setLocationRelativeTo(parent);

        taskTypeBox = new JComboBox<>(new String[]{"挖图", "打图", "师门", "分图","开图"});

        JButton loadBtn = new JButton("读取配置文件");
        JButton saveBtn = new JButton("保存配置文件");

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("任务类型:"));
        topPanel.add(taskTypeBox);
        topPanel.add(loadBtn);
        topPanel.add(saveBtn);

        configArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(configArea);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        loadBtn.addActionListener(e -> loadConfig());
        saveBtn.addActionListener(e -> saveConfig());
    }

    private void loadConfig() {
        String taskType = (String) taskTypeBox.getSelectedItem();
        String iniPath = "D:/myapp/config/" + deviceId + ".ini";
        File file = new File(iniPath);

        try {
            // 确保目录存在
            file.getParentFile().mkdirs();

            // 初始化 INI 文件对象
            Wini ini = new Wini(file);
            ini.getConfig().setFileEncoding(StandardCharsets.UTF_8);

            // 如果文件不存在，创建并添加初始配置
            if (!file.exists()) {
                ini.add(taskType);
                ini.put(taskType, "描述", "无其他配置");
                ini.store();
            }

            // 如果 section 不存在，也添加
            if (!ini.containsKey(taskType)) {
                ini.add(taskType);
                ini.put(taskType, "描述", "无其他配置");
                ini.store();
            }

            // 读取并显示该 section 的配置内容
            StringBuilder sb = new StringBuilder();
            for (String key : ini.get(taskType).keySet()) {
                sb.append(key).append("=").append(ini.get(taskType, key)).append("\n");
            }
            configArea.setText(sb.toString());
            JOptionPane.showMessageDialog(this, "配置加载成功");

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "读取失败: " + ex.getMessage());
        }
    }

    private void saveConfig() {
        String taskType = (String) taskTypeBox.getSelectedItem();
        String iniPath = "D:/myapp/config/" + deviceId + ".ini";
        File file = new File(iniPath);

        try {
            // 确保目录存在
            file.getParentFile().mkdirs();
            if (!file.exists()) {
                file.createNewFile(); // 创建空文件
            }
            Wini ini = new Wini(file);
            ini.getConfig().setFileEncoding(StandardCharsets.UTF_8);

            // 清空旧的 section 再写入新配置
            ini.remove(taskType);
            ini.add(taskType);

            String[] lines = configArea.getText().split("\n");
            for (String line : lines) {
                if (line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    ini.put(taskType, parts[0].trim(), parts[1].trim());
                }
            }

            ini.store();
            JOptionPane.showMessageDialog(this, "配置保存成功");

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "保存失败: " + ex.getMessage());
        }
    }
}
