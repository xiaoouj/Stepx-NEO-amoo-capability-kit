# 验证与排错

系统是否登记：

```sh
"$AMOO_ADB_PATH" -s "$AMOO_SERIAL" shell dumpsys app_function
```

检查 `com.amoo.executor`。直接诊断健康状态（不同系统cmd命令格式可能有差异，先看help）：

```sh
"$AMOO_ADB_PATH" -s "$AMOO_SERIAL" shell cmd app_function help
```

原生AMOO通过 `ability.call` 的 `executor_get_tool_status` 调用。必须解析 `content.returnValue` 的JSON并检查 `ok`；系统层“调用成功”不代表内部动作完成。

| 现象 | 检查与处理 |
| --- | --- |
| 设置服务不可用 | 检查Shell Root，重新运行安装器恢复并配对 |
| GUI工具不可用 | 检查系统无障碍授权与serviceConnected；保持手机解锁 |
| 原子能力搜索不到 | 核对AppFunctions登记及准确method，不凭搜索结果断言不存在 |
| AMOO只说能做、不实际调用 | 给出技能说明，在新会话要求实际调用健康检查 |
| 云侧路径找不到 | 手机路径不等于AMOO脚本环境；通过AppFunctions操作手机 |
| 截图有路径但模型看不到 | 路径是手机上的URI/文件；未传图时使用控件树，不能声称已看图 |
| 输入返回verified=false | 回读未确认，重新观察输入框，不重复发送消息 |
| 设置索引有名称但打不开 | 索引是静态候选，只对四条登记路由有直达实现 |
| 安装提示签名不兼容 | 使用原签名密钥重建，不自动卸载旧应用 |

验证时先用空白输入框和应用自身测试页面；动作后再次读取界面，确认实际状态。`home`只返回桌面。App安装用 `appstore_install_app`，`appstore_search_app`仅搜索。

第三方AgentTouch的诊断工具是 `tools/agenttouch_diagnostic.py`，与本仓库 `com.amoo.executor` 无障碍服务属于两套实现，接口不可混用。
