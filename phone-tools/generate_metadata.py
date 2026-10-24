#!/usr/bin/env python3
import pathlib
import xml.etree.ElementTree as ET

BASE = pathlib.Path(__file__).resolve().parent
FUNCTIONS = [
    ('openSettings', '补充高级设置直达：开发者选项、无线调试、输入法设置、电池健康度。原生能力可用时优先原生；用户指定本机工具或原生缺少入口、调用失败时使用。只打开页面，不切换开关。', [('page', '页面名称或id：developer/开发者选项、wireless-debugging/无线调试、keyboard/输入法设置、battery-health/电池健康度', True)]),
    ('navigate', '本机工具的补充系统导航。原生导航可用时优先原生；用户指定本机工具或原生失败时可用home返回桌面、back返回上一页、recents查看最近任务。home不强制结束进程。', [('action', '导航动作，只能是home、back或recents', True)]),
    ('searchSettings', '按中文名称、组件名或关键字查询这台手机的设置索引。返回静态候选；未登记的候选不能当作已验证直达页面。', [('query', '设置关键词，例如无线调试、开发者、键盘、电池健康', True)]),
    ('getToolStatus', '检查AMOO本机工具状态：本机执行服务是否就绪，无障碍服务是否已连接。不可用时报告实际缺项，不声称动作已完成。', []),
    ('screenshot', '截取当前屏幕并保存为PNG，返回图片URI、手机本机路径、宽高和字节数。不返回图片内容、不上传、不记录。可读取返回的path；要理解界面文字应优先使用executor_ui_dump。', []),
    ('uiDump', '读取当前屏幕的无障碍节点树，返回窗口、文字、描述、控件ID、可点击/可编辑状态和屏幕坐标bounds。点击或输入前先用它定位控件。maxNodes可选，字符串形式的整数，默认200，范围1-1000。', [('maxNodes', '可选，字符串形式的整数，默认200，范围1-1000', False)]),
    ('tap', '按屏幕坐标点击一次。先用executor_ui_dump取得目标bounds，取中心点作为x、y。x和y是字符串形式的整数屏幕坐标。坐标越界或无障碍未连接时返回错误。', [('x', '点击横坐标，字符串形式的整数', True), ('y', '点击纵坐标，字符串形式的整数', True)]),
    ('swipe', '按屏幕坐标滑动。x1,y1为起点，x2,y2为终点，均为字符串形式的整数屏幕坐标；durationMs可选，默认300毫秒，范围1-5000。', [('x1', '起点横坐标，字符串形式的整数', True), ('y1', '起点纵坐标，字符串形式的整数', True), ('x2', '终点横坐标，字符串形式的整数', True), ('y2', '终点纵坐标，字符串形式的整数', True), ('durationMs', '可选，滑动时长毫秒，字符串形式的整数，默认300', False)]),
    ('inputText', '向当前获得焦点或界面中的可编辑输入框写入文字，并回读确认。通常先用executor_tap点击输入框。text为空字符串可清空；最多2000字符。返回verified表示是否回读一致；密码框跳过回读。输入内容不会记录或上传。', [('text', '要写入的文字，可为空字符串以清空，最多2000字符', True)]),
]


def add(parent, name, text=None):
    e = ET.SubElement(parent, name)
    if text is not None:
        e.text = str(text)
    return e


def main():
    legacy = ET.Element('appfunctions'); modern = ET.Element('appfunctions')
    for method, description, parameters in FUNCTIONS:
        fid = 'com.amoo.executor.PhoneTools#' + method
        old = add(legacy, 'appfunction'); add(old, 'function_id', fid); add(old, 'enabled_by_default', 'true')
        row = add(modern, 'appfunction')
        add(row, 'id', fid); add(row, 'enabledByDefault', 'true'); add(row, 'description', description)
        for name, desc, required in parameters:
            p = add(row, 'parameters'); meta = add(p, 'dataTypeMetadata')
            add(meta, 'id', 'unused'); add(meta, 'isNullable', 'false' if required else 'true'); add(meta, 'type', 8)
            add(p, 'id', 'unused'); add(p, 'isRequired', 'true' if required else 'false'); add(p, 'name', name); add(p, 'description', desc)
        response = add(row, 'response'); add(response, 'id', 'unused'); value = add(response, 'valueType')
        add(value, 'id', 'unused'); add(value, 'isNullable', 'false'); add(value, 'type', 8)
        add(row, 'functionId', fid)
    for name, doc in [('app_functions.xml', legacy), ('app_functions_v2.xml', modern)]:
        ET.indent(doc)
        ET.ElementTree(doc).write(BASE/'app/assets'/name, encoding='utf-8', xml_declaration=True)


if __name__ == '__main__':
    main()
