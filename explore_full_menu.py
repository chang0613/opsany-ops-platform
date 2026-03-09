#!/usr/bin/env python3
"""
OpsAny 平台 1:1 菜单探索脚本
严格按照实际菜单结构探索每个页面
"""

from playwright.sync_api import sync_playwright
import json
import time

class OpsAnyMenuExplorer:
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
        
        self.page.fill('input[name="username"]', self.username)
        self.page.fill('input[name="password"]', self.password)
        self.page.click('button[type="submit"]')
        time.sleep(3)
        
        self.log(f"登录成功！当前页面: {self.page.title()}")
        return True
    
    def get_full_menu_structure(self):
        """获取完整菜单结构"""
        
        # 先获取侧边栏HTML结构
        menu_html = self.page.evaluate("""
            () => {
                const sidebar = document.querySelector('.el-scrollbar__wrap');
                if (sidebar) {
                    return sidebar.innerHTML;
                }
                return '';
            }
        """)
        
        # 保存菜单HTML用于分析
        with open("menu_structure.html", "w", encoding="utf-8") as f:
            f.write(menu_html)
        
        # 通过JavaScript获取菜单项
        menu_script = """
        () => {
            const menuItems = [];
            
            // 获取所有菜单元素
            const elements = document.querySelectorAll('.el-menu-item, .el-submenu');
            
            elements.forEach((el, index) => {
                const text = el.innerText.trim().split('\\n')[0].trim();
                const fullText = el.innerText.trim();
                
                // 跳过图标和空文本
                if (text && text.length > 0 && text.length < 30) {
                    menuItems.push({
                        index: index,
                        text: text,
                        fullText: fullText,
                        tagName: el.tagName,
                        className: el.className,
                        hasChildren: el.querySelectorAll('.el-menu-item').length > 0
                    });
                }
            });
            
            return menuItems;
        }
        """
        
        menu_items = self.page.evaluate(menu_script)
        self.log(f"找到 {len(menu_items)} 个菜单项")
        
        # 打印菜单结构
        for item in menu_items:
            self.log(f"  {item['index']}: {item['text']}")
        
        return menu_items
    
    def explore_by_clicking_menu(self):
        """通过点击菜单探索页面"""
        
        # 定义完整菜单路径 - 按照实际菜单结构
        menu_actions = [
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
        
        for menu in menu_actions:
            try:
                self.log(f"探索: {menu['category']} > {menu['name']}...")
                
                # 点击菜单
                self.page.click(menu['click'], timeout=5000)
                time.sleep(3)
                
                # 获取页面信息
                title = self.page.title()
                url = self.page.url
                
                # 截图
                safe_name = menu['name'].replace('/', '_')
                filename = f"full_explore/{menu['category']}_{safe_name}.png"
                self.page.screenshot(path=filename, full_page=True)
                
                # 提取页面详细内容
                page_data = self.extract_page_details()
                
                self.findings.append({
                    "category": menu['category'],
                    "module": menu['name'],
                    "title": title,
                    "url": url,
                    "screenshot": filename,
                    "page_data": page_data
                })
                
                self.log(f"  ✓ {menu['name']} - {title}")
                
            except Exception as e:
                self.log(f"  ✗ {menu['name']} - 失败: {str(e)[:80]}")
                continue
    
    def extract_page_details(self):
        """提取页面详细内容"""
        
        details_script = """
        () => {
            const result = {
                // 获取面包屑
                breadcrumbs: [],
                
                // 获取页面标题区域
                pageHeader: '',
                
                // 获取所有卡片/区块
                cards: [],
                
                // 获取表格信息
                tables: [],
                
                // 获取表单信息
                forms: [],
                
                // 获取按钮
                buttons: [],
                
                // 获取统计数字
                stats: [],
                
                // 获取所有文本内容
                allText: ''
            };
            
            // 面包屑
            const breadcrumbs = document.querySelectorAll('.el-breadcrumb__item');
            if (breadcrumbs.length > 0) {
                result.breadcrumbs = Array.from(breadcrumbs).map(el => el.innerText.trim());
            }
            
            // 页面标题
            const pageTitle = document.querySelector('.page-title, .page-header__title, h1');
            if (pageTitle) {
                result.pageHeader = pageTitle.innerText.trim();
            }
            
            // 统计卡片 (数字+标签)
            const statCards = document.querySelectorAll('.stat-card, .data-card, .number-card, [class*="stat"], [class*="count"]');
            statCards.forEach(card => {
                const num = card.innerText.match(/\\d+/);
                if (num) {
                    result.stats.push({
                        number: num[0],
                        text: card.innerText.replace(num[0], '').trim()
                    });
                }
            });
            
            // 表格
            const tables = document.querySelectorAll('table');
            tables.forEach(table => {
                const headers = Array.from(table.querySelectorAll('th')).map(th => th.innerText.trim());
                const rows = table.querySelectorAll('tbody tr').length;
                if (headers.length > 0) {
                    result.tables.push({
                        headers: headers,
                        rowCount: rows
                    });
                }
            });
            
            // 按钮
            const buttons = document.querySelectorAll('button, .el-button, a[role="button"]');
            const btnTexts = Array.from(buttons).map(btn => btn.innerText.trim()).filter(t => t);
            result.buttons = [...new Set(btnTexts)].slice(0, 20);
            
            // 所有文本内容
            result.allText = document.body.innerText.substring(0, 5000);
            
            return result;
        }
        """
        
        try:
            return self.page.evaluate(details_script)
        except Exception as e:
            return {"error": str(e)}
    
    def save_results(self):
        """保存探索结果"""
        
        # 保存JSON
        with open("full_explore/menu_findings.json", "w", encoding="utf-8") as f:
            json.dump(self.findings, f, ensure_ascii=False, indent=2)
        
        self.log(f"\n探索完成！共 {len(self.findings)} 个页面")
        
        # 生成Markdown报告
        self.generate_detailed_report()
    
    def generate_detailed_report(self):
        """生成详细Markdown报告"""
        
        md_content = """# OpsAny 平台 1:1 完整功能分析报告

> 严格按照实际菜单结构分析  
> 生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}

---

## 一、左侧菜单结构（完全按实际界面）

"""
        
        # 按类别分组
        categories = {}
        for item in self.findings:
            cat = item['category']
            if cat not in categories:
                categories[cat] = []
            categories[cat].append(item)
        
        for cat, items in categories.items():
            md_content += f"### {cat}\n\n"
            for item in items:
                md_content += f"- **{item['module']}** - {item['title']}\n"
            md_content += "\n"
        
        md_content += """
---

## 二、每个页面详细分析

"""
        
        for item in self.findings:
            pd = item.get('page_data', {})
            
            md_content += f"""
### 2.{self.findings.index(item)+1} {item['category']} - {item['module']}

**基本信息:**
- 页面标题: {item['title']}
- URL: {item['url']}
- 截图: {item['screenshot']}

"""
            
            # 面包屑
            if pd.get('breadcrumbs'):
                md_content += f"**面包屑:** {' > '.join(pd['breadcrumbs'])}\n\n"
            
            # 页面标题
            if pd.get('pageHeader'):
                md_content += f"**页面标题:** {pd['pageHeader']}\n\n"
            
            # 统计卡片
            if pd.get('stats'):
                md_content += "**统计卡片:**\n"
                for stat in pd['stats']:
                    md_content += f"- 数值: {stat['number']} - {stat['text']}\n"
                md_content += "\n"
            
            # 表格
            if pd.get('tables'):
                md_content += "**表格结构:**\n"
                for i, table in enumerate(pd['tables']):
                    md_content += f"- 表格{i+1}表头: {', '.join(table['headers'][:10])}\n"
                    md_content += f"  数据行数: {table['rowCount']}\n"
                md_content += "\n"
            
            # 按钮
            if pd.get('buttons'):
                md_content += f"**页面按钮:** {', '.join(pd['buttons'][:15])}\n\n"
            
            # 完整文本
            if pd.get('allText'):
                md_content += f"""**页面内容摘要:**
```
{pd['allText'][:1500]}
```

"""
            
            md_content += "---\n\n"
        
        with open("full_explore/1比1功能分析报告.md", "w", encoding="utf-8") as f:
            f.write(md_content)
        
        self.log("详细报告已保存!")
    
    def close(self):
        """关闭浏览器"""
        self.log("\n按 Enter 键关闭浏览器...")
        try:
            input()
        except:
            pass
        self.browser.close()

def main():
    explorer = OpsAnyMenuExplorer()
    explorer.playwright = sync_playwright().start()
    
    try:
        explorer.login()
        explorer.get_full_menu_structure()
        explorer.explore_by_clicking_menu()
        explorer.save_results()
    except Exception as e:
        print(f"探索出错: {e}")
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
