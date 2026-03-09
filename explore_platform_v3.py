#!/usr/bin/env python3
"""
OpsAny 平台功能探索脚本 - URL导航版
通过直接访问URL来探索功能模块
"""

from playwright.sync_api import sync_playwright
import json
import time

class OpsAnyExplorer:
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
        
        # 获取 cookies 以便后续请求使用
        self.cookies = self.context.cookies()
        return True
    
    def explore_by_url(self):
        """通过URL导航探索功能"""
        
        # 定义功能模块的URL路径
        modules = [
            # 工作台相关
            {"name": "工作台-概览", "url": "/o/workbench/", "category": "工作台"},
            {"name": "工作台", "url": "/o/workbench/", "category": "工作台"},
            
            # 运维管理
            {"name": "主机管理", "url": "/o/host/", "category": "运维管理"},
            {"name": "主机列表", "url": "/o/host/host-list/", "category": "运维管理"},
            {"name": "主机组", "url": "/o/host/host-group/", "category": "运维管理"},
            
            {"name": "容器管理", "url": "/o/container/", "category": "运维管理"},
            {"name": "容器列表", "url": "/o/container/pod/", "category": "运维管理"},
            {"name": "镜像仓库", "url": "/o/container/image/", "category": "运维管理"},
            
            {"name": "持续集成", "url": "/o/ci/", "category": "运维管理"},
            {"name": "CI流水线", "url": "/o/ci/pipeline/", "category": "运维管理"},
            {"name": "CI构建历史", "url": "/o/ci/build/", "category": "运维管理"},
            
            {"name": "发布中心", "url": "/o/cd/", "category": "运维管理"},
            {"name": "应用管理", "url": "/o/cd/app/", "category": "运维管理"},
            {"name": "发布记录", "url": "/o/cd/deploy/", "category": "运维管理"},
            
            {"name": "堡垒机", "url": "/o/jumpserver/", "category": "运维管理"},
            {"name": "会话管理", "url": "/o/jumpserver/session/", "category": "运维管理"},
            
            {"name": "命令控制台", "url": "/o/command/", "category": "运维管理"},
            
            {"name": "监控中心", "url": "/o/monitor/", "category": "运维管理"},
            {"name": "主机监控", "url": "/o/monitor/host/", "category": "运维管理"},
            {"name": "网络监控", "url": "/o/monitor/network/", "category": "运维管理"},
            {"name": "容器监控", "url": "/o/monitor/container/", "category": "运维管理"},
            {"name": "日志监控", "url": "/o/monitor/log/", "category": "运维管理"},
            
            {"name": "告警中心", "url": "/o/alarm/", "category": "运维管理"},
            {"name": "告警策略", "url": "/o/alarm/policy/", "category": "运维管理"},
            {"name": "告警记录", "url": "/o/alarm/record/", "category": "运维管理"},
            {"name": "告警联系人", "url": "/o/alarm/contact/", "category": "运维管理"},
            
            {"name": "资产管理", "url": "/o/assets/", "category": "运维管理"},
            {"name": "资产列表", "url": "/o/assets/device/", "category": "运维管理"},
            {"name": "IDC机房", "url": "/o/assets/idc/", "category": "运维管理"},
            {"name": "业务系统", "url": "/o/assets/business/", "category": "运维管理"},
            {"name": "IP管理", "url": "/o/assets/ip/", "category": "运维管理"},
            {"name": "组织架构", "url": "/o/assets/org/", "category": "运维管理"},
            
            # 系统设置
            {"name": "知识库", "url": "/o/wiki/", "category": "系统设置"},
            {"name": "系统配置", "url": "/o/config/", "category": "系统设置"},
            {"name": "用户管理", "url": "/o/user/", "category": "系统设置"},
            {"name": "角色管理", "url": "/o/user/role/", "category": "系统设置"},
            {"name": "审计日志", "url": "/o/audit/", "category": "系统设置"},
        ]
        
        for module in modules:
            try:
                full_url = self.base_url + module['url']
                self.log(f"访问: {module['name']} ({full_url})...")
                
                self.page.goto(full_url, timeout=10000)
                time.sleep(3)
                
                # 获取页面标题和URL
                title = self.page.title()
                url = self.page.url
                
                # 截图
                safe_name = module['name'].replace('/', '_')
                filename = f"exploration_v3/{module['category']}_{safe_name}.png"
                self.page.screenshot(path=filename, full_page=True)
                
                # 获取页面内容
                content = ""
                try:
                    content = self.page.evaluate("() => document.body.innerText.substring(0, 3000)")
                except:
                    pass
                
                # 检查是否404
                is_404 = "404" in title or "找不到" in content or "Page Not Found" in content
                
                self.findings.append({
                    "category": module['category'],
                    "module": module['name'],
                    "title": title,
                    "url": url,
                    "screenshot": filename,
                    "content": content[:800],
                    "status": "404" if is_404 else "OK"
                })
                
                status = "✗ 404" if is_404 else "✓"
                self.log(f"  {status} {module['name']} - {title}")
                
            except Exception as e:
                self.log(f"  ✗ {module['name']} - 错误: {str(e)[:50]}")
                continue
    
    def save_findings(self):
        """保存探索结果"""
        
        # 保存 JSON 报告
        with open("exploration_v3/exploration_report.json", "w", encoding="utf-8") as f:
            json.dump(self.findings, f, ensure_ascii=False, indent=2)
        
        # 统计
        total = len(self.findings)
        success = len([f for f in self.findings if f['status'] == 'OK'])
        not_found = total - success
        
        self.log(f"\n探索完成！")
        self.log(f"  总计: {total} 个模块")
        self.log(f"  可访问: {success} 个")
        self.log(f"  404: {not_found} 个")
        
        # 生成 Markdown 报告
        self.generate_markdown_report()
    
    def generate_markdown_report(self):
        """生成 Markdown 格式的报告"""
        
        # 按类别分组
        categories = {}
        for item in self.findings:
            cat = item['category']
            if cat not in categories:
                categories[cat] = []
            categories[cat].append(item)
        
        md_content = """# OpsAny 平台功能探索报告

> 生成时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
> 登录账号: demo

---

## 一、平台概述

OpsAny（Open Platform for Service Any）是一个**一站式运维管理平台**，集成 了主机管理、容器管理、CI/CD、堡垒机、监控告警、资产管理等核心运维能力。

---

## 二、功能模块清单

"""
        
        for cat, items in categories.items():
            md_content += f"\n### {cat}\n\n"
            md_content += "| 功能模块 | 页面标题 | 状态 | URL |\n"
            md_content += "|---------|---------|------|-----|\n"
            
            for item in items:
                status_icon = "✅" if item['status'] == 'OK' else "❌"
                md_content += f"| {item['module']} | {item['title']} | {status_icon} | {item['url']} |\n"
        
        md_content += """

---

## 三、详细功能说明

"""
        
        # 只包含可访问的模块
        for item in self.findings:
            if item['status'] == 'OK':
                md_content += f"""
### 3.{self.findings.index(item)+1} {item['category']} - {item['module']}

**基本信息:**
- **页面标题**: {item['title']}
- **访问URL**: {item['url']}
- **截图文件**: {item['screenshot']}

**页面内容摘要:**
```
{item['content'][:400]}
```

---

"""
        
        with open("exploration_v3/功能探索报告.md", "w", encoding="utf-8") as f:
            f.write(md_content)
        
        self.log("Markdown 报告已保存!")
    
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
        explorer.explore_by_url()
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
