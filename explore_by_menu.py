#!/usr/bin/env python3
"""
OpsAny 平台功能探索脚本 - 改进版
通过点击菜单来探索所有功能模块
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
OUTPUT_DIR = r"D:\自建agent\运维平台\exploration"
SCREENSHOTS_DIR = os.path.join(OUTPUT_DIR, "screenshots")
os.makedirs(SCREENSHOTS_DIR, exist_ok=True)

def explore_by_clicking_menu():
    """通过点击菜单探索平台"""
    
    results = []
    menu_structure = []
    
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False, slow_mo=300)
        context = browser.new_context(viewport={"width": 1920, "height": 1080})
        page = context.new_page()
        
        print("=" * 70)
        print("开始登录 OpsAny 平台...")
        print("=" * 70)
        
        # 登录
        page.goto(LOGIN_URL)
        page.wait_for_load_state("networkidle")
        time.sleep(3)
        
        # 登录
        page.fill('input[name="username"]', USERNAME)
        page.fill('input[name="password"]', PASSWORD)
        page.click('button[type="submit"]')
        time.sleep(5)
        
        print(f"✓ 登录成功: {page.title()}")
        
        # 保存首页
        page.screenshot(path=os.path.join(SCREENSHOTS_DIR, "01_首页_工作台.png"), full_page=True)
        
        # 等待菜单加载
        time.sleep(3)
        
        print("\n" + "=" * 70)
        print("开始探索菜单结构...")
        print("=" * 70)
        
        # 尝试多种菜单选择器
        menu_selectors = [
            '.el-menu',
            '.el-menu--horizontal', 
            '.sidebar-menu',
            'nav ul',
            '[class*="menu"]'
        ]
        
        menu = None
        for selector in menu_selectors:
            try:
                menu = page.query_selector(selector)
                if menu:
                    print(f"✓ 找到菜单: {selector}")
                    break
            except:
                continue
        
        # 获取所有一级菜单项（菜单分组）
        menu_groups = []
        group_selectors = [
            '.el-menu-item-group__title',
            '.menu-group',
            '.group-title',
            '[class*="group"]'
        ]
        
        # 获取所有菜单项
        menu_items = page.query_selector_all('.el-menu-item, .el-submenu')
        print(f"✓ 发现 {len(menu_items)} 个菜单/子菜单项")
        
        # 遍历每个菜单项并点击
        for i, item in enumerate(menu_items):
            try:
                # 获取菜单项文本
                item_text = ""
                try:
                    span = item.query_selector('span')
                    if span:
                        item_text = span.inner_text().strip()
                except:
                    pass
                
                if not item_text:
                    # 尝试其他方式获取文本
                    try:
                        item_text = item.inner_text()[:50].strip()
                    except:
                        continue
                
                if not item_text or len(item_text) < 2:
                    continue
                
                print(f"\n[{i+1}] 点击菜单: {item_text}")
                
                # 尝试点击菜单项
                try:
                    # 先尝试直接点击
                    item.click(timeout=2000)
                except:
                    try:
                        # 如果是子菜单，先展开
                        page.evaluate('(el) => el.classList.add("is-opened")', item)
                        item.click(timeout=2000)
                    except:
                        print(f"  ✗ 无法点击")
                        continue
                
                time.sleep(2)
                
                # 获取当前URL和标题
                current_url = page.url
                current_title = page.title()
                
                print(f"  ✓ URL: {current_url}")
                print(f"  ✓ 标题: {current_title}")
                
                # 截图
                safe_name = item_text.replace("/", "_").replace(" ", "_")[:30]
                screenshot_name = f"{i+1:02d}_{safe_name}.png"
                page.screenshot(path=os.path.join(SCREENSHOTS_DIR, screenshot_name), full_page=True)
                
                # 记录结果
                results.append({
                    "index": i + 1,
                    "name": item_text,
                    "url": current_url,
                    "title": current_title,
                    "screenshot": screenshot_name
                })
                
                menu_structure.append({
                    "name": item_text,
                    "path": current_url
                })
                
            except Exception as e:
                print(f"  ✗ 错误: {str(e)[:50]}")
        
        # 保存结果
        results_file = os.path.join(OUTPUT_DIR, "menu_exploration.json")
        with open(results_file, 'w', encoding='utf-8') as f:
            json.dump({
                "results": results,
                "menu_structure": menu_structure
            }, f, ensure_ascii=False, indent=2)
        
        print("\n" + "=" * 70)
        print(f"探索完成！共发现 {len(results)} 个功能页面")
        print("=" * 70)
        
        # 保持浏览器
        input("\n按 Enter 键关闭浏览器...")
        browser.close()
    
    return results

if __name__ == "__main__":
    try:
        results = explore_by_clicking_menu()
    except Exception as e:
        print(f"执行出错: {e}")
        import traceback
        traceback.print_exc()
        input("按 Enter 键退出...")
