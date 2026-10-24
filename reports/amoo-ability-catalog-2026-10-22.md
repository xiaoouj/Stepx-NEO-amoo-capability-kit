# AMOO 原子能力与工具完整清单 — 2026-10-22

本清单列出404个不同调用名称：手机包内文档397项、另列gui_agent 1项、已验证商店方法2项、本次新增端侧方法4项。其中有40项在文档中明确标为云侧参考；它们不能全部算作手机端原子能力，也不能把404理解为全部可用。

另外读取了手机系统当前登记的15个AppFunctions，全文放在末尾。它们与调用名称表有重叠，不能直接相加。登记、默认启用和真实调用成功是不同状态。本轮只读取清单，没有批量执行电话、短信、删除文件、改设置等动作。

参数为调用形态参考，不是完整schema。带“|”的值表示多个选项，实际调用应选其中一个。多数旧文档未逐项验证，可能有旧别名或参数错误；已知open_app_by_package的package已改为实机packageName。

## 分类总览

| 分类 | 调用名称数 |
| --- | ---: |
| 系统设置 | 121 |
| 电话与短信 | 80 |
| 联系人与黑名单 | 22 |
| 应用、设备、日历与实用工具 | 40 |
| 相册与媒体编辑 | 51 |
| 文件、笔记与待办 | 31 |
| 定位、地图与导航 | 18 |
| 时钟、定时任务与网关 | 14 |
| 网络搜索与网页 | 6 |
| 会话与运行时 | 12 |
| 日志录制 | 2 |
| 通用GUI自动化 | 1 |
| 商店原子能力（补充） | 2 |
| 本次新增的手机端工具 | 4 |

## 已确认的限制

- gui_agent：当前直接返回“GUI自动化功能暂未开放”，不能因为文档列出就承诺可操作微信等App。
- 输入法旧别名settings_open_settings_keyboard_settings_activity：返回20004；已有新工具executor_open_settings支持keyboard。
- AMOOsic的启动曾被签名信任检查拦截；这不等于所有应用都不能打开。
- 新增executor_*的页面启动与导航依赖手机当前Root执行服务；索引查询由普通应用自身完成。

## 完整调用名称

### 系统设置（121）

