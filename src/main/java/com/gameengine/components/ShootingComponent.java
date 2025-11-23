package com.gameengine.components;

import com.gameengine.graphics.IRenderer;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;

/**
 * 射击组件接口，定义射击相关操作
 */
public interface ShootingComponent {
    
    /**
     * 发射子弹（使用默认方向）
     * @param scene 场景引用，用于添加子弹
     * @return 是否成功发射
     */
    boolean shoot(Scene scene);
    
    /**
     * 发射子弹（指定方向）
     * @param scene 场景引用，用于添加子弹
     * @param direction 射击方向
     * @return 是否成功发射
     */
    boolean shoot(Scene scene, Vector2 direction);
    
    /**
     * 检查是否可以射击
     * @return 是否可以射击
     */
    boolean canShoot();
    
    /**
     * 设置渲染器引用
     * @param renderer 渲染器
     */
    void setRenderer(IRenderer renderer);
    
    /**
     * 获取剩余冷却时间
     * @return 剩余冷却时间
     */
    float getRemainingCooldown();
    
    /**
     * 更新组件状态
     * @param deltaTime 时间增量
     */
    void update(float deltaTime);
}
