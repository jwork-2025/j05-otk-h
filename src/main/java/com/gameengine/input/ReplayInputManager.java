package com.gameengine.input;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 回放输入管理器，从录制文件读取输入事件
 */
public class ReplayInputManager implements IInputManager {
    private static ReplayInputManager instance;
    private Set<Integer> pressedKeys;
    private Set<Integer> justPressedKeys;
    private List<InputEvent> inputEvents;
    private int currentEventIndex;
    private float currentTime;
    private boolean isPlaying;
    
    private ReplayInputManager() {
        pressedKeys = new HashSet<>();
        justPressedKeys = new HashSet<>();
        inputEvents = new ArrayList<>();
        currentEventIndex = 0;
        currentTime = 0;
        isPlaying = false;
    }
    
    public static ReplayInputManager getInstance() {
        if (instance == null) {
            instance = new ReplayInputManager();
        }
        return instance;
    }
    
    /**
     * 加载录制数据
     */
    public void loadRecording(List<InputEvent> events) {
        this.inputEvents.clear();
        this.inputEvents.addAll(events);
        this.currentEventIndex = 0;
        this.currentTime = 0;
        this.isPlaying = true;
        this.pressedKeys.clear();
        this.justPressedKeys.clear();
    }
    
    /**
     * 更新回放状态
     */
    public void update(float deltaTime) {
        if (!isPlaying || inputEvents.isEmpty()) {
            return;
        }
        
        currentTime += deltaTime;
        justPressedKeys.clear();
        
        // 处理所有在当前时间之前的事件
        while (currentEventIndex < inputEvents.size()) {
            InputEvent event = inputEvents.get(currentEventIndex);
            
            // 只处理当前时间之前的事件，防止提前读取未来输入
            if (event.time <= currentTime) {
                // 根据事件类型处理按键
                if (event.type == EventType.KEYDOWN) {
                    for (int keyCode : event.keys) {
                        if (!pressedKeys.contains(keyCode)) {
                            justPressedKeys.add(keyCode);
                        }
                        pressedKeys.add(keyCode);
                    }
                } else if (event.type == EventType.KEYUP) {
                    for (int keyCode : event.keys) {
                        pressedKeys.remove(keyCode);
                    }
                }
                currentEventIndex++;
            } else {
                // 遇到未来事件，停止处理
                break;
            }
        }
    }
    
    /**
     * 检查按键是否被按下
     */
    public boolean isKeyPressed(int keyCode) {
        return pressedKeys.contains(keyCode);
    }
    
    /**
     * 检查按键是否刚刚被按下
     */
    public boolean isKeyJustPressed(int keyCode) {
        return justPressedKeys.contains(keyCode);
    }
    
    /**
     * 重置回放状态
     */
    public void reset() {
        currentEventIndex = 0;
        currentTime = 0;
        pressedKeys.clear();
        justPressedKeys.clear();
        isPlaying = false;
    }
    
    /**
     * 输入事件类型
     */
    public enum EventType {
        KEYDOWN, KEYUP
    }
    
    /**
     * 输入事件数据结构
     */
    public static class InputEvent {
        public float time;
        public int[] keys;
        public EventType type;
        
        public InputEvent(float time, int[] keys, EventType type) {
            this.time = time;
            this.keys = keys;
            this.type = type;
        }
    }
    
    // 实现InputManager接口的方法
    public void onKeyPressed(int keyCode) {
        // 回放模式下不需要处理实时按键
    }
    
    public void onKeyReleased(int keyCode) {
        // 回放模式下不需要处理实时按键
    }
    
    public Set<Integer> getJustPressedKeysSnapshot() {
        return new HashSet<>(justPressedKeys);
    }
    
    public void update() {
        // 回放模式下使用自定义的update方法
    }
}
