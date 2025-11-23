package com.gameengine.example;

import com.gameengine.core.GameEngine;
import com.gameengine.graphics.IRenderer;
import com.gameengine.input.InputManager;
import com.gameengine.scene.Scene;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 回放文件选择场景
 */
public class ReplayMenuScene extends Scene {
    private final GameEngine engine;
    private IRenderer renderer;
    private InputManager inputManager;
    private List<String> recordingFiles;
    private int selectedIndex;
    private boolean fileListLoaded;
    
    public ReplayMenuScene(GameEngine engine) {
        super("ReplayMenuScene");
        this.engine = engine;
        this.recordingFiles = new ArrayList<>();
        this.selectedIndex = 0;
        this.fileListLoaded = false;
    }
    
    @Override
    public void initialize() {
        super.initialize();
        this.renderer = engine.getRenderer();
        this.inputManager = InputManager.getInstance();
        
        // 加载记录文件列表
        loadRecordingFiles();
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        
        if (!fileListLoaded) {
            return;
        }
        
        // 处理键盘输入
        handleInput();
    }
    
    @Override
    public void render() {
        // 绘制背景
        renderer.drawRect(0, 0, renderer.getWidth(), renderer.getHeight(), 0.1f, 0.1f, 0.2f, 1.0f);
        
        // 绘制标题
        String title = "选择回放文件";
        float titleX = (renderer.getWidth() - 200) / 2;
        float titleY = 50;
        renderer.drawText(titleX, titleY, title, 1.0f, 1.0f, 1.0f, 1.0f);
        
        if (!fileListLoaded) {
            // 显示加载中
            String loadingText = "正在加载记录文件...";
            float loadingX = (renderer.getWidth() - 200) / 2;
            float loadingY = renderer.getHeight() / 2;
            renderer.drawText(loadingX, loadingY, loadingText, 1.0f, 1.0f, 1.0f, 1.0f);
            return;
        }
        
        if (recordingFiles.isEmpty()) {
            // 显示无文件
            String noFilesText = "没有找到记录文件";
            float noFilesX = (renderer.getWidth() - 200) / 2;
            float noFilesY = renderer.getHeight() / 2;
            renderer.drawText(noFilesX, noFilesY, noFilesText, 1.0f, 1.0f, 1.0f, 1.0f);
            
            // 显示返回提示
            String backText = "按ESC返回菜单";
            float backX = (renderer.getWidth() - 200) / 2;
            float backY = renderer.getHeight() / 2 + 30;
            renderer.drawText(backX, backY, backText, 0.8f, 0.8f, 0.8f, 1.0f);
            return;
        }
        
        // 显示文件列表
        float startY = 100;
        float lineHeight = 30;
        
        for (int i = 0; i < recordingFiles.size(); i++) {
            String fileName = recordingFiles.get(i);
            float x = 50;
            float y = startY + i * lineHeight;
            
            // 高亮选中的文件
            if (i == selectedIndex) {
                renderer.drawRect(x - 10, y - 5, 400, 25, 0.3f, 0.3f, 0.5f, 0.5f);
                renderer.drawText(x, y, "> " + fileName, 1.0f, 1.0f, 0.0f, 1.0f);
            } else {
                renderer.drawText(x, y, fileName, 1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
        
        // 显示操作提示
        String hint1 = "使用上下箭头选择文件";
        String hint2 = "按ENTER开始回放";
        String hint3 = "按ESC返回菜单";
        
        float hintY = renderer.getHeight() - 80;
        renderer.drawText(50, hintY, hint1, 0.8f, 0.8f, 0.8f, 1.0f);
        renderer.drawText(50, hintY + 20, hint2, 0.8f, 0.8f, 0.8f, 1.0f);
        renderer.drawText(50, hintY + 40, hint3, 0.8f, 0.8f, 0.8f, 1.0f);
    }
    
    /**
     * 加载记录文件列表
     */
    private void loadRecordingFiles() {
        new Thread(() -> {
            try {
                File recordingsDir = new File("recordings");
                if (recordingsDir.exists() && recordingsDir.isDirectory()) {
                    File[] files = recordingsDir.listFiles((dir, name) -> name.endsWith(".jsonl"));
                    if (files != null) {
                        for (File file : files) {
                            recordingFiles.add(file.getName());
                        }
                    }
                }
                fileListLoaded = true;
            } catch (Exception e) {
                System.err.println("加载记录文件失败: " + e.getMessage());
                fileListLoaded = true;
            }
        }).start();
    }
    
    /**
     * 处理输入
     */
    private void handleInput() {
        // 上下选择
        if (inputManager.isKeyJustPressed(38)) { // 上箭头
            selectedIndex = Math.max(0, selectedIndex - 1);
        }
        if (inputManager.isKeyJustPressed(40)) { // 下箭头
            selectedIndex = Math.min(recordingFiles.size() - 1, selectedIndex + 1);
        }
        
        // 确认选择
        if (inputManager.isKeyJustPressed(10)) { // ENTER
            if (!recordingFiles.isEmpty()) {
                String selectedFile = recordingFiles.get(selectedIndex);
                String filePath = "recordings/" + selectedFile;
                startReplay(filePath);
            }
        }
        
        // 返回菜单
        if (inputManager.isKeyJustPressed(27)) { // ESC
            returnToMainMenu();
        }
    }
    
    /**
     * 开始回放
     */
    private void startReplay(String filePath) {
        System.out.println("开始回放文件: " + filePath);
        ReplayScene replayScene = new ReplayScene(engine, filePath);
        engine.setScene(replayScene);
    }
    
    /**
     * 返回主菜单
     */
    private void returnToMainMenu() {
        MenuScene menuScene = new MenuScene(engine, "MainMenu");
        engine.setScene(menuScene);
    }
}
