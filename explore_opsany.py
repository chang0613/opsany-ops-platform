#!/usr/bin/env python3
"""
OpsAny 平台功能探索脚本
自动遍历所有功能模块并截图
"""

from playwright.sync_api import sync_playwright
import time
import os

# 创建截图目录
screenshot_dir = "screenshots"
os.makedirs(screenshot_dir, exist_ok=True)

def get_nav_items():
    """获取导航菜单项和对应的截图名称"""
    return [
        {"name": "工作台", "selectors": ['text="工作台"', 'a:has-text("工作台")', '.el-menu-item:has-text("工作台")'], "filename": "01_workbench"},
        {"name": "概览", "selectors": ['text="概览"'], "filename": "01_overview"},
        {"name": "主机管理", "selectors": ['text="主机管理"', 'a:has-text("主机管理")'], "filename": "02_host"},
        {"name": "主机列表", "selectors": ['text="主机列表"'], "filename": "02_host_list"},
        {"name": "主机组", "selectors": ['text="主机组"'], "filename": "02_host_group"},
        {"name": "容器管理", "selectors": ['text="容器管理"'], "filename": "03_container"},
        {"name": "容器列表", "selectors": ['text="容器列表"'], "filename": "03_container_list"},
        {"name": "镜像仓库", "selectors": ['text="镜像仓库"'], "filename": "03_image_repo"},
        {"name": "持续集成", "selectors": ['text="持续集成"'], "filename": "04_ci"},
        {"name": "流水线", "selectors": ['text="流水线"'], "filename": "04_pipeline"},
        {"name": "构建历史", "selectors": ['text="构建历史"'], "filename": "04_build_history"},
        {"name": "发布中心", "selectors": ['text="发布中心"'], "filename": "05_deploy"},
        {"name": "应用管理", "selectors": ['text="应用管理"'], "filename": "05_app_manage"},
        {"name": "发布记录", "selectors": ['text="发布记录"'], "filename": "05_deploy_record"},
        {"name": "堡垒机", "selectors": ['text="堡垒机"'], "filename": "06_jumpserver"},
        {"name": "会话管理", "selectors": ['text="会话管理"'], "filename": "06_session"},
        {"name": "命令控制台", "selectors": ['text="命令控制台"'], "filename": "07_console"},
        {"name": "监控中心", "selectors": ['text="监控中心"'], "filename": "08_monitor"},
        {"name": "主机监控", "selectors": ['text="主机监控"'], "filename": "08_host_monitor"},
        {"name": "告警中心", "selectors": ['text="告警中心"'], "filename": "09_alert"},
        {"name": "告警策略", "selectors": ['text="告警策略"'], "filename": "09_alert_policy"},
        {"name": "告警记录", "selectors": ['text="告警记录"'], "filename": "09_alert_record"},
        {"name": "资产管理", "selectors": ['text="资产管理"'], "filename": "10_asset"},
        {"name": "组织架构", "selectors": ['text="组织架构"'], "filename": "10_org"},
        {"name": "用户管理", "selectors": ['text="用户管理"'], "filename": "11_user"},
        {"name": "角色管理", "selectors": ['text="角色管理"'], "filename": "11_role"},
        {"name": "系统设置", "selectors": ['text="系统设置"'], "filename": "12_settings"},
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
        # 启动浏览器（非无头模式以便观察）
        browser = p.chromium.launch(headless=False, slow_mo=300)
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
        
        # 获取导航菜单
        print("\n获取导航菜单结构...")
        
        # 等待菜单加载
        time.sleep(2)
        
        # 尝试获取左侧菜单的所有菜单项
        try:
            menu_items = page.query_selector_all('.el-menu-item, .el-submenu__title')
            print(f"找到 {len(menu_items)} 个菜单项")
            for i, item in enumerate(menu_items[:20]):  # 只打印前20个
                try:
                    text = item.inner_text().strip()[:30]
                    print(f"  {i+1}. {text}")
                except:
                    pass
        except Exception as e:
            print(f"获取菜单失败: {e}")
        
        # 截图登录后的初始状态
        page.screenshot(path=f"{screenshot_dir}/00_login_success.png")
        print(f"截图保存: {screenshot_dir}/00_login_success.png")
        results.append({"name": "登录成功", "filename": "00_login_success.png", "success": True})
        
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
                    print(f"  点击失败，尝试其他方式...")
                
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
        
        # 尝试展开所有子菜单
        print("\n" + "=" * 50)
        print("尝试展开子菜单...")
        print("=" * 50)
        
        try:
            # 点击展开所有子菜单
            submenu_toggles = page.query_selector_all('.el-submenu')
            for i, submenu in enumerate(submenu_toggles):
                try:
                    submenu.click()
                    time.sleep(0.5)
                    print(f"展开子菜单 {i+1}")
                except:
                    pass
            
            # 截图展开后的菜单
            page.screenshot(path=f"{screenshot_dir}/00_menu_expanded.png")
            print(f"菜单展开截图保存: {screenshot_dir}/00_menu_expanded.png")
        except Exception as e:
            print(f"展开菜单失败: {e}")
        
        # 保持浏览器打开
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
        
        # 打印结果摘要
        print("\n" + "=" * 50)
        print("探索结果摘要:")
        print("=" * 50)
        for r in results:
            status = "✓" if r.get('success') else "✗"
            print(f"{status} {r['name']}: {r['filename']}")
            
    except Exception as e:
        print(f"探索过程出错: {e}")
        import traceback
        traceback.print_exc()
        input("按 Enter 键退出...")
