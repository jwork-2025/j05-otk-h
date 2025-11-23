package com.gameengine.recording;

import com.gameengine.input.ReplayInputManager;
import com.gameengine.math.Vector2;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 录制数据解析器，解析JSON格式的录制文件
 */
public class RecordingParser {
    
    /**
     * 从文件解析录制数据
     */
    public static List<ReplayInputManager.InputEvent> parseRecordingFile(String filePath) throws IOException {
        List<ReplayInputManager.InputEvent> events = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                // 解析JSON行 - 支持keydown和keyup两种类型
                if (line.contains("\"type\":\"keydown\"") || line.contains("\"type\":\"keyup\"")) {
                    ReplayInputManager.InputEvent event = parseInputEvent(line);
                    if (event != null) {
                        events.add(event);
                    }
                }
            }
        }
        
        return events;
    }
    
    /**
     * 从文件解析快照数据
     */
    public static List<KeyFrame> parseSnapshotFile(String filePath) throws IOException {
        List<KeyFrame> keyframes = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                // 解析JSON行 - 支持snapshot类型
                if (line.contains("\"type\":\"snapshot\"")) {
                    KeyFrame keyframe = parseKeyFrame(line);
                    if (keyframe != null) {
                        keyframes.add(keyframe);
                    }
                }
            }
        }
        
        return keyframes;
    }
    
    /**
     * 解析输入事件
     */
    private static ReplayInputManager.InputEvent parseInputEvent(String jsonLine) {
        try {
            // 提取事件类型
            String typeStr = RecordingJson.field(jsonLine, "type");
            if (typeStr == null) {
                System.err.println("无法提取类型字段: " + jsonLine);
                return null;
            }
            
            ReplayInputManager.EventType eventType;
            if ("keydown".equals(RecordingJson.stripQuotes(typeStr))) {
                eventType = ReplayInputManager.EventType.KEYDOWN;
            } else if ("keyup".equals(RecordingJson.stripQuotes(typeStr))) {
                eventType = ReplayInputManager.EventType.KEYUP;
            } else {
                System.err.println("不支持的事件类型: " + typeStr);
                return null;
            }
            
            // 提取时间
            String timeStr = RecordingJson.field(jsonLine, "t");
            if (timeStr == null) {
                System.err.println("无法提取时间字段: " + jsonLine);
                return null;
            }
            
            float time = (float) RecordingJson.parseDouble(timeStr);
            
            // 提取按键数组
            int keysIndex = jsonLine.indexOf("\"keys\":");
            if (keysIndex < 0) {
                System.err.println("找不到keys字段: " + jsonLine);
                return null;
            }
            
            // 从冒号后面开始查找数组
            int arrayStart = jsonLine.indexOf('[', keysIndex);
            if (arrayStart < 0) {
                System.err.println("找不到数组开始位置: " + jsonLine);
                return null;
            }
            
            String keysArrayStr = RecordingJson.extractArray(jsonLine, arrayStart);
            if (keysArrayStr.isEmpty()) {
                System.err.println("无法提取keys数组: " + jsonLine);
                return null;
            }
            
            String[] keyStrs = RecordingJson.splitTopLevel(keysArrayStr);
            int[] keys = new int[keyStrs.length];
            for (int i = 0; i < keyStrs.length; i++) {
                keys[i] = Integer.parseInt(keyStrs[i].trim());
            }
            
            // System.out.println("解析成功: 类型=" + eventType + ", 时间=" + time + ", 按键=" + java.util.Arrays.toString(keys));
            return new ReplayInputManager.InputEvent(time, keys, eventType);
        } catch (Exception e) {
            System.err.println("解析输入事件失败: " + jsonLine);
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 解析关键帧数据
     */
    private static KeyFrame parseKeyFrame(String jsonLine) {
        try {
            // 提取时间
            String timeStr = RecordingJson.field(jsonLine, "t");
            if (timeStr == null) {
                System.err.println("无法提取时间字段: " + jsonLine);
                return null;
            }
            
            double timestamp = RecordingJson.parseDouble(timeStr);
            
            KeyFrame keyframe = new KeyFrame();
            keyframe.timestamp = timestamp;
            
            // 提取敌人数组（如果存在）
            int enemiesIndex = jsonLine.indexOf("\"enemies\":");
            if (enemiesIndex >= 0) {
                // 从冒号后面开始查找数组
                int arrayStart = jsonLine.indexOf('[', enemiesIndex);
                if (arrayStart >= 0) {
                    String enemiesArrayStr = RecordingJson.extractArray(jsonLine, arrayStart);
                    if (!enemiesArrayStr.isEmpty()) {
                        String[] enemyStrs = RecordingJson.splitTopLevel(enemiesArrayStr);
                        List<KeyFrame.EnemyInfo> enemyInfos = new ArrayList<>();
                        
                        for (String enemyStr : enemyStrs) {
                            KeyFrame.EnemyInfo enemyInfo = parseEnemyInfo(enemyStr);
                            if (enemyInfo != null) {
                                enemyInfos.add(enemyInfo);
                            }
                        }
                        keyframe.enemyInfos = enemyInfos;
                    }
                }
            }
            
            // 提取玩家数组（如果存在）
            int playersIndex = jsonLine.indexOf("\"players\":");
            if (playersIndex >= 0) {
                // 从冒号后面开始查找数组
                int arrayStart = jsonLine.indexOf('[', playersIndex);
                if (arrayStart >= 0) {
                    String playersArrayStr = RecordingJson.extractArray(jsonLine, arrayStart);
                    if (!playersArrayStr.isEmpty()) {
                        String[] playerStrs = RecordingJson.splitTopLevel(playersArrayStr);
                        List<KeyFrame.PlayerInfo> playerInfos = new ArrayList<>();
                        
                        for (String playerStr : playerStrs) {
                            KeyFrame.PlayerInfo playerInfo = parsePlayerInfo(playerStr);
                            if (playerInfo != null) {
                                playerInfos.add(playerInfo);
                            }
                        }
                        keyframe.playerInfos = playerInfos;
                    }
                }
            }
            
            return keyframe;
        } catch (Exception e) {
            System.err.println("解析关键帧失败: " + jsonLine);
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 解析敌人信息
     */
    private static KeyFrame.EnemyInfo parseEnemyInfo(String enemyJson) {
        try {
            String idStr = RecordingJson.field(enemyJson, "id");
            String xStr = RecordingJson.field(enemyJson, "x");
            String yStr = RecordingJson.field(enemyJson, "y");
            String vxStr = RecordingJson.field(enemyJson, "vx");
            String vyStr = RecordingJson.field(enemyJson, "vy");
            
            if (idStr == null || xStr == null || yStr == null || vxStr == null || vyStr == null) {
                System.err.println("无法提取敌人信息字段: " + enemyJson);
                return null;
            }
            
            int enemyId = (int) RecordingJson.parseDouble(idStr);
            float x = (float) RecordingJson.parseDouble(xStr);
            float y = (float) RecordingJson.parseDouble(yStr);
            float vx = (float) RecordingJson.parseDouble(vxStr);
            float vy = (float) RecordingJson.parseDouble(vyStr);
            
            KeyFrame.EnemyInfo enemyInfo = new KeyFrame.EnemyInfo();
            enemyInfo.enemyId = enemyId;
            enemyInfo.position = new Vector2(x, y);
            enemyInfo.velocity = new Vector2(vx, vy);
            
            return enemyInfo;
        } catch (Exception e) {
            System.err.println("解析敌人信息失败: " + enemyJson);
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 解析玩家信息
     */
    private static KeyFrame.PlayerInfo parsePlayerInfo(String playerJson) {
        try {
            String scoreStr = RecordingJson.field(playerJson, "score");
            String healthStr = RecordingJson.field(playerJson, "health");
            
            if (scoreStr == null || healthStr == null) {
                System.err.println("无法提取玩家信息字段: " + playerJson);
                return null;
            }
            
            int score = (int) RecordingJson.parseDouble(scoreStr);
            int health = (int) RecordingJson.parseDouble(healthStr);
            
            KeyFrame.PlayerInfo playerInfo = new KeyFrame.PlayerInfo();
            playerInfo.score = score;
            playerInfo.health = health;
            
            return playerInfo;
        } catch (Exception e) {
            System.err.println("解析玩家信息失败: " + playerJson);
            e.printStackTrace();
            return null;
        }
    }
}
