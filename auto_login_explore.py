#!/usr/bin/env python3
"""
OpsAny 平台自动登录并遍历所有功能模块
"""

import os
import json
import time
from playwright.sync_api import sync_playwright

# 登录凭据
USERNAME = "demo"
PASSWORD = "123456.coM"
LOGIN_URL = "https://demo.opsany.com/o/workbench/?login=1#/"

# 输出目录
OUTPUT_DIR = r"D:\自建agent\运维平台\auto_explore"
SCREENSHOTS_DIR = os.path.join(OUTPUT_DIR, "screenshots")
os.makedirs(SCREENSHOTS_DIR, exist_ok=True)

def login_and_explore():
    """登录并遍历所有功能模块"""
    
    results = []
    
    with sync_playwright() as p:
        # 启动浏览器（headless=False 可看到浏览器界面）
        browser = p.chromium.launch(headless=False, slow_mo=500)
        context = browser.new_context(viewport={"width": 1920, "height": 1080})
        page = context.new_page()
        
        print("=" * 60)
        print("开始登录 OpsAny 平台...")
        print("=" * 60)
        
        # 1. 访问登录页面
        page.goto(LOGIN_URL)
        page.wait_for_load_state("networkidle")
        time.sleep(3)
        
        # 2. 执行登录
        page.fill('input[name="username"]', USERNAME)
        page.fill('input[name="password"]', PASSWORD)
        page.click('button[type="submit"]')
        time.sleep(5)
        
        print(f"✓ 登录成功: {page.title()}")
        
        # 3. 等待菜单加载
        time.sleep(3)
        
        # 4. 保存登录后的首页截图
        page.screenshot(path=os.path.join(SCREENSHOTS_DIR, "00_登录后首页.png"), full_page=True)
        print("✓ 已保存首页截图")
        
        # 5. 定义所有菜单项
        menu_items = [
            # 工作台
            {"name": "概览", "category": "工作台", "click": "text=概览"},
            
            # 运维中心
            {"name": "服务门户", "category": "运维中心", "click": "text=服务门户"},
            {"name": "工单管理", "category": "运维中心", "click": "text=工单管理"},
            {"name": "任务管理", "category": "运维中心", "click": "text=任务管理"},
            {"name": "我的值班", "category": "运维中心", "click": "text=我的值班"},
            {"name": "大屏展示", "category": "运维中心", "click": "text=大屏展示"},
            
            # 消息中心
            {"name": "消息管理", "category": "消息中心", "click": "text=消息管理"},
            {"name": "订阅设置", "category": "消息中心", "click": "text=订阅设置"},
            
            # 流程管理
            {"name": "工单目录", "category": "流程管理", "click": "text=工单目录"},
            {"name": "工单流程", "category": "流程管理", "click": "text=工单流程"},
            {"name": "SLA管理", "category": "流程管理", "click": "text=SLA管理"},
            {"name": "API管理", "category": "流程管理", "click": "text=API管理"},
            {"name": "值班管理", "category": "流程管理", "click": "text=值班管理"},
            
            # 平台设置
            {"name": "导航管理", "category": "平台设置", "click": "text=导航管理"},
            {"name": "系统设置", "category": "平台设置", "click": "text=系统设置"},
        ]
        
        print("\n" + "=" * 60)
        print("开始遍历所有功能模块...")
        print("=" * 60)
        
        # 6. 遍历每个菜单项
        for i, menu in enumerate(menu_items, 1):
            try:
                print(f"\n[{i:02d}/{len(menu_items)}] 正在访问: {menu['category']} > {menu['name']}")
                
                # 点击菜单
                page.click(menu['click'], timeout=5000)
                time.sleep(3)  # 等待页面加载
                
                # 获取页面信息
                title = page.title()
                url = page.url
                
                # 截图
                safe_name = f"{menu['category']}_{menu['name']}"
                filename = f"{safe_name}.png"
                screenshot_path = os.path.join(SCREENSHOTS_DIR, filename)
                page.screenshot(path=screenshot_path, full_page=True)
                
                print(f"  ✓ 标题: {title}")
                print(f"  ✓ 截图: {filename}")
                
                # 记录结果
                results.append({
                    "index": i,
                    "category": menu['category'],
                    "module": menu['name'],
                    "title": title,
                    "url": url,
                    "screenshot": filename
                })
                
            except Exception as e:
                print(f"  ✗ 失败: {str(e)[:50]}")
                results.append({
                    "index": i,
                    "category": menu['category'],
                    "module": menu['name'],
                    "error": str(e)
                })
        
        # 7. 保存结果到JSON
        results_file = os.path.join(OUTPUT_DIR, "exploration_results.json")
        with open(results_file, 'w', encoding='utf-8') as f:
            json.dump(results, f, ensure_ascii=False, indent=2)
        
        print("\n" + "=" * 60)
        print(f"探索完成！共遍历 {len(results)} 个功能模块")
        print(f"截图保存在: {SCREENSHOTS_DIR}")
        print("=" * 60)
        
        # 8. 保持浏览器打开
        print("\n浏览器将保持打开状态，按 Enter 键关闭...")
        input()
        browser.close()
    
    return results

if __name__ == "__main__":
    try:
        results = login_and_explore()
    except Exception as e:
        print(f"执行出错: {e}")
        import traceback
        traceback.print_exc()
        input("按 Enter 键退出...")
