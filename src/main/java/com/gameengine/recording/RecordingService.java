package com.gameengine.recording;

import com.gameengine.components.TransformComponent;
import com.gameengine.components.PhysicsComponent;
import com.gameengine.components.HealthComponent;
import com.gameengine.components.ScoreComponent;
import com.gameengine.core.GameObject;
import com.gameengine.input.InputManager;
import com.gameengine.scene.Scene;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class RecordingService {
    private final RecordingConfig config;
    private final BlockingQueue<String> lineQueue;
    private volatile boolean recording;
    private Thread writerThread;
    private RecordingStorage storage = new FileRecordingStorage();
    private double elapsed;
    private double keyframeElapsed;
    private double sampleAccumulator;
    private final double warmupSec = 0.1; // 等待一帧让场景对象完成初始化
    private final DecimalFormat qfmt;
    private Scene lastScene;
    
    // 跟踪当前按下的按键状态
    private Set<Integer> currentPressedKeys;
    
    // 跟踪敌人对象到ID的映射，确保标号一致性
    private java.util.Map<GameObject, Integer> enemyIdMap;
    
    // 跟踪玩家对象到ID的映射，确保标号一致性
    private java.util.Map<GameObject, Integer> playerIdMap;

    public RecordingService(RecordingConfig config) {
        this.config = config;
        this.lineQueue = new ArrayBlockingQueue<>(config.queueCapacity);
        this.recording = false;
        this.elapsed = 0.0;
        this.keyframeElapsed = 0.0;
        this.sampleAccumulator = 0.0;
        this.qfmt = new DecimalFormat();
        this.qfmt.setMaximumFractionDigits(Math.max(0, config.quantizeDecimals));
        this.qfmt.setGroupingUsed(false);
        this.currentPressedKeys = new HashSet<>();
        this.enemyIdMap = new java.util.HashMap<>();
        this.playerIdMap = new java.util.HashMap<>();
    }

    public boolean isRecording() {
        return recording;
    }

    public void start(Scene scene, int width, int height) throws IOException {
        if (recording) return;
        storage.openWriter(config.outputPath);
        writerThread = new Thread(() -> {
            try {
                while (recording || !lineQueue.isEmpty()) {
                    String s = lineQueue.poll();
                    if (s == null) {
                        try { Thread.sleep(2); } catch (InterruptedException ignored) {}
                        continue;
                    }
                    storage.writeLine(s);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try { storage.closeWriter(); } catch (Exception ignored) {}
            }
        }, "record-writer");
        recording = true;
        writerThread.start();

        // header
        enqueue("{\"type\":\"header\",\"version\":1,\"w\":" + width + ",\"h\":" + height + "}");
        keyframeElapsed = 0.0;
        currentPressedKeys.clear();
    }

    public void stop() {
        if (!recording) return;
        recording = false;
        try { writerThread.join(500); } catch (InterruptedException ignored) {}
    }

    public void update(double deltaTime, Scene scene, InputManager input) {
        if (!recording) return;
        elapsed += deltaTime;
        keyframeElapsed += deltaTime;

        // 记录按键按下事件
        Set<Integer> justPressedKeys = input.getJustPressedKeysSnapshot();
        if (!justPressedKeys.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"type\":\"keydown\",\"t\":").append(qfmt.format(elapsed)).append(",\"keys\":[");
            boolean first = true;
            for (Integer k : justPressedKeys) {
                if (!first) sb.append(',');
                sb.append(k);
                first = false;
            }
            sb.append("]}");
            enqueue(sb.toString());
            
            // 更新当前按下的按键状态
            currentPressedKeys.addAll(justPressedKeys);
        }

        // 检查按键释放事件
        Set<Integer> releasedKeys = new HashSet<>();
        Set<Integer> currentKeys = getCurrentPressedKeys(input);
        
        // 找出被释放的按键
        for (Integer key : currentPressedKeys) {
            if (!currentKeys.contains(key)) {
                releasedKeys.add(key);
            }
        }
        
        // 记录按键释放事件
        if (!releasedKeys.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"type\":\"keyup\",\"t\":").append(qfmt.format(elapsed)).append(",\"keys\":[");
            boolean first = true;
            for (Integer k : releasedKeys) {
                if (!first) sb.append(',');
                sb.append(k);
                first = false;
            }
            sb.append("]}");
            enqueue(sb.toString());
            
            // 更新当前按下的按键状态
            currentPressedKeys.removeAll(releasedKeys);
        }
        
        // 更新当前按键状态（用于下一帧比较）
        currentPressedKeys.retainAll(currentKeys);
        currentPressedKeys.addAll(currentKeys);
        
        // 记录敌人快照
        if (keyframeElapsed >= config.keyframeIntervalSec) {
            createKeyframe(scene);
            keyframeElapsed = 0.0;
        }
    }
    
    /**
     * 创建关键帧
     */
    void createKeyframe(Scene scene) {
        recordEnemyInfo(scene);
        recordPlayerInfo(scene);
    }

    /**
     * 记录敌人信息
     */
    private void recordEnemyInfo(Scene scene) {
        List<KeyFrame.EnemyInfo> enemyInfos = new ArrayList<>();
        
        for (GameObject obj : scene.getGameObjects()) {
            if ("Enemy".equals(obj.getName())) {
                TransformComponent transform = obj.getComponent(TransformComponent.class);
                PhysicsComponent physics = obj.getComponent(PhysicsComponent.class);
                
                if (transform != null && physics != null) {
                    // 获取或分配敌人ID
                    Integer enemyId = enemyIdMap.get(obj);
                    if (enemyId == null) {
                        // 新敌人，分配新ID
                        enemyId = enemyIdMap.size() + 1;
                        enemyIdMap.put(obj, enemyId);
                    }
                    
                    KeyFrame.EnemyInfo info = new KeyFrame.EnemyInfo();
                    info.enemyId = enemyId;
                    info.position = transform.getPosition();
                    info.velocity = physics.getVelocity();
                    enemyInfos.add(info);
                }
            }
        }
        
        if (!enemyInfos.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"type\":\"snapshot\",\"t\":").append(qfmt.format(elapsed)).append(",\"enemies\":[");
            
            boolean first = true;
            for (KeyFrame.EnemyInfo info : enemyInfos) {
                if (!first) sb.append(',');
                sb.append(String.format("{\"id\":%d,\"x\":%.2f,\"y\":%.2f,\"vx\":%.2f,\"vy\":%.2f}",
                        info.enemyId, info.position.x, info.position.y, info.velocity.x, info.velocity.y));
                first = false;
            }
            
            sb.append("]}");
            enqueue(sb.toString());
        }
    }

    /**
     * 记录玩家信息
     */
    private void recordPlayerInfo(Scene scene) {
        // 记录玩家信息
        List<KeyFrame.PlayerInfo> playerInfos = new ArrayList<>();
        for (GameObject obj : scene.getGameObjects()) {
            if ("Player".equals(obj.getName())) {
                HealthComponent health = obj.getComponent(HealthComponent.class);
                ScoreComponent score = obj.getComponent(ScoreComponent.class);
                
                // 获取或分配玩家ID
                Integer playerId = playerIdMap.get(obj);
                if (playerId == null) {
                    // 新玩家，分配新ID
                    playerId = playerIdMap.size() + 1;
                    playerIdMap.put(obj, playerId);
                }
                
                KeyFrame.PlayerInfo playerInfo = new KeyFrame.PlayerInfo();
                if (health != null) {
                    playerInfo.health = health.getCurrentHealth();
                }
                if (score != null) {
                    playerInfo.score = score.getScore();
                }
                playerInfos.add(playerInfo);
            }
        }
        
        // 生成玩家快照记录
        if (!playerInfos.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\"type\":\"snapshot\",\"t\":").append(qfmt.format(elapsed)).append(",\"players\":[");
            
            boolean first = true;
            for (KeyFrame.PlayerInfo playerInfo : playerInfos) {
                if (!first) sb.append(',');
                sb.append(String.format("{\"score\":%d,\"health\":%d}",
                        playerInfo.score, playerInfo.health));
                first = false;
            }
            
            sb.append("]}");
            enqueue(sb.toString());
        }
    }

        /**
     * 获取当前按下的所有按键
     */
    private Set<Integer> getCurrentPressedKeys(InputManager input) {
        Set<Integer> currentKeys = new HashSet<>();
        if (input.isKeyPressed(87)) currentKeys.add(87); // W
        if (input.isKeyPressed(83)) currentKeys.add(83); // S
        if (input.isKeyPressed(65)) currentKeys.add(65); // A
        if (input.isKeyPressed(68)) currentKeys.add(68); // D
        if (input.isKeyPressed(90)) currentKeys.add(90); // Z (射击)
        if (input.isKeyPressed(38)) currentKeys.add(38); // 上箭头
        if (input.isKeyPressed(40)) currentKeys.add(40); // 下箭头
        if (input.isKeyPressed(37)) currentKeys.add(37); // 左箭头
        if (input.isKeyPressed(39)) currentKeys.add(39); // 右箭头
        return currentKeys;
    }

    private void enqueue(String line) {
        if (!lineQueue.offer(line)) {
            // 简单丢弃策略：队列满时丢弃低优先级数据（此处直接丢弃）
        }
    }
}
