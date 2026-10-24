# 第三方 AgentTouch 接入说明

历史测试使用“Agent手机控制器”1.6.1，包名life.suk.agenttouch。其完整应用源码不在本工作区。本仓库包含独立诊断客户端 `tools/agenttouch_diagnostic.py` 和 [验证报告](../../reports/amoo-gui-controller-2026-10-22.md)。

先在系统无障碍设置开启该服务，并在控制器中授权需要操作的应用。AMOO侧接口为agenttouch_health、agenttouch_observe、agenttouch_execute；Android function ID前缀为life.suk.agenttouch.AgentFunctionService#，后缀health/observe/execute。二者不能混用。

```sh
python3 tools/agenttouch_diagnostic.py --serial "$AMOO_SERIAL" health
python3 tools/agenttouch_diagnostic.py --serial "$AMOO_SERIAL" observe life.suk.agenttouch
```

execute需要明确的步骤JSON，工具会创建一次性requestId、deadline并保留请求，不自动重试变更。具体步骤schema以安装版本的接口说明为准。历史验证仅覆盖控制器自身允许列表；视觉观察没有验证。优先使用本仓库phone-tools自有无障碍实现时，不必同时安装两套控制器。
