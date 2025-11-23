package com.gameengine.components;

import com.gameengine.core.Component;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

/**
 * 音乐组件，用于游戏全局播放音乐
 */
public class MusicComponent extends Component<MusicComponent> {
    private static MusicComponent instance = null;
    
    private Clip backgroundMusic;
    private boolean isPlaying;
    private float volume;
    
    private MusicComponent() {
        this.isPlaying = false;
        this.volume = 0.1f;
    }
    
    public static MusicComponent getInstance() {
        if (instance == null) {
            instance = new MusicComponent();
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
    }
    
    /**
     * 加载并播放背景音乐
     * @param filePath 音乐文件路径
     * @param loop 是否循环播放
     * @return 是否成功播放
     */
    public boolean playBackgroundMusic(String filePath, boolean loop) {
        if (isPlaying) {
            stopMusic();
        }
        
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(new File(filePath));
            AudioFormat format = audioStream.getFormat();
            
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            
            backgroundMusic = (Clip) AudioSystem.getLine(info);
            backgroundMusic.open(audioStream);
            
            if (loop) {
                backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
            }
            
            setVolume(volume);
            
            backgroundMusic.start();
            isPlaying = true;
            
            return true;
            
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("无法播放音乐文件: " + filePath);
            System.err.println("错误信息: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 暂停音乐播放
     */
    public void pauseMusic() {
        if (backgroundMusic != null && isPlaying) {
            backgroundMusic.stop();
            isPlaying = false;
        }
    }
    
    /**
     * 恢复音乐播放
     */
    public void resumeMusic() {
        if (backgroundMusic != null && !isPlaying) {
            backgroundMusic.start();
            isPlaying = true;
        }
    }
    
    /**
     * 停止音乐播放
     */
    public void stopMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.stop();
            backgroundMusic.close();
            isPlaying = false;
        }
    }
    
    /**
     * 设置音量
     * @param volume 音量值 (0.0 - 1.0)
     */
    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, volume));
        
        if (backgroundMusic != null && backgroundMusic.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gainControl = (FloatControl) backgroundMusic.getControl(FloatControl.Type.MASTER_GAIN);
            
            // 将0.0-1.0的线性音量转换为分贝值
            // 分贝范围：-80.0f (静音) 到 6.0206f (最大)
            float dB = (float) (Math.log(volume) / Math.log(10.0) * 20.0);
            gainControl.setValue(dB);
        }
    }
    
    /**
     * 获取当前音量
     */
    public float getVolume() {
        return volume;
    }
    
    /**
     * 检查是否正在播放音乐
     */
    public boolean isPlaying() {
        return isPlaying;
    }
    
    /**
     * 清理音乐资源
     */
    public void cleanup() {
        stopMusic();
        if (backgroundMusic != null) {
            backgroundMusic.close();
            backgroundMusic = null;
        }
    }
    
}
