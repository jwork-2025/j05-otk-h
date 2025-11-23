package com.gameengine.example;

import com.gameengine.core.GameEngine;
import com.gameengine.graphics.RenderBackend;

public class Game {
    public static final int WINDOW_WIDTH = 1024;
    public static final int WINDOW_HEIGHT = 768;

    public static void main(String[] args) {
        System.out.println("启动游戏引擎...");

        GameEngine engine = null;
        try {
            System.out.println("使用渲染后端: GPU");
            engine = new GameEngine(WINDOW_WIDTH, WINDOW_HEIGHT, "游戏引擎", RenderBackend.GPU);

            MenuScene menuScene = new MenuScene(engine, "MainMenu");
            engine.setScene(menuScene);
            engine.run();
        } catch (Exception e) {
            System.err.println("游戏运行出错: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (engine != null) {
                engine.cleanup();
            }
        }

        System.out.println("游戏结束");
    }
}
