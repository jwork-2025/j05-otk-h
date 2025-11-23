package com.gameengine.example;

import com.gameengine.scene.Scene;
import com.gameengine.core.GameEngine;
import com.gameengine.core.GameLogic;
import com.gameengine.core.GameObject;
import com.gameengine.math.Vector2;
import com.gameengine.graphics.IRenderer;
import com.gameengine.components.*;
import com.gameengine.input.ReplayInputManager;
import com.gameengine.recording.RecordingParser;
import com.gameengine.recording.KeyFrame;

import java.util.Random;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.io.IOException;

public class ReplayScene extends Scene {
    public final GameEngine engine;
    private IRenderer renderer;
    private Random random;
    private GameLogic gameLogic;
    private ReplayInputManager replayInputManager;
    private String recordingFilePath;
    
    private List<KeyFrame> keyframes;
    private Map<Integer, GameObject> enemyIdMap; // 敌人ID到GameObject的映射
    private Map<Integer, GameObject> playerIdMap; // 玩家ID到GameObject的映射
    private double replayTime;
    private int currentKeyframeIndex;

    public ReplayScene(GameEngine engine, String recordingFilePath) {
        super("ReplayScene");
        this.engine = engine;
        this.recordingFilePath = recordingFilePath;
    }
    
    @Override
    public void initialize() {
        super.initialize();
        this.renderer = engine.getRenderer();
        this.random = new Random();
        this.replayTime = 0.0;
        this.currentKeyframeIndex = 0;
        this.enemyIdMap = new HashMap<>();
        this.playerIdMap = new HashMap<>();
        
        // 初始化回放输入管理器
        this.replayInputManager = ReplayInputManager.getInstance();
        
        try {
            // 加载录制数据
            var inputEvents = RecordingParser.parseRecordingFile(recordingFilePath);
            replayInputManager.loadRecording(inputEvents);
            System.out.println("加载录制文件成功: " + recordingFilePath + ", 输入事件数量: " + inputEvents.size());
            
            // 加载快照数据
            this.keyframes = RecordingParser.parseSnapshotFile(recordingFilePath);
            System.out.println("加载快照数据成功: " + recordingFilePath + ", 快照数量: " + keyframes.size());
        } catch (IOException e) {
            System.err.println("加载录制文件失败: " + recordingFilePath);
            e.printStackTrace();
            // 如果加载失败，返回菜单
            returnToMenu();
            return;
        }

        // 创建游戏逻辑，使用回放输入管理器
        this.gameLogic = new GameLogic(this);
        this.gameLogic.setInputManager(replayInputManager);

        // 创建玩家对象
        createPlayer();
        createDecorations();
        createUIArea();
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        replayTime += deltaTime;

        // 更新回放输入管理器
        replayInputManager.update(deltaTime);

        // 使用游戏逻辑类处理游戏规则
        gameLogic.handlePlayerInput();
        gameLogic.handleEnemyShooting();
        gameLogic.updatePhysics();
        gameLogic.checkCollisions();

        // 检查是否需要返回菜单
        if (gameLogic.getGameState() == GameLogic.GameState.GAME_OVER) {
            // 检测Enter键，返回菜单
            if (replayInputManager.isKeyPressed(10)) { // Enter键
                returnToMenu();
            }
            return;
        }

        processKeyframes();
    }
    
    /**
     * 处理关键帧数据，根据时间戳生成敌人
     */
    private void processKeyframes() {
        if (keyframes == null || keyframes.isEmpty()) {
            return;
        }
        
        // 检查当前时间是否到达下一个关键帧
        while (currentKeyframeIndex < keyframes.size()) {
            KeyFrame keyframe = keyframes.get(currentKeyframeIndex);
            
            if (replayTime >= keyframe.timestamp) {
                // 处理这个关键帧中的敌人
                processKeyframeEnemies(keyframe);
                // 处理这个关键帧中的玩家信息
                processKeyframePlayers(keyframe);
                currentKeyframeIndex++;
            } else {
                break;
            }
        }
    }
    
    /**
     * 处理关键帧中的敌人数据
     */
    private void processKeyframeEnemies(KeyFrame keyframe) {
        if (keyframe.enemyInfos == null) {
            return;
        }
        
        // 收集当前关键帧中存在的敌人ID
        Set<Integer> currentFrameEnemyIds = new HashSet<>();
        
        for (KeyFrame.EnemyInfo enemyInfo : keyframe.enemyInfos) {
            currentFrameEnemyIds.add(enemyInfo.enemyId);
            
            // 检查敌人是否已经存在
            GameObject existingEnemy = enemyIdMap.get(enemyInfo.enemyId);
            
            if (existingEnemy == null) {
                // 创建新敌人
                GameObject enemy = EntityFactory.createEnemy(enemyInfo.position, enemyInfo.velocity, renderer, this);
                enemyIdMap.put(enemyInfo.enemyId, enemy);
            } else {
                // 更新现有敌人的位置和速度
                updateEnemyState(existingEnemy, enemyInfo);
                // 确保敌人是活动的
                existingEnemy.setActive(true);
            }
        }
        
        // 处理在当前关键帧中消失的敌人
        handleMissingEnemies(currentFrameEnemyIds);
    }
    
    /**
     * 处理在当前关键帧中消失的敌人
     */
    private void handleMissingEnemies(Set<Integer> currentFrameEnemyIds) {
        // 找出在当前关键帧中不存在的敌人
        for (Map.Entry<Integer, GameObject> entry : enemyIdMap.entrySet()) {
            int enemyId = entry.getKey();
            GameObject enemy = entry.getValue();
            
            if (!currentFrameEnemyIds.contains(enemyId)) {
                // 这个敌人在当前关键帧中不存在，设置为非活动状态
                enemy.setActive(false);
            }
        }
    }
    
