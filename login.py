#!/usr/bin/env python3
"""
OpsAny 平台自动登录脚本
使用 Playwright 实现
"""

from playwright.sync_api import sync_playwright
import time

def login_opsany():
    """自动化登录 OpsAny 平台"""
    
    # 登录凭据
    username = "demo"
    password = "123456.coM"
    login_url = "https://demo.opsany.com/o/workbench/?login=1#/"
    
    print(f"正在启动浏览器并访问: {login_url}")
    
    with sync_playwright() as p:
        # 启动 Chromium 浏览器（无头模式设置为 False 以便观察）
        browser = p.chromium.launch(headless=False, slow_mo=500)
        context = browser.new_context(viewport={"width": 1280, "height": 800})
        page = context.new_page()
        
        # 访问登录页面
        print("正在访问登录页面...")
        page.goto(login_url)
        
        # 等待页面加载
        page.wait_for_load_state("networkidle")
        time.sleep(2)
        
        # 打印页面标题确认访问成功
        print(f"页面标题: {page.title()}")
        
        # 尝试查找并填写用户名
        print("正在填写用户名...")
        
        # 尝试多种可能的选择器来定位用户名输入框
        username_selectors = [
            'input[name="username"]',
            'input[id="username"]',
            'input[type="text"]',
            'input[placeholder*="用户"]',
            'input[placeholder*="账号"]',
            'input[placeholder*="username"]',
            'input[autocomplete="username"]',
        ]
        
        username_filled = False
        for selector in username_selectors:
            try:
                page.fill(selector, username, timeout=3000)
                print(f"  ✓ 用户名已填写: {selector}")
                username_filled = True
                break
            except:
                continue
        
        if not username_filled:
            # 打印页面结构帮助调试
            print("  未能自动找到用户名输入框，尝试点击第一个输入框...")
            try:
                page.click('input[type="text"]', timeout=3000)
                page.keyboard.type(username)
                print("  ✓ 用户名已通过键盘输入")
            except:
                print("  ✗ 无法找到用户名输入框")
                page.screenshot(filename="debug_before_login.png")
                print("  已保存截图: debug_before_login.png")
        
        # 尝试填写密码
        print("正在填写密码...")
        
        password_selectors = [
            'input[name="password"]',
            'input[id="password"]',
            'input[type="password"]',
            'input[placeholder*="密码"]',
            'input[placeholder*="password"]',
            'input[autocomplete="current-password"]',
        ]
        
        password_filled = False
        for selector in password_selectors:
            try:
                page.fill(selector, password, timeout=3000)
                print(f"  ✓ 密码已填写: {selector}")
                password_filled = True
                break
            except:
                continue
        
        if not password_filled:
            print("  ✗ 无法找到密码输入框")
        
        # 查找并点击登录按钮
        print("正在点击登录按钮...")
        
        button_selectors = [
            'button[type="submit"]',
            'button:has-text("登录")',
            'button:has-text("登录")',
            'input[type="submit"]',
            'a:has-text("登录")',
            'button:has-text("登 录")',
        ]
        
        login_clicked = False
        for selector in button_selectors:
            try:
                page.click(selector, timeout=3000)
                print(f"  ✓ 已点击登录按钮: {selector}")
                login_clicked = True
                break
            except:
                continue
        
        if not login_clicked:
            # 尝试按 Enter 键
            try:
                page.keyboard.press("Enter")
                print("  ✓ 已按 Enter 键提交")
            except:
                print("  ✗ 无法提交表单")
        
        # 等待登录结果
        print("等待登录结果...")
        time.sleep(5)
        
        # 截图保存登录后的状态
        page.screenshot(path="login_result.png")
        print("已保存截图: login_result.png")
        
        # 打印当前 URL
        print(f"当前页面 URL: {page.url}")
        
        # 获取页面标题
        print(f"当前页面标题: {page.title()}")
        
        # 保持浏览器打开，方便查看
        print("\n登录流程完成！浏览器将保持打开状态以便您查看。")
        print("按 Enter 键关闭浏览器...")
        
        input()
        
        browser.close()

if __name__ == "__main__":
    try:
        login_opsany()
    except Exception as e:
        print(f"登录过程中出现错误: {e}")
        import traceback
        traceback.print_exc()
        input("按 Enter 键退出...")
