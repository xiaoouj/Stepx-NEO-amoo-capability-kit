#!/usr/bin/env python3
"""Build a readable inventory from saved device documentation and registration metadata."""
import collections
import hashlib
import json
import pathlib
import re

ROOT = pathlib.Path(__file__).resolve().parents[1]
DOCS = ROOT / '.local/amoo/claw-openclaw/package/skills/android-abilities'
OUTPUT = ROOT / 'reports/amoo-ability-catalog-2026-10-22'
CATEGORIES = [
    ('system_settings', '系统设置'), ('phone_comm', '电话与短信'), ('contacts', '联系人与黑名单'),
    ('app_device', '应用、设备、日历与实用工具'), ('media_gallery', '相册与媒体编辑'),
    ('file_memory', '文件、笔记与待办'), ('map_navigation', '定位、地图与导航'),
    ('scheduled_tasks', '时钟、定时任务与网关'), ('web_search', '网络搜索与网页'),
    ('session_runtime', '会话与运行时'), ('log_report', '日志录制'),
]
VERIFIED = {
    'open_app_by_package': '已实测；参数为packageName，可传className',
    'settings_get_auto_brightness_state': '已实测查询',
    'appstore_search_app': '已实测，只搜索',
    'appstore_install_app': '已实测，商店下载安装QQ音乐成功',
    'executor_open_settings': '已实测；原生新会话打开无线调试/开发者选项',
    'executor_navigate': '已实测；原生新会话home/back；recents未逐项测',
    'executor_search_settings': '已实测；原生调用查询无线调试',
    'executor_get_tool_status': '已实测；原生调用成功',
    'gui_agent': '不可用：实机返回GUI自动化功能暂未开放',
    'settings_open_settings_keyboard_settings_activity': '文档过期：实机返回20004未找到对应原子能力',
    'open_app_by_name': '已确认入口；AMOOsic曾被启动签名检查拦截，不能据此判断所有目标',
}


def plain(text):
    text = re.sub(r'\*\*|`', '', text).strip('- ').strip()
    text = re.sub(r'\[[^]]*\]\([^)]*\)', '', text)
    return text.replace('|', '／').replace('\n', ' ')


def extract(category, title):
    path = DOCS / (category + '.md'); text = path.read_text()
    found = list(re.finditer(r'ability\.call\s+method\s*=\s*["\']([^"\']+)["\']', text))
    rows = []
    for i, match in enumerate(found):
        method = match[1]
        limit = found[i+1].start() if i+1 < len(found) else len(text)
        end_fence = text.find('```', match.end())
        after = text[end_fence+3:limit] if end_fence >= 0 else ''
        paragraphs = [x.strip() for x in after.splitlines() if x.strip() and not x.startswith(('#', '---', '>', '|', '```'))]
        description = plain(paragraphs[0]) if paragraphs else method
        description = re.split(r'可选参数[：:]|参数说明[：:]', description)[0].strip()
        if len(description) > 130:
            description = description[:127] + '…'
        if method == 'get_current_location': description = '获取设备当前实时地理位置（城市／区域级）'
        if method == 'start_record_log': description = '开始采集通用或指定类别的手机日志'
        if method == 'stop_record_log': description = '停止当前日志录制并保存；文档称系统会恢复通用录制'
        inline = text[match.end():end_fence] if end_fence >= 0 else ''
        params_match = re.search(r"params='([^\n]*)'", inline)
        params = params_match[1] if params_match else ''
        if method == 'open_app_by_package': params = '{"packageName":"<应用包名>","className":"<可选Activity类名>"}'
        cloud = category in ['web_search', 'session_runtime'] or (category == 'map_navigation' and method != 'get_current_location') or (category == 'scheduled_tasks' and (method.startswith('cron ') or method in ['gateway', 'get-current-time']))
        rows.append({'category':category, 'categoryTitle':title, 'method':method,
            'description':description, 'location':'云侧参考' if cloud else '原目录候选／未逐项定位',
            'status':VERIFIED.get(method, '仅文档候选，未逐项实测'), 'paramsTemplate':params,
            'source':str(path.relative_to(ROOT)), 'sourceLine':text[:match.start()].count('\n')+1})
    return rows