| 编号 | 中文用途 | 调用名称 | 执行位置／性质 | 验证状态 |
| ---: | --- | --- | --- | --- |
| 1 | 打开或关闭WIFI。 | `settings_wifi_switch` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 2 | 打开或关闭蓝牙。 | `settings_bluetooth_switch` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 3 | 开启或关闭热点。 | `settings_hotspot_switch` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 4 | 打开或关闭飞行模式。 | `settings_airplane_mode_switch` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 5 | 打开或关闭数据网络。 | `settings_data_switch` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 6 | 连接一个新的WIFI，需要提供对应的名称和密码。 | `settings_connect_wifi` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 7 | 切换到一个已保存的WIFI。 | `settings_choose_wifi` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 8 | 删除/忘记已保存的WIFI。 | `settings_forget_wifi` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 9 | 开启热点并设置热点名称和密码（密码长度需大于8位）。 | `settings_config_hotspot` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 10 | 打开当前连接的WIFI详情页面，可查看技术标准、IP地址、MAC地址等详情。 | `settings_open_wifi_details_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 11 | 调整屏幕亮度到指定数值。 | `settings_set_brightness` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 12 | 相对调整屏幕亮度（调亮一些或调暗一些）。 | `settings_adjust_brightness` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 13 | 打开或关闭自动亮度调节。 | `settings_auto_brightness_switch` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 14 | 打开或关闭屏幕自动旋转。 | `settings_auto_rotation_switch` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 15 | 打开或关闭系统深色模式。 | `settings_dark_mode_switch` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 16 | 打开或关闭屏保开关。 | `settings_screen_saver_switch` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 17 | 调整自动锁屏时间（15秒、30秒、1分钟等，单位为秒）。 | `settings_set_screen_off_time` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 18 | 延长或缩短自动锁屏时间。 | `settings_adjust_screen_off_time` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 19 | 调整媒体音量到指定百分比。 | `settings_set_media_sound` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 20 | 增大或减小媒体音量。 | `settings_adjust_sound` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 21 | 打开或关闭系统震动。 | `settings_vibration_switch` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 22 | 开启或关闭系统勿扰模式。 | `settings_disturb_mode_switch` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 23 | 打开或关闭拨号键盘提示音。 | `settings_dialing_keypad_sound_switch` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 24 | 打开或关闭屏幕锁定提示音。 | `settings_screen_lock_sound_switch` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 25 | 打开或关闭充电提示音和振动。 | `settings_charging_notif_sound_switch` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 26 | 打开或关闭点按和点击提示音。 | `settings_touch_sound_switch` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 27 | 将字体调大一些或调小一些。 | `settings_adjust_font_size` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 28 | 将界面显示调大一些或调小一些。 | `settings_adjust_display_size` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 29 | 打开或关闭省电模式。 | `settings_battery_saver_switch` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 30 | 显示或隐藏电池电量百分比。 | `settings_battery_percentage_switch` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 31 | 打开或关闭单手模式。 | `settings_one_handed_mode_switch` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 32 | 打开或关闭"点按手机即显示"（点击屏幕亮屏）。 | `settings_tapToCheck_phone_switch` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 33 | 打开或关闭拿起手机即显示（抬起亮屏）功能。 | `settings_liftToCheck_phone_switch` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 34 | 切换系统导航模式，支持手势导航和三键（按键）导航之间的切换。 | `settings_adjust_navigation_mode` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 35 | 切换系统语言，支持中文、英语、法语、德语、意大利语、日语、韩语等。 | `settings_adjust_system_language` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 36 | 打开设置主页面。 | `settings_open_settings_homepage_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 37 | 打开系统设置界面，提供语言、手势、日期和时间、备份、用户、重置选项等菜单。 | `settings_open_settings_system_dashboard_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 38 | 打开/进入关于本机界面。该界面展示了手机型号，系统版本号以及设备标识符：ip地址，mac地址 | `settings_open_settings_device_info_settings_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 39 | 打开重置选项界面，包括重置移动网络、蓝牙和WLAN、应用偏好设置、恢复出厂设置等选项。 | `settings_open_sub_settings_reset_dashboard_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 40 | 打开设置的网络页面，可访问WIFI、热点、SIM卡、VPN、DNS等功能。 | `settings_open_settings_network_dashboard_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 41 | 打开设置的WIFI页面，包含WIFI网络搜索、添加、连接、已保存网络等入口。 | `settings_open_settings_network_provider_settings_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 42 | 打开WIFI偏好设置页面，包含自动打开WIFI控制、公共网络提醒、证书安装等功能。 | `settings_open_settings_configure_wifi_settings_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 43 | 打开已保存WIFI页面，可进行取消保存、断开连接、分享、查看详情等操作。 | `settings_open_settings_saved_access_points_settings_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 44 | 打开SIM卡管理页面，包含双卡数据切换、数据流量开关控制等功能。 | `settings_open_settings_mobile_network_list_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 45 | 打开数据网络重置页面（仅展示，不直接重置）。 | `settings_open_settings_reset_mobile_network_settings_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 46 | 打开热点/网络共享页面，供用户手动开关热点。 | `settings_open_settings_tether_settings_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 47 | 打开P2P连接页面，可查看搜索到的支持P2P的设备并发起连接。 | `settings_open_settings_wifi_p_2_p_settings_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 48 | 打开VPN连接页面，可对VPN进行添加/配置。 | `settings_open_settings_vpn_settings_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 49 | 打开省流模式页面，可配置不限制移动数据用量的应用及手动开关省流模式。 | `settings_open_settings_data_saver_summary_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 50 | 打开投屏设置界面，可搜索附近可投屏的设备。 | `settings_open_settings_wifi_display_settings_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 51 | 打开已连接设备页面，展示已连接的蓝牙、USB相关功能。 | `settings_open_settings_connected_device_dashboard_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 52 | 打开蓝牙页面，供用户连接/配置蓝牙。 | `settings_open_settings_blue_tooth_pairing_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 53 | 打开NFC设置页面，可进行NFC功能的设置。 | `settings_open_settings_nfc_settings_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 54 | 打开连接偏好设置页面，供用户对蓝牙、NFC、投屏、打印、超带宽功能的偏好进行设置。 | `settings_open_settings_advanced_connected_device_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 55 | 打开USB详情页面，提供USB偏好设置。 | `settings_open_settings_usb_details_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 56 | 打开打印页面，可选择默认打印服务或其他已安装的打印服务。 | `settings_open_settings_print_settings_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 57 | 打开自动锁屏界面（仅展示，不直接调整时间）。 | `settings_open_settings_screen_timeout_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 58 | 打开显示大小和文字大小调整界面，还提供粗体文字和高对比度文字开关。 | `settings_open_settings_text_reading_settings_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 59 | 打开颜色对比度调整界面，还提供高对比度文字开关。 | `settings_open_settings_color_contrast_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 60 | 打开颜色设置界面，提供颜色自然色和自适应的切换功能。 | `settings_open_sub_settings_color_mode_preference_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 61 | 打开屏保界面（仅展示，不直接打开或关闭屏保）。 | `settings_open_settings_dream_settings_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 62 | 打开壁纸界面，可通过不同壁纸来源进行壁纸设置。 | `settings_open_settings_wallpaper_settings_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 63 | 打开锁屏界面，可调整锁屏显示的通知内容、显示文字及动态时钟等。 | `settings_open_settings_lock_screen_settings_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 64 | 打开音量管理页面，包含音量、振动、勿扰模式及手机铃声等设置项。 | `settings_open_settings_sound_settings_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 65 | 打开通知管理页面，可显示历史通知及通知常规设置项。 | `settings_open_settings_notification_assistant_settings_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 66 | 打开勿扰模式管理页面，可配置排除在外的联系人、应用、时间段等。 | `settings_open_settings_zen_mode_settings_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 67 | 打开锁屏密码设置页面，包含滑动、图案、PIN码、密码等锁屏方式选择。 | `settings_open_settings_fingerprint_settings_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 68 | 打开键盘管理页面，包含键盘切换入口、个人字典配置、指针速度调整等功能。 | `settings_open_settings_keyboard_settings_activity` | 原目录候选／未逐项定位 | 文档过期：实机返回20004未找到对应原子能力 |
| 69 | 打开语言设置界面（仅展示，不直接调整系统语言）。 | `settings_open_settings_date_time_settings_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 70 | 打开语言设置界面（仅展示，不直接调整系统语言）。 | `settings_open_settings_locale_picker_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 71 | 打开导航模式设置界面，提供手势导航和"三按钮"导航的切换。 | `settings_open_settings_measurement_system_settings_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 72 | 打开所有应用页面，展示所有已安装的应用。 | `settings_open_manage_applications` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 73 | 打开应用管理页面，可查看最近使用和已安装的应用，也是默认应用设置的入口。 | `settings_open_settings_app_dashboard_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 74 | 打开存储空间界面，展示存储空间使用量及存储空间管理。 | `settings_open_settings_storage_dashboard_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 75 | 打开电池界面，展示电池使用信息及电池电量相关开关设置。 | `settings_open_settings_power_usage_summary_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 76 | 打开电池使用情况界面，展示电池充满电使用时间、应用耗电量等信息。 | `settings_open_sub_settings_power_usage_advanced_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 77 | 打开手势界面，包含快速打开相机、导航模式、单手模式、点按/拿起手机即显示等功能。 | `settings_open_sub_settings_gesture_settings_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 78 | 获取当前手机是否支持指纹识别。 | `common_ability_has_fingerprint` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 79 | 获取当前手机是否支持人脸识别。 | `common_ability_has_face_rec` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 80 | 通过packageName获取应用非普通权限使用详情。 | `common_ability_get_app_permissions` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 81 | 通过packageName和指定的permission字段，检查应用是否获取指定的permission。 | `common_ability_check_app_permission` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 82 | 获取电池电量百分比是否开启的状态。 | `settings_get_battery_percentage_state` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 83 | 获取WIFI的开关状态。 | `settings_get_wifi_state` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 84 | 获取蓝牙的开关状态。 | `settings_get_bluetooth_state` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 85 | 获取热点的开关状态。 | `settings_get_hotspot_state` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 86 | 获取飞行模式的开关状态。 | `settings_get_airplane_mode_state` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 87 | 获取数据网络的开关状态。 | `settings_get_data_state` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 88 | 获取系统震动的开关状态。 | `settings_get_vibration_state` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 89 | 获取深色模式的开关状态。 | `settings_get_dark_mode_state` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 90 | 获取拨号键的提示音的开关状态。 | `settings_get_dialing_keypad_sound_state` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 91 | 获取屏幕锁定提示音的开关状态。 | `settings_get_screen_lock_sound_state` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 92 | 获取充电提示音和振动的开关状态。 | `settings_get_charging_notif_sound_state` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 93 | 获取点按和点击提示音的开关状态。 | `settings_get_touch_sound_state` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 94 | 获取自动亮度调节的开关状态。 | `settings_get_auto_brightness_state` | 原目录候选／未逐项定位 | 已实测查询 |
| 95 | 获取屏幕自动旋转的开关状态。 | `settings_get_auto_rotation_state` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 96 | 获取屏保的开关状态。 | `settings_get_screen_saver_state` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 97 | 获取"单手模式"的开关状态。 | `settings_get_one_handed_mode_state` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 98 | 获取"点按手机即显示"的开关状态。 | `settings_get_tapToCheck_phone_state` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 99 | 获取"捂起手机即显示"的开关状态。 | `settings_get_liftToCheck_phone_state` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 100 | 获取"省电模式"的开关状态。 | `settings_get_battery_saver_state` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 101 | 获取"应用锁"的开关状态。 | `settings_get_app_lock_state` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 102 | 打开/关闭应用锁，需要提供密码。 | `settings_app_lock_config_switch` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 103 | 开启/关闭 全局位置信息权限。 | `security_permissioncontroller_global_location` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 104 | 开启/关闭 全局麦克风权限。 | `security_permissioncontroller_global_microphone` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 105 | 开启/关闭 全局摄像头权限。 | `security_permissioncontroller_global_camera` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 106 | 打开手机管家应用。 | `security_center_open_main` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 107 | 开启/关闭 省电模式（手机管家）。 | `security_center_power_saving_enable` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 108 | 打开骚扰拦截页面。 | `security_manager_open_harassment_interception` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 109 | 打开病毒查杀页面。 | `security_manager_into_virus_page` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 110 | 开启/关闭 移动网络安全支付。 | `security_center_network_pay_enable` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 111 | 开启/关闭 隐私支付保护。 | `security_center_pay_enable` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 112 | 开启/关闭 USB安装管控。 | `security_center_usb_install_enable` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 113 | 开启/关闭 手机管家功能。 | `security_center_enable_function` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 114 | 开启/关闭 私密保险箱中的自动填写。 | `security_safebox_auto_fill` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 115 | 打开设置-安全-私密保险箱。 | `security_safebox_open` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 116 | 打开OAID设置页。 | `security_service_open_oaid_settings` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 117 | 获取手机UUID。 | `security_service_get_uuid` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 118 | 开关手电筒。 | `open_flash_light` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 119 | 筛选/查询系统的通知内容，如"帮我查一下'微信'发来了什么消息"。 | `query_notification_info` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 120 | 录制设备屏幕视频（录屏）。 | `screen_capture_record` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 121 | 截取当前设备屏幕内容（截屏）。 | `screen_capture_screenshot` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |

### 电话与短信（80）

| 编号 | 中文用途 | 调用名称 | 执行位置／性质 | 验证状态 |
| ---: | --- | --- | --- | --- |
| 122 | 给指定的号码拨打电话。 | `dialer_call_to` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 123 | 通话过程中，拉起通话界面。 | `dialer_show_incall_screen` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 124 | 如果当前有来电，接听电话。 | `dialer_accept_call` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 125 | 如果当前有来电，挂断来电；如果当前有通话，挂断当前通话。 | `dialer_hangup_call` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 126 | 回拨通话记录中最近一次通话的号码，如果没有通话记录，不做任何操作。 | `dialer_call_back` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 127 | 查询通话记录中最近一次来电的号码，如果没有通话记录，返回空字符串。 | `dialer_check_last_caller` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 128 | 打开拨号盘，并填充输入的数字。 | `dialer_show_dialpad` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 129 | 开启或关闭按电源键挂断电话功能。 | `dialer_settings_enable_power_button_ends_call` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 130 | 开启或关闭来电翻转静音功能。 | `dialer_settings_enable_incoming_flip_silent` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 131 | 开启或关闭接通时振动功能。 | `dialer_settings_vibrate_when_mocall_connected` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 132 | 打开通话录音设置界面。 | `dialer_settings_open_auto_record_screen` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 133 | 开启或关闭未解锁时直接查看医疗急救信息的功能。 | `dialer_settings_allow_viewing_medical_information` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 134 | 开启或关闭SOS紧急求助时自动报警的功能。 | `dialer_settings_sos_auto_call` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 135 | 开启或关闭SOS紧急求助时自动发送位置给警方功能。 | `dialer_settings_sos_send_position` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 136 | 给指定联系人拨打电话（支持模糊匹配）。 | `dialer_call_contact` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 137 | 按日期或日期范围查询通话记录。 | `dialer_query_calllog` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 138 | 查询某个联系人的通话记录。 | `dialer_query_calllog_of_contact` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 139 | 按通话类型查询通话记录。 | `dialer_query_calllog_by_type` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 140 | 按联系人号码查询通话记录。 | `dialer_query_calllog_by_number` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 141 | 查询今天早上的通话记录。 | `dialer_query_calllog_this_morning` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 142 | 查询今天下午的通话记录。 | `dialer_query_calllog_this_afternoon` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 143 | 查询今晚的通话记录。 | `dialer_query_calllog_tonight` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 144 | 查询今天的通话记录。 | `dialer_query_calllog_today` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 145 | 查询昨天的通话记录。 | `dialer_query_calllog_yesterday` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 146 | 查询前天的通话记录。 | `dialer_query_calllog_day_before_yesterday` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 147 | 查看通话时长排行榜，按时长由长到短排序。 | `dialer_query_call_duration_ranking` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 148 | 按条件删除通话记录。 | `dialer_delete_calllog` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 149 | 按联系人姓名删除通话记录。 | `dialer_delete_calllog_by_contact_name` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 150 | 按联系人号码删除通话记录。 | `dialer_delete_calllog_by_number` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 151 | 删除最近一次通话记录。 | `dialer_delete_last_calllog` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 152 | 删除今天的通话记录（00:00:00 到当前时刻）。 | `dialer_delete_calllog_today` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 153 | 打开电话设置界面。 | `dialer_open_call_settings` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 154 | 打开SIM卡设置界面。 | `dialer_open_sim_card_settings` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 155 | 打开医疗急救信息界面。 | `dialer_open_medical_emergency_information` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 156 | 开启或关闭SOS紧急求助时的自动求助紧急联系人开关。 | `dialer_settings_sos_auto_help_emergency_contact` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 157 | 开启或关闭SOS紧急求助时的报警辅助开关。 | `dialer_settings_sos_dial_assist` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 158 | 获取手机号码。 | `tele_mobilenet_phone_numbers` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 159 | 获取信号强度。 | `tele_mobilenet_signal_strength` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 160 | 开启或关闭移动数据。 | `tele_mobilenet_data_enable` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 161 | 给指定联系人或号码发送一条短信。 | `sms_send` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 162 | 搜索短信，支持按关键词、日期范围、联系人号码搜索。 | `sms_search` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 163 | 删除短信，支持按短信ID、会话ID或电话号码删除。 | `sms_delete` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 164 | 删除短信会话，删除整个对话会话及其包含的所有短信。 | `sms_thread_delete` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 165 | 转发短信给其他联系人，支持按短信ID获取内容或直接提供消息内容。 | `sms_forward` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 166 | 标记短信已读或未读状态，支持按短信ID、会话ID或电话号码操作。 | `sms_read_status` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 167 | 获取短信统计信息，包括收件箱、发件箱、草稿箱数量、未读数及会话数等。 | `sms_statistics` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 168 | 设置定时发送短信，在指定时间自动发送短信。 | `sms_schedule_send` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 169 | 保存短信草稿到草稿箱。 | `sms_draft_save` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 170 | 获取短信草稿，支持按草稿ID、电话号码、会话ID查询，或获取所有草稿。 | `sms_draft_get` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 171 | 删除短信草稿，支持按草稿ID、电话号码、会话ID删除，或删除所有草稿。 | `sms_draft_delete` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 172 | 给指定联系人或号码发送短信。 | `sms_send_direct` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 173 | 通过号码查询指定短信。 | `sms_query` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 174 | 设置短信发送声音开关。 | `sms_send_sound` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 175 | 设置短信通知声音。 | `sms_notify_sound` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 176 | 设置短信通知振动开关。 | `sms_notify_vibration` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 177 | 设置自动下载彩信开关。 | `sms_auto_retrieve_mms` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 178 | 设置短信送达报告开关。 | `sms_delivery_reports` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 179 | 开启/关闭智能短信开关。 | `sms_settings_intelligent_msg` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 180 | 开启/关闭短信的显示通知详情开关。 | `sms_settings_notif_detail` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 181 | 开启/关闭漫游时彩信自动下载开关。 | `sms_settings_auto_retrieval` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 182 | 开启/关闭风险网址识别开关。 | `sms_settings_risk_url` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 183 | 开启/关闭自动删除验证码短信开关。 | `sms_settings_expired_veri` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 184 | 开启/关闭自动删除取件码短信开关。 | `sms_settings_expired_pickup` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 185 | 开启/关闭短信送达报告开关（设置侧）。 | `sms_settings_delivery_reports` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 186 | 编辑快速回复内容。 | `sms_settings_quick_reply` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 187 | 设置短信中心号码。 | `sms_settings_center_num` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 188 | 获取SIM卡存储的短信列表。 | `sms_settings_sim_messages` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 189 | 查话费余额，通过发送短信查询本机话费。 | `sms_account_balance_inquiry` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 190 | 查流量余额，通过发送短信查询本机数据流量。 | `sms_data_balance_inquiry` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 191 | 查询今天收/发的短信列表。 | `sms_list_today_query` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 192 | 查询昨天的短信列表。 | `sms_list_yesterday_query` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 193 | 查询前天的短信列表。 | `sms_list_day_before_yesterday_query` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 194 | 查询上周的短信列表。 | `sms_list_last_week_query` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 195 | 查询今天上午的短信列表。 | `sms_list_morning_query` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 196 | 查询今天下午的短信列表。 | `sms_list_afternoon_query` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 197 | 查询今天晚上的短信列表。 | `sms_list_evening_query` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 198 | 打开个人短信列表页面。 | `sms_open_personal_list` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 199 | 打开通知短信列表页面。 | `sms_open_notice_list` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 200 | 打开收藏短信列表页面。 | `sms_open_collected_list` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 201 | 打开短信设置页面。 | `sms_open_settings_page` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |

### 联系人与黑名单（22）

| 编号 | 中文用途 | 调用名称 | 执行位置／性质 | 验证状态 |
| ---: | --- | --- | --- | --- |
| 202 | 添加联系人，需要提供号码，还可选填名称、邮箱、公司、职位、备注、地址等信息。 | `dialer_contacts_add` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 203 | 修改联系人的信息。 | `dialer_contacts_update` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 204 | 查询联系人，可以通过号码、名称等信息查询。 | `dialer_contacts_query` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 205 | 删除联系人，支持按号码、姓名、公司、地址匹配删除。 | `dialer_contacts_delete` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 206 | 删除指定号码对应的单个联系人。 | `dialer_contacts_delete_one` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 207 | 打开联系人编辑界面。 | `dialer_open_contacts_editor` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 208 | 添加联系人群组，需要提供群组名。 | `dialer_add_group` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 209 | 收藏或取消收藏联系人。 | `dialer_contacts_set_starred` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 210 | 复制指定联系人的手机号码到系统剪贴板。 | `dialer_contacts_copy_number` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 211 | 打开我的名片（本地个人档案）。 | `dialer_open_my_business_card` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 212 | 打开黑名单管理界面。 | `dialer_open_blocked_number_list` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 213 | 将指定的号码加入黑名单数据库。 | `dialer_add_number_to_blocked_list` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 214 | 从黑名单数据库移除指定的号码。 | `dialer_delete_number_from_blocked_list` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 215 | 查询指定的号码是否在黑名单中。 | `dialer_query_blocked_number` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 216 | 查询联系人。 | `tele_contacts_query` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 217 | 删除联系人，支持按号码、姓名、公司、地址匹配删除。 | `tele_contacts_delete` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 218 | 删除指定号码对应的单个联系人。 | `tele_contacts_delete_one` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 219 | 添加号码黑名单。 | `tele_blocked_add_number` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 220 | 删除黑名单号码。 | `tele_blocked_delete_number` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 221 | 查询某号码是否为黑名单号码。 | `tele_blocked_query_number` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 222 | 打开黑名单管理界面。 | `tele_blocked_open_blocked_number` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 223 | 将最近一次通话记录号码加入黑名单。 | `tele_dialer_add_last_call_number_to_blocked_list` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |

### 应用、设备、日历与实用工具（40）

| 编号 | 中文用途 | 调用名称 | 执行位置／性质 | 验证状态 |
| ---: | --- | --- | --- | --- |
| 224 | 通过应用显示名称打开应用。 | `open_app_by_name` | 原目录候选／未逐项定位 | 已确认入口；AMOOsic曾被启动签名检查拦截，不能据此判断所有目标 |
| 225 | 通过应用包名打开应用。 | `open_app_by_package` | 原目录候选／未逐项定位 | 已实测；参数为packageName，可传className |
| 226 | 对应 nodes：invokeCommand="ability.call"，invokeParamsJson 为 {"method":"app.openUrl","params":{"url":"<完整 URL>"}}（JSON 内注意转义）。 | `app.openUrl` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 227 | 通过packageName检查应用是否已安装。 | `check_app_installed_by_package` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 228 | 通过appName检查应用是否已安装。 | `check_app_installed_by_name` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 229 | 随机显示一个Widget，也可以通过指定package和className显示特定Widget。 | `show_widget_by_package_class` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 230 | 播放音乐，展示音乐小组件。 | `common_ability_play_music` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 231 | 获取播放音乐的widget。 | `aicopilot_play_music` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 232 | 在用户日历中新增一个日程事件。 | `calendar_add_event` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 233 | 在用户日历中根据开始时间和结束时间查询日程。 | `calendar_query_event` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 234 | 在用户日历中新增一个特殊事件（生日、纪念日、倒数日）。 | `calendar_add_special_event` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 235 | 在用户日历中根据事件的时间等字段进行删除事件。 | `calendar_delete_event` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 236 | 在用户日历中编辑一个日程事件，仅支持修改时间或标题（不能同时修改）。 | `calendar_edit_event` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 237 | 在用户日历中编辑一个特殊事件（生日/纪念日/倒数日），支持修改时间、标题或提醒设置。 | `calendar_edit_special_event` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 238 | 在用户日历中删除特殊事件（生日、纪念日、倒数日）。 | `calendar_delete_special_event` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 239 | 打开添加日程页面（仅打开，不新增内容）。 | `calendar_open_new_edit_event_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 240 | 打开黄历详情页面。 | `calendar_open_almanac_detail_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 241 | 打开星座详情页面。 | `calendar_open_horoscope_detail_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 242 | 打开日历设置页面。 | `calendar_open_general_settings` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 243 | 查询特殊事件（生日/纪念日/倒数日）。 | `calendar_query_special_event` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 244 | 获取当前手机屏幕尺寸。 | `common_ability_get_screen_size` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 245 | 通过packageName获取应用的versionCode。 | `common_ability_get_app_versioncode` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 246 | 卸载指定应用。 | `security_packageinstaller_uninstall` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 247 | 根据数学表达式字符串计算结果，三角函数默认角度制。 | `evaluate_expression` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 248 | 打开录音机并开始录音。 | `recorder_open_recording` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 249 | 打开录音机搜索页面。 | `recorder_open_search` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 250 | 打开录音机编辑第N个录音。 | `recorder_open_edit_by_position` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 251 | 打开录音机编辑命名为X的录音。 | `recorder_open_edit_by_name` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 252 | 打开录音机删除第N个录音。 | `recorder_delete_file_by_position` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 253 | 打开录音机删除命名为X的录音。 | `recorder_delete_file_by_name` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 254 | 打开录音机转写第N个录音。 | `recorder_open_recoginition_by_position` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 255 | 打开录音机转写命名为X的录音。 | `recorder_open_recoginition_by_name` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 256 | 打开工具箱放大镜页面。 | `toolbox_open_magnifier_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 257 | 打开工具箱指南针页面。 | `toolbox_open_compass_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 258 | 打开工具箱镜子页面。 | `toolbox_open_mirror_new_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 259 | 打开工具箱分贝仪页面，查看当前分贝数值。 | `toolbox_open_decibel_activity` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 260 | 打开工具箱里的SOS闪烁灯功能。 | `open_tool_sos` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 261 | 获取当前分贝值。 | `get_current_decibel` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 262 | 提交用户问题反馈到帮助与反馈系统。系统会自动收集设备信息（机型、系统版本等）并上传。 | `feedback_submit_question` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 263 | UI模板渲染。 | `renderTemplate` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |

### 相册与媒体编辑（51）

| 编号 | 中文用途 | 调用名称 | 执行位置／性质 | 验证状态 |
| ---: | --- | --- | --- | --- |
| 264 | 获取本地设备中最新创建的媒体文件（图片或视频）的完整路径。 | `gallery_get_latest_media` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 265 | 按时间范围查询视频或图片。注意：必须先获取当前系统时间后再调用，严禁使用硬编码日期。 | `gallery_get_media_by_time_range` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 266 | 对本地相册的媒体资源进行语义智能搜索。 | `gallery_intelligent_search` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 267 | 获取照片详细信息，包括宽高、文件大小、文件名、拍摄时间、相机型号、焦距、ISO、光圈、快门速度、GPS 信息等。 | `gallery_get_photo_info` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 268 | 获取视频详细信息，包括宽高、文件大小、文件名、时长（毫秒）、码率、音频采样率等。 | `gallery_get_video_info` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 269 | 在图片上添加文字，系统会自动读取图片真实宽高并根据位置描述自动计算坐标。 | `gallery_add_picture_text` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 270 | 图片水印编辑。 | `gallery_add_picture_watermark` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 271 | 在图片指定位置添加马赛克效果，支持单区域或多区域打码。 | `gallery_add_picture_mosaic` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 272 | 给图片添加滤镜效果，支持 brightness、contrast、documentary、duotone、fillLight、fisheye、tint 等类型。 | `gallery_add_picture_filter` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 273 | 图片亮度编辑。 | `gallery_edit_picture_brightness` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 274 | 图片饱和度编辑。 | `gallery_edit_picture_saturation` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 275 | 图片色温值编辑，负值偏冷色调，正值偏暖色调。 | `gallery_edit_picture_temperature` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 276 | 图片锐化强度编辑，path 必须是具体的图片文件完整路径。 | `gallery_edit_picture_sharpen` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 277 | 降低图片分辨率（仅支持降低，不支持提高）。 | `gallery_reduce_picture_resolution` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 278 | 按照指定宽高比对图片进行中心裁剪，仅支持 1:1/4:3/16:9/3:4/9:16 这些比例。 | `gallery_edit_picture_aspect_ratio` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 279 | 对图片进行旋转操作，旋转角度仅支持 90/180/270 度顺时针方向。 | `gallery_edit_picture_degree` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 280 | 对图片进行翻转操作，支持水平翻转（左右镜像）和垂直翻转（上下镜像）。 | `gallery_edit_picture_flip` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 281 | 图片画笔编辑。 | `gallery_edit_picture_paint` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 282 | 打开图片编辑页面。 | `gallery_open_picture_editor` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 283 | 打开图片编辑页并直接进入 AI 扩图能力。支持传入本地图片文件路径，也支持 file:// 与 content:// URI。 | `gallery_ai_extend_image` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 284 | 打开视频编辑页面。 | `gallery_open_video_editor` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 285 | 打开视频播放器。 | `gallery_open_video_player` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 286 | 启动视频编辑器卡片。 | `gallery_video_editor_start` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 287 | 将相册的图片或视频自动生成视频剪辑草稿。优先不传 medias 参数，由用户在相册中主动选择素材；禁止默认或自动使用“最近素材”发起剪辑。 | `gallery_video_clip_arrangement` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 288 | 创建高光LivePhoto生成任务卡片，用户点击按钮后选择视频，AI分析并生成LivePhoto文件。 | `gallery_create_highlight_task` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 289 | 创建口播创作任务卡片。该能力固定使用空参数调用（params='{}'），由用户在相册中主动选择素材后再开始分析与生成；禁止默认或自动使用“最近素材”发起口播剪辑。 | `gallery_create_voiceover_task` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 290 | 新建相册。 | `gallery_create_album` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 291 | 删除相册。 | `gallery_delete_album` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 292 | 拷贝图片到相册。 | `gallery_copy_files_to_album` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 293 | 将媒体文件移动到回收站（非永久删除），支持批量清理媒体文件。 | `gallery_delete_media_files` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 294 | 移动图片到相册。将指定的图片或视频移动到目标相册，如果目标相册不存在则先创建相册。 | `gallery_move_media_to_album` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 295 | 隐藏相册。将相册中的所有图片和视频隐藏，同时相册本身也从相册列表中隐藏。 | `gallery_hide_album` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 296 | 设置相册封面。将指定的图片设置为相册的封面图片。 | `gallery_set_album_cover` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 297 | 重命名媒体文件。将指定的图片或视频文件重命名为新的名称。newName 可不带扩展名，系统会自动保留原文件扩展名。 | `gallery_rename_media` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 298 | 复制媒体文件到指定位置。 | `gallery_copy_media` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 299 | 移动媒体文件到指定文件夹。 | `gallery_move_to_folder` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 300 | 隐藏媒体文件，从相册中移除但不删除。 | `gallery_hide_media` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 301 | 取消媒体文件隐藏，恢复显示到相册中。 | `gallery_unhide_media` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 302 | 分享媒体文件，弹出系统分享对话框。 | `gallery_share_media` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 303 | 收藏或取消收藏媒体文件。 | `gallery_favorite_media` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 304 | 压缩媒体文件，支持图片和视频压缩。压缩后的文件保存在系统图片目录的 compressed 文件夹下，返回压缩后的文件路径。 | `gallery_compress_media` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 305 | 将指定路径的图片设置为手机壁纸。 | `gallery_set_wallpaper` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 306 | 更新意图理解任务卡片内容（包含思考过程、意图理解内容）。 | `gallery_update_intent_understand_card` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 307 | 更新素材分析任务卡片内容（包含思考过程、分析进度、分析结果、高光提取内容）。 | `gallery_update_material_analysis_card` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 308 | 更新故事构造任务卡片内容（包含思考过程、故事结构）。 | `gallery_update_story_construct_card` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 309 | 更新视频输出任务卡片内容（包含思考过程、视频草稿工程文件列表）。 | `gallery_update_video_output_card` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 310 | 视频自动剪辑（AI 剪辑）优先不传 mediaPaths 参数，或传空数组，由用户在任务卡片中主动选择素材；禁止默认或自动使用“最近素材”直接发起剪辑。 | `gallery_create_auto_edit_video_task` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 311 | 获取相册中贴纸类目。 | `gallery_get_sticker_columns` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 312 | 获取相册中指定类目的贴纸列表，支持查询多个类目。 | `gallery_get_stickers` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 313 | 获取相册本地的贴纸列表，该贴纸是从抠图中添加的。 | `gallery_get_local_stickers` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 314 | 获取风格转换的模版列表。 | `gallery_get_style_templates` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |

### 文件、笔记与待办（31）

| 编号 | 中文用途 | 调用名称 | 执行位置／性质 | 验证状态 |
| ---: | --- | --- | --- | --- |
| 315 | 执行文件上传。 | `batch_file_upload` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 316 | 执行文件上传，返回所有结果。 | `batch_file_upload_return_all` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 317 | 执行文件上传，并返回实时进度。 | `batch_file_upload_all_progress` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 318 | 获取粘贴板文本内容。 | `common_ability_clipboard_text` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 319 | 在笔记应用中添加一个新的笔记。 | `phone_note_add` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 320 | 根据关键词和最近的时间范围访问和搜索笔记应用中的笔记。 | `phone_note_access` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 321 | 通过标题查找指定笔记，并向其内容末尾追加新的文字内容。 | `phone_note_edit` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 322 | 访问和查询已设置提醒信息的笔记。 | `phone_note_remind_sub` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 323 | 在笔记应用中添加一条新的待办事项（Todo），支持设置标题、正文、提醒时间和地点，并支持子任务。 | `phone_note_todo_add` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 324 | 通过标题将最近一条匹配到的待办事项标记为已完成。 | `phone_note_todo_complete` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 325 | 从笔记应用中根据关键词和最近的时间范围访问和查询待办事项。 | `phone_note_todo_access` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 326 | 安全删除文件或目录到可恢复回收站。删除手机外部存储文件时优先使用此能力，禁止用 rm 或 shell 删除替代。 | `file_safe_delete` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 327 | 按原始路径从回收站恢复文件或目录。目标路径已存在时不会覆盖。 | `file_restore` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 328 | 获取文件路径URI。 | `file_path_get` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 329 | 获取文件大小。 | `file_size_get` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 330 | 获取文件创建时间。 | `file_create_time_get` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 331 | 获取文件更新时间。 | `file_update_time_get` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 332 | 获取文件详细信息。 | `file_detail_get` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 333 | 创建文件夹。 | `folder_create` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 334 | 创建文件。 | `file_create` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 335 | 文件搜索。 | `file_search` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 336 | 查询APK文件列表。 | `file_apk_list` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 337 | 查询压缩类文件列表。 | `file_compress_list` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 338 | 查询文档类文件列表。 | `file_document_list` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 339 | 查询音频类文件列表。 | `file_voice_list` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 340 | 查询视频类文件列表。 | `file_video_list` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 341 | 查询下载文件列表。 | `file_download_list` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 342 | 微信类文件列表。 | `file_weixin_list` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 343 | 查询浏览器缓存文件列表。 | `file_browser_list` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 344 | 查询蓝牙传输文件列表。 | `file_bluetooth_list` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 345 | 文件移动。 | `file_move` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |

### 定位、地图与导航（18）

| 编号 | 中文用途 | 调用名称 | 执行位置／性质 | 验证状态 |
| ---: | --- | --- | --- | --- |
| 346 | 获取设备当前实时地理位置（城市／区域级） | `get_current_location` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 347 | 关键词搜索 API，根据用户输入的关键词进行 POI 搜索，并返回相关信息。 | `maps_text_search` | 云侧参考 | 仅文档候选，未逐项实测 |
| 348 | 周边搜索，根据关键词和坐标，搜索出指定半径范围内的 POI。 | `maps_around_search` | 云侧参考 | 仅文档候选，未逐项实测 |
| 349 | 查询关键词搜索或周边搜索获取到的 POI ID 的详细信息。 | `maps_search_detail` | 云侧参考 | 仅文档候选，未逐项实测 |
| 350 | 驾车路径规划，根据起终点经纬度坐标规划驾车方案。 | `maps_direction_driving` | 云侧参考 | 仅文档候选，未逐项实测 |
| 351 | 步行路径规划，规划 100km 以内的步行路线方案。 | `maps_direction_walking` | 云侧参考 | 仅文档候选，未逐项实测 |
| 352 | 骑行路径规划，会考虑天桥、单行线、小路等情况，最大支持 500km。 | `maps_direction_bicycling` | 云侧参考 | 仅文档候选，未逐项实测 |
| 353 | 综合公共交通路线规划，包含火车、公交、地铁等方式。跨城市场景必须传起点城市和终点城市。 | `maps_direction_transit_integrated` | 云侧参考 | 仅文档候选，未逐项实测 |
| 354 | Schema 唤起客户端导航页面，根据终点经纬度返回拼装好的客户端唤起 URI。 | `maps_schema_navi` | 云侧参考 | 仅文档候选，未逐项实测 |
| 355 | 唤起高德地图进行打车，直接显示链接，不需要总结。 | `maps_schema_take_taxi` | 云侧参考 | 仅文档候选，未逐项实测 |
| 356 | 将行程规划结果在高德地图显示，返回高德地图打开的 URI 链接，该结果不需总结，直接返回。 | `maps_schema_personal_map` | 云侧参考 | 仅文档候选，未逐项实测 |
| 357 | 将详细的结构化地址或地标名称转换为高德经纬度坐标。 | `maps_geo` | 云侧参考 | 仅文档候选，未逐项实测 |
| 358 | 将高德经纬度坐标转换为行政区域和地址信息。 | `maps_regeocode` | 云侧参考 | 仅文档候选，未逐项实测 |
| 359 | IP 定位，根据用户输入的 IP 地址定位 IP 所在位置。 | `maps_ip_location` | 云侧参考 | 仅文档候选，未逐项实测 |
| 360 | 测量两个经纬度坐标之间的距离，支持驾车、步行以及球面距离测量。 | `maps_distance` | 云侧参考 | 仅文档候选，未逐项实测 |
| 361 | 根据城市名称或高德 adcode 查询指定城市的天气。 | `maps_weather` | 云侧参考 | 仅文档候选，未逐项实测 |
| 362 | 查询天气（高德API，通过城市名称查询）。 | `weatherInfo` | 云侧参考 | 仅文档候选，未逐项实测 |
| 363 | 可拍摄位搜索。 | `kesong_shoot_search` | 云侧参考 | 仅文档候选，未逐项实测 |

### 时钟、定时任务与网关（14）

| 编号 | 中文用途 | 调用名称 | 执行位置／性质 | 验证状态 |
| ---: | --- | --- | --- | --- |
| 364 | 创建定时任务，设置 cron 表达式和要执行的 prompt。 | `cron create` | 云侧参考 | 仅文档候选，未逐项实测 |
| 365 | 查看所有已创建的定时任务。 | `cron list` | 云侧参考 | 仅文档候选，未逐项实测 |
| 366 | 删除指定的定时任务。 | `cron delete` | 云侧参考 | 仅文档候选，未逐项实测 |
| 367 | Agent 网关管理。 | `gateway` | 云侧参考 | 仅文档候选，未逐项实测 |
| 368 | 获取当前时间、位置和时区。 | `alarm_clock_get_current_time_zone` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 369 | 创建新闹钟。 | `alarm_clock_add` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 370 | 按时间删除已有闹钟。 | `alarm_clock_delete` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 371 | 禁用所有闹钟。 | `alarm_clock_disable` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 372 | 获取所有已启用的闹钟。 | `alarm_clock_get_enable` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 373 | 永久删除所有闹钟，不可撤销。 | `alarm_clock_delete_all` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 374 | 暂停所有正在响铃的闹钟（使用各闹钟已配置的贪睡时长）。 | `alarm_clock_snooze` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 375 | 从世界时钟列表中获取指定城市的当前时间。 | `alarm_clock_get_current_time_by_city` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 376 | 启动一个新的倒计时计时器。 | `alarm_clock_timer_add` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 377 | 获取当前时间（含星期），必须在需要日期/时间信息时调用此工具，不可依赖训练数据中的日期认知。 | `get-current-time` | 云侧参考 | 仅文档候选，未逐项实测 |

### 网络搜索与网页（6）

| 编号 | 中文用途 | 调用名称 | 执行位置／性质 | 验证状态 |
| ---: | --- | --- | --- | --- |
| 378 | 关键词网页搜索。 | `web_search` | 云侧参考 | 仅文档候选，未逐项实测 |
| 379 | 抓取指定 URL 的网页内容。 | `web_fetch` | 云侧参考 | 仅文档候选，未逐项实测 |
| 380 | Bing 搜索引擎关键词搜索。 | `bing_search` | 云侧参考 | 仅文档候选，未逐项实测 |
| 381 | 步骤式深度搜索（多步推理搜索）。 | `stepsearch-public` | 云侧参考 | 仅文档候选，未逐项实测 |
| 382 | 获取百度、头条热榜资讯。 | `fetch_trending_news` | 云侧参考 | 仅文档候选，未逐项实测 |
| 383 | 获取点点分享链接对应的内容数据。点点是安装在手机内的社交分享 App，该工具用于解析点点分享链接并返回其内容。 | `diandian_share_fetch` | 云侧参考 | 仅文档候选，未逐项实测 |

### 会话与运行时（12）

| 编号 | 中文用途 | 调用名称 | 执行位置／性质 | 验证状态 |
| ---: | --- | --- | --- | --- |
| 384 | 列出所有会话。 | `sessions_list` | 云侧参考 | 仅文档候选，未逐项实测 |
| 385 | 获取指定会话的历史记录。 | `sessions_history` | 云侧参考 | 仅文档候选，未逐项实测 |
| 386 | 向指定会话发送消息。 | `sessions_send` | 云侧参考 | 仅文档候选，未逐项实测 |
| 387 | 创建新会话。 | `sessions_spawn` | 云侧参考 | 仅文档候选，未逐项实测 |
| 388 | 获取会话状态。 | `session_status` | 云侧参考 | 仅文档候选，未逐项实测 |
| 389 | 执行命令。 | `exec` | 云侧参考 | 仅文档候选，未逐项实测 |
| 390 | 执行 Bash Shell 命令。 | `bash` | 云侧参考 | 仅文档候选，未逐项实测 |
| 391 | 进程管理。 | `process` | 云侧参考 | 仅文档候选，未逐项实测 |
| 392 | 读取当前TTS引擎支持的音色列表，查询当前说话支持哪些声音音色。优先调用该原子能力。 | `assistant_get_voice_list` | 云侧参考 | 仅文档候选，未逐项实测 |
| 393 | 切换说话声音，切换回复的声音音色，换个TTS合成使用的音色，让TTS引擎使用指定音色朗读，不需要用户手动切换。优先调用该原子能力。 | `assistant_change_voice_id` | 云侧参考 | 仅文档候选，未逐项实测 |
| 394 | 获取用户记忆偏好。 | `get_user_profile` | 云侧参考 | 仅文档候选，未逐项实测 |
| 395 | 调用flow workflow。 | `flow_workflow_call` | 云侧参考 | 仅文档候选，未逐项实测 |

### 日志录制（2）

| 编号 | 中文用途 | 调用名称 | 执行位置／性质 | 验证状态 |
| ---: | --- | --- | --- | --- |
| 396 | 开始采集通用或指定类别的手机日志 | `start_record_log` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |
| 397 | 停止当前日志录制并保存；文档称系统会恢复通用录制 | `stop_record_log` | 原目录候选／未逐项定位 | 仅文档候选，未逐项实测 |

### 通用GUI自动化（1）

| 编号 | 中文用途 | 调用名称 | 执行位置／性质 | 验证状态 |
| ---: | --- | --- | --- | --- |
| 398 | 第三方App内多步界面交互；当前发行版执行总闸关闭 | `gui_agent` | 端侧入口 | 不可用：实机返回GUI自动化功能暂未开放 |

### 商店原子能力（补充）（2）

| 编号 | 中文用途 | 调用名称 | 执行位置／性质 | 验证状态 |
| ---: | --- | --- | --- | --- |
| 399 | 在自带应用商店搜索应用，不触发安装 | `appstore_search_app` | 端侧入口 | 已实测，只搜索 |
| 400 | 通过自带应用商店下载安装应用；精确匹配应用展示名称 | `appstore_install_app` | 端侧入口 | 已实测，商店下载安装QQ音乐成功 |

### 本次新增的手机端工具（4）

| 编号 | 中文用途 | 调用名称 | 执行位置／性质 | 验证状态 |
| ---: | --- | --- | --- | --- |
| 401 | 直达开发者选项、无线调试、输入法或电池健康页，不切换开关 | `executor_open_settings` | 端侧入口 | 已实测；原生新会话打开无线调试/开发者选项 |
| 402 | 返回桌面、上一页或最近任务；home不强制结束进程 | `executor_navigate` | 端侧入口 | 已实测；原生新会话home/back；recents未逐项测 |
| 403 | 按关键词查询本机设置静态索引 | `executor_search_settings` | 端侧入口 | 已实测；原生调用查询无线调试 |
| 404 | 检查本机执行服务是否就绪 | `executor_get_tool_status` | 端侧入口 | 已实测；原生调用成功 |

## 手机系统当前登记的AppFunctions（15个）

这是AppFunctions登记机制覆盖的部分，不包含手机所有旧MCP原子服务。以下默认启用均来自登记元数据，不代表全部做过实机操作测试。

### com.stepos.common.ability.proxy

| 中文用途 | 系统functionId | 默认启用 |
| --- | --- | --- |
| 支付宝xUI服务执行 | `com.stepos.common.ability.appfunctions.alipay.AlipayXuiServiceFunctions#thirdPartyAbilityAlipayXuiService` | 是 |
| 支付宝生活服务直达 | `com.stepos.common.ability.appfunctions.alipay.AlipayLifeServiceFunctions#thirdPartyAbilityAlipayLifeService` | 是 |