    /**
     * 更新敌人的状态（位置和速度）
     */
    private void updateEnemyState(GameObject enemy, KeyFrame.EnemyInfo enemyInfo) {
        // 更新变换组件
        var transform = enemy.getComponent(com.gameengine.components.TransformComponent.class);
        if (transform != null) {
            transform.setPosition(enemyInfo.position);
        }
        
        // 更新物理组件
        var physics = enemy.getComponent(com.gameengine.components.PhysicsComponent.class);
        if (physics != null) {
            physics.setVelocity(enemyInfo.velocity);
        }
    }
    
    /**
     * 处理关键帧中的玩家数据
     */
    private void processKeyframePlayers(KeyFrame keyframe) {
        if (keyframe.playerInfos == null) {
            return;
        }
        
        // 收集当前关键帧中存在的玩家ID
        Set<Integer> currentFramePlayerIds = new HashSet<>();
        
        for (KeyFrame.PlayerInfo playerInfo : keyframe.playerInfos) {
            int playerId = keyframe.playerInfos.indexOf(playerInfo) + 1;
            currentFramePlayerIds.add(playerId);
            
            GameObject existingPlayer = playerIdMap.get(playerId);
            
            if (existingPlayer == null) {
                // 在回放模式下，玩家应该已经存在，不应该创建新的玩家
                // 查找场景中已有的玩家对象
                for (GameObject obj : getGameObjects()) {
                    if ("Player".equals(obj.getName()) && !playerIdMap.containsValue(obj)) {
                        // 找到未映射的玩家对象
                        playerIdMap.put(playerId, obj);
                        existingPlayer = obj;
                        break;
                    }
                }
                
                // 如果仍然没有找到玩家，则创建玩家（这种情况不应该发生）
                if (existingPlayer == null) {
                    System.err.println("警告：在回放模式下创建新玩家，这可能不是预期的行为");
                    GameObject player = EntityFactory.createPlayer(renderer, this);
                    playerIdMap.put(playerId, player);
                    existingPlayer = player;
                }
            }
            
            // 更新玩家状态
            updatePlayerState(existingPlayer, playerInfo);
            existingPlayer.setActive(true);
        }

        handleMissingPlayers(currentFramePlayerIds);
    }
    
    /**
     * 更新玩家的状态（血量和分数）
     */
    private void updatePlayerState(GameObject player, KeyFrame.PlayerInfo playerInfo) {
        HealthComponent health = player.getComponent(HealthComponent.class);
        ScoreComponent score = player.getComponent(ScoreComponent.class);
        
        if (health != null) {
            health.setCurrentHealth(playerInfo.health);
        }
        
        if (score != null) {
            score.setScore(playerInfo.score);
        }
    }
    
    /**
     * 处理在当前关键帧中消失的玩家
     */
    private void handleMissingPlayers(Set<Integer> currentFramePlayerIds) {
        // 找出在当前关键帧中不存在的玩家
        for (Map.Entry<Integer, GameObject> entry : playerIdMap.entrySet()) {
            int playerId = entry.getKey();
            GameObject player = entry.getValue();
            
            if (!currentFramePlayerIds.contains(playerId)) {
                // 这个玩家在当前关键帧中不存在，设置为非活动状态
                player.setActive(false);
            }
        }
    }

    /**
     * 返回菜单场景
     */
    private void returnToMenu() {
        replayInputManager.reset(); // 重置回放状态
        MenuScene menuScene = new MenuScene(engine, "MainMenu");
        engine.setScene(menuScene);
    }

    @Override
    public void render() {
        // 绘制背景
        renderer.drawRect(0, 0, renderer.getWidth(), renderer.getHeight(), 0.1f, 0.1f, 0.2f, 1.0f);

        // 渲染所有对象
        super.render();

        // 检查游戏状态，显示死亡画面
        if (gameLogic.getGameState() == GameLogic.GameState.GAME_OVER) {
            renderGameOverScreen();
        }
    }
    
    private void createPlayer() {
        EntityFactory.createPlayer(renderer, this);
    }
    
    private void createDecorations() {
        for (int i = 0; i < 5; i++) {
            createDecoration();
        }
    }
    
    private void createDecoration() {
        // 随机位置 - 避免在UI区域生成
        Vector2 position = new Vector2(
            random.nextFloat() * (renderer.getWidth() - UIComponent.UI_WIDTH),
            random.nextFloat() * renderer.getHeight()
        );
        
        // 使用 EntityFactory 创建装饰物
        EntityFactory.createDecoration(position, renderer, this);
    }
    
    private void createUIArea() {
        // 创建UI区域对象
        GameObject uiArea = new GameObject("UIArea") {
            @Override
            public void update(float deltaTime) {
                super.update(deltaTime);
                updateComponents(deltaTime);
            }
            
            @Override
            public void render() {
                renderComponents();
            }
        };
        
        UIComponent ui = UIComponent.getInstance();
        ui.setRenderer(renderer);
        uiArea.addComponent(ui);
        
        addGameObject(uiArea);
    }
    
    private void renderGameOverScreen() {
        renderer.drawRect(0, 0, renderer.getWidth(), renderer.getHeight(), 0.0f, 0.0f, 0.0f, 0.7f);
        
        String deathMessage1 = "YOU DIED";
        String deathMessage2 = "press 'ENTER' to continue";
        
        float centerX = (renderer.getWidth() - 200) / 2;
        float centerY = (renderer.getHeight() - 200) / 2;
        
        renderer.drawText(centerX - 80, centerY - 20, deathMessage1, 1.0f, 0.0f, 0.0f, 1.0f);
        renderer.drawText(centerX - 180, centerY + 20, deathMessage2, 1.0f, 1.0f, 1.0f, 1.0f);
    }
}
