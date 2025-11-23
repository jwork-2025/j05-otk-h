package com.gameengine.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * 配置管理器
 * 负责读取和管理游戏配置
 */
public class ConfigManager {
    private static final ConfigManager INSTANCE = new ConfigManager();
    private Properties properties;
    
    private ConfigManager() {
        properties = new Properties();
        loadConfig();
    }
    
    public static ConfigManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 加载配置文件
     */
    private void loadConfig() {
        try (FileInputStream input = new FileInputStream("src/main/resources/config/game.properties")) {
            properties.load(input);
            // System.out.println("配置文件加载成功");
        } catch (IOException e) {
            System.err.println("无法加载配置文件: " + e.getMessage());
            setDefaultValues();
        }
    }
    
    /**
     * 设置默认值
     */
    private void setDefaultValues() {
        properties.setProperty("game.title", "Gameengine");
        properties.setProperty("game.version", "1.0.0");
        properties.setProperty("window.width", "800");
        properties.setProperty("window.height", "600");
        properties.setProperty("window.target_fps", "60");
        properties.setProperty("player.health", "100");
        properties.setProperty("player.speed", "200");
        properties.setProperty("enemy.spawn_rate", "2.0");
        properties.setProperty("audio.enabled", "true");
        properties.setProperty("audio.volume", "0.8");
        properties.setProperty("debug.enabled", "false");
        properties.setProperty("profiling.enabled", "true");
    }
    
    /**
     * 获取字符串配置值
     */
    public String getString(String key) {
        return properties.getProperty(key, "");
    }
    
    /**
     * 获取整数配置值
     */
    public int getInt(String key) {
        try {
            return Integer.parseInt(properties.getProperty(key, "0"));
        } catch (NumberFormatException e) {
            System.err.println("配置值格式错误: " + key);
            return 0;
        }
    }
    
    /**
     * 获取浮点数配置值
     */
    public float getFloat(String key) {
        try {
            return Float.parseFloat(properties.getProperty(key, "0.0"));
        } catch (NumberFormatException e) {
            System.err.println("配置值格式错误: " + key);
            return 0.0f;
        }
    }
    
    /**
     * 获取布尔值配置值
     */
    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(properties.getProperty(key, "false"));
    }
    
    /**
     * 重新加载配置
     */
    public void reload() {
        loadConfig();
    }
    
    /**
     * 获取所有配置
     */
    public Properties getAllProperties() {
        return new Properties(properties);
    }
}
