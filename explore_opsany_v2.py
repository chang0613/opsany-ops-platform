#!/usr/bin/env python3
"""
OpsAny 平台功能探索脚本 - 改进版
自动遍历所有功能模块并截图
"""

from playwright.sync_api import sync_playwright
import time
import os

# 创建截图目录
screenshot_dir = "screenshots"
os.makedirs(screenshot_dir, exist_ok=True)

def get_nav_items():
    """获取导航菜单项和对应的截图名称 - 根据实际菜单结构"""
    return [
        # 一级菜单
        {"name": "概览_工作台", "selectors": ['a:has-text("概览")', '.el-menu-item:has-text("概览")'], "filename": "01_overview"},
        
        # 主机管理
        {"name": "主机管理", "selectors": ['span:has-text("主机管理")'], "filename": "02_host"},
        {"name": "主机列表", "selectors": ['.el-menu-item:has-text("主机列表")'], "filename": "02_host_list"},
        {"name": "主机组", "selectors": ['.el-menu-item:has-text("主机组")'], "filename": "02_host_group"},
        
        # 容器管理
        {"name": "容器管理_父菜单", "selectors": ['span:has-text("容器管理")'], "filename": "03_container"},
        {"name": "容器列表", "selectors": ['.el-menu-item:has-text("容器列表")'], "filename": "03_container_list"},
        {"name": "镜像仓库", "selectors": ['.el-menu-item:has-text("镜像仓库")'], "filename": "03_image_repo"},
        
        # 持续集成
        {"name": "持续集成_父菜单", "selectors": ['span:has-text("持续集成")'], "filename": "04_ci"},
        {"name": "流水线", "selectors": ['.el-menu-item:has-text("流水线")'], "filename": "04_pipeline"},
        {"name": "构建历史", "selectors": ['.el-menu-item:has-text("构建历史")'], "filename": "04_build_history"},
        
        # 发布中心
        {"name": "发布中心_父菜单", "selectors": ['span:has-text("发布中心")'], "filename": "05_deploy"},
        {"name": "应用管理", "selectors": ['.el-menu-item:has-text("应用管理")'], "filename": "05_app_manage"},
        {"name": "发布记录", "selectors": ['.el-menu-item:has-text("发布记录")'], "filename": "05_deploy_record"},
        
        # 堡垒机
        {"name": "堡垒机_父菜单", "selectors": ['span:has-text("堡垒机")'], "filename": "06_jumpserver"},
        {"name": "会话管理", "selectors": ['.el-menu-item:has-text("会话管理")'], "filename": "06_session"},
        
        # 命令控制台
        {"name": "命令控制台", "selectors": ['.el-menu-item:has-text("命令控制台")'], "filename": "07_console"},
        
        # 监控中心
        {"name": "监控中心_父菜单", "selectors": ['span:has-text("监控中心")'], "filename": "08_monitor"},
        {"name": "主机监控", "selectors": ['.el-menu-item:has-text("主机监控")'], "filename": "08_host_monitor"},
        {"name": "容器监控", "selectors": ['.el-menu-item:has-text("容器监控")'], "filename": "08_container_monitor"},
        {"name": "网络监控", "selectors": ['.el-menu-item:has-text("网络监控")'], "filename": "08_network_monitor"},
        {"name": "日志监控", "selectors": ['.el-menu-item:has-text("日志监控")'], "filename": "08_log_monitor"},
        
        # 告警中心
        {"name": "告警中心_父菜单", "selectors": ['span:has-text("告警中心")'], "filename": "09_alert"},
        {"name": "告警策略", "selectors": ['.el-menu-item:has-text("告警策略")'], "filename": "09_alert_policy"},
        {"name": "告警记录", "selectors": ['.el-menu-item:has-text("告警记录")'], "filename": "09_alert_record"},
        {"name": "告警联系人", "selectors": ['.el-menu-item:has-text("告警联系人")'], "filename": "09_alert_contact"},
        
        # 资产管理
        {"name": "资产管理_父菜单", "selectors": ['span:has-text("资产管理")'], "filename": "10_asset"},
        {"name": "组织架构", "selectors": ['.el-menu-item:has-text("组织架构")'], "filename": "10_org"},
        {"name": "业务线", "selectors": ['.el-menu-item:has-text("业务线")'], "filename": "10_business"},
        {"name": "机房管理", "selectors": ['.el-menu-item:has-text("机房管理")'], "filename": "10_idc"},
        {"name": "设备列表", "selectors": ['.el-menu-item:has-text("设备列表")'], "filename": "10_device"},
        {"name": "IP地址管理", "selectors": ['.el-menu-item:has-text("IP地址管理")'], "filename": "10_ip"},
        
        # 系统管理
        {"name": "系统管理_父菜单", "selectors": ['span:has-text("系统管理")'], "filename": "11_system"},
        {"name": "用户管理", "selectors": ['.el-menu-item:has-text("用户管理")'], "filename": "11_user"},
        {"name": "角色管理", "selectors": ['.el-menu-item:has-text("角色管理")'], "filename": "11_role"},
        {"name": "系统设置", "selectors": ['.el-menu-item:has-text("系统设置")'], "filename": "11_settings"},
    ]