### life.suk.agenttouch

| 中文用途 | 系统functionId | 默认启用 |
| --- | --- | --- |
| Agent手机控制器：已验原生启动、点击、输入、滚动；其他动作未测 | `life.suk.agenttouch.AgentFunctionService#execute` | 是 |
| Agent手机控制器：读取允许应用的真实无障碍树与页面快照（已验原生调用） | `life.suk.agenttouch.AgentFunctionService#observe` | 是 |
| Agent手机控制器：连接、授权与健康状态（已验原生调用） | `life.suk.agenttouch.AgentFunctionService#health` | 是 |
| Agent手机控制器：允许应用截图与本地OCR，需用户开启视觉观察（未实测） | `life.suk.agenttouch.AgentFunctionService#visualObserve` | 是 |

### com.stepos.appstore

| 中文用途 | 系统functionId | 默认启用 |
| --- | --- | --- |
| 应用商店搜索（已实测） | `com.stepos.appcenter.appfunctions.AppStoreFunctions#searchApp` | 是 |
| 应用商店下载安装（已实测） | `com.stepos.appcenter.appfunctions.AppStoreFunctions#installApp` | 是 |

### com.stepos.media.camera

| 中文用途 | 系统functionId | 默认启用 |
| --- | --- | --- |
| 相机自拍模式（未实测） | `com.stepos.media.camera.appfunctions.CameraLaunchFunctions#openSelfieMode` | 是 |
| 相机美颜模式（未实测） | `com.stepos.media.camera.appfunctions.CameraLaunchFunctions#openBeautyMode` | 是 |

