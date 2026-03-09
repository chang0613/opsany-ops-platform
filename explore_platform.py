#!/usr/bin/env python3
"""
OpsAny 平台功能探索脚本
自动遍历所有功能模块并记录信息
"""

from playwright.sync_api import sync_playwright
import json
import time
from datetime import datetime

class OpsAnyExplorer:
    def __init__(self):
        self.username = "demo"
        self.password = "123456.coM"
        self.login_url = "https://demo.opsany.com/o/workbench/?login=1#/"
        self.base_url = "https://demo.opsany.com"
        self.findings = []
        
    def log(self, message):
        print(f"[{datetime.now().strftime('%H:%M:%S')}] {message}")
        
    def login(self):
        """登录平台"""
        self.browser = self.playwright.chromium.launch(headless=False, slow_mo=300)
        self.context = self.browser.new_context(viewport={"width": 1400, "height": 900})
        self.page = self.context.new_page()
        
        self.log("正在访问登录页面...")
        self.page.goto(self.login_url)
        self.page.wait_for_load_state("networkidle")
        time.sleep(2)
        
        # 填写用户名
        self.page.fill('input[name="username"]', self.username)
        self.log("✓ 用户名已填写")
        
        # 填写密码
        self.page.fill('input[name="password"]', self.password)
        self.log("✓ 密码已填写")
        
        # 点击登录
        self.page.click('button[type="submit"]')
        time.sleep(3)
        
        self.log(f"登录成功！当前页面: {self.page.title()}")
        return True
    
    def get_menu_items(self):
        """获取左侧菜单项"""
        try:
            # 尝试多种选择器获取菜单
            menu_selectors = [
                '.menu-container .menu-item',
                '.sidebar .menu-item', 
                '.el-menu .menu-item',
                'ul.el-menu li',
                '.nav-menu li'
            ]
            
            for selector in menu_selectors:
                try:
                    menu_items = self.page.query_selector_all(selector)
                    if menu_items:
                        items = []
                        for item in menu_items:
                            text = item.inner_text().strip()
                            if text and len(text) < 50:
                                items.append({'text': text, 'element': item})
                        if items:
                            self.log(f"找到菜单项: {len(items)} 个")
                            return items
                except:
                    continue
            
            # 如果CSS选择器都不行，尝试获取所有可点击元素
            self.log("尝试获取页面所有链接和按钮...")
            return []
        except Exception as e:
            self.log(f"获取菜单失败: {e}")
            return []
    
    def click_menu_and_screenshot(self, menu_text, category):
        """点击菜单并截图"""
        try:
            # 查找并点击菜单项
            selectors = [
                f'text="{menu_text}"',
                f'.menu-item:has-text("{menu_text}")',
                f'li:has-text("{menu_text}")',
                f'a:has-text("{menu_text}")'
            ]
            
            for selector in selectors:
                try:
                    self.page.click(selector, timeout=3000)
                    self.log(f"  点击了: {menu_text}")
                    time.sleep(2)
                    break
                except:
                    continue
            
            # 截图
            filename = f"screenshots/{category}_{menu_text}.png"
            self.page.screenshot(path=filename, full_page=True)
            self.log(f"  截图已保存: {filename}")
            
            # 获取页面标题和URL
            title = self.page.title()
            url = self.page.url
            
            # 尝试获取页面主要内容
            content = ""
            try:
                # 获取主要内容区域
                main_content = self.page.query_selector('.main-content, .content, .container, main')
                if main_content:
                    content = main_content.inner_text()[:2000]
            except:
                pass
            
            self.findings.append({
                'category': category,
                'function': menu_text,
                'title': title,
                'url': url,
                'screenshot': filename,
                'content': content
            })
            
            return True
        except Exception as e:
            self.log(f"  ✗ 处理菜单 {menu_text} 失败: {e}")
            return False
    
    def explore_workbench(self):
        """探索工作台"""
        self.log("\n=== 探索工作台 ===")
        self.findings.append({'category': '工作台', 'function': '概览', 'title': self.page.title(), 'url': self.page.url})
        self.page.screenshot(path="screenshots/工作台_概览.png", full_page=True)
        
    def explore_by_navigation(self):
        """根据导航栏探索所有功能"""
        self.log("\n=== 开始探索所有功能模块 ===")
        
        # 等待页面完全加载
        time.sleep(3)
        
        # 获取当前页面结构信息
        try:
            # 打印页面HTML结构的关键部分
            html = self.page.content()
            # 保存完整HTML
            with open("screenshots/page_structure.html", "w", encoding="utf-8") as f:
                f.write(html)
            self.log("已保存页面结构HTML")
        except Exception as e:
            self.log(f"保存页面结构失败: {e}")
    
    def save_findings(self):
        """保存探索结果"""
        with open("screenshots/exploration_report.json", "w", encoding="utf-8") as f:
            json.dump(self.findings, f, ensure_ascii=False, indent=2)
        self.log(f"探索报告已保存，共 {len(self.findings)} 个功能点")
    
    def close(self):
        """关闭浏览器"""
        self.log("\n探索完成，按 Enter 键关闭浏览器...")
        try:
            input()
        except:
            pass
        self.browser.close()

def main():
    explorer = OpsAnyExplorer()
    explorer.playwright = sync_playwright().start()
    
    try:
        explorer.login()
        explorer.explore_workbench()
        explorer.explore_by_navigation()
        explorer.save_findings()
    except Exception as e:
        print(f"探索过程中出错: {e}")
        import traceback
        traceback.print_exc()
    finally:
        explorer.close()
        explorer.playwright.stop()

if __name__ == "__main__":
    main()