def try_click_element(page, selectors, max_retries=2):
    """尝试点击元素"""
    for selector in selectors:
        try:
            page.click(selector, timeout=3000)
            return True, selector
        except:
            continue
    return False, None

def explore_platform():
    """探索平台所有功能"""
    
    login_url = "https://demo.opsany.com/o/workbench/?login=1#/"
    username = "demo"
    password = "123456.coM"
    
    results = []
    
    with sync_playwright() as p:
        # 启动浏览器
        browser = p.chromium.launch(headless=False, slow_mo=200)
        context = browser.new_context(viewport={"width": 1920, "height": 1080})
        page = context.new_page()
        
        # 访问登录页面
        print("=" * 50)
        print("开始登录 OpsAny 平台...")
        print("=" * 50)
        page.goto(login_url)
        page.wait_for_load_state("networkidle")
        time.sleep(2)
        
        # 登录
        page.fill('input[name="username"]', username)
        page.fill('input[name="password"]', password)
        page.click('button[type="submit"]')
        page.wait_for_load_state("networkidle")
        time.sleep(3)
        
        print(f"登录成功! 当前页面: {page.title()}")
        
        # 等待菜单加载
        time.sleep(2)
        
        # 截图登录后的初始状态
        page.screenshot(path=f"{screenshot_dir}/00_login_success.png")
        print(f"截图保存: {screenshot_dir}/00_login_success.png")
        
        # 遍历主要功能模块
        nav_items = get_nav_items()
        
        for item in nav_items:
            print(f"\n探索: {item['name']}...")
            try:
                # 尝试点击导航
                success, selector = try_click_element(page, item['selectors'])
                if success:
                    print(f"  点击成功: {selector}")
                    time.sleep(2)
                    page.wait_for_load_state("networkidle")
                    time.sleep(1)
                else:
                    print(f"  点击失败")
                
                # 截图
                filepath = f"{screenshot_dir}/{item['filename']}.png"
                page.screenshot(path=filepath)
                print(f"  截图保存: {filepath}")
                results.append({
                    "name": item['name'], 
                    "filename": f"{item['filename']}.png", 
                    "success": True
                })
                
            except Exception as e:
                print(f"  探索失败: {e}")
                results.append({
                    "name": item['name'], 
                    "filename": f"{item['filename']}.png", 
                    "success": False,
                    "error": str(e)
                })
        
        print("\n" + "=" * 50)
        print("探索完成!")
        print("=" * 50)
        print(f"共探索 {len(results)} 个功能模块")
        success_count = sum(1 for r in results if r.get('success'))
        print(f"成功: {success_count}")
        
        print("\n按 Enter 键关闭浏览器...")
        input()
        
        browser.close()
    
    return results

if __name__ == "__main__":
    try:
        results = explore_platform()
    except Exception as e:
        print(f"探索过程出错: {e}")
        import traceback
        traceback.print_exc()
