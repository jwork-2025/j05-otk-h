package com.gameengine.core;

import com.gameengine.components.TransformComponent;
import com.gameengine.components.PhysicsComponent;
import com.gameengine.components.HealthComponent;
import com.gameengine.components.EnemyShootingComponent;
import com.gameengine.components.PlayerShootingComponent;
import com.gameengine.components.ScoreComponent;
import com.gameengine.components.UIComponent;
import com.gameengine.example.Game;
import com.gameengine.input.IInputManager;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * 游戏逻辑类，处理具体的游戏规则
 */
public class GameLogic {
    private ExecutorService executor;
    // private final static int Parallel_Threshhold = 100;

    public static enum GameState {
        PLAYING,
        GAME_OVER
    }

    private Scene scene;
    private IInputManager inputManager;
    private GameState gameState;
    
    public GameLogic(Scene scene) {
        this.scene = scene;
        this.inputManager = InputManager.getInstance();
        this.gameState = GameState.PLAYING;
        int threadCount = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
        this.executor = Executors.newFixedThreadPool(threadCount);
    }
    
    /**
     * 设置自定义输入管理器（用于回放）
     */
    public void setInputManager(IInputManager inputManager) {
        this.inputManager = inputManager;
    }

    /**
     * 停止所有游戏对象（敌人、子弹等）
     */
    private void stopAllGameObjects() {
        for (GameObject obj : scene.getGameObjects()) {
            String name = obj.getName();
            // 停止敌人和子弹
            if ("Enemy".equals(name) || "PlayerBullet".equals(name) || "EnemyBullet".equals(name)) {
                // 停止物理运动
                PhysicsComponent physics = obj.getComponent(PhysicsComponent.class);
                if (physics != null) {
                    physics.setVelocity(new Vector2(0, 0));
                }
                // 停止射击组件 - 分别检查玩家和敌人射击组件
                PlayerShootingComponent playerShooting = obj.getComponent(PlayerShootingComponent.class);
                if (playerShooting != null) {
                    playerShooting.setShootCooldown(Float.MAX_VALUE);
                }
                EnemyShootingComponent enemyShooting = obj.getComponent(EnemyShootingComponent.class);
                if (enemyShooting != null) {
                    enemyShooting.setShootCooldown(Float.MAX_VALUE);
                }
            }
        }
    }
    
    /**
     * 处理玩家输入
     */
    public void handlePlayerInput() {
        // 处理游戏结束状态的输入
        if (gameState == GameState.GAME_OVER) {
            // 检测Enter键，返回菜单
            if (inputManager.isKeyPressed(10)) { // Enter键
                // 返回菜单的逻辑将在GameScene中处理
            }
            return;
        }
        
        // 正常游戏状态的处理
        List<GameObject> players = scene.findGameObjectsByComponent(TransformComponent.class);
        if (players.isEmpty()) return;
        
        GameObject player = players.get(0);
        TransformComponent transform = player.getComponent(TransformComponent.class);
        PhysicsComponent physics = player.getComponent(PhysicsComponent.class);
        PlayerShootingComponent shooting = player.getComponent(PlayerShootingComponent.class);
        
        if (transform == null || physics == null) return;
        
        Vector2 movement = new Vector2();
        
        if (inputManager.isKeyPressed(87) || inputManager.isKeyPressed(38)) { // W或上箭头
            movement.y -= 1;
        }
        if (inputManager.isKeyPressed(83) || inputManager.isKeyPressed(40)) { // S或下箭头
            movement.y += 1;
        }
        if (inputManager.isKeyPressed(65) || inputManager.isKeyPressed(37)) { // A或左箭头
            movement.x -= 1;
        }
        if (inputManager.isKeyPressed(68) || inputManager.isKeyPressed(39)) { // D或右箭头
            movement.x += 1;
        }
        
        if (movement.magnitude() > 0) {
            movement = movement.normalize().multiply(200);
            physics.setVelocity(movement);
        }
        
        // 处理射击
        if (shooting != null && inputManager.isKeyPressed(90)) { // Z键
            shooting.shoot(scene);
        }
        
        // 边界检查 - 动态计算游戏区域边界
        Vector2 pos = transform.getPosition();
        if (pos.x < 0) pos.x = 0;
        if (pos.y < 0) pos.y = 0;
        // 游戏区域右边界 = 窗口宽度 - UI区域宽度 - 安全边距
        float gameAreaRight = Game.WINDOW_WIDTH - UIComponent.UI_WIDTH - 20;
        if (pos.x > gameAreaRight) pos.x = gameAreaRight;
        if (pos.y > Game.WINDOW_HEIGHT - 20) pos.y = Game.WINDOW_HEIGHT - 20;
        transform.setPosition(pos);
    }
    
