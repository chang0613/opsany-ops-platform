#!/usr/bin/env python3
"""
OpsAny 平台深度探索脚本 - 1:1复制专用
获取每个菜单项的完整页面结构、字段、布局
"""

from playwright.sync_api import sync_playwright
import json
import time

class OpsAnyDeepExplorer:
    def __init__(self):
        self.username = "demo"
        self.password = "123456.coM"
        self.login_url = "https://demo.opsany.com/o/workbench/?login=1#/"
        self.base_url = "https://demo.opsany.com"
        self.findings = []
        
    def log(self, message):
        print(f"[+] {message}")
        
    def login(self):
        """登录平台"""
        self.browser = self.playwright.chromium.launch(headless=False, slow_mo=200)
        self.context = self.browser.new_context(viewport={"width": 1600, "height": 900})
        self.page = self.context.new_page()
        
        self.log("正在登录...")
        self.page.goto(self.login_url)
        self.page.wait_for_load_state("networkidle")
        time.sleep(2)
        
        self.page.fill('input[name="username"]', self.username)
        self.page.fill('input[name="password"]', self.password)
        self.page.click('button[type="submit"]')
        time.sleep(3)
        
        self.log(f"登录成功: {self.page.title()}")
        return True
    
    def extract_page_structure(self):
        """提取页面完整结构"""
        
        # 获取完整的菜单导航
        menu_script = """
        () => {
            const result = {
                leftMenus: [],
                topNav: [],
                pageInfo: {}
            };
            
            // 左侧菜单 - el-menu
            const leftMenu = document.querySelector('.el-menu');
            if (leftMenu) {
                // 获取所有一级菜单
                const items = leftMenu.querySelectorAll(':scope > .el-menu-item, :scope > .el-submenu');
                items.forEach((item, idx) => {
                    const isSubmenu = item.classList.contains('el-submenu');
                    const name = item.querySelector('.el-menu-item, .el-submenu__title')?.innerText?.trim();
                    
                    if (name) {
                        const menuData = {
                            name: name,
                            type: isSubmenu ? 'group' : 'item',
                            children: []
                        };
                        
                        // 如果是子菜单，获取子菜单项
                        if (isSubmenu) {
                            const subItems = item.querySelectorAll('.el-menu-item');
                            subItems.forEach(sub => {
                                const subName = sub.innerText?.trim();
                                if (subName) {
                                    menuData.children.push(subName);
                                }
                            });
                        }
                        
                        result.leftMenus.push(menuData);
                    }
                });
            }
            
            // 顶部导航
            const topNav = document.querySelector('.header, .top-header, nav');
            if (topNav) {
                const links = topNav.querySelectorAll('a, button, span');
                links.forEach(link => {
                    const text = link.innerText?.trim();
                    if (text && text.length > 0 && text.length < 30) {
                        result.topNav.push(text);
                    }
                });
            }
            
            // 当前页面信息
            result.pageInfo = {
                title: document.title,
                url: window.location.href,
                breadcrumbs: []
            };
            
            // 面包屑
            const breadcrumbs = document.querySelectorAll('.el-breadcrumb__item, .breadcrumb, .breadcrumbs');
            breadcrumbs.forEach(b => {
                result.pageInfo.breadcrumbs.push(b.innerText?.trim());
            });
            
            return result;
        }
        """
        
        try:
            structure = self.page.evaluate(menu_script)
            return structure
        except Exception as e:
            self.log(f"提取结构失败: {e}")
            return {}
    
    def extract_form_fields(self):
        """提取页面中的表单字段"""
        script = """
        () => {
            const fields = [];
            
            // 输入框
            document.querySelectorAll('input').forEach(input => {
                const label = input.closest('.el-form-item')?.querySelector('.el-form-item__label')?.innerText;
                fields.push({
                    type: 'input',
                    placeholder: input.placeholder,
                    label: label,
                    name: input.name || input.id,
                    type_attr: input.type
                });
            });
            
            // 下拉框
            document.querySelectorAll('select').forEach(select => {
                const label = select.closest('.el-form-item')?.querySelector('.el-form-item__label')?.innerText;
                const options = Array.from(select.options).map(o => o.value);
                fields.push({
                    type: 'select',
                    label: label,
                    name: select.name || select.id,
                    options: options
                });
            });
            
            // 按钮
            document.querySelectorAll('button').forEach(btn => {
                fields.push({
                    type: 'button',
                    text: btn.innerText?.trim(),
                    class: btn.className
                });
            });
            
            // 表格列
            const tables = document.querySelectorAll('table, .el-table');
            tables.forEach((table, idx) => {
                const headers = Array.from(table.querySelectorAll('th')).map(th => th.innerText?.trim());
                if (headers.length > 0) {
                    fields.push({
                        type: 'table',
                        headers: headers
                    });
                }
            });
            
            return fields;
        }
        """
        try:
            return self.page.evaluate(script)
        except:
            return []
    
    def extract_cards_and_widgets(self):
        """提取页面中的卡片和组件"""
        script = """
        () => {
            const widgets = [];
            
            // 统计卡片
            document.querySelectorAll('.stat-card, .card, .el-card, .info-card, [class*="card"]').forEach(card => {
                const title = card.querySelector('.title, .card-title, h3, h4, .el-card__header')?.innerText?.trim();
                const content = card.querySelector('.content, .card-content, .el-card__body')?.innerText?.trim();
                if (title || content) {
                    widgets.push({
                        type: 'card',
                        title: title,
                        content: content?.substring(0, 200)
                    });
                }
            });
            
            // 图表容器
            document.querySelectorAll('div[id*="chart"], div[id*="graph"], .echarts-container').forEach(chart => {
                widgets.push({
                    type: 'chart',
                    id: chart.id
                });
            });
            
            return widgets;
        }
        """
        try:
            return self.page.evaluate(script)
        except:
            return []
    
    def explore_by_menu(self):
        """通过左侧菜单逐个探索"""
        
        # 基于截图分析的菜单结构
        menu_structure = [
            {
                "group": "工作台",
                "items": ["概览"]
            },
            {
                "group": "运维中心",
                "items": ["服务门户", "工单管理", "任务管理", "我的值班", "大屏展示"]
            },
            {
                "group": "消息中心",
                "items": ["消息管理", "订阅设置"]
            },
            {
                "group": "流程管理",
                "items": ["工单目录", "工单流程", "SLA管理", "API管理", "值班管理"]
            },
            {
                "group": "平台设置",
                "items": ["导航管理", "系统设置"]
            }
        ]
        
        self.log("\n=== 开始深度探索 ===")
        
        for group in menu_structure:
            self.log(f"\n--- {group['group']} ---")
            
            for item in group['items']:
                try:
                    self.log(f"探索: {item}...")
                    
                    # 尝试点击菜单
                    try:
                        self.page.click(f'text="{item}"', timeout=3000)
                        time.sleep(2)
                    except:
                        pass
                    
                    # 获取页面结构
                    structure = self.extract_page_structure()
                    
                    # 获取表单字段
                    fields = self.extract_form_fields()
                    
                    # 获取卡片组件
                    widgets = self.extract_cards_and_widgets()
                    
                    # 截图
                    safe_name = f"{group['group']}_{item}".replace('/', '_')
                    screenshot_path = f"deep_explore/{safe_name}.png"
                    self.page.screenshot(path=screenshot_path, full_page=True)
                    
                    # 获取页面主要内容
                    content_script = """
                    () => {
                        // 获取主内容区域
                        const main = document.querySelector('.main-content, .content, .container, main, .app-main, #app');
                        if (main) {
                            return main.innerText?.substring(0, 5000);
                        }
                        return document.body.innerText?.substring(0, 3000);
                    }
                    """
                    content = self.page.evaluate(content_script)
                    
                    finding = {
                        "group": group['group'],
                        "module": item,
                        "title": self.page.title(),
                        "url": self.page.url,
                        "screenshot": screenshot_path,
                        "structure": structure,
                        "fields": fields,
                        "widgets": widgets,
                        "content": content[:2000]
                    }
                    
                    self.findings.append(finding)
                    self.log(f"  ✓ {item} - {self.page.title()}")
                    
                except Exception as e:
                    self.log(f"  ✗ {item} 失败: {str(e)[:50]}")
        
        # 探索顶部导航
        self.log("\n=== 探索顶部导航 ===")
        
        # 平台导航探索
        try:
            # 点击平台导航
            platform_nav_selectors = ['text=平台导航', 'text=控制台', '.platform-nav', '[class*="nav"]']
            for sel in platform_nav_selectors:
                try:
                    self.page.click(sel, timeout=2000)
                    time.sleep(2)
                    self.page.screenshot(path="deep_explore/顶部_平台导航.png", full_page=True)
                    self.log("  ✓ 平台导航截图已保存")
                    break
                except:
                    continue
        except Exception as e:
            self.log(f"  探索顶部导航失败: {e}")
    
    def save_findings(self):
        """保存探索结果"""
        
        # 保存完整JSON
        with open("deep_explore/exploration_results.json", "w", encoding="utf-8") as f:
            json.dump(self.findings, f, ensure_ascii=False, indent=2)
        
        # 生成详细Markdown报告
        self.generate_detailed_report()
        
        self.log(f"\n探索完成！共 {len(self.findings)} 个模块")
    
    def generate_detailed_report(self):
        """生成详细报告"""
        
        md = """# OpsAny 平台 1:1 复制指南

> 基于实际页面结构分析生成
> 生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}

---

## 一、菜单结构总览

""".format()
        
        # 按分组整理
        groups = {}
        for f in self.findings:
            g = f['group']
            if g not in groups:
                groups[g] = []
            groups[g].append(f)
        
        for group, items in groups.items():
            md += f"### {group}\n\n"
            for item in items:
                md += f"- **{item['module']}**: `{item['title']}`\n"
            md += "\n"
        
        md += """

---

## 二、详细功能分析

"""
        
        for finding in self.findings:
            md += f"""
### 2.{self.findings.index(finding)+1} {finding['group']} - {finding['module']}

**基本信息:**
- 页面标题: {finding['title']}
- URL: {finding['url']}
- 截图: {finding['screenshot']}

**菜单路径:** {finding['group']} > {finding['module']}

**页面结构:**
```json
{json.dumps(finding.get('structure', {}), ensure_ascii=False, indent=2)}
```

**页面内容摘要:**
```
{finding['content'][:1000]}
```

**检测到的表单字段:**
```json
{json.dumps(finding.get('fields', [])[:10], ensure_ascii=False, indent=2)}
```

**检测到的卡片/组件:**
```json
{json.dumps(finding.get('widgets', [])[:10], ensure_ascii=False, indent=2)}
```

---

"""
        
        with open("deep_explore/1比1复制指南.md", "w", encoding="utf-8") as f:
            f.write(md)
        
        self.log("详细报告已保存!")
    
    def close(self):
        self.log("\n按 Enter 关闭浏览器...")
        try:
            input()
        except:
            pass
        self.browser.close()

def main():
    explorer = OpsAnyDeepExplorer()
    explorer.playwright = sync_playwright().start()
    
    try:
        explorer.login()
        explorer.explore_by_menu()
        explorer.save_findings()
    except Exception as e:
        print(f"错误: {e}")
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
