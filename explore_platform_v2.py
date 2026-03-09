#!/usr/bin/env python3
"""
OpsAny 平台功能探索脚本 - 改进版
智能遍历所有功能模块
"""

from playwright.sync_api import sync_playwright
import json
import time

class OpsAnyExplorer:
    def __init__(self):
        self.username = "demo"
        self.password = "123456.coM"
        self.login_url = "https://demo.opsany.com/o/workbench/?login=1#/"
        self.findings = []
        
    def log(self, message):
        print(f"[+] {message}")
        
    def login(self):
        """登录平台"""
        self.browser = self.playwright.chromium.launch(headless=False, slow_mo=200)
        self.context = self.browser.new_context(viewport={"width": 1400, "height": 900})
        self.page = self.context.new_page()
        
        self.log("正在访问登录页面...")
        self.page.goto(self.login_url)
        self.page.wait_for_load_state("networkidle")
        time.sleep(2)
        
        # 填写用户名和密码
        self.page.fill('input[name="username"]', self.username)
        self.page.fill('input[name="password"]', self.password)
        
        # 点击登录按钮
        self.page.click('button[type="submit"]')
        time.sleep(3)
        
        self.log(f"登录成功！当前页面: {self.page.title()}")
        
        # 等待左侧菜单加载
        time.sleep(2)
        return True
    
    def get_menu_structure(self):
        """通过JavaScript获取菜单结构"""
        menu_script = """
        () => {
            const menu = [];
            
            // 尝试获取所有菜单项
            // 先尝试 Element UI 的菜单结构
            const menuGroups = document.querySelectorAll('.el-menu-item, .el-submenu');
            
            if (menuGroups.length > 0) {
                menuGroups.forEach(item => {
                    const text = item.innerText.trim().split('\\n')[0];
                    if (text && text.length > 0 && text.length < 30) {
                        menu.push({
                            text: text,
                            tag: item.tagName,
                            class: item.className
                        });
                    }
                });
            }
            
            return menu;
        }
        """
        
        try:
            menu_items = self.page.evaluate(menu_script)
            self.log(f"找到 {len(menu_items)} 个菜单项")
            return menu_items
        except Exception as e:
            self.log(f"获取菜单结构失败: {e}")
            return []
    
    def explore_all_menus(self):
        """遍历所有菜单"""
        
        # 定义要点击的菜单项文本映射
        menu_actions = [
            # 运营分析
            {"name": "首页", "selector": "text=首页", "category": "运营分析"},
            
            # 运维管理
            {"name": "主机管理", "selector": "text=主机管理", "category": "运维管理"},
            {"name": "容器管理", "selector": "text=容器管理", "category": "运维管理"},
            {"name": "持续集成", "selector": "text=持续集成", "category": "运维管理"},
            {"name": "CI", "selector": "text=CI", "category": "运维管理"},
            {"name": "发布中心", "selector": "text=发布中心", "category": "运维管理"},
            {"name": "CD", "selector": "text=CD", "category": "运维管理"},
            {"name": "堡垒机", "selector": "text=堡垒机", "category": "运维管理"},
            {"name": "命令控制台", "selector": "text=命令控制台", "category": "运维管理"},
            {"name": "监控中心", "selector": "text=监控中心", "category": "运维管理"},
            {"name": "告警中心", "selector": "text=告警中心", "category": "运维管理"},
            {"name": "资产管理", "selector": "text=资产管理", "category": "运维管理"},
            
            # 系统设置
            {"name": "知识库", "selector": "text=知识库", "category": "系统设置"},
            {"name": "系统配置", "selector": "text=系统配置", "category": "系统设置"},
            {"name": "用户管理", "selector": "text=用户管理", "category": "系统设置"},
            {"name": "审计日志", "selector": "text=审计日志", "category": "系统设置"},
        ]
        
        # 先回到首页
        self.page.goto(self.login_url)
        time.sleep(3)
        
        for menu in menu_actions:
            try:
                self.log(f"尝试进入: {menu['name']}...")
                
                # 点击菜单项
                self.page.click(menu['selector'], timeout=5000)
                time.sleep(3)
                
                # 获取页面标题和URL
                title = self.page.title()
                url = self.page.url
                
                # 截图
                filename = f"exploration_v2/{menu['category']}_{menu['name']}.png"
                self.page.screenshot(path=filename, full_page=True)
                
                # 尝试获取主要内容
                content = ""
                try:
                    # 获取 body 中的文本内容
                    content = self.page.evaluate("() => document.body.innerText.substring(0, 3000)")
                except:
                    pass
                
                self.findings.append({
                    "category": menu['category'],
                    "module": menu['name'],
                    "title": title,
                    "url": url,
                    "screenshot": filename,
                    "content": content[:1000]  # 限制内容长度
                })
                
                self.log(f"  ✓ {menu['name']} - {title}")
                
            except Exception as e:
                self.log(f"  ✗ {menu['name']} - 失败: {str(e)[:50]}")
                continue
        
        # 尝试获取当前可见的菜单项
        self.log("\n尝试获取侧边栏菜单结构...")
        try:
            # 获取侧边栏的所有链接
            sidebar_links = self.page.query_selector_all('.el-menu-item a, .el-menu a, .sidebar a, nav a')
            for link in sidebar_links[:20]:
                try:
                    href = link.get_attribute('href')
                    text = link.inner_text().strip()
                    if href and text:
                        self.log(f"  菜单链接: {text} -> {href}")
                except:
                    pass
        except Exception as e:
            self.log(f"  获取菜单链接失败: {e}")
    
    def save_findings(self):
        """保存探索结果"""
        # 保存 JSON 报告
        with open("exploration_v2/exploration_report.json", "w", encoding="utf-8") as f:
            json.dump(self.findings, f, ensure_ascii=False, indent=2)
        
        self.log(f"\n探索完成！共记录 {len(self.findings)} 个功能模块")
        self.log("报告已保存到 exploration_v2/exploration_report.json")
        
        # 生成 Markdown 报告
        self.generate_markdown_report()
    
    def generate_markdown_report(self):
        """生成 Markdown 格式的报告"""
        md_content = """# OpsAny 平台功能探索报告

## 概述

本报告详细记录了 OpsAny 运维平台的功能模块、业务逻辑和界面结构。

---
"""
        for item in self.findings:
            md_content += f"""
## {item['category']} - {item['module']}

- **页面标题**: {item['title']}
- **URL**: {item['url']}
- **截图**: {item['screenshot']}

### 页面内容摘要

```
{item['content'][:500]}...
```

---

"""
        
        with open("exploration_v2/功能探索报告.md", "w", encoding="utf-8") as f:
            f.write(md_content)
        
        self.log("Markdown 报告已保存到 exploration_v2/功能探索报告.md")
    
    def close(self):
        """关闭浏览器"""
        self.log("\n按 Enter 键关闭浏览器...")
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
        explorer.get_menu_structure()
        explorer.explore_all_menus()
        explorer.save_findings()
    except Exception as e:
        print(f"探索过程中出错: {e}")
        import traceback
        traceback.print_exc()
    finally:
        try:
            explorer.close()
        except:
            pass
        explorer.playwright.stop()

if __name__ == "__main__":
    main()
