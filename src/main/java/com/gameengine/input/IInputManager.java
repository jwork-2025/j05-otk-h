package com.gameengine.input;

import java.util.Set;

/**
 * 输入管理器接口
 */
public interface IInputManager {
    void update();
    void onKeyPressed(int keyCode);
    void onKeyReleased(int keyCode);
    boolean isKeyPressed(int keyCode);
    boolean isKeyJustPressed(int keyCode);
    Set<Integer> getJustPressedKeysSnapshot();
}
