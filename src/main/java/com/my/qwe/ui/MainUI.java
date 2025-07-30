package com.my.qwe.ui;

import com.my.qwe.core.DeviceManager;
import com.my.qwe.core.DeviceTask;
import com.my.qwe.task.*;
import com.my.qwe.http.DeviceHttpClient;
import com.my.qwe.model.DeviceInfo;
import com.my.qwe.util.DefaultConfigGenerator;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MainUI extends JFrame {
    private JTable deviceTable;
    private DeviceTableModel tableModel;
    private final List<String> taskTypes = List.of("挖图","测试任务","取图", "接图",  "打图", "师门", "转图", "开图", "读图");

    public MainUI() {
        setTitle("控制中心");
        setSize(1000, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initializeUI();
        loadDevices();
        setupNotificationListeners();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        // 顶部工具栏
        JPanel toolBar = createToolBar();
        add(toolBar, BorderLayout.NORTH);

        // 创建表格
        tableModel = new DeviceTableModel();
        deviceTable = new JTable(tableModel);
        setupTable();

        // 添加滚动面板
        JScrollPane scrollPane = new JScrollPane(deviceTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createToolBar() {
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton refreshButton = new JButton("刷新设备");
        refreshButton.addActionListener(e -> loadDevices());

        JButton generateConfigBtn = new JButton("一键生成配置文件");
        generateConfigBtn.addActionListener(e -> generateAllDeviceConfigs());

        JButton startAllBtn = new JButton("启动所有");
        startAllBtn.addActionListener(e -> startAllTasks());

        JButton stopAllBtn = new JButton("停止所有");
        stopAllBtn.addActionListener(e -> stopAllTasks());

        toolBar.add(refreshButton);
        toolBar.add(generateConfigBtn);
        toolBar.add(new JSeparator(SwingConstants.VERTICAL));
        toolBar.add(startAllBtn);
        toolBar.add(stopAllBtn);

        return toolBar;
    }

    private void setupTable() {
        deviceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        deviceTable.setRowHeight(50); // 增加行高以支持多行文本
        deviceTable.getTableHeader().setReorderingAllowed(false);

        // 设置列宽
        deviceTable.getColumnModel().getColumn(0).setPreferredWidth(80);  // 设备名 - 缩短
        deviceTable.getColumnModel().getColumn(1).setPreferredWidth(50);  // 状态
        deviceTable.getColumnModel().getColumn(2).setPreferredWidth(70);  // 任务选择 - 缩短
        deviceTable.getColumnModel().getColumn(3).setPreferredWidth(60);  // 当前任务 - 缩短
        deviceTable.getColumnModel().getColumn(4).setPreferredWidth(200); // 当前步骤 - 加长
        deviceTable.getColumnModel().getColumn(5).setPreferredWidth(60);  // 线程状态 - 缩短
        deviceTable.getColumnModel().getColumn(6).setPreferredWidth(200); // 操作按钮 - 增加宽度容纳更多按钮

        // 设置任务选择列的编辑器
        JComboBox<String> taskComboBox = new JComboBox<>(taskTypes.toArray(new String[0]));
        deviceTable.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(taskComboBox));

        // 设置操作按钮列的渲染器和编辑器
        deviceTable.getColumnModel().getColumn(6).setCellRenderer(new ButtonRenderer());
        deviceTable.getColumnModel().getColumn(6).setCellEditor(new ButtonEditor());

        // 设置状态列的渲染器
        deviceTable.getColumnModel().getColumn(1).setCellRenderer(new StatusRenderer());
        deviceTable.getColumnModel().getColumn(5).setCellRenderer(new ThreadStatusRenderer());

        // 设置当前步骤列的渲染器 - 支持多行文本
        deviceTable.getColumnModel().getColumn(4).setCellRenderer(new MultiLineRenderer());
    }

    private void setupNotificationListeners() {
        // 监听任务步骤通知 - 根据你原来的代码结构调整
        // 你需要为每个设备注册监听器，或者修改通知系统支持全局监听
        // 这里提供两种方案，你可以根据实际情况选择

        // 方案1：如果你的通知系统支持全局监听
        /*
        TaskStepNotifier.registerGlobalListener((String deviceId, String message) -> {
            SwingUtilities.invokeLater(() -> {
                tableModel.updateStepStatus(deviceId, message);
            });
        });

        TaskThreadStatusNotifier.registerGlobal((String deviceId, String status) -> {
            SwingUtilities.invokeLater(() -> {
                tableModel.updateThreadStatus(deviceId, status);
            });
        });
        */

        // 方案2：按照你原来的方式，为每个设备注册（在loadDevices中调用）
        // 这个方法会在loadDevices()中被调用
    }

    private void registerDeviceListeners() {
        // 为每个设备注册监听器
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            DeviceInfo device = tableModel.getDeviceAt(i);
            String deviceId = device.deviceId;

            // 注册步骤监听器
            TaskStepNotifier.registerListener(deviceId, message -> {
                SwingUtilities.invokeLater(() -> {
                    tableModel.updateStepStatus(deviceId, message);
                });
            });

            // 注册线程状态监听器
            TaskThreadStatusNotifier.register(deviceId, status -> {
                SwingUtilities.invokeLater(() -> {
                    tableModel.updateThreadStatus(deviceId, status);
                });
            });
        }
    }

    private void loadDevices() {
        try {
            List<DeviceInfo> deviceList = DeviceHttpClient.getDeviceList();
            deviceList.sort(Comparator.comparing(
                    d -> d.name == null ? "" : d.name,
                    String.CASE_INSENSITIVE_ORDER));

            tableModel.setDevices(deviceList);

            // 设备加载完成后注册监听器
            registerDeviceListeners();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "加载设备失败: " + e.getMessage());
        }
    }

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

    private void startAllTasks() {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            DeviceInfo device = tableModel.getDeviceAt(i);
            String selectedTask = tableModel.getSelectedTaskAt(i);
            if (selectedTask != null && !selectedTask.isEmpty()) {
                startTask(device, selectedTask);
            }
        }
    }

    private void stopAllTasks() {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            DeviceInfo device = tableModel.getDeviceAt(i);
            DeviceManager.stopTask(device.deviceId);
            tableModel.updateTaskStatus(device.deviceId, "", "", "已停止");
        }
    }

    private void startTask(DeviceInfo deviceInfo, String taskName) {
        if (deviceInfo == null || deviceInfo.deviceId == null) {
            JOptionPane.showMessageDialog(this, "设备ID无效，无法启动任务");
            return;
        }

        ITask task = createTask(taskName);
        if (task == null) {
            JOptionPane.showMessageDialog(this, "未知任务类型: " + taskName);
            return;
        }

        TaskContext context = new TaskContext(deviceInfo.deviceId, deviceInfo.name);
        DeviceManager.startTask(deviceInfo.deviceId, task, context);

        tableModel.updateTaskStatus(deviceInfo.deviceId, taskName, "等待中...", "运行中");
    }

    private ITask createTask(String taskName) {
        System.out.println("进入创建任务");
        switch (taskName) {
            case "取图": return new QutuTask();
            case "挖图": return new WatuTask();
            case "测试任务": return new CeshiTask();
            case "打图": return new DatuTask();
            case "师门": return new ShimenTask();
            case "转图": return new ZhuantuTask();
            case "接图": return new JietuTask();
            case "开图": return new KaituTask();
            case "读图": return new DutuTask();
            default: return null;
        }
    }

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            MainUI ui = new MainUI();
            ui.setVisible(true);
        });
    }

    // 表格数据模型
    private class DeviceTableModel extends AbstractTableModel {
        private List<DeviceInfo> devices = new ArrayList<>();
        private List<String> selectedTasks = new ArrayList<>();
        private List<String> currentTasks = new ArrayList<>();
        private List<String> currentSteps = new ArrayList<>();
        private List<String> threadStatuses = new ArrayList<>();

        private final String[] columnNames = {
                "设备名", "状态", "任务选择", "当前任务", "当前步骤", "线程状态", "操作"
        };

        public void setDevices(List<DeviceInfo> devices) {
            this.devices = new ArrayList<>(devices);
            this.selectedTasks = new ArrayList<>();
            this.currentTasks = new ArrayList<>();
            this.currentSteps = new ArrayList<>();
            this.threadStatuses = new ArrayList<>();

            for (int i = 0; i < devices.size(); i++) {
                selectedTasks.add("挖图"); // 默认选择
                currentTasks.add("-");
                currentSteps.add("-");
                threadStatuses.add("未启动");
            }
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return devices.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex >= devices.size()) return null;

            DeviceInfo device = devices.get(rowIndex);
            switch (columnIndex) {
                case 0: return device.name;
                case 1: return (device.state == 1) ? "在线" : "离线";
                case 2: return selectedTasks.get(rowIndex);
                case 3: return currentTasks.get(rowIndex);
                case 4: return currentSteps.get(rowIndex);
                case 5: return threadStatuses.get(rowIndex);
                case 6: return "操作";
                default: return null;
            }
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex == 2 && rowIndex < selectedTasks.size()) {
                selectedTasks.set(rowIndex, value.toString());
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 2 || columnIndex == 6; // 任务选择和操作列可编辑
        }

        public DeviceInfo getDeviceAt(int rowIndex) {
            return rowIndex < devices.size() ? devices.get(rowIndex) : null;
        }

        public String getSelectedTaskAt(int rowIndex) {
            return rowIndex < selectedTasks.size() ? selectedTasks.get(rowIndex) : null;
        }

        public void updateTaskStatus(String deviceId, String currentTask, String currentStep, String threadStatus) {
            for (int i = 0; i < devices.size(); i++) {
                if (devices.get(i).deviceId.equals(deviceId)) {
                    if (!currentTask.isEmpty()) currentTasks.set(i, currentTask);
                    if (!currentStep.isEmpty()) currentSteps.set(i, currentStep);
                    if (!threadStatus.isEmpty()) threadStatuses.set(i, threadStatus);
                    fireTableRowsUpdated(i, i);
                    break;
                }
            }
        }

        public void updateStepStatus(String deviceId, String step) {
            updateTaskStatus(deviceId, "", step, "");
        }

        public void updateThreadStatus(String deviceId, String status) {
            updateTaskStatus(deviceId, "", "", status);
        }
    }

    // 多行文本渲染器 - 用于当前步骤列
    private class MultiLineRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {

            JTextArea textArea = new JTextArea();
            textArea.setText(value != null ? value.toString() : "");
            textArea.setWrapStyleWord(true);
            textArea.setLineWrap(true);
            textArea.setOpaque(true);
            textArea.setBorder(null);
            textArea.setFont(table.getFont());

            if (isSelected) {
                textArea.setBackground(table.getSelectionBackground());
                textArea.setForeground(table.getSelectionForeground());
            } else {
                textArea.setBackground(table.getBackground());
                textArea.setForeground(table.getForeground());
            }

            return textArea;
        }
    }

    // 状态渲染器
    private class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if ("在线".equals(value)) {
                c.setForeground(Color.GREEN.darker());
            } else {
                c.setForeground(Color.RED);
            }

            return c;
        }
    }

    // 线程状态渲染器
    private class ThreadStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            String status = value.toString();
            if ("运行中".equals(status)) {
                c.setForeground(Color.GREEN.darker());
            } else if ("暂停中".equals(status)) {
                c.setForeground(Color.ORANGE);
            } else if ("已停止".equals(status) || "未启动".equals(status)) {
                c.setForeground(Color.GRAY);
            }

            return c;
        }
    }

    // 按钮渲染器
    private class ButtonRenderer extends JPanel implements TableCellRenderer {
        private JButton startBtn, pauseBtn, resumeBtn, stopBtn, settingBtn;

        public ButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 1, 1));
            startBtn = new JButton("开始");
            pauseBtn = new JButton("暂停");
            resumeBtn = new JButton("继续");
            stopBtn = new JButton("停止");
            settingBtn = new JButton("设置");

            // 设置更小的按钮边距
            Insets margin = new Insets(1, 3, 1, 3);
            startBtn.setMargin(margin);
            pauseBtn.setMargin(margin);
            resumeBtn.setMargin(margin);
            stopBtn.setMargin(margin);
            settingBtn.setMargin(margin);

            // 设置字体大小
            Font smallFont = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
            startBtn.setFont(smallFont);
            pauseBtn.setFont(smallFont);
            resumeBtn.setFont(smallFont);
            stopBtn.setFont(smallFont);
            settingBtn.setFont(smallFont);

            add(startBtn);
            add(pauseBtn);
            add(resumeBtn);
            add(stopBtn);
            add(settingBtn);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }

    // 按钮编辑器
    private class ButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private JPanel panel;
        private JButton startBtn, pauseBtn, resumeBtn, stopBtn, settingBtn;
        private int currentRow;

        public ButtonEditor() {
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 1, 1));
            startBtn = new JButton("开始");
            pauseBtn = new JButton("暂停");
            resumeBtn = new JButton("继续");
            stopBtn = new JButton("停止");
            settingBtn = new JButton("设置");

            // 设置更小的按钮边距
            Insets margin = new Insets(1, 3, 1, 3);
            startBtn.setMargin(margin);
            pauseBtn.setMargin(margin);
            resumeBtn.setMargin(margin);
            stopBtn.setMargin(margin);
            settingBtn.setMargin(margin);

            // 设置字体大小
            Font smallFont = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
            startBtn.setFont(smallFont);
            pauseBtn.setFont(smallFont);
            resumeBtn.setFont(smallFont);
            stopBtn.setFont(smallFont);
            settingBtn.setFont(smallFont);

            startBtn.addActionListener(e -> {
                DeviceInfo device = tableModel.getDeviceAt(currentRow);
                String selectedTask = tableModel.getSelectedTaskAt(currentRow);
                startTask(device, selectedTask);
                fireEditingStopped();
            });

            pauseBtn.addActionListener(e -> {
                DeviceInfo device = tableModel.getDeviceAt(currentRow);
                DeviceManager.pauseTask(device.deviceId);
                tableModel.updateThreadStatus(device.deviceId, "暂停中");
                fireEditingStopped();
            });

            resumeBtn.addActionListener(e -> {
                DeviceInfo device = tableModel.getDeviceAt(currentRow);
                DeviceManager.resumeTask(device.deviceId);
                tableModel.updateThreadStatus(device.deviceId, "运行中");
                fireEditingStopped();
            });

            stopBtn.addActionListener(e -> {
                DeviceInfo device = tableModel.getDeviceAt(currentRow);
                DeviceManager.stopTask(device.deviceId);
                tableModel.updateTaskStatus(device.deviceId, "-", "-", "已停止");
                fireEditingStopped();
            });

            settingBtn.addActionListener(e -> {
                DeviceInfo device = tableModel.getDeviceAt(currentRow);
                TaskSettingDialog dialog = new TaskSettingDialog(MainUI.this, device.name);
                dialog.setVisible(true);
                fireEditingStopped();
            });

            panel.add(startBtn);
            panel.add(pauseBtn);
            panel.add(resumeBtn);
            panel.add(stopBtn);
            panel.add(settingBtn);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            currentRow = row;
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return "操作";
        }
    }
}