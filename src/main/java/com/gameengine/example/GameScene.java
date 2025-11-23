package com.gameengine.example;

import com.gameengine.scene.Scene;
import com.gameengine.core.GameEngine;
import com.gameengine.core.GameLogic;
import com.gameengine.core.GameObject;
import com.gameengine.math.Vector2;
import com.gameengine.graphics.IRenderer;
import com.gameengine.components.*;
import com.gameengine.input.InputManager;
import java.util.Random;

public class GameScene extends Scene {
    public final GameEngine engine;
    private IRenderer renderer;
    private Random random;
    private float time;
    private GameLogic gameLogic;

    public GameScene(GameEngine engine) {
        super("GameEngine");
        this.engine = engine;
    }
    
    @Override
    public void initialize() {
        super.initialize();
        this.renderer = engine.getRenderer();
        this.random = new Random();
        this.time = 0;
        this.gameLogic = new GameLogic(this);

        // 创建游戏对象
        createPlayer();
        createDecorations();
        createUIArea();

        // playBGM();
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        time += deltaTime;

        // 使用游戏逻辑类处理游戏规则
        gameLogic.handlePlayerInput();
        gameLogic.handleEnemyShooting();
        gameLogic.updatePhysics();
        gameLogic.checkCollisions();

        // 检查是否需要返回菜单
        if (gameLogic.getGameState() == GameLogic.GameState.GAME_OVER) {
            // 检测Enter键，返回菜单
            if (InputManager.getInstance().isKeyPressed(10)) { // Enter键
                returnToMenu();
            }
            return;
        }

        // 生成新敌人 - 仅在游戏进行中生成
        if (gameLogic.getGameState() == GameLogic.GameState.PLAYING && time > 2.0f) {
            createEnemy();
            time = 0;
        }
    }

    /**
     * 返回菜单场景
     */
    private void returnToMenu() {
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

    private void createEnemy() {
        // 随机位置 - 避免在UI区域生成
        Vector2 position = new Vector2(
                random.nextFloat() * (renderer.getWidth() - UIComponent.UI_WIDTH),
                random.nextFloat() * renderer.getHeight() / 2);

        // 生成随机速度和方向
        Random random = new Random();
        // 随机速度：50-150 像素/秒
        float speed = 50 + random.nextFloat() * 10;
        // 随机方向：360度随机角度
        float angle = random.nextFloat() * 2 * (float) Math.PI;
        Vector2 direction = new Vector2((float) Math.cos(angle), (float) Math.sin(angle));
        
        Vector2 velocity = new Vector2(direction.x * speed, direction.y * speed);

        // 使用 EntityFactory 创建敌人
        EntityFactory.createEnemy(position, velocity, renderer, this);
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
    
    // private void playBGM() {
    //     MusicComponent musicComponent = MusicComponent.getInstance();
        
    //     String musicPath = "resources/bgm.wav";
        
    //     boolean success = musicComponent.playBackgroundMusic(musicPath, true);
    //     if (success) {
    //         System.out.println("背景音乐开始播放");
    //     } else {
    //         System.out.println("无法播放背景音乐，请确保音乐文件存在: " + musicPath);
    //         System.out.println("音乐文件应为WAV格式，放置在项目根目录的resources文件夹中");
    //     }
    // }
    
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
