package com.my.qwe.ui;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

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

        taskTypeBox = new JComboBox<>(new String[]{"打图", "师门", "挖图"});

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
        String path = "D:/myapp/config/" + deviceId + "_" + taskType.toLowerCase() + ".json";

        try {
            File file = new File(path);
            if (!file.exists()) {
                configArea.setText("{\n  \"param1\": \"value\",\n  \"param2\": 123\n}");
                JOptionPane.showMessageDialog(this, "未找到配置，生成默认内容");
            } else {
                String content = Files.readString(Paths.get(path));
                configArea.setText(content);
                JOptionPane.showMessageDialog(this, "配置加载成功");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "读取失败: " + ex.getMessage());
        }
    }

    private void saveConfig() {
        String taskType = (String) taskTypeBox.getSelectedItem();
        String path = "D:/myapp/config/" + deviceId + "_" + taskType.toLowerCase() + ".json";

        try {
            Files.writeString(Paths.get(path), configArea.getText(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            JOptionPane.showMessageDialog(this, "配置保存成功");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "保存失败: " + ex.getMessage());
        }
    }
}
