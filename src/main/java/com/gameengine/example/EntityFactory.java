package com.gameengine.example;

import com.gameengine.components.HealthComponent;
import com.gameengine.components.PhysicsComponent;
import com.gameengine.components.RenderComponent;
import com.gameengine.components.TransformComponent;
import com.gameengine.components.PlayerShootingComponent;
import com.gameengine.components.EnemyShootingComponent;
import com.gameengine.core.GameObject;
import com.gameengine.graphics.IRenderer;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;

public final class EntityFactory {

    private EntityFactory() {
    }

    public static GameObject createPlayer(IRenderer renderer, Scene scene) {
        // 创建葫芦娃 - 所有部位都在一个GameObject中
        GameObject player = new GameObject("Player") {
            private Vector2 basePosition;

            @Override
            public void update(float deltaTime) {
                super.update(deltaTime);
                updateComponents(deltaTime);
                TransformComponent tc = getComponent(TransformComponent.class);
                if (tc != null) { basePosition = tc.getPosition(); }
            }

            @Override
            public void render() {
                if (basePosition == null) { return; }

                HealthComponent health = getComponent(HealthComponent.class);
                boolean isInvincible = health != null && health.isInvincible();
                // 根据无敌状态决定颜色和透明度
                float alpha = 1.0f;
                if (isInvincible) {
                    // 无敌状态下闪烁效果
                    float time = System.currentTimeMillis() % 1000 / 1000.0f;
                    alpha = 0.5f + 0.5f * (float) Math.sin(time * Math.PI * 4);
                }

                renderer.drawRect(basePosition.x - 8, basePosition.y - 10, 16, 20, 1.0f, 0.0f, 0.0f, alpha);
                renderer.drawRect(basePosition.x - 6, basePosition.y - 22, 12, 12, 1.0f, 0.5f, 0.0f, alpha);
                renderer.drawRect(basePosition.x - 13, basePosition.y - 5, 6, 12, 1.0f, 0.8f, 0.0f, alpha);
                renderer.drawRect(basePosition.x + 7, basePosition.y - 5, 6, 12, 0.0f, 1.0f, 0.0f, alpha);

                // 渲染血量条
                renderHealthBar(basePosition.x - 15, basePosition.y - 30);

            }

            private void renderHealthBar(float x, float y) {
                HealthComponent health = getComponent(HealthComponent.class);
                if (health == null)
                    return;

                float healthPercentage = health.getHealthPercentage();

                // 血量条背景
                renderer.drawRect(x, y, 30, 4, 0.3f, 0.3f, 0.3f, 1.0f);

                // 血量条前景
                float healthWidth = 30 * healthPercentage;
                if (healthPercentage > 0.6f) {
                    renderer.drawRect(x, y, healthWidth, 4, 0.0f, 1.0f, 0.0f, 1.0f);
                } else if (healthPercentage > 0.3f) {
                    renderer.drawRect(x, y, healthWidth, 4, 1.0f, 1.0f, 0.0f, 1.0f);
                } else {
                    renderer.drawRect(x, y, healthWidth, 4, 1.0f, 0.0f, 0.0f, 1.0f);
                }
            }
        };

        // 添加变换组件
        float gameAreaWidth = 1024 - 200;
        float playerX = gameAreaWidth / 2;
        float playerY = 768 - 50;
        player.addComponent(new TransformComponent(new Vector2(playerX, playerY)));

        // 添加物理组件
        PhysicsComponent physics = player.addComponent(new PhysicsComponent(1.0f));
        physics.setFriction(0.95f);

        // 添加血量组件
        player.addComponent(new HealthComponent(HealthComponent.DEFAULT_HEALTH, true));

        // 添加射击组件
        PlayerShootingComponent shooting = player.addComponent(new PlayerShootingComponent());
        shooting.setRenderer(renderer);

        scene.addGameObject(player);

        return player;
    }

    public static GameObject createBullet(String bulletType, Vector2 position, Vector2 direction, 
                                        float speed, Vector2 size, 
                                        RenderComponent.Color color, 
                                        IRenderer renderer, Scene scene) {
        // 创建子弹对象
        GameObject bullet = new GameObject(bulletType) {
            private TransformComponent transform;
            
            @Override
            public void update(float deltaTime) {
                super.update(deltaTime);
                updateComponents(deltaTime);
                
                if (transform == null) {
                    transform = getComponent(TransformComponent.class);
                }
                
                if (transform != null) {
                    Vector2 pos = transform.getPosition();
                    // 动态计算游戏区域边界
                    float windowWidth = 1024;   // Game.WINDOW_WIDTH
                    float uiWidth = 200;        // UIComponent.UI_WIDTH
                    float gameAreaRight = windowWidth - uiWidth - 5; // 窗口宽度 - UI宽度 - 安全边距
                    float gameAreaBottom = 768 - 5; // 窗口高度 - 安全边距
                    if (pos.y < 5 || pos.y > gameAreaBottom || pos.x < 5 || pos.x > gameAreaRight) {
                        setActive(false);
                    }
                }
            }
        };
        
        // 添加变换组件
        bullet.addComponent(new TransformComponent(position));
        
        // 添加渲染组件
        RenderComponent render = bullet.addComponent(new RenderComponent(
            RenderComponent.RenderType.RECTANGLE,
            new Vector2(size),
            color
        ));
        
        // 设置渲染器引用
        if (renderer != null) {
            render.setRenderer(renderer);
        }
        
        // 添加物理组件
        PhysicsComponent physics = bullet.addComponent(new PhysicsComponent(0.0f));
        physics.setVelocity(new Vector2(
            direction.x * speed,
            direction.y * speed
        ));
        physics.setFriction(1.0f); // 无摩擦力
        
        // 添加到场景
        scene.addGameObject(bullet);
        
        return bullet;
    }

    public static GameObject createEnemy(Vector2 position, Vector2 velocity, IRenderer renderer, Scene scene) {
        GameObject enemy = new GameObject("Enemy") {
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

        // 添加变换组件
        enemy.addComponent(new TransformComponent(position));

        // 添加渲染组件 - 改为矩形，使用橙色
        RenderComponent render = enemy.addComponent(new RenderComponent(
                RenderComponent.RenderType.RECTANGLE,
                new Vector2(20, 20),
                new RenderComponent.Color(1.0f, 0.5f, 0.0f, 1.0f) // 橙色
        ));
        render.setRenderer(renderer);

        // 添加物理组件
        PhysicsComponent physics = enemy.addComponent(new PhysicsComponent(1.0f));
        physics.setFriction(1.0f);

        physics.setVelocity(velocity);

        // 添加血量组件
        enemy.addComponent(new HealthComponent(HealthComponent.DEFAULT_HEALTH, false));

        // 添加射击组件
        EnemyShootingComponent enemyShooting = enemy.addComponent(new EnemyShootingComponent());
        enemyShooting.setRenderer(renderer);

        // 添加到场景
        scene.addGameObject(enemy);

        return enemy;
    }

    public static GameObject createDecoration(Vector2 position, IRenderer renderer, Scene scene) {
        GameObject decoration = new GameObject("Decoration") {
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
        
        // 添加变换组件
        decoration.addComponent(new TransformComponent(position));
        
        // 添加渲染组件
        RenderComponent render = decoration.addComponent(new RenderComponent(
            RenderComponent.RenderType.CIRCLE,
            new Vector2(5, 5),
            new RenderComponent.Color(0.5f, 0.5f, 1.0f, 0.8f)
        ));
        render.setRenderer(renderer);
        
        // 添加到场景
        scene.addGameObject(decoration);
        
        return decoration;
    }
}