    /**
     * 更新物理系统
     */
    public void updatePhysics() {
        List<PhysicsComponent> physicsComponents = scene.getComponents(PhysicsComponent.class);
        for (PhysicsComponent physics : physicsComponents) {
            TransformComponent transform = physics.getOwner().getComponent(TransformComponent.class);
            if (transform != null) {
                Vector2 pos = transform.getPosition();
                Vector2 velocity = physics.getVelocity();
                
                // 检查对象类型
                String objectName = physics.getOwner().getName();
                boolean isBullet = "PlayerBullet".equals(objectName) || "EnemyBullet".equals(objectName);
                
                if (isBullet) {
                    // 子弹碰到边界时消失
                    float gameAreaRight = Game.WINDOW_WIDTH - UIComponent.UI_WIDTH;
                    if (pos.x <= 20 || pos.x >= gameAreaRight - 20 || pos.y <= 20 || pos.y >= Game.WINDOW_HEIGHT - 20) {
                        physics.getOwner().setActive(false);
                        continue;
                    }
                } else {
                    // 其他对象进行边界反弹
                    float gameAreaRight = Game.WINDOW_WIDTH - UIComponent.UI_WIDTH - 15;
                    if (pos.x <= 0 || pos.x >= gameAreaRight) {
                        velocity.x = -velocity.x;
                        physics.setVelocity(velocity);
                    }
                    if (pos.y <= 0 || pos.y >= Game.WINDOW_HEIGHT - 15) {
                        velocity.y = -velocity.y;
                        physics.setVelocity(velocity);
                    }
                    
                    // 确保在边界内
                    if (pos.x < 0) pos.x = 0;
                    if (pos.y < 0) pos.y = 0;
                    if (pos.x > gameAreaRight) pos.x = gameAreaRight;
                    if (pos.y > Game.WINDOW_HEIGHT - 15) pos.y = Game.WINDOW_HEIGHT - 15;
                    transform.setPosition(pos);
                }
            }
        }
    }
    
    /**
     * 处理敌人射击
     */
    public void handleEnemyShooting() {
        if (gameState != GameState.PLAYING) return;
        
        List<GameObject> players = scene.getGameObjects().stream()
            .filter(obj -> obj.getName().equals("Player"))
            .collect(Collectors.toList());
        
        List<GameObject> enemies = scene.getGameObjects().stream()
            .filter(obj -> obj.getName().equals("Enemy"))
            .collect(Collectors.toList());
            
        if (players.isEmpty() || enemies.isEmpty()) return;
        
        GameObject player = players.get(0);
        TransformComponent playerTransform = player.getComponent(TransformComponent.class);
        if (playerTransform == null) return;
        
        Vector2 playerPosition = playerTransform.getPosition();
        
        // 处理每个敌人的射击
        for (GameObject enemy : enemies) {
            EnemyShootingComponent enemyShooting = enemy.getComponent(EnemyShootingComponent.class);
            if (enemyShooting != null && enemyShooting.canShoot()) {
                // 计算从敌人指向玩家的方向
                TransformComponent enemyTransform = enemy.getComponent(TransformComponent.class);
                if (enemyTransform != null) {
                    Vector2 enemyPos = enemyTransform.getPosition();
                    Vector2 direction = new Vector2(
                        playerPosition.x - enemyPos.x,
                        playerPosition.y - enemyPos.y
                    );
                    direction = direction.normalize();
                    enemyShooting.shoot(scene, direction);
                }
            }
        }
    }
    
