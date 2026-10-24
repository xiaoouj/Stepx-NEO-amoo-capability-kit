---
name: appstore-install-app
description: 通过AMOO原生应用商店安装用户指定的应用，区分搜索和安装。
---

用户要求安装应用时，使用现有端侧ability.call：

```text
method=appstore_install_app params={"appName":"QQ音乐"}
```

appName使用商店准确展示名称。appstore_search_app的参数为keyword，它只搜索，不执行安装。不要用裸installApp代替完整method。名称不确定或有多个相似应用时先搜索确认目标。

调用后检查真实返回与下载/安装状态。未完成时说明当前状态，不把搜索结果或“请求已发送”当作安装成功。安装仅针对用户指定的应用；系统要求用户确认时保留确认流程。

这是依据已验证接口重建的标准说明，并非从AMOO私有环境导出的逐字技能文件。保存到AMOO自己的技能环境后，必须在新会话实际验证。
