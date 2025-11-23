package com.gameengine.util;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

public class FrameProfiler {
    private static final FrameProfiler INSTANCE = new FrameProfiler();

    // 方法级性能分析
    private final Map<String, MethodProfile> methodProfiles = new LinkedHashMap<>();
    private long totalExecutionTime = 0;
    private int frameCount = 0;

    private FrameProfiler() { }

    public static FrameProfiler getInstance() {
        return INSTANCE;
    }

    /**
     * 记录一帧的总执行时间
     */
    public synchronized void recordFrameTime(long frameTime) {
        totalExecutionTime += frameTime;
        frameCount++;
    }
    
    /**
     * 开始方法性能追踪
     */
    public synchronized void startMethod(String methodName) {
        MethodProfile profile = methodProfiles.computeIfAbsent(methodName, k -> new MethodProfile(methodName));
        profile.startTime = System.nanoTime();
    }
    
    /**
     * 结束方法性能追踪
     */
    public synchronized void endMethod(String methodName) {
        MethodProfile profile = methodProfiles.get(methodName);
        if (profile != null && profile.startTime > 0) {
            long endTime = System.nanoTime();
            long duration = endTime - profile.startTime;
            
            profile.callCount++;
            profile.totalTime += duration;
            profile.startTime = 0;
        }
    }
    
    /**
     * 输出方法分析数据到CSV文件
     */
    public synchronized void outputMethodAnalysis() {
        try (PrintWriter methodWriter = new PrintWriter(new FileWriter("method_analysis.csv"))) {
            // 写入CSV头部
            methodWriter.println("method_name,call_count,total_time_ms,avg_time_ms,percentage");
            
            // 按总耗时排序
            methodProfiles.values().stream()
                .sorted((a, b) -> Long.compare(b.totalTime, a.totalTime))
                .forEach(profile -> {
                    double totalTimeMs = profile.totalTime / 1_000_000.0;
                    double avgTimeMs = profile.getAverageTime() / 1_000_000.0;
                    double percentage = profile.getPercentage(totalExecutionTime);
                    
                    String line = String.format("%s,%d,%.3f,%.3f,%.2f",
                        profile.methodName, profile.callCount, totalTimeMs, avgTimeMs, percentage);
                    methodWriter.println(line);
                });
            
            System.out.println("方法分析数据已输出到: method_analysis.csv");
            
            // 输出性能瓶颈摘要
            printPerformanceSummary();
            
        } catch (Exception e) {
            System.err.println("输出方法分析数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 打印性能摘要
     */
    private void printPerformanceSummary() {
        System.out.println("\n=== 性能分析摘要 ===");
        System.out.println("总帧数: " + frameCount);
        System.out.println("总执行时间: " + (totalExecutionTime / 1_000_000.0) + " ms");
        
        if (!methodProfiles.isEmpty()) {
            System.out.println("\n耗时最多的前5个方法:");
            methodProfiles.values().stream()
                .sorted((a, b) -> Long.compare(b.totalTime, a.totalTime))
                .limit(5)
                .forEach(profile -> {
                    double totalTimeMs = profile.totalTime / 1_000_000.0;
                    double percentage = profile.getPercentage(totalExecutionTime);
                    System.out.printf("  %s: %.3f ms (%.1f%%), 调用次数: %d, 平均耗时: %.3f ms\n",
                        profile.methodName, totalTimeMs, percentage, 
                        profile.callCount, profile.getAverageTime() / 1_000_000.0);
                });
        }
    }
    
    public synchronized void cleanup() {
        // 输出方法分析数据
        outputMethodAnalysis();
    }
    
    /**
     * 方法性能分析结构
     */
    private static class MethodProfile {
        public String methodName;
        public long callCount;
        public long totalTime;
        public long startTime;
        
        public MethodProfile(String methodName) {
            this.methodName = methodName;
            this.callCount = 0;
            this.totalTime = 0;
            this.startTime = 0;
        }
        
        public double getAverageTime() {
            return callCount > 0 ? (double)totalTime / callCount : 0;
        }
        
        public double getPercentage(long totalExecutionTime) {
            return totalExecutionTime > 0 ? (totalTime * 100.0) / totalExecutionTime : 0;
        }
    }
}
