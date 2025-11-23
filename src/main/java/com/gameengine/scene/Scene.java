package com.gameengine.scene;

import com.gameengine.core.GameObject;
import com.gameengine.core.Component;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Collectors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 场景类，管理游戏对象和组件
 */
public class Scene {
    private String name;
    private List<GameObject> gameObjects;
    private List<GameObject> objectsToAdd;
    private List<GameObject> objectsToRemove;
    private boolean initialized;

    private ExecutorService executor;

    public Scene(String name) {
        this.name = name;
        this.gameObjects = new ArrayList<>();
        this.objectsToAdd = new ArrayList<>();
        this.objectsToRemove = new ArrayList<>();
        this.initialized = false;

        int threadCount = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
        this.executor = Executors.newFixedThreadPool(threadCount);
    }

    /**
     * 初始化场景
     */
    public void initialize() {
        for (GameObject obj : gameObjects) {
            obj.initialize();
        }
        initialized = true;
    }

    /**
     * 更新场景
     */
    public void update(float deltaTime) {
        for (GameObject obj : objectsToAdd) {
            gameObjects.add(obj);
            if (initialized) {
                obj.initialize();
            }
        }
        objectsToAdd.clear();

        // 移除标记的对象
        for (GameObject obj : objectsToRemove) {
            gameObjects.remove(obj);
        }
        objectsToRemove.clear();

        // 更新所有活跃的游戏对象
        Iterator<GameObject> iterator = gameObjects.iterator();
        while (iterator.hasNext()) {
            GameObject obj = iterator.next();
            if (obj.isActive()) {
                obj.update(deltaTime);
            } else {
                iterator.remove();
            }
        }
    }

    /**
     * 渲染场景
     */
    public void render() {
        List<GameObject> aliveObjects = gameObjects.stream()
            .filter(obj -> obj != null && obj.isActive() && !obj.getName().equals("UIArea"))
            .collect(Collectors.toList());
        
        List<GameObject> UIObjects = gameObjects.stream()
            .filter(obj -> obj != null && obj.isActive() && obj.getName().equals("UIArea"))
            .collect(Collectors.toList());
            
        for (GameObject obj : UIObjects) {
            obj.render();
        }

        // Serial
        for (GameObject obj : aliveObjects) {
            obj.render();
        }

        // // Parallel
        // if (aliveObjects.size() < 100) {
        //     for (GameObject obj : aliveObjects) {
        //         obj.render();
        //     }
        //     return;
        // }

        // int threadCount = Runtime.getRuntime().availableProcessors() - 1;
        // threadCount = Math.max(2, threadCount);
        // int batchSize = Math.max(1, aliveObjects.size() / threadCount + 1);
        // List<Future<?>> futures = new ArrayList<>();

        // for (int i = 0; i < aliveObjects.size(); i += batchSize) {
        //     final int start = i;
        //     final int end = Math.min(i + batchSize, aliveObjects.size());

        //     Future<?> future = executor.submit(() -> {
        //         for (int j = start; j < end; j++) {
        //             aliveObjects.get(j).render();
        //         }
        //     });

        //     futures.add(future);
        // }

        // for (Future<?> future : futures) {
        //     try {
        //         future.get();
        //     } catch (Exception e) {
        //         e.printStackTrace();
        //     }
        // }

    }

    /**
     * 添加游戏对象到场景
     */
    public void addGameObject(GameObject gameObject) {
        objectsToAdd.add(gameObject);
    }

    /**
     * 根据组件类型查找游戏对象
     */
    public <T extends Component<T>> List<GameObject> findGameObjectsByComponent(Class<T> componentType) {
        return gameObjects.stream()
                .filter(obj -> obj.hasComponent(componentType))
                .collect(Collectors.toList());
    }

    /**
     * 获取所有具有指定组件的游戏对象
     */
    public <T extends Component<T>> List<T> getComponents(Class<T> componentType) {
        return findGameObjectsByComponent(componentType).stream()
                .map(obj -> obj.getComponent(componentType))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 清空场景
     */
    public void clear() {
        gameObjects.clear();
        objectsToAdd.clear();
        objectsToRemove.clear();
    }

    /**
     * 获取场景名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取所有游戏对象
     */
    public List<GameObject> getGameObjects() {
        return new ArrayList<>(gameObjects);
    }

}
