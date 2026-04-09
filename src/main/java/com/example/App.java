package com.example;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class App extends JFrame {
    public static class KeyMapping {
        private String sourceName; // 源代码名称（XML里的Input Name）
        private String displayName; // 多语言显示名（cn.txt/en.txt）
        private String rawKey; // 原始键位
        private String rawMethod; // 原始触发方式
        private String modifiedKey; // 修改键位
        private String modifiedMethod; // 修改触发方式
        private String inputNameLine; // Input标签行
        private String gamePadLine; // GamePad标签行

        public KeyMapping(String sourceName, String displayName, String rawKey, String rawMethod,
                String modifiedKey, String modifiedMethod, String inputNameLine, String gamePadLine) {
            this.sourceName = sourceName;
            this.displayName = displayName;
            this.rawKey = rawKey;
            this.rawMethod = rawMethod;
            this.modifiedKey = modifiedKey;
            this.modifiedMethod = modifiedMethod;
            this.inputNameLine = inputNameLine;
            this.gamePadLine = gamePadLine;
        }

        public String getSourceName() {
            return sourceName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getRawKey() {
            return rawKey;
        }

        public String getRawMethod() {
            return rawMethod;
        }

        public String getModifiedKey() {
            return modifiedKey;
        }

        public void setModifiedKey(String modifiedKey) {
            this.modifiedKey = modifiedKey;
        }

        public String getModifiedMethod() {
            return modifiedMethod;
        }

        public void setModifiedMethod(String modifiedMethod) {
            this.modifiedMethod = modifiedMethod;
        }

        public String getInputNameLine() {
            return inputNameLine;
        }

        public String getGamePadLine() {
            return gamePadLine;
        }
    }

    private java.util.List<KeyMapping> fullDataList = new ArrayList<>();
    private java.util.List<KeyMapping> filteredDataList = new ArrayList<>();
    private Map<String, String> funcNameMap = new HashMap<>(); // key=源代码名称, value=多语言显示名
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextFieldHint searchField;
    private Path sourceControlDir;
    private Path sourceInputMapXml;
    private Path csvDir;
    private JTextArea xmlSourceArea;
    private static final String I18N_BASE = "i18n.Messages";
    private ResourceBundle resourceBundle;
    private Locale currentLocale;

    // 模式切换与多语言TXT对照相关成员变量
    private Set<String> txtSourceSet = new HashSet<>(); // 存储TXT内的源代码名称，用于简单模式过滤
    private boolean isSimpleMode = false; // 当前是否为简单模式
    private JButton simpleModeBtn; // 简单模式按钮
    private JButton advancedModeBtn; // 高级模式按钮
    // 新增：直接保存原文件按钮
    private JButton saveDirectBtn;

    public App() {
        currentLocale = Locale.CHINA;
        try {
            resourceBundle = getResourceBundle(currentLocale);
        } catch (Exception e) {
            System.err.println("多语言文件加载失败，使用默认提示: " + e.getMessage());
            resourceBundle = null;
        }
        String projectRoot = System.getProperty("user.dir");
        sourceControlDir = Paths.get(projectRoot, "Control_Remap");
        sourceInputMapXml = sourceControlDir.resolve("files/0012/ui/inputmap.xml");
        csvDir = Paths.get(projectRoot);
        System.out.println("配置文件路径: " + sourceInputMapXml.toAbsolutePath());
        System.out.println("对照文件目录: " + csvDir.toAbsolutePath());
        loadCsvMapping(); // 初始化加载当前语言的对照文件
        setTitle(getI18n("title", "键位配置工具 (Java 21)"));
        setSize(1490, 960);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
    }

    // 加载对应语言的对照文件（cn.txt/en.txt）+ 兼容原有CSV
    private void loadCsvMapping() {
        funcNameMap.clear();
        txtSourceSet.clear(); // 切换语言时清空，重新加载
        try {
            // 原有CSV加载逻辑（兼容）
            Files.walkFileTree(csvDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String fileName = file.getFileName().toString();
                    if (fileName.endsWith(".csv")) {
                        System.out.println("加载对照CSV: " + fileName);
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(Files.newInputStream(file), StandardCharsets.UTF_8))) {
                            String line;
                            boolean isFirstLine = true;
                            while ((line = reader.readLine()) != null) {
                                line = line.trim();
                                if (isFirstLine) {
                                    isFirstLine = false;
                                    continue;
                                }
                                if (line.isEmpty())
                                    continue;
                                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                                if (parts.length >= 2) {
                                    String displayName = parts[0].replace("\"", "").trim();
                                    String sourceName = parts[1].replace("\"", "").trim();
                                    if (!sourceName.isEmpty() && !displayName.isEmpty()) {
                                        funcNameMap.put(sourceName, displayName);
                                    }
                                }
                            }
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            // 核心：根据当前语言加载cn.txt/en.txt（严格匹配文件名，支持等号前后有空格）
            String txtFileName = currentLocale.equals(Locale.CHINA) ? "cn.txt" : "en.txt";
            Path txtPath = csvDir.resolve(txtFileName);
            if (Files.exists(txtPath)) {
                System.out.println("成功加载对照TXT: " + txtFileName);
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(Files.newInputStream(txtPath), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("["))
                            continue; // 跳过空行/分组行
                        String[] parts = line.split("=", 2); // 仅拆分第一个等号，兼容名称含等号
                        if (parts.length >= 2) {
                            String displayName = parts[0].trim(); // 英文/中文显示名
                            String sourceName = parts[1].trim(); // 源代码名
                            if (!sourceName.isEmpty() && !displayName.isEmpty()) {
                                funcNameMap.put(sourceName, displayName); // TXT优先级高于CSV
                                txtSourceSet.add(sourceName); // 加入简单模式过滤集合
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null,
                            txtFileName + "加载失败: " + e.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                System.err.println("未找到对照TXT: " + txtFileName + "，将使用源代码名显示");
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "对照文件加载失败: " + e.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private ResourceBundle getResourceBundle(Locale locale) throws IOException {
        String language = locale.getLanguage();
        String country = locale.getCountry();
        StringBuilder bundleSuffix = new StringBuilder();
        if (!language.isEmpty()) {
            bundleSuffix.append("_").append(language);
            if (!country.isEmpty())
                bundleSuffix.append("_").append(country);
        }
        String resourcePath = I18N_BASE.replace(".", "/") + bundleSuffix + ".properties";
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                String defaultPath = I18N_BASE.replace(".", "/") + "_zh_CN.properties";
                InputStream defaultIs = getClass().getClassLoader().getResourceAsStream(defaultPath);
                if (defaultIs == null)
                    throw new FileNotFoundException("多语言文件未找到: " + defaultPath);
                return new PropertyResourceBundle(new InputStreamReader(defaultIs, StandardCharsets.UTF_8));
            }
            return new PropertyResourceBundle(new InputStreamReader(is, StandardCharsets.UTF_8));
        }
    }

    public String getI18n(String key, String defaultValue) {
        if (resourceBundle == null)
            return defaultValue;
        try {
            return resourceBundle.getString(key);
        } catch (MissingResourceException e) {
            System.err.println("缺失多语言key: " + key);
            return defaultValue;
        }
    }

    // 核心修复：切换语言时重新加载配置，确保功能名实时切换为当前语言
    private void switchLocale(Locale newLocale) {
        try {
            currentLocale = newLocale;
            resourceBundle = getResourceBundle(newLocale);
            loadCsvMapping(); // 加载新语言的对照文件
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "语言切换失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 更新界面文本
        setTitle(getI18n("title", "Key Mapping Tool (Java 21)"));
        String[] newColNames = currentLocale.equals(Locale.CHINA)
                ? new String[] { "功能", "源代码名称", "原始键位", "原始触发方式", "修改键位", "修改触发方式" }
                : new String[] { "Function", "Source Name", "Original Key", "Original Method", "Modify Key",
                        "Modify Method" };
        tableModel.setColumnIdentifiers(newColNames);

        // 搜索栏组件文本更新
        JPanel searchPanel = (JPanel) getContentPane().getComponent(0);
        ((JLabel) searchPanel.getComponent(0))
                .setText(currentLocale.equals(Locale.CHINA) ? "功能搜索：" : "Function Search:");
        ((JTextFieldHint) searchPanel.getComponent(1)).setHintText(
                currentLocale.equals(Locale.CHINA) ? "输入功能名，支持模糊搜索" : "Enter function name, fuzzy search supported");
        ((JButton) searchPanel.getComponent(2)).setText(currentLocale.equals(Locale.CHINA) ? "搜索" : "Search");
        ((JButton) searchPanel.getComponent(3)).setText(currentLocale.equals(Locale.CHINA) ? "重置搜索" : "Reset Search");
        // 模式按钮文本更新
        simpleModeBtn.setText(currentLocale.equals(Locale.CHINA) ? "简单模式" : "Simple Mode");
        advancedModeBtn.setText(currentLocale.equals(Locale.CHINA) ? "高级模式" : "Advanced Mode");
        // XML区域标题更新
        JPanel xmlPanel = (JPanel) ((JSplitPane) getContentPane().getComponent(1)).getRightComponent();
        xmlPanel.setBorder(
                BorderFactory.createTitledBorder(currentLocale.equals(Locale.CHINA) ? "XML原始代码" : "XML Original Code"));
        // 底部按钮文本更新
        JPanel bottomPanel = (JPanel) getContentPane().getComponent(2);
        Component[] buttons = bottomPanel.getComponents();
        ((JButton) buttons[0]).setText(currentLocale.equals(Locale.CHINA) ? "读取配置文件" : "Load Config File");
        ((JButton) buttons[1]).setText(currentLocale.equals(Locale.CHINA) ? "直接保存原文件" : "Save Directly"); // 新增按钮
        ((JButton) buttons[2]).setText(currentLocale.equals(Locale.CHINA) ? "创建配置副本" : "Create Backup"); // 原保存按钮改名
        ((JButton) buttons[3]).setText(currentLocale.equals(Locale.CHINA) ? "中文" : "Chinese");
        ((JButton) buttons[4]).setText(currentLocale.equals(Locale.CHINA) ? "English" : "English");

        // 【核心修复】如果已有配置数据，重新读取XML重建列表，确保功能名是新语言
        if (!fullDataList.isEmpty()) {
            loadConfigWithoutTip(); // 无提示重新加载配置
        }

        validate();
        repaint();
    }

    private void createAndShowGUI() {
        Font btnFont = new Font("微软雅黑", Font.PLAIN, 14);
        // 搜索面板
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        searchPanel.setBorder(
                BorderFactory.createTitledBorder(currentLocale.equals(Locale.CHINA) ? "功能搜索" : "Function Search"));
        JLabel searchLabel = new JLabel(currentLocale.equals(Locale.CHINA) ? "功能搜索：" : "Function Search:");
        searchLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        searchField = new JTextFieldHint(35);
        searchField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        searchField.setHintText(
                currentLocale.equals(Locale.CHINA) ? "输入功能名，支持模糊搜索" : "Enter function name, fuzzy search supported");
        // 输入框实时过滤（无提示）
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterData(false);
            }
        });

        // 搜索按钮【取消提示】：调用filterData(false)
        JButton searchBtn = new JButton(currentLocale.equals(Locale.CHINA) ? "搜索" : "Search");
        searchBtn.setFont(btnFont);
        searchBtn.setPreferredSize(new Dimension(100, 35));
        searchBtn.addActionListener(e -> filterData(false));

        // 重置搜索按钮（无提示，仅清空输入+过滤）
        JButton resetBtn = new JButton(currentLocale.equals(Locale.CHINA) ? "重置搜索" : "Reset Search");
        resetBtn.setFont(btnFont);
        resetBtn.setPreferredSize(new Dimension(120, 35));
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            filterData(false);
        });

        // 简单/高级模式按钮【取消切换提示】：仅切换状态+过滤，无弹窗
        simpleModeBtn = new JButton(currentLocale.equals(Locale.CHINA) ? "简单模式" : "Simple Mode");
        advancedModeBtn = new JButton(currentLocale.equals(Locale.CHINA) ? "高级模式" : "Advanced Mode");
        for (JButton btn : new JButton[] { simpleModeBtn, advancedModeBtn }) {
            btn.setFont(btnFont);
            btn.setPreferredSize(new Dimension(140, 35));
        }
        // 简单模式：仅切换状态+过滤，无弹窗
        simpleModeBtn.addActionListener(e -> {
            if (!isSimpleMode) {
                isSimpleMode = true;
                filterData(false);
            }
        });
        // 高级模式：仅切换状态+过滤，无弹窗
        advancedModeBtn.addActionListener(e -> {
            if (isSimpleMode) {
                isSimpleMode = false;
                filterData(false);
            }
        });

        // 组装搜索面板
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        searchPanel.add(resetBtn);
        searchPanel.add(simpleModeBtn);
        searchPanel.add(advancedModeBtn);
        add(searchPanel, BorderLayout.NORTH);

        // 主分割面板（表格+XML）
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setDividerLocation(1050);
        mainSplitPane.setDividerSize(3);
        // 表格面板
        JPanel tablePanel = new JPanel(new BorderLayout());
        String[] columnNames = currentLocale.equals(Locale.CHINA)
                ? new String[] { "功能", "源代码名称", "原始键位", "原始触发方式", "修改键位", "修改触发方式" }
                : new String[] { "Function", "Source Name", "Original Key", "Original Method", "Modify Key",
                        "Modify Method" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= 4;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }
        };
        table = new JTable(tableModel);
        table.setFont(new Font("微软雅黑", Font.PLAIN, 13));
        table.setRowHeight(26);
        table.getTableHeader().setFont(new Font("微软雅黑", Font.BOLD, 14));
        // 列宽配置
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(120);
        // 表格选中行，显示XML原始代码
        table.getSelectionModel().addListSelectionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow != -1 && selectedRow < filteredDataList.size()) {
                KeyMapping km = filteredDataList.get(selectedRow);
                xmlSourceArea.setText(km.getInputNameLine() + "\n" + km.getGamePadLine());
            }
        });
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        tablePanel.add(tableScroll, BorderLayout.CENTER);
        mainSplitPane.setLeftComponent(tablePanel);
        // XML面板（修复双重边框）
        JPanel xmlPanel = new JPanel(new BorderLayout(5, 5));
        xmlPanel.setBorder(
                BorderFactory.createTitledBorder(currentLocale.equals(Locale.CHINA) ? "XML原始代码" : "XML Original Code"));
        xmlSourceArea = new JTextArea();
        xmlSourceArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        xmlSourceArea.setLineWrap(true);
        xmlSourceArea.setEditable(false);
        JScrollPane xmlScroll = new JScrollPane(xmlSourceArea);
        xmlPanel.add(xmlScroll, BorderLayout.CENTER);
        mainSplitPane.setRightComponent(xmlPanel);
        add(mainSplitPane, BorderLayout.CENTER);

        // 底部按钮面板（新增直接保存按钮）
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton loadBtn = new JButton(currentLocale.equals(Locale.CHINA) ? "读取配置文件" : "Load Config File");
        // 新增：直接保存原文件按钮
        saveDirectBtn = new JButton(currentLocale.equals(Locale.CHINA) ? "直接保存原文件" : "Save Directly");
        // 原保存按钮改名：创建配置副本
        JButton saveBackupBtn = new JButton(currentLocale.equals(Locale.CHINA) ? "创建配置副本" : "Create Backup");
        JButton btnZh = new JButton(currentLocale.equals(Locale.CHINA) ? "中文" : "Chinese");
        JButton btnEn = new JButton(currentLocale.equals(Locale.CHINA) ? "English" : "English");
        // 统一按钮样式
        for (JButton btn : new JButton[] { loadBtn, saveDirectBtn, saveBackupBtn, btnZh, btnEn }) {
            btn.setFont(btnFont);
            btn.setPreferredSize(new Dimension(160, 35));
        }
        // 读取配置【取消成功提示，保留错误提示】
        loadBtn.addActionListener(e -> loadConfig());
        // 新增：直接保存原文件按钮事件
        saveDirectBtn.addActionListener(e -> saveDirectly());
        // 原保存按钮：仅创建副本，不修改原文件
        saveBackupBtn.addActionListener(e -> saveBackup());
        // 语言切换
        btnZh.addActionListener(e -> switchLocale(Locale.CHINA));
        btnEn.addActionListener(e -> switchLocale(Locale.US));
        // 组装底部按钮
        bottomPanel.add(loadBtn);
        bottomPanel.add(saveDirectBtn);
        bottomPanel.add(saveBackupBtn);
        bottomPanel.add(btnZh);
        bottomPanel.add(btnEn);
        add(bottomPanel, BorderLayout.SOUTH);

        // 整体内边距
        Border padding = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        ((JComponent) getContentPane()).setBorder(padding);
        setVisible(true);
    }

    // 数据过滤：支持简单/高级模式+模糊搜索【无提示】
    private void filterData(boolean showTip) {
        filteredDataList.clear();
        tableModel.setRowCount(0);
        if (fullDataList.isEmpty())
            return; // 无数据时直接返回，取消提示
        String searchText = searchField.getText().toLowerCase().trim();
        for (KeyMapping km : fullDataList) {
            // 简单模式过滤：仅保留当前语言TXT中的配置项
            if (isSimpleMode && !txtSourceSet.contains(km.getSourceName()))
                continue;
            // 模糊搜索：多语言显示名 + 源代码名
            boolean isMatch = searchText.isEmpty()
                    || km.getDisplayName().toLowerCase().contains(searchText)
                    || km.getSourceName().toLowerCase().contains(searchText);
            if (!isMatch)
                continue;
            filteredDataList.add(km);
            // 表格渲染：直接用当前语言的displayName
            tableModel.addRow(new Object[] {
                    km.getDisplayName(),
                    km.getSourceName(),
                    km.getRawKey(),
                    km.getRawMethod(),
                    km.getModifiedKey(),
                    km.getModifiedMethod()
            });
        }
    }

    // 读取配置文件【取消成功提示，保留文件不存在等错误提示】
    private void loadConfig() {
        // 检查文件夹/文件是否存在（保留错误提示）
        if (!Files.exists(sourceControlDir)) {
            JOptionPane.showMessageDialog(this,
                    getI18n("folder_not_found", "未找到Control_Remap文件夹！\n路径：") + sourceControlDir.toAbsolutePath(),
                    getI18n("error", "错误"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!Files.exists(sourceInputMapXml)) {
            JOptionPane.showMessageDialog(this,
                    getI18n("xml_not_found", "未找到inputmap.xml！\n路径：") + sourceInputMapXml.toAbsolutePath(),
                    getI18n("error", "错误"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        // 重新加载当前语言的对照文件（支持TXT修改后实时生效）
        loadCsvMapping();
        fullDataList.clear();
        filteredDataList.clear();
        tableModel.setRowCount(0);
        xmlSourceArea.setText("");
        System.out.println("开始解析inputmap.xml...");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(skipBOM(Files.newInputStream(sourceInputMapXml)), StandardCharsets.UTF_8))) {
            String lastInputLine = "";
            String line;
            int matchCount = 0;
            while ((line = reader.readLine()) != null) {
                String trimLine = line.trim();
                if (trimLine.startsWith("<Input") && trimLine.contains("Name=")) {
                    lastInputLine = line;
                    continue;
                }
                if (trimLine.toLowerCase().contains("<gamepad") && !trimLine.startsWith("<!--")) {
                    Matcher keyMatcher = Pattern.compile("Key=(['\"])(.*?)\\1", Pattern.CASE_INSENSITIVE)
                            .matcher(trimLine);
                    Matcher methodMatcher = Pattern.compile("Method=(['\"])(.*?)\\1", Pattern.CASE_INSENSITIVE)
                            .matcher(trimLine);
                    if (!keyMatcher.find() || !methodMatcher.find())
                        continue;
                    String sourceName = extractSourceName(lastInputLine);
                    if (sourceName.isEmpty())
                        continue;
                    // 取当前语言的显示名，无则用源代码名
                    String displayName = funcNameMap.getOrDefault(sourceName, sourceName);
                    String rawKey = keyMatcher.group(2).trim();
                    String rawMethod = methodMatcher.group(2).trim();
                    // 新建KeyMapping（用displayName存储多语言名称）
                    KeyMapping km = new KeyMapping(
                            sourceName, displayName, rawKey, rawMethod,
                            rawKey, rawMethod, lastInputLine, line);
                    fullDataList.add(km);
                    matchCount++;
                }
            }
            filterData(false);
            System.out.println("解析完成，共加载" + matchCount + "条键位配置");
            // 取消读取成功的弹窗提示
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    getI18n("load_failed", "读取失败：") + e.getMessage(),
                    getI18n("error", "错误"), JOptionPane.ERROR_MESSAGE);
        }
    }

    // 语言切换时专用：无提示重新加载配置（内部方法）
    private void loadConfigWithoutTip() {
        if (!Files.exists(sourceControlDir) || !Files.exists(sourceInputMapXml))
            return;
        loadCsvMapping();
        fullDataList.clear();
        filteredDataList.clear();
        tableModel.setRowCount(0);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(skipBOM(Files.newInputStream(sourceInputMapXml)), StandardCharsets.UTF_8))) {
            String lastInputLine = "";
            String line;
            while ((line = reader.readLine()) != null) {
                String trimLine = line.trim();
                if (trimLine.startsWith("<Input") && trimLine.contains("Name=")) {
                    lastInputLine = line;
                    continue;
                }
                if (trimLine.toLowerCase().contains("<gamepad") && !trimLine.startsWith("<!--")) {
                    Matcher keyMatcher = Pattern.compile("Key=(['\"])(.*?)\\1", Pattern.CASE_INSENSITIVE)
                            .matcher(trimLine);
                    Matcher methodMatcher = Pattern.compile("Method=(['\"])(.*?)\\1", Pattern.CASE_INSENSITIVE)
                            .matcher(trimLine);
                    if (!keyMatcher.find() || !methodMatcher.find())
                        continue;
                    String sourceName = extractSourceName(lastInputLine);
                    if (sourceName.isEmpty())
                        continue;
                    String displayName = funcNameMap.getOrDefault(sourceName, sourceName);
                    String rawKey = keyMatcher.group(2).trim();
                    String rawMethod = methodMatcher.group(2).trim();
                    fullDataList.add(new KeyMapping(sourceName, displayName, rawKey, rawMethod, rawKey, rawMethod,
                            lastInputLine, line));
                }
            }
            filterData(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 提取Input标签的Name值
    private String extractSourceName(String inputLine) {
        if (inputLine.isEmpty())
            return "";
        Matcher m = Pattern.compile("Name=(['\"])(.*?)\\1", Pattern.CASE_INSENSITIVE).matcher(inputLine);
        return m.find() ? m.group(2).trim() : "";
    }

    // 跳过UTF-8 BOM头，兼容非标XML
    private InputStream skipBOM(InputStream is) throws IOException {
        if (!is.markSupported())
            is = new BufferedInputStream(is);
        is.mark(3);
        byte[] bom = new byte[3];
        if (is.read(bom) == 3 && bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
            System.out.println("跳过UTF-8 BOM头");
            return is;
        }
        is.reset();
        return is;
    }

    // 【修复+增强】直接保存原文件，自动备份原XML，永不损坏
    private void saveDirectly() {
        if (fullDataList.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    getI18n("load_first_tip", "请先读取配置文件！"),
                    getI18n("tip", "提示"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 保存表格修改到内存
        for (int i = 0; i < filteredDataList.size(); i++) {
            KeyMapping km = filteredDataList.get(i);
            String modKey = (String) tableModel.getValueAt(i, 4);
            String modMethod = (String) tableModel.getValueAt(i, 5);
            if (modKey != null && !modKey.trim().isEmpty())
                km.setModifiedKey(modKey.trim());
            if (modMethod != null && !modMethod.trim().isEmpty())
                km.setModifiedMethod(modMethod.trim());
        }

        try {
            // ✅ 安全机制：保存前自动备份原文件（inputmap.xml.bak），防止误操作损坏
            Path backupXml = sourceInputMapXml.resolveSibling("inputmap.xml.bak");
            Files.copy(sourceInputMapXml, backupXml, StandardCopyOption.REPLACE_EXISTING);

            // ✅ 重写原文件（100%保留格式，无丢失）
            rewriteInputMapXml(sourceInputMapXml);

            JOptionPane.showMessageDialog(this,
                    getI18n("save_direct_success", "原文件保存成功！\n原文件已自动备份为: inputmap.xml.bak") + "\n路径："
                            + sourceInputMapXml.toAbsolutePath(),
                    getI18n("success", "成功"), JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    getI18n("save_direct_failed", "原文件保存失败：") + e.getMessage(),
                    getI18n("error", "错误"), JOptionPane.ERROR_MESSAGE);
        }
    }

    // ===================== 原保存方法改名：仅创建副本，不修改原文件 =====================
    private void saveBackup() {
        if (fullDataList.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    getI18n("load_first_tip", "请先读取配置文件！"),
                    getI18n("tip", "提示"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        // 保存表格中的修改到内存
        for (int i = 0; i < filteredDataList.size(); i++) {
            KeyMapping km = filteredDataList.get(i);
            String modKey = (String) tableModel.getValueAt(i, 4);
            String modMethod = (String) tableModel.getValueAt(i, 5);
            if (modKey != null && !modKey.trim().isEmpty())
                km.setModifiedKey(modKey.trim());
            if (modMethod != null && !modMethod.trim().isEmpty())
                km.setModifiedMethod(modMethod.trim());
        }
        try {
            Path myKeyBoardDir = Paths.get(System.getProperty("user.dir"), "My_KeyBoard");
            Path targetDir = findNextDir(myKeyBoardDir, "Control_Remap_");
            copyDirectory(sourceControlDir, targetDir); // 复制整个Control_Remap目录
            Path targetXml = targetDir.resolve("files/0012/ui/inputmap.xml");
            rewriteInputMapXml(targetXml); // 重写修改后的键位
            // 保留保存成功提示
            JOptionPane.showMessageDialog(this,
                    getI18n("save_backup_success", "副本创建成功！\n路径：") + targetDir.toAbsolutePath(),
                    getI18n("success", "成功"), JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            // 保留保存失败提示
            JOptionPane.showMessageDialog(this,
                    getI18n("save_backup_failed", "副本创建失败：") + e.getMessage(),
                    getI18n("error", "错误"), JOptionPane.ERROR_MESSAGE);
        }
    }

    // 【彻底修复】重写inputmap.xml，100%保留原文件格式/注释/空行，仅修改键位
    private void rewriteInputMapXml(Path targetXml) throws IOException {
        Map<String, KeyMapping> sourceToKmMap = new HashMap<>();
        for (KeyMapping km : fullDataList) {
            sourceToKmMap.put(km.getSourceName(), km);
        }

        // 关键：读取【完整的原始文件】（带BOM、注释、空行、所有内容）
        List<String> allLines = Files.readAllLines(sourceInputMapXml, StandardCharsets.UTF_8);
        String lastSourceName = "";

        // 逐行处理，仅修改需要改的行，其余完全保留
        for (int i = 0; i < allLines.size(); i++) {
            String line = allLines.get(i);
            String trimLine = line.trim();

            // 匹配Input标签，记录当前源代码名
            if (trimLine.startsWith("<Input") && trimLine.contains("Name=")) {
                lastSourceName = extractSourceName(line);
                continue;
            }

            // 匹配GamePad标签，且有对应修改配置 → 仅修改这一行
            if (trimLine.toLowerCase().contains("<gamepad")
                    && !trimLine.startsWith("<!--")
                    && sourceToKmMap.containsKey(lastSourceName)) {

                KeyMapping km = sourceToKmMap.get(lastSourceName);
                // 安全替换Key和Method，不破坏行格式
                line = line.replaceFirst("(Key|key)=([\"'])[^\"']*\\2", "Key=\"" + km.getModifiedKey() + "\"")
                        .replaceFirst("(Method|method)=([\"'])[^\"']*\\2", "Method=\"" + km.getModifiedMethod() + "\"");

                allLines.set(i, line);
            }
        }

        // 【关键】完整写入所有行，1:1还原原文件格式，无任何内容丢失
        Files.write(targetXml, allLines, StandardCharsets.UTF_8);
    }

    // 复制整个目录（保留原目录结构）
    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                if (!Files.exists(targetDir))
                    Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    // 生成自增的副本目录（Control_Remap_1/2/3...）
    private Path findNextDir(Path baseDir, String prefix) {
        int i = 1;
        while (true) {
            Path candidate = baseDir.resolve(prefix + i);
            if (!Files.exists(candidate))
                return candidate;
            i++;
        }
    }

    // 主方法：Java 21兼容，UI线程调度
    public static void main(String[] args) {
        System.setProperty("sun.java2d.uiScale", "1.0");
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new App().createAndShowGUI();
        });
    }

    // 带占位提示的输入框（原有逻辑保留）
    static class JTextFieldHint extends JTextField {
        private String hintText;

        public JTextFieldHint(int columns) {
            super(columns);
        }

        public void setHintText(String hintText) {
            this.hintText = hintText;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && hintText != null && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(Color.GRAY);
                g2.setFont(getFont().deriveFont(Font.ITALIC));
                FontMetrics fm = g2.getFontMetrics();
                int y = getHeight() - fm.getDescent() - (getHeight() - fm.getHeight()) / 2;
                g2.drawString(hintText, 5, y);
            }
        }
    }
}