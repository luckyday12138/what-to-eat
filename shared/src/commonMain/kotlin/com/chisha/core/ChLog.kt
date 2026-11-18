package com.chisha.core

/**
 * 轻量诊断日志。仅用于在真机上量取网耗时：桌面端打到 stdout，Android 打到 logcat（tag CHISHA）。
 * 不做级别管理，不要用它输出业务数据。
 */
expect fun chLog(msg: String)
