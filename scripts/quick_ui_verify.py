#!/usr/bin/env python3
"""Quick verification: BUG-005 fix - login + page navigation works
Creates a fresh browser context for each test scenario.
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


def fresh_page(p, scenario_fn):
    """Run a scenario in a fresh browser context to isolate state."""
    browser = p.chromium.launch(headless=True)
    context = browser.new_context(viewport={'width': 1440, 'height': 900})
    page = context.new_page()
    errors = []
    page.on('pageerror', lambda e: errors.append(f'pageerror: {e.message}'))
    try:
        return scenario_fn(page), errors
    finally:
        browser.close()


def scenario_login(p):
    t0 = time.time()
    p.goto(BASE + '/login', wait_until='networkidle', timeout=10000)
    p.locator('input[autocomplete="username"]').fill('admin')
    p.locator('input[type="password"]').fill(PASSWORD)
    p.locator('.login-btn').click()
    p.wait_for_url('**/dashboard', timeout=10000)
    return {'url': p.url, 'ms': int((time.time() - t0) * 1000)}


def scenario_employees(p):
    t0 = time.time()
    p.goto(BASE + '/login', wait_until='networkidle', timeout=10000)
    p.locator('input[autocomplete="username"]').fill('admin')
    p.locator('input[type="password"]').fill(PASSWORD)
    p.locator('.login-btn').click()
    p.wait_for_url('**/dashboard', timeout=10000)
    p.goto(BASE + '/hr/employees', wait_until='networkidle', timeout=8000)
    p.wait_for_timeout(1500)
    body_text = p.locator('body').text_content() or ''
    error_msg = p.locator('.el-message--error').first
    error_text = error_msg.text_content() if error_msg.count() > 0 else None
    return {'body_len': len(body_text), 'err': error_text, 'ms': int((time.time() - t0) * 1000)}


def scenario_org_tree(p):
    t0 = time.time()
    p.goto(BASE + '/login', wait_until='networkidle', timeout=10000)
    p.locator('input[autocomplete="username"]').fill('admin')
    p.locator('input[type="password"]').fill(PASSWORD)
    p.locator('.login-btn').click()
    p.wait_for_url('**/dashboard', timeout=10000)
    p.goto(BASE + '/org/tree', wait_until='networkidle', timeout=8000)
    p.wait_for_timeout(1500)
    body_text = p.locator('body').text_content() or ''
    error_msg = p.locator('.el-message--error').first
    error_text = error_msg.text_content() if error_msg.count() > 0 else None
    return {'body_len': len(body_text), 'err': error_text, 'ms': int((time.time() - t0) * 1000)}


def scenario_wrong_pwd(p):
    t0 = time.time()
    p.goto(BASE + '/login', wait_until='networkidle', timeout=10000)
    p.locator('input[autocomplete="username"]').fill('admin')
    p.locator('input[type="password"]').fill('wrong')
    p.locator('.login-btn').click()
    p.wait_for_timeout(2500)
    error_msg = p.locator('.el-message--error').first
    error_text = error_msg.text_content() if error_msg.count() > 0 else None
    return {'err': error_text, 'url': p.url, 'ms': int((time.time() - t0) * 1000)}


with sync_playwright() as p:
    # TC-UI-002: Login + dashboard
    r, errs = fresh_page(p, scenario_login)
    log('TC-UI-002', '登录跳转 dashboard', '/dashboard' in r['url'], f"url={r['url']}", r['ms'])

    # TC-UI-004: 员工列表
    r, errs = fresh_page(p, scenario_employees)
    passed = not r['err'] and r['body_len'] > 100
    log('TC-UI-004', '员工列表页加载', passed, f"len={r['body_len']} err={r['err']}", r['ms'])

    # TC-UI-005: 部门树
    r, errs = fresh_page(p, scenario_org_tree)
    passed = not r['err'] and r['body_len'] > 100
    log('TC-UI-005', '部门树页加载', passed, f"len={r['body_len']} err={r['err']}", r['ms'])

    # TC-UI-014: 错误密码
    r, errs = fresh_page(p, scenario_wrong_pwd)
    passed = r['err'] is not None and '/login' in r['url']
    log('TC-UI-014', '错误密码被拒', passed, f"err={r['err']!r} url={r['url']}", r['ms'])

total = len(results)
passed = sum(1 for r in results if r['passed'])
print(f"\nQuick verify: {passed}/{total} = {passed/total*100:.1f}%")
with open('/tmp/hrms-quick-verify.json', 'w') as f:
    json.dump({'total': total, 'passed': passed, 'results': results}, f, ensure_ascii=False, indent=2)