    /**
     * 检查碰撞
     */
    public void checkCollisions() {
        List<GameObject> players = scene.getGameObjects().stream()
            .filter(obj -> obj.getName().equals("Player"))
            .collect(Collectors.toList());
        
        List<GameObject> enemies = scene.getGameObjects().stream()
            .filter(obj -> obj.getName().equals("Enemy"))
            .collect(Collectors.toList());
            
        List<GameObject> playerBullets = scene.getGameObjects().stream()
            .filter(obj -> obj.getName().equals("PlayerBullet"))
            .collect(java.util.stream.Collectors.toList());
            
        List<GameObject> enemyBullets = scene.getGameObjects().stream()
            .filter(obj -> obj.getName().equals("EnemyBullet"))
            .collect(java.util.stream.Collectors.toList());

        if (players.isEmpty()) return;
        
        checkPlayerEnemyCollisions_Serial(players, enemies);
        checkPlayerBulletEnemyCollisions_Serial(playerBullets, enemies);
        checkEnemyBulletPlayerCollisions_Serial(enemyBullets, players);

        // if (enemies.size() < Parallel_Threshhold) {
        //     checkPlayerEnemyCollisions_Serial(players, enemies);
        // } else {
        //     checkPlayerEnemyCollisions_Parallel(players, enemies);
        // }
        
        // if (bullets.size() < Parallel_Threshhold && enemies.size() < Parallel_Threshhold) {
        //     checkBulletEnemyCollisions_Serial(bullets, enemies);
        // } else {
        //     checkBulletEnemyCollisions_Parallel(bullets, enemies);
        // }
    }

    private void checkPlayerEnemyCollisions_Serial(List<GameObject> players, List<GameObject> enemies) {
        for (GameObject enemy : enemies) {
            TransformComponent enemyTransform = enemy.getComponent(TransformComponent.class);
            HealthComponent enemyHealth = enemy.getComponent(HealthComponent.class);
            if (enemyTransform == null || enemyHealth == null) {
                continue;
            }

            for (GameObject player : players) {
                TransformComponent playerTransform = player.getComponent(TransformComponent.class);
                HealthComponent playerHealth = player.getComponent(HealthComponent.class);
                if (playerTransform == null || playerHealth == null) {
                    continue;
                }

                float distance = playerTransform.getPosition().distance(enemyTransform.getPosition());
                if (distance > 25) {
                    continue;
                }

                playerHealth.takeDamage(1);
                enemyHealth.takeDamage(enemyHealth.getMaxHealth());

                ScoreComponent scoreComponent = ScoreComponent.getInstance();
                scoreComponent.setCurrentHealth(playerHealth.getCurrentHealth());

                if (playerHealth.isAlive()) {
                    continue;
                }

                gameState = GameState.GAME_OVER;
                stopAllGameObjects();

                break;
            }
        }
    }

