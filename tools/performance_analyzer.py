#!/usr/bin/env python3
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import numpy as np
from datetime import datetime
import os
import sys

class PerformanceAnalyzer:
    def __init__(self):
        self.method_data = None
        
    def load_data(self, method_file="method_analysis.csv"):
        """加载方法分析数据文件"""
        try:
            # 加载方法分析数据
            if os.path.exists(method_file):
                self.method_data = pd.read_csv(method_file)
                print(f"✓ 已加载方法分析数据: {method_file} ({len(self.method_data)} 个方法)")
            else:
                print(f"✗ 方法分析数据文件不存在: {method_file}")
                
        except Exception as e:
            print(f"加载数据时出错: {e}")
            
    def generate_summary_report(self):
        """生成性能摘要报告"""
        if self.method_data is None:
            print("没有性能数据可分析")
            return
            
        print("\n" + "="*60)
        print("           游戏性能分析报告")
        print("="*60)
        
        # 基础统计
        print(f"\n📊 基础统计:")
        print(f"   总方法数: {len(self.method_data)}")
        print(f"   总调用次数: {self.method_data['call_count'].sum()}")
        print(f"   总执行时间: {self.method_data['total_time_ms'].sum():.3f} ms")
        
    def generate_method_analysis(self):
        """生成方法级性能分析"""
        if self.method_data is None:
            print("没有方法分析数据")
            return
            
        print(f"\n🔧 方法级性能分析:")
        print(f"   总方法数: {len(self.method_data)}")
        
        # 显示耗时最多的前10个方法
        top_methods = self.method_data.nlargest(10, 'total_time_ms')
        print(f"\n🏆 耗时最多的前10个方法:")
        for i, (_, row) in enumerate(top_methods.iterrows(), 1):
            print(f"   {i:2d}. {row['method_name']:30s} {row['total_time_ms']:8.3f} ms "
                  f"({row['percentage']:5.1f}%) 调用: {row['call_count']:4d} 次")
                  
        # 显示平均耗时最多的前10个方法
        avg_top_methods = self.method_data.nlargest(10, 'avg_time_ms')
        print(f"\n⏱️  平均耗时最多的前10个方法:")
        for i, (_, row) in enumerate(avg_top_methods.iterrows(), 1):
            print(f"   {i:2d}. {row['method_name']:30s} {row['avg_time_ms']:8.3f} ms "
                  f"调用: {row['call_count']:4d} 次")

    def identify_bottlenecks(self):
        """识别性能瓶颈"""
        if self.method_data is None:
            return
            
        print(f"\n🔍 性能瓶颈识别:")
        
        # 方法级瓶颈
        bottleneck_methods = self.method_data[self.method_data['percentage'] > 10]  # 耗时超过10%的方法
        if len(bottleneck_methods) > 0:
            print(f"   🔧 耗时占比超过10%的方法:")
            for _, row in bottleneck_methods.iterrows():
                print(f"      {row['method_name']}: {row['percentage']:.1f}%")
                
        # 高平均耗时方法
        high_avg_time_methods = self.method_data[self.method_data['avg_time_ms'] > 1.0]  # 平均耗时超过1ms的方法
        if len(high_avg_time_methods) > 0:
            print(f"\n   ⚠️  平均耗时超过1ms的方法:")
            for _, row in high_avg_time_methods.iterrows():
                print(f"      {row['method_name']}: {row['avg_time_ms']:.3f} ms")
                    
    def export_report(self):
        """导出详细报告到文件"""
        report_file = f"performance_report.txt"
        
        with open(report_file, 'w', encoding='utf-8') as f:
            f.write("游戏性能分析报告\n")
            f.write("=" * 50 + "\n\n")
            
            if self.method_data is not None:
                f.write("方法性能分析:\n")
                for _, row in self.method_data.iterrows():
                    f.write(f"  {row['method_name']}: {row['total_time_ms']:.3f} ms "
                           f"({row['percentage']:.1f}%), 调用次数: {row['call_count']}, "
                           f"平均耗时: {row['avg_time_ms']:.3f} ms\n")
                           
        print(f"\n📄 详细报告已导出为: {report_file}")

def main():
    """主函数"""
    analyzer = PerformanceAnalyzer()
    
    # 检查命令行参数
    method_file = "method_analysis.csv"
    
    if len(sys.argv) > 1:
        method_file = sys.argv[1]
        
    print("🎮 游戏性能分析器启动...")
    print(f"   方法分析文件: {method_file}")
    
    # 加载数据
    analyzer.load_data(method_file)
    
    # 生成分析报告
    analyzer.generate_summary_report()
    analyzer.generate_method_analysis()
    analyzer.identify_bottlenecks()
    
    # 导出报告
    analyzer.export_report()
    
    print(f"\n✅ 分析完成！")

if __name__ == "__main__":
    main()