def main():
    entries = []
    for c, title in CATEGORIES:
        entries.extend(extract(c, title))
    assert len(entries) == len({r['method'] for r in entries}) == 397
    extra = [
        ('gui', '通用GUI自动化', 'gui_agent', '第三方App内多步界面交互；当前发行版执行总闸关闭', '{"app_name":"<App名>","task":"<任务>"}'),
        ('appstore', '商店原子能力（补充）', 'appstore_search_app', '在自带应用商店搜索应用，不触发安装', '{"keyword":"<应用名>"}'),
        ('appstore', '商店原子能力（补充）', 'appstore_install_app', '通过自带应用商店下载安装应用；精确匹配应用展示名称', '{"appName":"QQ音乐"}'),
        ('executor', '本次新增的手机端工具', 'executor_open_settings', '直达开发者选项、无线调试、输入法或电池健康页，不切换开关', '{"page":"developer|wireless-debugging|keyboard|battery-health"}'),
        ('executor', '本次新增的手机端工具', 'executor_navigate', '返回桌面、上一页或最近任务；home不强制结束进程', '{"action":"home|back|recents"}'),
        ('executor', '本次新增的手机端工具', 'executor_search_settings', '按关键词查询本机设置静态索引', '{"query":"无线调试"}'),
        ('executor', '本次新增的手机端工具', 'executor_get_tool_status', '检查本机执行服务是否就绪', '{}'),
    ]
    for c, title, method, desc, params in extra:
        entries.append({'category':c,'categoryTitle':title,'method':method,'description':desc,
            'location':'端侧入口','status':VERIFIED[method],'paramsTemplate':params,
            'source':'本工作区实机调查报告','sourceLine':None})
    assert len(entries) == len({r['method'] for r in entries}) == 404
    live = json.loads((ROOT/'.local/amoo/ability-catalog-live.json').read_text())
    counts = collections.Counter(r['categoryTitle'] for r in entries)
    cloud_count = sum(r['location'] == '云侧参考' for r in entries)
    result = {'date':'2026-10-22','scope':'397 documentation entries + GUI entry + 2 store methods + 4 custom executor methods; not a runtime success count',
        'methodCount':len(entries),'cloudReferenceCount':cloud_count,'registeredAppFunctionCount':len(live),
        'categories':dict(counts),'entries':entries,'registeredAppFunctions':live}
    OUTPUT.with_suffix('.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    lines = ['# AMOO 原子能力与工具完整清单 — 2026-10-22', '',
        '本清单列出404个不同调用名称：手机包内文档397项、另列gui_agent 1项、已验证商店方法2项、本次新增端侧方法4项。其中有'+str(cloud_count)+'项在文档中明确标为云侧参考；它们不能全部算作手机端原子能力，也不能把404理解为全部可用。', '',
        '另外读取了手机系统当前登记的15个AppFunctions，全文放在末尾。它们与调用名称表有重叠，不能直接相加。登记、默认启用和真实调用成功是不同状态。本轮只读取清单，没有批量执行电话、短信、删除文件、改设置等动作。', '',
        '参数为调用形态参考，不是完整schema。带“|”的值表示多个选项，实际调用应选其中一个。多数旧文档未逐项验证，可能有旧别名或参数错误；已知open_app_by_package的package已改为实机packageName。', '',
        '## 分类总览', '', '| 分类 | 调用名称数 |', '| --- | ---: |']
    for title, count in counts.items(): lines.append('| '+title+' | '+str(count)+' |')
    lines += ['', '## 已确认的限制', '',
        '- gui_agent：当前直接返回“GUI自动化功能暂未开放”，不能因为文档列出就承诺可操作微信等App。',
        '- 输入法旧别名settings_open_settings_keyboard_settings_activity：返回20004；已有新工具executor_open_settings支持keyboard。',
        '- AMOOsic的启动曾被签名信任检查拦截；这不等于所有应用都不能打开。',
        '- 新增executor_*的页面启动与导航依赖手机当前Root执行服务；索引查询由普通应用自身完成。', '',
        '## 完整调用名称', '']
    index = 0
    for title in counts:
        group = [r for r in entries if r['categoryTitle'] == title]
        lines += ['### '+title+'（'+str(len(group))+'）', '', '| 编号 | 中文用途 | 调用名称 | 执行位置／性质 | 验证状态 |', '| ---: | --- | --- | --- | --- |']
        for row in group:
            index += 1
            lines.append('| '+str(index)+' | '+plain(row['description'])+' | `'+row['method']+'` | '+row['location']+' | '+row['status']+' |')
        lines.append('')
    lines += ['## 手机系统当前登记的AppFunctions（15个）', '',
        '这是AppFunctions登记机制覆盖的部分，不包含手机所有旧MCP原子服务。以下默认启用均来自登记元数据，不代表全部做过实机操作测试。', '']
    labels = {
        'thirdPartyAbilityAlipayXuiService':'支付宝xUI服务执行',
        'thirdPartyAbilityAlipayLifeService':'支付宝生活服务直达',
        'execute':'Agent手机控制器：已验原生启动、点击、输入、滚动；其他动作未测',
        'observe':'Agent手机控制器：读取允许应用的真实无障碍树与页面快照（已验原生调用）',
        'health':'Agent手机控制器：连接、授权与健康状态（已验原生调用）',
        'visualObserve':'Agent手机控制器：允许应用截图与本地OCR，需用户开启视觉观察（未实测）',
        'searchApp':'应用商店搜索（已实测）','installApp':'应用商店下载安装（已实测）',
        'openSelfieMode':'相机自拍模式（未实测）','openBeautyMode':'相机美颜模式（未实测）',
        'searchSettings':'本机设置索引查询（已实测）','openSettings':'本机设置页面直达（已实测）',
        'getToolStatus':'本机执行服务健康检查（已实测）','navigate':'本机系统导航（已实测home/back）',
        'get_current_location_weather':'当前地点天气查询（未实测）'}
    for pkg in dict.fromkeys(x['package'] for x in live):
        lines += ['### '+pkg,'','| 中文用途 | 系统functionId | 默认启用 |','| --- | --- | --- |']
        for item in live:
            if item['package'] != pkg:continue
            fid = item['functionId']; method=fid.split('#')[-1]
            lines.append('| '+labels.get(method, method)+' | `'+fid+'` | '+('是' if item['enabledByDefault'] else '否／未标明')+' |')
        lines.append('')
    lines += ['## 来源与后续使用', '',
        '- 原目录来自本工作区留存的手机APK内android-abilities文档，397个方法已按名称去重。',
        '- 与StepBridge公开的ability-catalog.json进行了名称集合核对；该项目也明确称其为文档候选，而不是实时可用列表。',
        '- 系统AppFunctions取自本次手机cmd app_function list-app-functions的只读结果。',
        '- 已实测状态来自本工作区当日的设置、安装及端侧工具报告；未逐项测试全部目录。',
        '- JSON同名文件包含全部条目的来源行号、参数模板和登记函数参数，便于按需继续验证。', '']
    OUTPUT.with_suffix('.md').write_text('\n'.join(lines))
    remote = json.loads((ROOT/'.local/amoo/stepbridge-ability-catalog.json').read_text())
    remote_names = {x['method'] for c in remote['categories'] for x in c['examples']}
    local_names = {x['method'] for x in entries[:397]}
    assert local_names == remote_names
    assert index == 404
    print(json.dumps({'methods':len(entries),'cloudReferences':cloud_count,'registeredAppFunctions':len(live),'categories':dict(counts),'directoryMatchesStepBridge':True},ensure_ascii=False,indent=2))


if __name__ == '__main__': main()