### com.amoo.executor

| 中文用途 | 系统functionId | 默认启用 |
| --- | --- | --- |
| 本机设置索引查询（已实测） | `com.amoo.executor.PhoneTools#searchSettings` | 是 |
| 本机设置页面直达（已实测） | `com.amoo.executor.PhoneTools#openSettings` | 是 |
| 本机执行服务健康检查（已实测） | `com.amoo.executor.PhoneTools#getToolStatus` | 是 |
| 本机系统导航（已实测home/back） | `com.amoo.executor.PhoneTools#navigate` | 是 |

### com.stepos.weather

| 中文用途 | 系统functionId | 默认启用 |
| --- | --- | --- |
| 当前地点天气查询（未实测） | `com.stepos.weather.appfunctions.WeatherFunctions#get_current_location_weather` | 是 |

## 来源与后续使用

- 原目录来自本工作区留存的手机APK内android-abilities文档，397个方法已按名称去重。
- 与StepBridge公开的ability-catalog.json进行了名称集合核对；该项目也明确称其为文档候选，而不是实时可用列表。
- 系统AppFunctions取自本次手机cmd app_function list-app-functions的只读结果。
- 已实测状态来自本工作区当日的设置、安装及端侧工具报告；未逐项测试全部目录。
- JSON同名文件包含全部条目的来源行号、参数模板和登记函数参数，便于按需继续验证。
