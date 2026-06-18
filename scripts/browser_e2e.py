#!/usr/bin/env python3
"""HRMS Browser E2E Test - using Playwright (Python)
Tests key UI flows in Chromium browser
"""
import json
import time
from playwright.sync_api import sync_playwright

BASE = 'http://localhost:5173'
PASSWORD = 'Admin@2026'

results = []


def log(tc_id, title, passed, note='', ms=0):
    results.append({'id': tc_id, 'title': title, 'passed': passed, 'note': note, 'ms': ms})
    print(f"{'✅' if passed else '❌'} {tc_id} {title} ({ms}ms) {note}")


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(viewport={'width': 1440, 'height': 900})
        page = context.new_page()

        console_errors = []
        page.on('pageerror', lambda err: console_errors.append(f'pageerror: {err.message}'))
        page.on('console', lambda msg: console_errors.append(f'console.error: {msg.text}') if msg.type == 'error' else None)

        # ============ TC-UI-001 登录页加载 ============
        t0 = time.time()
        try:
            page.goto(BASE + '/login', wait_until='networkidle', timeout=10000)
            title = page.locator('.login-title').first.text_content() or ''
            has_username = page.locator('input[autocomplete="username"]').count() > 0
            has_password = page.locator('input[type="password"]').count() > 0
            passed = 'HRMS' in title and has_username and has_password
            log('TC-UI-001', '登录页加载', passed, f'title="{title}"', int((time.time()-t0)*1000))
        except Exception as e:
            log('TC-UI-001', '登录页加载', False, str(e)[:80], int((time.time()-t0)*1000))

    # ============ TC-UI-002 登录成功跳转 ============
    t0 = time.time()
    try:
        page.locator('input[autocomplete="username"]').fill('admin')
        page.locator('input[type="password"]').fill(PASSWORD)
        page.locator('.login-btn').click()
        page.wait_for_url('**/dashboard', timeout=10000)
        url = page.url
        passed = '/dashboard' in url
        log('TC-UI-002', '登录成功跳转 dashboard', passed, f'url={url}', int((time.time()-t0)*1000))
    except Exception as e:
        log('TC-UI-002', '登录成功跳转 dashboard', False, str(e)[:80], int((time.time()-t0)*1000))

        # ============ TC-UI-003 Dashboard 渲染 ============
        t0 = time.time()
        try:
            page.wait_for_load_state('networkidle', timeout=5000)
            body_text = page.locator('body').text_content() or ''
            has_content = len(body_text) > 50
            log('TC-UI-003', 'Dashboard 渲染', has_content, f'len={len(body_text)}', int((time.time()-t0)*1000))
        except Exception as e:
            log('TC-UI-003', 'Dashboard 渲染', False, str(e)[:80], int((time.time()-t0)*1000))

        # 捕获每个页面加载时的错误
        def test_page(tc_id, title, path):
            t0 = time.time()
            try:
                page.goto(BASE + path, wait_until='networkidle', timeout=8000)
                page.wait_for_timeout(1500)
                error_msg = page.locator('.el-message--error').first
                error_text = error_msg.text_content() if error_msg.count() > 0 else None
                if error_text:
                    log(tc_id, title, False, f'error="{error_text[:60]}"', int((time.time()-t0)*1000))
                    return
                body_text = page.locator('body').text_content() or ''
                tables = page.locator('table').count()
                trees = page.locator('.el-tree').count()
                forms = page.locator('form').count()
                # 至少有 table/tree/form 或有内容
                passed = tables > 0 or trees > 0 or forms > 0 or len(body_text) > 200
                detail = f'tables={tables} trees={trees} forms={forms} len={len(body_text)}'
                log(tc_id, title, passed, detail, int((time.time()-t0)*1000))
            except Exception as e:
                log(tc_id, title, False, str(e)[:80], int((time.time()-t0)*1000))

        test_page('TC-UI-004', '员工列表页加载', '/hr/employees')
        test_page('TC-UI-005', '部门树页加载', '/org/tree')
        test_page('TC-UI-006', '工资批次页加载', '/payroll/runs')
        test_page('TC-UI-007', '招聘 Offer 页加载', '/recruit/offers')
        test_page('TC-UI-008', '绩效页加载', '/performance/reviews')
        test_page('TC-UI-009', '角色管理页加载', '/system/roles')
        test_page('TC-UI-010', 'ESS 自助页加载', '/portal/ess')
        test_page('TC-UI-011', 'MSS 自助页加载', '/portal/mss')
        test_page('TC-UI-012', '请假新建页加载', '/attendance/leave/new')

        # ============ TC-UI-013 班次排班页（无独立路由，测试 shift schedule） ============
        t0 = time.time()
        try:
            # 检查菜单项
            page.goto(BASE + '/dashboard', wait_until='networkidle', timeout=5000)
            page.wait_for_timeout(800)
            # 不一定有此路径，尝试 /attendance/shifts
            page.goto(BASE + '/attendance/shifts', wait_until='networkidle', timeout=5000)
            page.wait_for_timeout(800)
            body_text = page.locator('body').text_content() or ''
            error_msg = page.locator('.el-message--error').first
            error_text = error_msg.text_content() if error_msg.count() > 0 else None
            passed = (not error_text) and len(body_text) > 50
            log('TC-UI-013', '班次页可达性', passed, f'err={error_text} len={len(body_text)}', int((time.time()-t0)*1000))
        except Exception as e:
            log('TC-UI-013', '班次页可达性', False, str(e)[:80], int((time.time()-t0)*1000))

        # ============ TC-UI-014 错误密码登录 ============
        t0 = time.time()
        try:
            page.evaluate('localStorage.clear()')
            page.goto(BASE + '/login', wait_until='networkidle')
            page.locator('input[autocomplete="username"]').fill('admin')
            page.locator('input[type="password"]').fill('wrongpass')
            page.locator('.login-btn').click()
            page.wait_for_timeout(2000)
            error_msg = page.locator('.el-message--error').first
            error_text = error_msg.text_content() if error_msg.count() > 0 else None
            still_on_login = '/login' in page.url
            passed = bool(error_text) and still_on_login
            log('TC-UI-014', '错误密码被拒', passed, f'err="{error_text}" url={page.url}', int((time.time()-t0)*1000))
        except Exception as e:
            log('TC-UI-014', '错误密码被拒', False, str(e)[:80], int((time.time()-t0)*1000))

        # 截图
        try:
            page.screenshot(path='/tmp/hrms-e2e-final.png', full_page=True)
        except Exception:
            pass

        # 汇总
        total = len(results)
        passed = sum(1 for r in results if r['passed'])
        failed = total - passed
        rate = (passed / total * 100) if total else 0
        print(f"\n{'='*60}")
        print(f"Browser E2E: total={total} pass={passed} fail={failed} rate={rate:.1f}%")
        print(f"Console errors: {len(console_errors)}")
        print(f"{'='*60}\n")
        if console_errors:
            print('=== Console Errors (前 15 条) ===')
            for e in console_errors[:15]:
                print(' - ' + e[:150])

        with open('/tmp/hrms-browser-results.json', 'w') as f:
            json.dump({
                'total': total,
                'passed': passed,
                'failed': failed,
                'rate': rate,
                'consoleErrors': console_errors,
                'results': results,
            }, f, ensure_ascii=False, indent=2)

        browser.close()


if __name__ == '__main__':
    main()
