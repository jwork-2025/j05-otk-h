package com.gameengine.components;

import com.gameengine.core.Component;

/**
 * 分数组件，管理游戏分数
 */
public class ScoreComponent extends Component<ScoreComponent> {
    private static ScoreComponent instance = null;

    private int score;
    private int currentHealth;
    private int maxHealth;
    
    private ScoreComponent() {
        this.score = 0;
        this.currentHealth = HealthComponent.DEFAULT_HEALTH;
        this.maxHealth = HealthComponent.DEFAULT_HEALTH;
    }

    public static ScoreComponent getInstance(){
        if(instance == null) {
            instance = new ScoreComponent();
        }
        return instance;
    }
    
    @Override
    public void initialize() {
    }
    
    @Override
    public void update(float deltaTime) {
    }
    
    @Override
    public void render() {
    }
    
    /**
     * 增加分数
     * @param points 增加的分数
     */
    public synchronized void addScore(int points) {
        this.score += points;
    }
    
    /**
     * 获取当前分数
     */
    public int getScore() {
        return score;
    }
    
    /**
     * 设置分数
     */
    public void setScore(int score) {
        this.score = score;
    }
    
    /**
     * 设置当前生命值
     */
    public void setCurrentHealth(int health) {
        this.currentHealth = Math.max(0, Math.min(health, maxHealth));
    }
    
    /**
     * 设置最大生命值
     */
    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
        if (currentHealth > maxHealth) {
            currentHealth = maxHealth;
        }
    }
    
    /**
     * 获取当前生命值
     */
    public int getCurrentHealth() {
        return currentHealth;
    }
    
    /**
     * 获取最大生命值
     */
    public int getMaxHealth() {
        return maxHealth;
    }
    
}
