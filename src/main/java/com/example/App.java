package com.example;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.JComponent;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JSplitPane;
import javax.swing.border.TitledBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumnModel;
import javax.swing.AbstractCellEditor;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.BorderFactory;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class App extends JFrame {
    // ===================== 语言配置核心 =====================
    private boolean isChinese = true; // 默认中文
    // 中文TXT路径
    private final String TXT_PATH_CN = "cn_same_name_method _partial.txt";
    // 英文TXT路径
    private final String TXT_PATH_EN = "cn_same_name_method _partial_EN.txt";
    // 界面文本资源
    private final Map<String, String> TEXT_CN = new HashMap<>();
    private final Map<String, String> TEXT_EN = new HashMap<>();
    // 操作列表多语言配置
    private final List<String> MOVE_OPS_CN = Arrays.asList("奔跑", "跳跃", "滑翔", "滑行", "悬挂", "蹲下");
    private final List<String> MOVE_OPS_EN = Arrays.asList("Run", "Jump", "Glide", "Slide", "Hang", "Crouch");
    private final List<String> BATTLE_OPS_CN = Arrays.asList("格挡", "闪避", "轻攻击", "重攻击", "搏击", "远程攻击", "软锁", "硬锁");
    private final List<String> BATTLE_OPS_EN = Arrays.asList("Guard", "Evade", "Normal attack", "HardAttack",
            "KickAttack", "bow and arrow", "Toggle LockOn", "Toggle Hard LockOn");
    private final List<String> SKILL_OPS_CN = Arrays.asList("劲法", "法则之力");
    private final List<String> SKILL_OPS_EN = Arrays.asList("Jin Fa(劲法)", "The Power of Law");

    // ===================== 原有常量配置 =====================
    private static final String DETECT_START_FLAG = "<!--程序开始检测标志-->";
    private static final int CORNER_RADIUS = 12;
    private static final Map<String, String> GAMEPAD_BUTTONS = new LinkedHashMap<>();
    static {
        GAMEPAD_BUTTONS.put("A", "buttonA");
        GAMEPAD_BUTTONS.put("B", "buttonB");
        GAMEPAD_BUTTONS.put("X", "buttonX");
        GAMEPAD_BUTTONS.put("Y", "buttonY");
        GAMEPAD_BUTTONS.put("LB", "buttonLB");
        GAMEPAD_BUTTONS.put("RB", "buttonRB");
        GAMEPAD_BUTTONS.put("LT", "buttonLT");
        GAMEPAD_BUTTONS.put("RT", "buttonRT");
        GAMEPAD_BUTTONS.put("LS", "buttonLS");
        GAMEPAD_BUTTONS.put("RS", "buttonRS");
        GAMEPAD_BUTTONS.put("padD", "padD");
        GAMEPAD_BUTTONS.put("LB+A", "buttonLB buttonA");
        GAMEPAD_BUTTONS.put("LB+B", "buttonLB buttonB");
        GAMEPAD_BUTTONS.put("LB+X", "buttonLB buttonX");
        GAMEPAD_BUTTONS.put("LB+Y", "buttonLB buttonY");
    }

    private static final Color BG_MAIN = new Color(30, 30, 46);
    private static final Color BG_PANEL = new Color(37, 37, 56);
    private static final Color BG_TABLE = new Color(45, 45, 66);
    private static final Color BG_SELECTED = new Color(22, 93, 255);
    private static final Color BG_BUTTON_NORMAL = new Color(54, 54, 80);
    private static final Color BG_BUTTON_HOVER = new Color(64, 64, 96);
    private static final Color TEXT_WHITE = new Color(255, 255, 255);
    private static final Color TEXT_GRAY = new Color(180, 180, 180);
    private static final Color BORDER_COLOR = new Color(60, 60, 85);

    // ===================== 界面组件全局引用（用于切换语言刷新）=====================
    private RoundedButton loadBtn, saveBtn, backupBtn, langBtn;
    private RoundedList<String> moveList, battleList, skillList;
    private DefaultTableModel tableModel;
    private JTable table;
    private TitledBorder leftPanelBorder, moveListBorder, battleListBorder, skillListBorder;
    private JSplitPane mainSplitPane;
    private RoundedPanel rightPanel;

    // ===================== 原有成员变量 =====================
    private Map<String, List<OpConfig>> opConfigMap = new HashMap<>();
    private List<ButtonMapping> buttonList = new ArrayList<>();
    private List<String> xmlLines = new ArrayList<>();
    private Path xmlPath;
    private Path currentTxtPath;

    // ===================== 内部类 =====================
    static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color bgColor;

        public RoundedPanel(int radius, Color bgColor) {
            this.radius = radius;
            this.bgColor = bgColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
        }
    }

    static class RoundedList<T> extends JList<T> {
        private final int radius;

        public RoundedList(T[] data, int radius) {
            super(data);
            this.radius = radius;
            setOpaque(false);
            setBorder(new EmptyBorder(8, 12, 8, 12));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    static class RoundedScrollPane extends JScrollPane {
        private final int radius;

        public RoundedScrollPane(Component view, int radius, Color bgColor) {
            super(view);
            this.radius = radius;
            setOpaque(false);
            getViewport().setOpaque(false);
            setBorder(new EmptyBorder(0, 0, 0, 0));
            setViewportBorder(null);
            getVerticalScrollBar().setUnitIncrement(16);
            getVerticalScrollBar().setBackground(BG_MAIN);
            getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getViewport().getView().getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
        }
    }

    public static class OpConfig {
        public String displayName;
        public String sourceName;
        public String method;
        public String time;
        public String rollValue;
        public String releaseTime;

        public OpConfig(String dn, String sn, String m, String t, String rv, String rt) {
            displayName = dn;
            sourceName = sn;
            method = m;
            time = t;
            rollValue = rv;
            releaseTime = rt;
        }
    }

    public static class ButtonMapping {
        public String displayName;
        public String xmlKey;
        public Set<String> ops = new LinkedHashSet<>();

        public ButtonMapping(String dn, String xk) {
            displayName = dn;
            xmlKey = xk;
        }
    }

    static class RoundedButton extends JButton {
        private final int radius;

        public RoundedButton(String text, int radius) {
            super(text);
            this.radius = radius;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(TEXT_WHITE);
            setFont(new Font("微软雅黑", Font.PLAIN, 14));
            setBackground(BG_BUTTON_NORMAL);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    setBackground(BG_BUTTON_HOVER);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setBackground(BG_BUTTON_NORMAL);
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    setBackground(BG_SELECTED);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    setBackground(BG_BUTTON_HOVER);
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    // ===================== 构造方法 =====================
    public App() {
        // 初始化文本资源
        initTextResource();
        // 初始化当前TXT路径
        currentTxtPath = Paths.get(System.getProperty("user.dir"), TXT_PATH_CN);
        // XML路径
        xmlPath = Paths.get(System.getProperty("user.dir"), "Control_Remap", "files", "0012", "ui", "inputmap.xml");
        // 初始化按键列表
        for (Map.Entry<String, String> e : GAMEPAD_BUTTONS.entrySet()) {
            buttonList.add(new ButtonMapping(e.getKey(), e.getValue()));
        }
        // 窗口配置
        setTitle(getText("app_title"));
        setSize(1280, 920);
        setMinimumSize(new Dimension(1280, 920));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));
        getContentPane().setBackground(BG_MAIN);
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(12, 12, 12, 12));
    }

    // ===================== 文本资源初始化 =====================
    private void initTextResource() {
        // 中文文本
        TEXT_CN.put("app_title", "红色沙漠 手柄键位配置工具");
        TEXT_CN.put("btn_load", "读取配置文件");
        TEXT_CN.put("btn_save", "直接保存原文件");
        TEXT_CN.put("btn_backup", "创建配置文件副本");
        TEXT_CN.put("btn_lang_en", "English");
        TEXT_CN.put("btn_lang_cn", "中文");
        TEXT_CN.put("title_mapping", "手柄按键映射");
        TEXT_CN.put("title_move", "移动类");
        TEXT_CN.put("title_battle", "战斗类");
        TEXT_CN.put("title_skill", "技能类");
        TEXT_CN.put("tip_no_bind", "暂无绑定操作，点击右侧操作后点+号添加");
        TEXT_CN.put("btn_add", "+ 添加操作");
        TEXT_CN.put("tip_select_op_first", "请先在右侧选择要添加的操作！");
        TEXT_CN.put("tip_op_already_bind", "该按键已绑定此操作！");
        TEXT_CN.put("tip_load_first", "请先读取配置文件！");
        TEXT_CN.put("tip_no_detect_flag", "XML中未找到检测起始标志：" + DETECT_START_FLAG + "\n请先在XML中添加该标志！");
        TEXT_CN.put("tip_source_folder_missing", "源文件夹Control_Remap不存在！");
        TEXT_CN.put("error_txt_load", "TXT加载失败: ");
        TEXT_CN.put("error_xml_load", "XML加载失败: ");
        TEXT_CN.put("error_xml_save", "保存失败: ");
        TEXT_CN.put("error_backup", "副本创建失败：");

        // 英文文本
        TEXT_EN.put("app_title", "Red Desert Gamepad Key Config Tool");
        TEXT_EN.put("btn_load", "Load Config");
        TEXT_EN.put("btn_save", "Save Original File");
        TEXT_EN.put("btn_backup", "Create Config Backup");
        TEXT_EN.put("btn_lang_en", "English");
        TEXT_EN.put("btn_lang_cn", "中文");
        TEXT_EN.put("title_mapping", "Gamepad Key Mapping");
        TEXT_EN.put("title_move", "Movement");
        TEXT_EN.put("title_battle", "Combat");
        TEXT_EN.put("title_skill", "Skill");
        TEXT_EN.put("tip_no_bind", "No bound operations, select an operation on the right and click + to add");
        TEXT_EN.put("btn_add", "+ Add Operation");
        TEXT_EN.put("tip_select_op_first", "Please select an operation on the right first!");
        TEXT_EN.put("tip_op_already_bind", "This operation is already bound to this key!");
        TEXT_EN.put("tip_load_first", "Please load the config file first!");
        TEXT_EN.put("tip_no_detect_flag",
                "Detection flag not found in XML: " + DETECT_START_FLAG + "\nPlease add this flag to your XML first!");
        TEXT_EN.put("tip_source_folder_missing", "Source folder Control_Remap not found!");
        TEXT_EN.put("error_txt_load", "TXT load failed: ");
        TEXT_EN.put("error_xml_load", "XML load failed: ");
        TEXT_EN.put("error_xml_save", "Save failed: ");
        TEXT_EN.put("error_backup", "Backup create failed: ");
    }

    // 获取当前语言的文本
    private String getText(String key) {
        return isChinese ? TEXT_CN.getOrDefault(key, key) : TEXT_EN.getOrDefault(key, key);
    }

    // 获取当前语言的操作列表
    private List<String> getCurrentMoveOps() {
        return isChinese ? MOVE_OPS_CN : MOVE_OPS_EN;
    }

    private List<String> getCurrentBattleOps() {
        return isChinese ? BATTLE_OPS_CN : BATTLE_OPS_EN;
    }

    private List<String> getCurrentSkillOps() {
        return isChinese ? SKILL_OPS_CN : SKILL_OPS_EN;
    }

    // ===================== 核心功能方法 =====================
    private int getDetectStartIndex() {
        for (int i = 0; i < xmlLines.size(); i++) {
            if (xmlLines.get(i).trim().equals(DETECT_START_FLAG)) {
                System.out.println("找到检测起始标志，行号: " + (i + 1));
                return i + 1;
            }
        }
        return -1;
    }

    private void loadTxt() {
        opConfigMap.clear();
        System.out.println("========== 开始加载TXT ==========");
        System.out.println("当前加载文件: " + currentTxtPath.getFileName());
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(Files.newInputStream(currentTxtPath), StandardCharsets.UTF_8))) {
            String line;
            Pattern p = Pattern.compile("^(.+?)=(.+?)<(.+?)>(?:\\((.*?)\\))?(?:\\[(.*?)\\])?(?:\\{(.*?)\\})?$");
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("["))
                    continue;
                Matcher m = p.matcher(line);
                if (m.find()) {
                    String dn = m.group(1).trim();
                    String sn = m.group(2).trim();
                    String mt = m.group(3).trim();
                    String t = m.group(4) != null ? m.group(4).trim() : "";
                    String rv = m.group(5) != null ? m.group(5).trim() : "";
                    String rt = m.group(6) != null ? m.group(6).trim() : "";
                    OpConfig cfg = new OpConfig(dn, sn, mt, t, rv, rt);
                    opConfigMap.computeIfAbsent(dn, k -> new ArrayList<>()).add(cfg);
                    System.out.println("  加载: " + dn + " -> " + sn + " [" + mt + "]");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, getText("error_txt_load") + e.getMessage());
        }
        System.out.println("========== TXT加载完成，共" + opConfigMap.size() + "个操作 ==========");
    }

    private void loadXml() {
        System.out.println("========== 开始加载XML ==========");
        xmlLines.clear();
        for (ButtonMapping b : buttonList)
            b.ops.clear();
        try {
            if (Files.exists(xmlPath)) {
                Path bak = xmlPath.resolveSibling("inputmap.xml.bak");
                Files.copy(xmlPath, bak, StandardCopyOption.REPLACE_EXISTING);
            }
            xmlLines = Files.readAllLines(xmlPath, StandardCharsets.UTF_8);
            System.out.println("  XML读取成功，共" + xmlLines.size() + "行");

            int startIndex = getDetectStartIndex();
            if (startIndex == -1) {
                System.out.println("  警告：未找到检测起始标志，将解析整个XML");
                startIndex = 0;
            }

            String currentSource = "";
            Map<String, String> sourceToOp = new HashMap<>();
            for (List<OpConfig> list : opConfigMap.values()) {
                for (OpConfig cfg : list) {
                    sourceToOp.put(cfg.sourceName, cfg.displayName);
                }
            }

            Pattern inputP = Pattern.compile("<Input\\s+Name=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
            Pattern gamePadP = Pattern.compile("<GamePad\\s+Key=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
            Pattern inputEndP = Pattern.compile("</(Input)?>", Pattern.CASE_INSENSITIVE);

            boolean inTargetInput = false;
            for (int i = startIndex; i < xmlLines.size(); i++) {
                String line = xmlLines.get(i);
                String trim = line.trim();
                if (trim.startsWith("<!--"))
                    continue;

                Matcher m1 = inputP.matcher(trim);
                if (m1.find()) {
                    String sourceName = m1.group(1).trim();
                    if (sourceToOp.containsKey(sourceName)) {
                        currentSource = sourceName;
                        inTargetInput = true;
                    } else {
                        currentSource = "";
                        inTargetInput = false;
                    }
                    continue;
                }

                Matcher endMatcher = inputEndP.matcher(trim);
                if (endMatcher.find()) {
                    currentSource = "";
                    inTargetInput = false;
                    continue;
                }

                if (inTargetInput && !currentSource.isEmpty()) {
                    Matcher m2 = gamePadP.matcher(trim);
                    if (m2.find()) {
                        String key = m2.group(1).trim();
                        String opName = sourceToOp.get(currentSource);
                        if (opName != null) {
                            for (ButtonMapping b : buttonList) {
                                if (b.xmlKey.equalsIgnoreCase(key)) {
                                    b.ops.add(opName);
                                    System.out.println("  绑定: " + b.displayName + " -> " + opName + " (节点: "
                                            + currentSource + ")");
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, getText("error_xml_load") + e.getMessage());
        }
        System.out.println("========== XML加载完成 ==========");
        refreshTable();
    }

    private void saveXml() {
        System.out.println("========== 开始保存XML ==========");
        try {
            Files.write(xmlPath, xmlLines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("  XML保存成功！共" + xmlLines.size() + "行");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, getText("error_xml_save") + e.getMessage());
        }
    }

    private void copyDirectory(Path sourceDir, Path targetDir) throws IOException {
        if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
            throw new IOException("源文件夹不存在或不是目录: " + sourceDir);
        }
        Files.createDirectories(targetDir);
        try (Stream<Path> stream = Files.list(sourceDir)) {
            stream.forEach(sourcePath -> {
                try {
                    Path targetPath = targetDir.resolve(sourcePath.getFileName());
                    if (Files.isDirectory(sourcePath)) {
                        copyDirectory(sourcePath, targetPath);
                    } else {
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.COPY_ATTRIBUTES);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("复制文件失败: " + sourcePath, e);
                }
            });
        }
    }

    private void addOperation(ButtonMapping button, String opName) {
        System.out.println("========== 开始添加操作 ==========");
        System.out.println("  目标按键: " + button.displayName + " (XML: " + button.xmlKey + ")");
        System.out.println("  目标操作: " + opName);

        List<OpConfig> configs = opConfigMap.get(opName);
        if (configs == null || configs.isEmpty()) {
            System.out.println("  错误：未找到操作[" + opName + "]的配置！");
            return;
        }

        int startIndex = getDetectStartIndex();
        if (startIndex == -1) {
            JOptionPane.showMessageDialog(this, getText("tip_no_detect_flag"));
            return;
        }

        List<String> newXmlLines = new ArrayList<>(xmlLines);

        for (OpConfig cfg : configs) {
            System.out.println("  处理源码节点: " + cfg.sourceName);

            int inputStartLine = -1;
            int inputEndLine = -1;
            String indent = "\t\t";
            boolean keyExists = false;
            boolean isSelfClosed = false;

            Pattern inputStartPattern = Pattern.compile(
                    "^\\s*<Input\\s+Name=\"" + Pattern.quote(cfg.sourceName) + "\"(\\s+[^>]*)?(/?)>\\s*$",
                    Pattern.CASE_INSENSITIVE);
            Pattern keyMatchPattern = Pattern.compile(
                    "^\\s*<GamePad\\s+[^>]*Key=\"" + Pattern.quote(button.xmlKey) + "\"(\\s+[^>]*)?/>\\s*$",
                    Pattern.CASE_INSENSITIVE);
            Pattern inputEndPattern = Pattern.compile(
                    "^\\s*</(Input)?>\\s*$",
                    Pattern.CASE_INSENSITIVE);

            for (int i = startIndex; i < newXmlLines.size(); i++) {
                String line = newXmlLines.get(i);
                String trim = line.trim();
                if (trim.startsWith("<!--"))
                    continue;

                if (inputStartLine == -1) {
                    Matcher startMatcher = inputStartPattern.matcher(line);
                    if (startMatcher.find()) {
                        inputStartLine = i;
                        isSelfClosed = "/".equals(startMatcher.group(2));
                        System.out.println("    [1/5] 找到目标Input起始，行号: " + (i + 1) + " | 内容: " + trim);
                        System.out.println("    是否自闭合: " + isSelfClosed);
                    }
                    continue;
                }

                Matcher endMatcher = inputEndPattern.matcher(line);
                if (endMatcher.find()) {
                    inputEndLine = i;
                    indent = line.replaceAll("</.*?>", "");
                    System.out.println("    [2/5] 找到目标Input结束，行号: " + (i + 1) + " | 结束标签: " + trim);
                    break;
                }

                if (isSelfClosed && i == inputStartLine) {
                    inputEndLine = i;
                    System.out.println("    [2/5] 自闭合标签，结束行: " + (i + 1));
                    break;
                }

                Matcher keyMatcher = keyMatchPattern.matcher(line);
                if (keyMatcher.find()) {
                    keyExists = true;
                    System.out.println("    [3/5] 检测到已存在的按键行，行号: " + (i + 1) + " | 内容: " + trim);
                    break;
                }
            }

            if (inputStartLine == -1) {
                System.out.println("    [跳过] 未找到目标Input节点，跳过");
                continue;
            }
            if (keyExists) {
                System.out.println("    [跳过] 该按键已存在于此节点，跳过");
                continue;
            }
            if (inputEndLine == -1 && !isSelfClosed) {
                System.out.println("    [警告] 未找到目标Input的结束行，跳过");
                continue;
            }

            System.out.println("    [4/5] 准备插入按键");
            if (isSelfClosed) {
                System.out.println("      转换自闭合标签为可插入格式");
                String startLine = newXmlLines.get(inputStartLine);
                String newStartLine = startLine.replaceAll("/>\\s*$", ">");
                newXmlLines.set(inputStartLine, newStartLine);
                newXmlLines.add(inputStartLine + 1, indent + "</>");
                inputEndLine = inputStartLine + 1;
            }

            StringBuilder insertLine = new StringBuilder();
            insertLine.append(indent).append("<GamePad Key=\"").append(button.xmlKey).append("\" Method=\"")
                    .append(cfg.method).append("\"");
            if (!cfg.time.isEmpty())
                insertLine.append(" Time=\"").append(cfg.time).append("\"");
            if (!cfg.rollValue.isEmpty())
                insertLine.append(" RollTriggerValue=\"").append(cfg.rollValue).append("\"");
            if (!cfg.releaseTime.isEmpty())
                insertLine.append(" ReleaseTime=\"").append(cfg.releaseTime).append("\"");
            insertLine.append("/>");

            System.out.println("      插入行: " + insertLine);
            newXmlLines.add(inputStartLine + 1, insertLine.toString());
            System.out.println("    [5/5] 插入成功！");
        }

        xmlLines = newXmlLines;
        System.out.println("========== 操作添加完成！ ==========");
    }

    private void removeOperation(ButtonMapping button, String opName) {
        System.out.println("========== 开始删除操作 ==========");
        System.out.println("  目标按键: " + button.displayName + " (XML: " + button.xmlKey + ")");
        System.out.println("  目标操作: " + opName);

        List<OpConfig> configs = opConfigMap.get(opName);
        if (configs == null || configs.isEmpty()) {
            System.out.println("  错误：未找到操作[" + opName + "]的配置！");
            return;
        }

        int startIndex = getDetectStartIndex();
        if (startIndex == -1) {
            JOptionPane.showMessageDialog(this, getText("tip_no_detect_flag"));
            return;
        }

        List<String> newXmlLines = new ArrayList<>(xmlLines);

        for (OpConfig cfg : configs) {
            System.out.println("  处理源码节点: " + cfg.sourceName);

            int inputStartLine = -1;
            int inputEndLine = -1;
            boolean isSelfClosed = false;

            Pattern inputStartPattern = Pattern.compile(
                    "^\\s*<Input\\s+Name=\"" + Pattern.quote(cfg.sourceName) + "\"(\\s+[^>]*)?(/?)>\\s*$",
                    Pattern.CASE_INSENSITIVE);
            Pattern keyMatchPattern = Pattern.compile(
                    "^\\s*<GamePad\\s+[^>]*Key=\"" + Pattern.quote(button.xmlKey) + "\"(\\s+[^>]*)?/>\\s*$",
                    Pattern.CASE_INSENSITIVE);
            Pattern inputEndPattern = Pattern.compile(
                    "^\\s*</(Input)?>\\s*$",
                    Pattern.CASE_INSENSITIVE);

            for (int i = startIndex; i < newXmlLines.size(); i++) {
                String line = newXmlLines.get(i);
                String trim = line.trim();
                if (trim.startsWith("<!--"))
                    continue;

                if (inputStartLine == -1) {
                    Matcher startMatcher = inputStartPattern.matcher(line);
                    if (startMatcher.find()) {
                        inputStartLine = i;
                        isSelfClosed = "/".equals(startMatcher.group(2));
                        System.out.println("    [1/4] 找到目标Input起始，行号: " + (i + 1) + " | 内容: " + trim);
                        System.out.println("    是否自闭合: " + isSelfClosed);
                    }
                    continue;
                }

                Matcher endMatcher = inputEndPattern.matcher(line);
                if (endMatcher.find()) {
                    inputEndLine = i;
                    System.out.println("    [2/4] 找到目标Input结束，行号: " + (i + 1) + " | 结束标签: " + trim);
                    break;
                }

                if (isSelfClosed && i == inputStartLine) {
                    inputEndLine = i;
                    System.out.println("    [2/4] 自闭合标签，无内容可删除，跳过");
                    break;
                }
            }

            if (inputStartLine == -1) {
                System.out.println("    [跳过] 未找到目标Input节点，跳过");
                continue;
            }
            if (isSelfClosed) {
                System.out.println("    [跳过] 自闭合标签，无GamePad行可删除");
                continue;
            }
            if (inputEndLine == -1) {
                System.out.println("    [警告] 未找到目标Input的结束行，跳过");
                continue;
            }

            System.out.println("    [3/4] 开始扫描目标节点，范围: 行" + (inputStartLine + 1) + " ~ 行" + (inputEndLine + 1));
            List<String> tempLines = new ArrayList<>();
            int deleteCount = 0;

            for (int i = 0; i < newXmlLines.size(); i++) {
                String line = newXmlLines.get(i);
                String trim = line.trim();

                if (i < inputStartLine || i > inputEndLine) {
                    tempLines.add(line);
                    continue;
                }

                if (trim.startsWith("<!--")) {
                    tempLines.add(line);
                    continue;
                }

                Matcher keyMatcher = keyMatchPattern.matcher(line);
                if (keyMatcher.find()) {
                    System.out.println("    [删除成功] 行号: " + (i + 1) + " | 内容: " + trim);
                    deleteCount++;
                    continue;
                }

                tempLines.add(line);
            }

            newXmlLines = tempLines;
            System.out.println("    [4/4] 节点处理完成，共删除 " + deleteCount + " 行");
        }

        xmlLines = newXmlLines;
        System.out.println("========== 删除操作完成！ ==========");
    }

    // ===================== 语言切换核心方法 =====================
    private void switchLanguage() {
        // 切换语言标记
        isChinese = !isChinese;
        // 更新TXT路径
        currentTxtPath = Paths.get(System.getProperty("user.dir"), isChinese ? TXT_PATH_CN : TXT_PATH_EN);
        // 重新加载TXT和XML
        loadTxt();
        loadXml();
        // 刷新界面所有文字
        refreshUI();
        System.out.println("========== 语言切换完成，当前语言: " + (isChinese ? "中文" : "英文") + " ==========");
    }

    // 刷新界面所有组件文字
    private void refreshUI() {
        // 窗口标题
        setTitle(getText("app_title"));
        // 按钮文字
        loadBtn.setText(getText("btn_load"));
        saveBtn.setText(getText("btn_save"));
        backupBtn.setText(getText("btn_backup"));
        langBtn.setText(isChinese ? getText("btn_lang_en") : getText("btn_lang_cn"));
        // 边框标题
        leftPanelBorder.setTitle(getText("title_mapping"));
        moveListBorder.setTitle(getText("title_move"));
        battleListBorder.setTitle(getText("title_battle"));
        skillListBorder.setTitle(getText("title_skill"));
        // 操作列表刷新
        refreshOperationList();
        // 表格刷新
        refreshTable();
        // 重绘界面
        SwingUtilities.updateComponentTreeUI(this);
    }

    // 刷新操作列表
    private void refreshOperationList() {
        // 清除原有选中监听
        for (ListSelectionListener l : moveList.getListSelectionListeners()) {
            moveList.removeListSelectionListener(l);
        }
        for (ListSelectionListener l : battleList.getListSelectionListeners()) {
            battleList.removeListSelectionListener(l);
        }
        for (ListSelectionListener l : skillList.getListSelectionListeners()) {
            skillList.removeListSelectionListener(l);
        }
        // 更新列表数据
        moveList.setListData(getCurrentMoveOps().toArray(new String[0]));
        battleList.setListData(getCurrentBattleOps().toArray(new String[0]));
        skillList.setListData(getCurrentSkillOps().toArray(new String[0]));
        // 重新添加互斥选中监听
        ListSelectionListener mutualExclusionListener = e -> {
            if (e.getValueIsAdjusting())
                return;
            JList<?> source = (JList<?>) e.getSource();
            if (source.isSelectionEmpty())
                return;
            if (source == moveList) {
                battleList.clearSelection();
                skillList.clearSelection();
            } else if (source == battleList) {
                moveList.clearSelection();
                skillList.clearSelection();
            } else if (source == skillList) {
                moveList.clearSelection();
                battleList.clearSelection();
            }
        };
        moveList.addListSelectionListener(mutualExclusionListener);
        battleList.addListSelectionListener(mutualExclusionListener);
        skillList.addListSelectionListener(mutualExclusionListener);
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (ButtonMapping b : buttonList) {
            tableModel.addRow(new Object[] { b.displayName, b });
        }
        for (int i = 0; i < table.getRowCount(); i++) {
            int h = 40;
            Component c = table.prepareRenderer(table.getCellRenderer(i, 1), i, 1);
            h = Math.max(h, c.getPreferredSize().height + 10);
            table.setRowHeight(i, h);
        }
    }

    private String getSelectedOp() {
        if (!moveList.isSelectionEmpty())
            return moveList.getSelectedValue();
        if (!battleList.isSelectionEmpty())
            return battleList.getSelectedValue();
        if (!skillList.isSelectionEmpty())
            return skillList.getSelectedValue();
        return null;
    }

    // ===================== 渲染器和编辑器 =====================
    class OpCellRenderer extends RoundedPanel implements TableCellRenderer {
        public OpCellRenderer() {
            super(CORNER_RADIUS - 4, BG_TABLE);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(5, 5, 5, 5));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            removeAll();
            if (value instanceof ButtonMapping b) {
                if (b.ops.isEmpty()) {
                    JLabel l = new JLabel(getText("tip_no_bind"));
                    l.setForeground(TEXT_GRAY);
                    l.setFont(new Font("微软雅黑", Font.PLAIN, 13));
                    add(l);
                } else {
                    for (String op : b.ops) {
                        JPanel opRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
                        opRow.setOpaque(false);
                        opRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
                        RoundedButton delBtn = new RoundedButton("-", 4);
                        delBtn.setPreferredSize(new Dimension(25, 22));
                        delBtn.setFont(new Font("微软雅黑", Font.BOLD, 12));
                        opRow.add(delBtn);
                        JLabel opLabel = new JLabel(op);
                        opLabel.setForeground(TEXT_WHITE);
                        opLabel.setFont(new Font("微软雅黑", Font.PLAIN, 13));
                        opRow.add(opLabel);
                        add(opRow);
                    }
                }
                JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
                addRow.setOpaque(false);
                addRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                RoundedButton addBtn = new RoundedButton(getText("btn_add"), 4);
                addBtn.setPreferredSize(new Dimension(120, 26));
                addBtn.setFont(new Font("微软雅黑", Font.PLAIN, 12));
                addRow.add(addBtn);
                add(addRow);
            }
            return this;
        }
    }

    class OpCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final OpCellRenderer renderer = new OpCellRenderer();
        private ButtonMapping currentButton;

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
                int column) {
            currentButton = (ButtonMapping) value;
            renderer.getTableCellRendererComponent(table, value, isSelected, true, row, column);

            for (Component comp : renderer.getComponents()) {
                if (comp instanceof JPanel opRow) {
                    for (Component inner : opRow.getComponents()) {
                        if (inner instanceof RoundedButton btn) {
                            for (ActionListener al : btn.getActionListeners()) {
                                btn.removeActionListener(al);
                            }
                            if (btn.getText().equals("-")) {
                                JLabel opLabel = (JLabel) opRow.getComponent(1);
                                String opName = opLabel.getText();
                                btn.addActionListener(e -> {
                                    removeOperation(currentButton, opName);
                                    currentButton.ops.remove(opName);
                                    refreshTable();
                                    fireEditingStopped();
                                });
                            } else if (btn.getText().contains("+")) {
                                btn.addActionListener(e -> {
                                    String selOp = getSelectedOp();
                                    if (selOp == null) {
                                        JOptionPane.showMessageDialog(table, getText("tip_select_op_first"));
                                        fireEditingStopped();
                                        return;
                                    }
                                    if (currentButton.ops.contains(selOp)) {
                                        JOptionPane.showMessageDialog(table, getText("tip_op_already_bind"));
                                        fireEditingStopped();
                                        return;
                                    }
                                    addOperation(currentButton, selOp);
                                    currentButton.ops.add(selOp);
                                    refreshTable();
                                    fireEditingStopped();
                                });
                            }
                        }
                    }
                }
            }
            return renderer;
        }

        @Override
        public Object getCellEditorValue() {
            return currentButton;
        }
    }

    // ===================== UI创建 =====================
    private void createUI() {
        // 顶部按钮栏
        RoundedPanel topPanel = new RoundedPanel(CORNER_RADIUS, BG_PANEL);
        topPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));
        topPanel.setBorder(new EmptyBorder(8, 10, 8, 10));

        // 初始化按钮
        loadBtn = new RoundedButton(getText("btn_load"), CORNER_RADIUS - 4);
        saveBtn = new RoundedButton(getText("btn_save"), CORNER_RADIUS - 4);
        backupBtn = new RoundedButton(getText("btn_backup"), CORNER_RADIUS - 4);
        langBtn = new RoundedButton(getText("btn_lang_en"), CORNER_RADIUS - 4);
        saveBtn.setBackground(BG_SELECTED);
        langBtn.setBackground(new Color(80, 80, 120));

        for (RoundedButton btn : new RoundedButton[] { loadBtn, saveBtn, backupBtn, langBtn }) {
            btn.setPreferredSize(new Dimension(180, 40));
            topPanel.add(btn);
        }
        add(topPanel, BorderLayout.NORTH);

        // 主分割面板
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplitPane.setDividerLocation(800);
        mainSplitPane.setDividerSize(2);
        mainSplitPane.setBackground(BG_MAIN);
        mainSplitPane.setBorder(null);
        mainSplitPane.setResizeWeight(0.65);

        // 左侧映射面板
        RoundedPanel leftPanel = new RoundedPanel(CORNER_RADIUS, BG_PANEL);
        leftPanel.setLayout(new BorderLayout());
        leftPanelBorder = BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(),
                getText("title_mapping"),
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 14),
                TEXT_WHITE);
        leftPanel.setBorder(leftPanelBorder);
        ((TitledBorder) leftPanel.getBorder()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // 表格
        String[] columnNames = isChinese ? new String[] { "手柄按键", "映射操作列表" }
                : new String[] { "Gamepad Key", "Bound Operations" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 1 ? ButtonMapping.class : String.class;
            }
        };

        table = new JTable(tableModel);
        table.setBackground(BG_TABLE);
        table.setForeground(TEXT_WHITE);
        table.setGridColor(BORDER_COLOR);
        table.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        table.setSelectionBackground(BG_SELECTED);
        table.setSelectionForeground(TEXT_WHITE);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 5));
        table.setRowMargin(5);
        table.getTableHeader().setBackground(BG_PANEL);
        table.getTableHeader().setForeground(TEXT_WHITE);
        table.getTableHeader().setFont(new Font("微软雅黑", Font.BOLD, 14));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(false);
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(700);
        table.getColumnModel().getColumn(1).setCellRenderer(new OpCellRenderer());
        table.getColumnModel().getColumn(1).setCellEditor(new OpCellEditor());

        RoundedScrollPane tableScroll = new RoundedScrollPane(table, CORNER_RADIUS - 4, BG_TABLE);
        tableScroll.setBorder(new EmptyBorder(10, 10, 10, 10));
        leftPanel.add(tableScroll, BorderLayout.CENTER);
        mainSplitPane.setLeftComponent(leftPanel);

        // 右侧操作面板
        rightPanel = new RoundedPanel(CORNER_RADIUS, BG_PANEL);
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // 操作列表初始化
        moveList = createOperationList(getCurrentMoveOps(), getText("title_move"));
        battleList = createOperationList(getCurrentBattleOps(), getText("title_battle"));
        skillList = createOperationList(getCurrentSkillOps(), getText("title_skill"));

        // 保存边框引用，用于切换语言刷新
        moveListBorder = (TitledBorder) moveList.getBorder();
        battleListBorder = (TitledBorder) battleList.getBorder();
        skillListBorder = (TitledBorder) skillList.getBorder();

        // 互斥选中监听
        ListSelectionListener mutualExclusionListener = e -> {
            if (e.getValueIsAdjusting())
                return;
            JList<?> source = (JList<?>) e.getSource();
            if (source.isSelectionEmpty())
                return;
            if (source == moveList) {
                battleList.clearSelection();
                skillList.clearSelection();
            } else if (source == battleList) {
                moveList.clearSelection();
                skillList.clearSelection();
            } else if (source == skillList) {
                moveList.clearSelection();
                battleList.clearSelection();
            }
        };
        moveList.addListSelectionListener(mutualExclusionListener);
        battleList.addListSelectionListener(mutualExclusionListener);
        skillList.addListSelectionListener(mutualExclusionListener);

        rightPanel.add(moveList);
        rightPanel.add(Box.createVerticalStrut(20));
        rightPanel.add(battleList);
        rightPanel.add(Box.createVerticalStrut(20));
        rightPanel.add(skillList);
        rightPanel.add(Box.createVerticalGlue());

        RoundedScrollPane rightScroll = new RoundedScrollPane(rightPanel, CORNER_RADIUS, BG_PANEL);
        rightScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        mainSplitPane.setRightComponent(rightScroll);

        add(mainSplitPane, BorderLayout.CENTER);

        // 按钮事件绑定
        loadBtn.addActionListener(e -> {
            loadTxt();
            loadXml();
        });
        saveBtn.addActionListener(e -> {
            if (xmlLines.isEmpty())
                JOptionPane.showMessageDialog(this, getText("tip_load_first"));
            else
                saveXml();
        });
        backupBtn.addActionListener(e -> {
            if (xmlLines.isEmpty()) {
                JOptionPane.showMessageDialog(this, getText("tip_load_first"));
                return;
            }
            try {
                String root = System.getProperty("user.dir");
                Path sourceRoot = Paths.get(root, "Control_Remap");
                if (!Files.exists(sourceRoot) || !Files.isDirectory(sourceRoot)) {
                    JOptionPane.showMessageDialog(this, getText("tip_source_folder_missing"));
                    return;
                }
                Path myKeyBoardDir = Paths.get(root, "My_KeyBoard");
                if (!Files.exists(myKeyBoardDir)) {
                    Files.createDirectories(myKeyBoardDir);
                }
                int i = 1;
                Path targetDir;
                while (true) {
                    targetDir = myKeyBoardDir.resolve("Control_Remap_" + i);
                    if (!Files.exists(targetDir))
                        break;
                    i++;
                }
                copyDirectory(sourceRoot, targetDir);
                System.out.println("========== 配置副本创建完成 ==========");
                System.out.println("  副本路径: " + targetDir.toAbsolutePath());
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, getText("error_backup") + ex.getMessage());
            }
        });
        // 语言切换按钮事件
        langBtn.addActionListener(e -> switchLanguage());

        setVisible(true);
    }

    private RoundedList<String> createOperationList(List<String> operations, String title) {
        RoundedList<String> list = new RoundedList<>(operations.toArray(new String[0]), CORNER_RADIUS - 4);
        list.setBackground(BG_TABLE);
        list.setForeground(TEXT_WHITE);
        list.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        list.setFixedCellHeight(36);
        list.setSelectionBackground(BG_SELECTED);
        list.setSelectionForeground(TEXT_WHITE);
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(),
                title,
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 14),
                TEXT_WHITE);
        list.setBorder(border);
        ((TitledBorder) list.getBorder()).setBorder(new EmptyBorder(8, 8, 8, 8));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setMinimumSize(new Dimension(300, operations.size() * 36 + 50));
        list.setPreferredSize(new Dimension(300, operations.size() * 36 + 50));
        list.setMaximumSize(new Dimension(Integer.MAX_VALUE, operations.size() * 36 + 50));
        return list;
    }

    // ===================== 主方法 =====================
    public static void main(String[] args) {
        System.setProperty("sun.java2d.uiScale", "1.0");
        SwingUtilities.invokeLater(() -> new App().createUI());
    }
}