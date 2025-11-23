package com.gameengine.components;

import com.gameengine.core.Component;
import com.gameengine.example.Game;
import com.gameengine.graphics.IRenderer;
import com.gameengine.math.Vector2;

/**
 * 分数显示组件，负责在屏幕上显示分数
 */
public class UIComponent extends Component<UIComponent> {
    public static final int UI_WIDTH = 200; // 固定宽度，但位置动态计算
    public static final int UI_LENGTH = Game.WINDOW_HEIGHT; // UI区域高度等于窗口高度
    public static int UI_X = 0; // 将在构造函数中动态计算
    public static final int UI_Y = 0;

    private static UIComponent instance = null;
    
    private IRenderer renderer;
    private Vector2 position;
    private ScoreComponent scoreComponent;
    
    private UIComponent() {
        // 动态计算UI区域位置
        UI_X = Game.WINDOW_WIDTH - UI_WIDTH;
        this.position = new Vector2(UI_X + 10, 50); // 在UI区域内定位
        this.scoreComponent = ScoreComponent.getInstance();
    }

    public static UIComponent getInstance() {
        if (instance == null) {
            instance = new UIComponent();
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
        if (renderer == null) return;
        
        // 动态计算UI区域位置
        UI_X = renderer.getWidth() - UI_WIDTH;
        
        renderer.drawRect(UI_X, UI_Y, UI_WIDTH, UI_LENGTH, 0.1f, 0.1f, 0.1f, 1.0f);
        renderer.drawRect(0, 0, renderer.getWidth(), 10, 0.1f, 0.1f, 0.1f, 1.0f);
        renderer.drawRect(0, 0, 10, renderer.getHeight(), 0.1f, 0.1f, 0.1f, 1.0f);
        renderer.drawRect(0, renderer.getHeight() - 10, renderer.getWidth(), 10, 0.1f, 0.1f, 0.1f, 1.0f);
        
        // 动态计算UI元素位置
        float scoreBoxX = UI_X + 10;
        float scoreBoxY = 50;
        renderer.drawRect(scoreBoxX, scoreBoxY - 25, 180, 150, 0.2f, 0.2f, 0.2f, 0.8f);
        
        String scoreText = "Score: " + scoreComponent.getScore();
        String gameText = "JAVA: STG";
        renderer.drawText(scoreBoxX + 5, scoreBoxY - 20, scoreText, 1.0f, 1.0f, 1.0f, 1.0f);

        float gameTextY = renderer.getHeight() - 50;
        renderer.drawText(scoreBoxX + 5, gameTextY, gameText, 1.0f, 1.0f, 1.0f, 1.0f);
        
        // 绘制生命值圆圈
        drawHealthCircles(scoreBoxX + 5, scoreBoxY + 50);
    }
    
    /**
     * 设置渲染器
     */
    public void setRenderer(IRenderer renderer) {
        this.renderer = renderer;
    }
    
    /**
     * 获取位置
     */
    public Vector2 getPosition() {
        return new Vector2(position);
    }
    
    /**
     * 设置位置
     */
    public void setPosition(Vector2 position) {
        this.position = new Vector2(position);
    }
    
    /**
     * 绘制生命值圆圈
     */
    private void drawHealthCircles(float startX, float startY) {
        int currentHealth = scoreComponent.getCurrentHealth();
        int maxHealth = scoreComponent.getMaxHealth();
        
        // 绘制生命值标题
        renderer.drawText(startX, startY - 30, "Health:", 1.0f, 1.0f, 1.0f, 1.0f);
        
        // 绘制生命值圆圈
        float circleRadius = 8.0f;
        float circleSpacing = 20.0f;
        
        for (int i = 0; i < maxHealth; i++) {
            float x = startX + i * circleSpacing + 10;
            float y = startY + circleSpacing + 5;
            
            if (i < currentHealth) {
                // 满生命值 - 绿色圆圈
                renderer.drawCircle(x, y, circleRadius, 16, 0.0f, 1.0f, 0.0f, 1.0f);
            } else {
                // 空生命值 - 红色圆圈
                renderer.drawCircle(x, y, circleRadius, 16, 1.0f, 0.0f, 0.0f, 0.3f);
            }
            
            // 圆圈边框
            renderer.drawCircle(x, y, circleRadius, 16, 1.0f, 1.0f, 1.0f, 0.5f);
        }
    }
}
