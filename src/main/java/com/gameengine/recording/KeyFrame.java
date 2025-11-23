package com.gameengine.recording;

import com.gameengine.math.Vector2;

import java.util.List;

public class KeyFrame {
    public double timestamp;
    public List<EnemyInfo> enemyInfos;
    public List<PlayerInfo> playerInfos;
    
    public static class EnemyInfo {
        public int     enemyId;
        public Vector2 position;
        public Vector2 velocity;
    }

    public static class PlayerInfo {
        public int score;
        public int health;
    }
}
