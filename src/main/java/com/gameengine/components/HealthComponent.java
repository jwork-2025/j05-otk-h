package com.gameengine.components;

import com.gameengine.core.Component;

/**
 * 血量组件，管理游戏对象的生命值
 */
public class HealthComponent extends Component<HealthComponent> {
    public static final int DEFAULT_HEALTH = 5;
    public static final float DEFAULT_INVINCIBLE_TIME = 3.0f;

    private int currentHealth;
    private int maxHealth;
    private boolean isAlive;
    private boolean invincible;
    private float invincibleTimer;
    private float invincibleDuration;
    
    public HealthComponent(int maxHealth, boolean isPlayer) {
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
        this.isAlive = true;
        this.invincible = false;
        this.invincibleTimer = 0.0f;
        this.invincibleDuration = isPlayer ? DEFAULT_INVINCIBLE_TIME : 0.0f;
    }
    
    @Override
    public void initialize() {
    }
    
    @Override
    public void update(float deltaTime) {
        if (invincible) {
            invincibleTimer -= deltaTime;
            if (invincibleTimer <= 0) {
                invincible = false;
                invincibleTimer = 0.0f;
            }
        }
    }
    
    @Override
    public void render() {
        // 血量组件不直接渲染，由其他组件负责显示
    }
    
    /**
     * 受到伤害
     * @param damage 伤害值
     * @return 是否还存活
     */
    public synchronized boolean takeDamage(int damage) {
        if (!isAlive || invincible) return false;
        
        currentHealth -= damage;
        if (currentHealth <= 0) {
            currentHealth = 0;
            isAlive = false;
            onDeath();
        } else {
            // 受到伤害后开始无敌时间
            startInvincibility();
        }
        
        return isAlive;
    }
    
    /**
     * 死亡处理
     */
    private void onDeath() {
        if (owner != null) {
            owner.setActive(false);
        }
    }
    
    // /**
    //  * 复活
    //  */
    // public synchronized void revive() {
    //     currentHealth = maxHealth;
    //     isAlive = true;
    //     if (owner != null) {
    //         owner.setActive(true);
    //     }
    // }
    
    // Getters and Setters
    public int getCurrentHealth() {
        return currentHealth;
    }

    public void setCurrentHealth(int health) {
        currentHealth = health > maxHealth ? maxHealth : health;
    }
    
    public int getMaxHealth() {
        return maxHealth;
    }
    
    public boolean isAlive() {
        return isAlive;
    }
    
    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
        if (currentHealth > maxHealth) {
            currentHealth = maxHealth;
        }
    }
    
    /**
     * 获取血量百分比
     */
    public float getHealthPercentage() {
        return (float) currentHealth / maxHealth;
    }
    
    /**
     * 开始无敌时间
     */
    public void startInvincibility() {
        this.invincible = true;
        this.invincibleTimer = invincibleDuration;
    }
    
    /**
     * 检查是否处于无敌状态
     */
    public boolean isInvincible() {
        return invincible;
    }
    
    /**
     * 获取剩余无敌时间
     */
    public float getRemainingInvincibleTime() {
        return invincibleTimer;
    }
    
    /**
     * 设置无敌持续时间
     */
    public void setInvincibleDuration(float duration) {
        this.invincibleDuration = duration;
    }
}
