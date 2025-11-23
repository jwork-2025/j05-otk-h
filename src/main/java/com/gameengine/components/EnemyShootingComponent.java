package com.gameengine.components;

import com.gameengine.core.Component;
import com.gameengine.graphics.IRenderer;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;
import com.gameengine.example.EntityFactory;

/**
 * 敌人射击组件实现
 */
public class EnemyShootingComponent extends Component<EnemyShootingComponent> implements ShootingComponent {
    private static final float DEFAULT_COOLDOWN = 3.0f;
    private static final float DEFAULT_SPEED = 200.0f;
    private static final int DEFAULT_DAMAGE = 1;

    private float shootTimer;
    private float shootCooldown;
    private float bulletSpeed;
    private int bulletDamage;
    private Vector2 bulletSize;
    private RenderComponent.Color bulletColor;
    private Vector2 shootDirection;
    private IRenderer renderer;
    
    public EnemyShootingComponent() {
        this.shootTimer = 0.0f;
        this.shootCooldown = DEFAULT_COOLDOWN;
        this.bulletSpeed = DEFAULT_SPEED;
        this.bulletDamage = DEFAULT_DAMAGE;
        this.bulletSize = new Vector2(4, 8);
        this.bulletColor = new RenderComponent.Color(0.0f, 0.0f, 1.0f, 1.0f); // 蓝色
        this.shootDirection = new Vector2(0, 1); // 默认向下射击
    }
    
    @Override
    public void initialize() {
    }
    
    @Override
    public void update(float deltaTime) {
        if (shootTimer > 0) {
            shootTimer -= deltaTime;
        }
    }
    
    @Override
    public void render() {
        // 敌人射击组件不需要渲染
    }
    
    @Override
    public boolean shoot(Scene scene) {
        return shoot(scene, shootDirection);
    }
    
    @Override
    public boolean shoot(Scene scene, Vector2 direction) {
        if (scene == null || renderer == null) {
            return false;
        }
        
        // 检查是否可以射击
        if (!canShoot()) {
            return false;
        }
        
        shootTimer = shootCooldown;
        
        // 使用 EntityFactory 创建子弹
        TransformComponent ownerTransform = owner.getComponent(TransformComponent.class);
        if (ownerTransform == null) return false;
        
        Vector2 ownerPos = ownerTransform.getPosition();
        Vector2 bulletPos = new Vector2(ownerPos.x, ownerPos.y + 15); // 向下发射
        
        EntityFactory.createBullet("EnemyBullet", bulletPos, direction, bulletSpeed, 
                                  bulletSize, bulletColor, renderer, scene);
        return true;
    }
    
    @Override
    public boolean canShoot() {
        return shootTimer <= 0.0f;
    }
    
    @Override
    public void setRenderer(IRenderer renderer) {
        this.renderer = renderer;
    }
    
    @Override
    public float getRemainingCooldown() {
        return Math.max(0, shootTimer);
    }
    
    // Getters and setters
    public void setShootCooldown(float cooldown) {
        this.shootCooldown = cooldown;
    }
    
    public void setBulletSpeed(float speed) {
        this.bulletSpeed = speed;
    }
    
    public void setBulletDamage(int damage) {
        this.bulletDamage = damage;
    }
    
    public void setBulletSize(Vector2 size) {
        this.bulletSize = new Vector2(size);
    }
    
    public void setBulletColor(RenderComponent.Color color) {
        this.bulletColor = color;
    }
    
    public void setShootDirection(Vector2 direction) {
        this.shootDirection = new Vector2(direction);
    }
    
    public float getShootCooldown() {
        return shootCooldown;
    }
    
    public float getBulletSpeed() {
        return bulletSpeed;
    }
    
    public int getBulletDamage() {
        return bulletDamage;
    }
    
    public Vector2 getBulletSize() {
        return new Vector2(bulletSize);
    }
    
    public Vector2 getShootDirection() {
        return new Vector2(shootDirection);
    }
}