    private void checkPlayerEnemyCollisions_Parallel(List<GameObject> players, List<GameObject> enemies) {
        int threadCount = Runtime.getRuntime().availableProcessors() - 1;
        threadCount = Math.max(2, threadCount);
        int batchSize = Math.max(1, enemies.size() / threadCount + 1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < enemies.size(); i += batchSize) {
            final int start = i;
            final int end = Math.min(i + batchSize, enemies.size());
            
            Future<?> future = executor.submit(() -> {
                for (int j = start; j < end; j++) {
                    GameObject enemy = enemies.get(j);

                    TransformComponent enemyTransform = enemy.getComponent(TransformComponent.class);
                    HealthComponent enemyHealth = enemy.getComponent(HealthComponent.class);
                    if (enemyTransform == null || enemyHealth == null) { continue; }

                    for (GameObject player : players) {
                        TransformComponent playerTransform = player.getComponent(TransformComponent.class);
                        HealthComponent playerHealth = player.getComponent(HealthComponent.class);
                        if (playerTransform == null || playerHealth == null) { continue; }

                        float distance = playerTransform.getPosition().distance(enemyTransform.getPosition());
                        if (distance > 25) { continue; }
                        
                        playerHealth.takeDamage(1);
                        enemyHealth.takeDamage(enemyHealth.getMaxHealth());

                        ScoreComponent scoreComponent = ScoreComponent.getInstance();
                        scoreComponent.setCurrentHealth(playerHealth.getCurrentHealth());

                        if (playerHealth.isAlive()) { continue; }
                        
                        gameState = GameState.GAME_OVER;
                        // playerTransform.setPosition(new Vector2(400, 300));
                        // playerHealth.revive();
                        // scoreComponent.setCurrentHealth(playerHealth.getCurrentHealth());
                        // scoreComponent.setScore(0);
                        
                        break;
                    }
                }
            });
            
            futures.add(future);
        }
        
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 检查玩家子弹与敌人的碰撞
     */
    private void checkPlayerBulletEnemyCollisions_Serial(List<GameObject> playerBullets, List<GameObject> enemies) {
        for (GameObject bullet : playerBullets) {
            TransformComponent bulletTransform = bullet.getComponent(TransformComponent.class);
            if (bulletTransform == null)
                continue;

            for (GameObject enemy : enemies) {
                TransformComponent enemyTransform = enemy.getComponent(TransformComponent.class);
                HealthComponent enemyHealth = enemy.getComponent(HealthComponent.class);
                if (enemyTransform == null || enemyHealth == null) {
                    continue;
                }

                float distance = bulletTransform.getPosition().distance(enemyTransform.getPosition());
                if (distance > 25) {
                    continue;
                }

                enemyHealth.takeDamage(1);
                bullet.setActive(false);

                if (!enemyHealth.isAlive()) {
                    ScoreComponent scoreComponent = ScoreComponent.getInstance();
                    scoreComponent.addScore(1);
                }
                break;
            }
        }
    }
    
    /**
     * 检查敌人子弹与玩家的碰撞
     */
    private void checkEnemyBulletPlayerCollisions_Serial(List<GameObject> enemyBullets, List<GameObject> players) {
        for (GameObject bullet : enemyBullets) {
            TransformComponent bulletTransform = bullet.getComponent(TransformComponent.class);
            if (bulletTransform == null)
                continue;

            for (GameObject player : players) {
                TransformComponent playerTransform = player.getComponent(TransformComponent.class);
                HealthComponent playerHealth = player.getComponent(HealthComponent.class);
                if (playerTransform == null || playerHealth == null) {
                    continue;
                }

                float distance = bulletTransform.getPosition().distance(playerTransform.getPosition());
                if (distance > 25) {
                    continue;
                }

                // 敌人子弹击中玩家，玩家生命值减1
                playerHealth.takeDamage(1);
                bullet.setActive(false);

                ScoreComponent scoreComponent = ScoreComponent.getInstance();
                scoreComponent.setCurrentHealth(playerHealth.getCurrentHealth());

                if (!playerHealth.isAlive()) {
                    gameState = GameState.GAME_OVER;
                    stopAllGameObjects(); // 停止所有敌人和子弹
                }
                break;
            }
        }
    }
    
    private void checkBulletEnemyCollisions_Parallel(List<GameObject> bullets, List<GameObject> enemies) {
        int threadCount = Runtime.getRuntime().availableProcessors() - 1;
        threadCount = Math.max(2, threadCount);
        List<Future<?>> futures = new ArrayList<>();

        if (bullets.size() > enemies.size()) {
            int batchSize = Math.max(1, bullets.size() / threadCount + 1);
            for (int i = 0; i < bullets.size(); i += batchSize) {
                final int start = i;
                final int end = Math.min(i + batchSize, enemies.size());
                
                Future<?> future = executor.submit(() -> {
                    for (int j = start; j < end; j++) {
                        GameObject bullet = bullets.get(j);
                        TransformComponent bulletTransform = bullet.getComponent(TransformComponent.class);
                        if (bulletTransform == null) { continue; }
                        
                        for (GameObject enemy : enemies) {
                            TransformComponent enemyTransform = enemy.getComponent(TransformComponent.class);
                            HealthComponent enemyHealth = enemy.getComponent(HealthComponent.class);
                            if (enemyTransform == null || enemyHealth == null) { continue; }
                            
                            float distance = bulletTransform.getPosition().distance(enemyTransform.getPosition());
                            if (distance > 25) { continue; }
                                    
                            enemyHealth.takeDamage(1);
                            bullet.setActive(false);
                            
                            if (!enemyHealth.isAlive()) {
                                ScoreComponent scoreComponent = ScoreComponent.getInstance();
                                scoreComponent.addScore(1);
                            }
                            break;
                        }
                    }
                });
                
                futures.add(future);
            }
        } else {
            int batchSize = Math.max(1, enemies.size() / threadCount + 1);
            for (int i = 0; i < enemies.size(); i += batchSize) {
                final int start = i;
                final int end = Math.min(i + batchSize, enemies.size());
                
                Future<?> future = executor.submit(() -> {
                    for (int j = start; j < end; j++) {
                        GameObject enemy = enemies.get(j);
                        TransformComponent enemyTransform = enemy.getComponent(TransformComponent.class);
                        HealthComponent enemyHealth = enemy.getComponent(HealthComponent.class);
                        if (enemyTransform == null || enemyHealth == null) { continue; }
                        
                        
                        for (GameObject bullet : bullets) {
                            TransformComponent bulletTransform = bullet.getComponent(TransformComponent.class);
                            if (bulletTransform == null) { continue; }
                            
                            float distance = bulletTransform.getPosition().distance(enemyTransform.getPosition());
                            if (distance > 25) { continue; }
                                    
                            enemyHealth.takeDamage(1);
                            bullet.setActive(false);
                            
                            if (!enemyHealth.isAlive()) {
                                ScoreComponent scoreComponent = ScoreComponent.getInstance();
                                scoreComponent.addScore(1);
                            }
                            break;
                        }
                    }
                });
                
                futures.add(future);
            }
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
    
    /**
     * 获取当前游戏状态
     */
    public GameState getGameState() {
        return gameState;
    }
    
    /**
     * 设置游戏状态
     */
    public void setGameState(GameState state) {
        this.gameState = state;
    }
    
    // /**
    //  * 重置游戏
    //  */
    // private void resetGame() {
    //     // 重置玩家
    //     List<GameObject> players = scene.findGameObjectsByComponent(TransformComponent.class);
    //     if (!players.isEmpty()) {
    //         GameObject player = players.get(0);
    //         TransformComponent transform = player.getComponent(TransformComponent.class);
    //         HealthComponent health = player.getComponent(HealthComponent.class);
            
    // //         health.revive();

    //         if (transform != null) {
    //             transform.setPosition(new Vector2(400, 300));
    //         }
    //         if (health != null) {
    //             health.revive();
    //         }
    //     }
        
    //     ScoreComponent scoreComponent = ScoreComponent.getInstance();
    //     scoreComponent.setScore(0);
    //     scoreComponent.setCurrentHealth(HealthComponent.DEFAULT_HEALTH);
    // }
    
}
