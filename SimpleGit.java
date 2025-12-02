import javax.swing.*;
import javax.swing.border.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.text.SimpleDateFormat;

public class SimpleGit extends JFrame {
    
    private JTextField repoPathField;
    private JTextArea outputArea;
    private JTextArea commitMessageArea;
    private JList<String> changedFilesList;
    private DefaultListModel<String> changedFilesModel;
    private JList<String> stagedFilesList;
    private DefaultListModel<String> stagedFilesModel;
    private JLabel branchLabel;
    private JLabel statusLabel;
    private JComboBox<String> branchCombo;
    private JProgressBar progressBar;
    private File currentRepo;
    
    private static final Color BG_DARK = new Color(30, 30, 30);
    private static final Color BG_MEDIUM = new Color(45, 45, 45);
    private static final Color BG_LIGHT = new Color(60, 60, 60);
    private static final Color TEXT_PRIMARY = new Color(220, 220, 220);
    private static final Color TEXT_SECONDARY = new Color(150, 150, 150);
    private static final Color ACCENT_BLUE = new Color(66, 135, 245);
    private static final Color ACCENT_GREEN = new Color(75, 181, 67);
    private static final Color ACCENT_RED = new Color(220, 80, 80);
    private static final Color ACCENT_ORANGE = new Color(227, 160, 55);
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            new SimpleGit().setVisible(true);
        });
    }
    
    public SimpleGit() {
        setTitle("🐙 SimpleGit - GitHub Made Easy");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(900, 600));
        
        initComponents();
        
        // Set app icon
        try {
            setIconImage(createGitIcon());
        } catch (Exception e) {}
    }
    
    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBackground(BG_DARK);
        
        // Top toolbar
        mainPanel.add(createToolbar(), BorderLayout.NORTH);
        
        // Main content with split panes
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setBackground(BG_DARK);
        mainSplit.setBorder(null);
        mainSplit.setDividerLocation(350);
        
        // Left panel - File changes
        mainSplit.setLeftComponent(createFilePanel());
        
        // Right panel - Output and actions
        mainSplit.setRightComponent(createRightPanel());
        
        mainPanel.add(mainSplit, BorderLayout.CENTER);
        
        // Status bar
        mainPanel.add(createStatusBar(), BorderLayout.SOUTH);
        
        setContentPane(mainPanel);
    }
    
    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(BG_MEDIUM);
        toolbar.setBorder(new EmptyBorder(10, 15, 10, 15));
        
        // Repository path section
        JPanel repoPanel = new JPanel(new BorderLayout(10, 0));
        repoPanel.setOpaque(false);
        
        JLabel repoLabel = new JLabel("📁 Repository: ");
        repoLabel.setForeground(TEXT_PRIMARY);
        repoLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        repoPathField = new JTextField();
        repoPathField.setFont(new Font("Consolas", Font.PLAIN, 13));
        repoPathField.setBackground(BG_LIGHT);
        repoPathField.setForeground(TEXT_PRIMARY);
        repoPathField.setCaretColor(TEXT_PRIMARY);
        repoPathField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BG_LIGHT),
            new EmptyBorder(8, 10, 8, 10)
        ));
        
        JButton browseBtn = createStyledButton("Browse", BG_LIGHT);
        browseBtn.addActionListener(e -> browseRepository());
        
        JButton openBtn = createStyledButton("Open", ACCENT_BLUE);
        openBtn.addActionListener(e -> openRepository());
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(browseBtn);
        btnPanel.add(openBtn);
        
        repoPanel.add(repoLabel, BorderLayout.WEST);
        repoPanel.add(repoPathField, BorderLayout.CENTER);
        repoPanel.add(btnPanel, BorderLayout.EAST);
        
        // Quick actions
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionsPanel.setOpaque(false);
        
        branchLabel = new JLabel("⎇ Branch: --");
        branchLabel.setForeground(ACCENT_GREEN);
        branchLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        branchCombo = new JComboBox<>();
        branchCombo.setBackground(BG_LIGHT);
        branchCombo.setForeground(TEXT_PRIMARY);
        branchCombo.setPreferredSize(new Dimension(150, 30));
        branchCombo.addActionListener(e -> {
            if (branchCombo.getSelectedItem() != null && currentRepo != null) {
                // Don't auto-switch, just update label
            }
        });
        
        JButton refreshBtn = createStyledButton("🔄 Refresh", BG_LIGHT);
        refreshBtn.addActionListener(e -> refreshStatus());
        
        actionsPanel.add(branchLabel);
        actionsPanel.add(branchCombo);
        actionsPanel.add(refreshBtn);
        
        toolbar.add(repoPanel, BorderLayout.CENTER);
        toolbar.add(actionsPanel, BorderLayout.EAST);
        
        return toolbar;
    }
    
    private JPanel createFilePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(10, 10, 10, 5));
        
        // Staged files
        JPanel stagedPanel = new JPanel(new BorderLayout());
        stagedPanel.setBackground(BG_MEDIUM);
        stagedPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BG_LIGHT),
            new EmptyBorder(10, 10, 10, 10)
        ));
        
        JLabel stagedLabel = new JLabel("✓ Staged Changes");
        stagedLabel.setForeground(ACCENT_GREEN);
        stagedLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        stagedFilesModel = new DefaultListModel<>();
        stagedFilesList = new JList<>(stagedFilesModel);
        stagedFilesList.setBackground(BG_DARK);
        stagedFilesList.setForeground(TEXT_PRIMARY);
        stagedFilesList.setSelectionBackground(ACCENT_BLUE);
        stagedFilesList.setFont(new Font("Consolas", Font.PLAIN, 12));
        stagedFilesList.setCellRenderer(new FileListRenderer());
        
        JScrollPane stagedScroll = new JScrollPane(stagedFilesList);
        stagedScroll.setBorder(null);
        stagedScroll.setPreferredSize(new Dimension(300, 150));
        
        JButton unstageBtn = createStyledButton("− Unstage", ACCENT_ORANGE);
        unstageBtn.addActionListener(e -> unstageSelected());
        
        JPanel stagedBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        stagedBtnPanel.setOpaque(false);
        stagedBtnPanel.add(unstageBtn);
        
        stagedPanel.add(stagedLabel, BorderLayout.NORTH);
        stagedPanel.add(stagedScroll, BorderLayout.CENTER);
        stagedPanel.add(stagedBtnPanel, BorderLayout.SOUTH);
        
        // Changed files
        JPanel changedPanel = new JPanel(new BorderLayout());
        changedPanel.setBackground(BG_MEDIUM);
        changedPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BG_LIGHT),
            new EmptyBorder(10, 10, 10, 10)
        ));
        
        JLabel changedLabel = new JLabel("○ Changed Files");
        changedLabel.setForeground(ACCENT_ORANGE);
        changedLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        changedFilesModel = new DefaultListModel<>();
        changedFilesList = new JList<>(changedFilesModel);
        changedFilesList.setBackground(BG_DARK);
        changedFilesList.setForeground(TEXT_PRIMARY);
        changedFilesList.setSelectionBackground(ACCENT_BLUE);
        changedFilesList.setFont(new Font("Consolas", Font.PLAIN, 12));
        changedFilesList.setCellRenderer(new FileListRenderer());
        
        JScrollPane changedScroll = new JScrollPane(changedFilesList);
        changedScroll.setBorder(null);
        
        JPanel changeBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        changeBtnPanel.setOpaque(false);
        
        JButton stageBtn = createStyledButton("+ Stage", ACCENT_GREEN);
        stageBtn.addActionListener(e -> stageSelected());
        
        JButton stageAllBtn = createStyledButton("++ Stage All", ACCENT_GREEN);
        stageAllBtn.addActionListener(e -> stageAll());
        
        changeBtnPanel.add(stageBtn);
        changeBtnPanel.add(stageAllBtn);
        
        changedPanel.add(changedLabel, BorderLayout.NORTH);
        changedPanel.add(changedScroll, BorderLayout.CENTER);
        changedPanel.add(changeBtnPanel, BorderLayout.SOUTH);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(stagedPanel);
        splitPane.setBottomComponent(changedPanel);
        splitPane.setDividerLocation(200);
        splitPane.setBackground(BG_DARK);
        splitPane.setBorder(null);
        
        panel.add(splitPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(10, 5, 10, 10));
        
        JPanel actionsPanel = new JPanel(new GridLayout(2, 4, 10, 10));
        actionsPanel.setBackground(BG_MEDIUM);
        actionsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BG_LIGHT),
            new EmptyBorder(15, 15, 15, 15)
        ));
        
        // Row 1 - Basic operations
        JButton cloneBtn = createActionButton("📥 Clone", "Clone a repository");
        cloneBtn.addActionListener(e -> cloneRepository());
        
        JButton initBtn = createActionButton("🆕 Init", "Initialize new repo");
        initBtn.addActionListener(e -> initRepository());
        
        JButton pullBtn = createActionButton("⬇️ Pull", "Pull from remote");
        pullBtn.addActionListener(e -> pull());
        
        JButton pushBtn = createActionButton("⬆️ Push", "Push to remote");
        pushBtn.addActionListener(e -> push());
        
        // Row 2 - Branch operations
        JButton newBranchBtn = createActionButton("🌿 New Branch", "Create new branch");
        newBranchBtn.addActionListener(e -> createBranch());
        
        JButton switchBtn = createActionButton("⎇ Switch", "Switch branch");
        switchBtn.addActionListener(e -> switchBranch());
        
        JButton mergeBtn = createActionButton("🔀 Merge", "Merge branches");
        mergeBtn.addActionListener(e -> mergeBranch());
        
        JButton historyBtn = createActionButton("📜 History", "View commit log");
        historyBtn.addActionListener(e -> viewHistory());
        
        actionsPanel.add(cloneBtn);
        actionsPanel.add(initBtn);
        actionsPanel.add(pullBtn);
        actionsPanel.add(pushBtn);
        actionsPanel.add(newBranchBtn);
        actionsPanel.add(switchBtn);
        actionsPanel.add(mergeBtn);
        actionsPanel.add(historyBtn);
        
        // Middle - Commit section
        JPanel commitPanel = new JPanel(new BorderLayout(10, 10));
        commitPanel.setBackground(BG_MEDIUM);
        commitPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BG_LIGHT),
            new EmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel commitLabel = new JLabel("📝 Commit Message");
        commitLabel.setForeground(TEXT_PRIMARY);
        commitLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        commitMessageArea = new JTextArea(3, 40);
        commitMessageArea.setBackground(BG_DARK);
        commitMessageArea.setForeground(TEXT_PRIMARY);
        commitMessageArea.setCaretColor(TEXT_PRIMARY);
        commitMessageArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        commitMessageArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        commitMessageArea.setLineWrap(true);
        commitMessageArea.setWrapStyleWord(true);
        
        JScrollPane commitScroll = new JScrollPane(commitMessageArea);
        commitScroll.setBorder(BorderFactory.createLineBorder(BG_LIGHT));
        
        JPanel commitBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        commitBtnPanel.setOpaque(false);
        
        JButton commitBtn = createStyledButton("✓ Commit", ACCENT_GREEN);
        commitBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        commitBtn.addActionListener(e -> commit());
        
        JButton commitPushBtn = createStyledButton("✓ Commit & Push", ACCENT_BLUE);
        commitPushBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        commitPushBtn.addActionListener(e -> commitAndPush());
        
        commitBtnPanel.add(commitBtn);
        commitBtnPanel.add(commitPushBtn);
        
        commitPanel.add(commitLabel, BorderLayout.NORTH);
        commitPanel.add(commitScroll, BorderLayout.CENTER);
        commitPanel.add(commitBtnPanel, BorderLayout.SOUTH);
        
        // Bottom - Output console
        JPanel consolePanel = new JPanel(new BorderLayout());
        consolePanel.setBackground(BG_MEDIUM);
        consolePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BG_LIGHT),
            new EmptyBorder(10, 10, 10, 10)
        ));
        
        JPanel consoleHeader = new JPanel(new BorderLayout());
        consoleHeader.setOpaque(false);
        
        JLabel consoleLabel = new JLabel("💻 Console Output");
        consoleLabel.setForeground(TEXT_PRIMARY);
        consoleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        JButton clearBtn = createStyledButton("Clear", BG_LIGHT);
        clearBtn.addActionListener(e -> outputArea.setText(""));
        
        consoleHeader.add(consoleLabel, BorderLayout.WEST);
        consoleHeader.add(clearBtn, BorderLayout.EAST);
        
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setBackground(new Color(20, 20, 20));
        outputArea.setForeground(ACCENT_GREEN);
        outputArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        outputArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBorder(BorderFactory.createLineBorder(BG_LIGHT));
        
        consolePanel.add(consoleHeader, BorderLayout.NORTH);
        consolePanel.add(outputScroll, BorderLayout.CENTER);
        
        // Layout
        JPanel topSection = new JPanel(new BorderLayout(0, 10));
        topSection.setOpaque(false);
        topSection.add(actionsPanel, BorderLayout.NORTH);
        topSection.add(commitPanel, BorderLayout.CENTER);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent(topSection);
        splitPane.setBottomComponent(consolePanel);
        splitPane.setDividerLocation(280);
        splitPane.setBackground(BG_DARK);
        splitPane.setBorder(null);
        
        panel.add(splitPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(BG_MEDIUM);
        statusBar.setBorder(new EmptyBorder(8, 15, 8, 15));
        
        statusLabel = new JLabel("Ready - Open a repository to begin");
        statusLabel.setForeground(TEXT_SECONDARY);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(150, 15));
        progressBar.setVisible(false);
        
        JLabel versionLabel = new JLabel("SimpleGit v1.0");
        versionLabel.setForeground(TEXT_SECONDARY);
        versionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        
        statusBar.add(statusLabel, BorderLayout.WEST);
        statusBar.add(progressBar, BorderLayout.CENTER);
        statusBar.add(versionLabel, BorderLayout.EAST);
        
        return statusBar;
    }
    
    private JButton createStyledButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setBackground(bgColor);
        btn.setForeground(TEXT_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 15, 8, 15));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(bgColor.brighter());
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(bgColor);
            }
        });
        
        return btn;
    }
    
    private JButton createActionButton(String text, String tooltip) {
        JButton btn = new JButton(text);
        btn.setBackground(BG_LIGHT);
        btn.setForeground(TEXT_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setToolTipText(tooltip);
        btn.setBorder(new EmptyBorder(15, 10, 15, 10));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(ACCENT_BLUE);
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(BG_LIGHT);
            }
        });
        
        return btn;
    }
    
    // ==================== GIT OPERATIONS ====================
    
    private void browseRepository() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Git Repository");
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            repoPathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }
    
    private void openRepository() {
        String path = repoPathField.getText().trim();
        if (path.isEmpty()) {
            showError("Please enter a repository path");
            return;
        }
        
        File repo = new File(path);
        File gitDir = new File(repo, ".git");
        
        if (!gitDir.exists()) {
            showError("Not a Git repository: " + path);
            return;
        }
        
        currentRepo = repo;
        setStatus("Opened repository: " + repo.getName());
        log("📂 Opened repository: " + path);
        refreshStatus();
    }
    
    private void refreshStatus() {
        if (currentRepo == null) {
            showError("No repository open");
            return;
        }
        
        runAsync(() -> {
            // Get current branch
            String branch = runGitCommand("rev-parse", "--abbrev-ref", "HEAD");
            SwingUtilities.invokeLater(() -> {
                branchLabel.setText("⎇ Branch: " + branch.trim());
            });
            
            // Get all branches
            String branches = runGitCommand("branch", "-a");
            SwingUtilities.invokeLater(() -> {
                branchCombo.removeAllItems();
                for (String b : branches.split("\n")) {
                    b = b.trim().replace("* ", "");
                    if (!b.isEmpty() && !b.contains("->")) {
                        branchCombo.addItem(b);
                    }
                }
            });
            
            // Get status
            String status = runGitCommand("status", "--porcelain");
            
            SwingUtilities.invokeLater(() -> {
                stagedFilesModel.clear();
                changedFilesModel.clear();
                
                for (String line : status.split("\n")) {
                    if (line.length() < 3) continue;
                    
                    char staged = line.charAt(0);
                    char unstaged = line.charAt(1);
                    String file = line.substring(3);
                    
                    if (staged != ' ' && staged != '?') {
                        stagedFilesModel.addElement(staged + " " + file);
                    }
                    if (unstaged != ' ' || staged == '?') {
                        String prefix = staged == '?' ? "?" : String.valueOf(unstaged);
                        changedFilesModel.addElement(prefix + " " + file);
                    }
                }
                
                setStatus("Repository refreshed - " + 
                    stagedFilesModel.size() + " staged, " + 
                    changedFilesModel.size() + " changed");
            });
        });
    }
    
    private void stageSelected() {
        if (currentRepo == null) return;
        
        List<String> selected = changedFilesList.getSelectedValuesList();
        if (selected.isEmpty()) {
            showError("Select files to stage");
            return;
        }
        
        for (String item : selected) {
            String file = item.substring(2); // Remove status prefix
            runGitCommand("add", file);
            log("➕ Staged: " + file);
        }
        
        refreshStatus();
    }
    
    private void stageAll() {
        if (currentRepo == null) return;
        
        runGitCommand("add", "-A");
        log("➕ Staged all changes");
        refreshStatus();
    }
    
    private void unstageSelected() {
        if (currentRepo == null) return;
        
        List<String> selected = stagedFilesList.getSelectedValuesList();
        if (selected.isEmpty()) {
            showError("Select files to unstage");
            return;
        }
        
        for (String item : selected) {
            String file = item.substring(2);
            runGitCommand("reset", "HEAD", file);
            log("➖ Unstaged: " + file);
        }
        
        refreshStatus();
    }
    
    private void commit() {
        if (currentRepo == null) return;
        
        String message = commitMessageArea.getText().trim();
        if (message.isEmpty()) {
            showError("Please enter a commit message");
            return;
        }
        
        if (stagedFilesModel.isEmpty()) {
            showError("No files staged for commit");
            return;
        }
        
        runAsync(() -> {
            String result = runGitCommand("commit", "-m", message);
            log("✓ Committed: " + message);
            log(result);
            
            SwingUtilities.invokeLater(() -> {
                commitMessageArea.setText("");
                refreshStatus();
            });
        });
    }
    
    private void commitAndPush() {
        if (currentRepo == null) return;
        
        String message = commitMessageArea.getText().trim();
        if (message.isEmpty()) {
            showError("Please enter a commit message");
            return;
        }
        
        runAsync(() -> {
            // Commit
            String commitResult = runGitCommand("commit", "-m", message);
            log("✓ Committed: " + message);
            
            // Push
            setStatus("Pushing to remote...");
            String pushResult = runGitCommand("push");
            log("⬆️ Pushed to remote");
            log(pushResult);
            
            SwingUtilities.invokeLater(() -> {
                commitMessageArea.setText("");
                refreshStatus();
                setStatus("Committed and pushed successfully");
            });
        });
    }
    
    private void pull() {
        if (currentRepo == null) return;
        
        runAsync(() -> {
            setStatus("Pulling from remote...");
            String result = runGitCommand("pull");
            log("⬇️ Pull result:");
            log(result);
            
            SwingUtilities.invokeLater(() -> {
                refreshStatus();
                setStatus("Pull completed");
            });
        });
    }
    
    private void push() {
        if (currentRepo == null) return;
        
        runAsync(() -> {
            setStatus("Pushing to remote...");
            String result = runGitCommand("push");
            log("⬆️ Push result:");
            log(result);
            
            SwingUtilities.invokeLater(() -> {
                setStatus("Push completed");
            });
        });
    }
    
    private void cloneRepository() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 5, 5));
        
        JTextField urlField = new JTextField(40);
        JTextField destField = new JTextField(40);
        JButton browseBtn = new JButton("Browse...");
        
        JPanel destPanel = new JPanel(new BorderLayout(5, 0));
        destPanel.add(destField, BorderLayout.CENTER);
        destPanel.add(browseBtn, BorderLayout.EAST);
        
        browseBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                destField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        
        panel.add(new JLabel("Repository URL:"));
        panel.add(urlField);
        panel.add(new JLabel("Destination folder:"));
        panel.add(destPanel);
        
        int result = JOptionPane.showConfirmDialog(this, panel, 
            "Clone Repository", JOptionPane.OK_CANCEL_OPTION);
        
        if (result == JOptionPane.OK_OPTION) {
            String url = urlField.getText().trim();
            String dest = destField.getText().trim();
            
            if (url.isEmpty() || dest.isEmpty()) {
                showError("Please fill in all fields");
                return;
            }
            
            runAsync(() -> {
                setStatus("Cloning repository...");
                try {
                    ProcessBuilder pb = new ProcessBuilder("git", "clone", url, dest);
                    pb.redirectErrorStream(true);
                    Process p = pb.start();
                    
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(p.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log(line);
                    }
                    
                    p.waitFor();
                    
                    SwingUtilities.invokeLater(() -> {
                        repoPathField.setText(dest);
                        openRepository();
                        setStatus("Clone completed");
                    });
                } catch (Exception e) {
                    log("❌ Clone failed: " + e.getMessage());
                }
            });
        }
    }
    
    private void initRepository() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select folder for new repository");
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File folder = chooser.getSelectedFile();
            
            try {
                ProcessBuilder pb = new ProcessBuilder("git", "init");
                pb.directory(folder);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    log(line);
                }
                
                p.waitFor();
                
                repoPathField.setText(folder.getAbsolutePath());
                openRepository();
                log("🆕 Initialized new repository: " + folder.getName());
                
            } catch (Exception e) {
                showError("Failed to initialize: " + e.getMessage());
            }
        }
    }
    
    private void createBranch() {
        if (currentRepo == null) return;
        
        String name = JOptionPane.showInputDialog(this, 
            "Enter new branch name:", "Create Branch", JOptionPane.PLAIN_MESSAGE);
        
        if (name != null && !name.trim().isEmpty()) {
            String result = runGitCommand("checkout", "-b", name.trim());
            log("🌿 Created and switched to branch: " + name);
            log(result);
            refreshStatus();
        }
    }
    
    private void switchBranch() {
        if (currentRepo == null) return;
        
        String selected = (String) branchCombo.getSelectedItem();
        if (selected == null) return;
        
        // Clean up remote branch names
        if (selected.startsWith("remotes/origin/")) {
            selected = selected.replace("remotes/origin/", "");
        }
        
        String result = runGitCommand("checkout", selected);
        log("⎇ Switched to branch: " + selected);
        log(result);
        refreshStatus();
    }
    
    private void mergeBranch() {
        if (currentRepo == null) return;
        
        String[] branches = new String[branchCombo.getItemCount()];
        for (int i = 0; i < branches.length; i++) {
            branches[i] = branchCombo.getItemAt(i);
        }
        
        String selected = (String) JOptionPane.showInputDialog(this,
            "Select branch to merge into current:", "Merge Branch",
            JOptionPane.PLAIN_MESSAGE, null, branches, branches[0]);
        
        if (selected != null) {
            String result = runGitCommand("merge", selected);
            log("🔀 Merged branch: " + selected);
            log(result);
            refreshStatus();
        }
    }
    
    private void viewHistory() {
        if (currentRepo == null) return;
        
        runAsync(() -> {
            String result = runGitCommand("log", "--oneline", "-20");
            log("\n📜 Recent commits:");
            log("─".repeat(50));
            log(result);
            log("─".repeat(50));
        });
    }
    
    // ==================== HELPER METHODS ====================
    
    private String runGitCommand(String... args) {
        try {
            List<String> command = new ArrayList<>();
            command.add("git");
            command.addAll(Arrays.asList(args));
            
            ProcessBuilder pb = new ProcessBuilder(command);
            if (currentRepo != null) {
                pb.directory(currentRepo);
            }
            pb.redirectErrorStream(true);
            
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream()));
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            p.waitFor();
            return output.toString().trim();
            
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    private void runAsync(Runnable task) {
        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);
        
        new Thread(() -> {
            try {
                task.run();
            } finally {
                SwingUtilities.invokeLater(() -> {
                    progressBar.setVisible(false);
                    progressBar.setIndeterminate(false);
                });
            }
        }).start();
    }
    
    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            outputArea.append("[" + timestamp + "] " + message + "\n");
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
        });
    }
    
    private void setStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(message);
        });
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private Image createGitIcon() {
        int size = 64;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
            size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(ACCENT_ORANGE);
        g.fillOval(5, 5, size - 10, size - 10);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.drawString("G", 20, 45);
        g.dispose();
        return img;
    }
    
    // Custom cell renderer for file lists
    class FileListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, 
                int index, boolean isSelected, boolean cellHasFocus) {
            
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            String text = value.toString();
            if (text.length() > 0) {
                char status = text.charAt(0);
                switch (status) {
                    case 'M': setForeground(ACCENT_ORANGE); break;  // Modified
                    case 'A': setForeground(ACCENT_GREEN); break;   // Added
                    case 'D': setForeground(ACCENT_RED); break;     // Deleted
                    case '?': setForeground(ACCENT_BLUE); break;    // Untracked
                    case 'R': setForeground(Color.CYAN); break;     // Renamed
                    default: setForeground(TEXT_PRIMARY);
                }
            }
            
            if (isSelected) {
                setBackground(ACCENT_BLUE);
                setForeground(Color.WHITE);
            } else {
                setBackground(BG_DARK);
            }
            
            setBorder(new EmptyBorder(5, 10, 5, 10));
            return this;
        }
    }
}