package com.yhl.lanlink

/**
 * 建立连接
 */
const val URI_CONNECTION_CONNECT = "/connection/connect"

/**
 * 心跳
 */
const val URI_CONNECTION_HEART_BEAT = "/connection/heartbeat"

/**
 * 断开连接
 */
const val URI_CONNECTION_DISCONNECT = "/connection/disconnect"

/**
 * 媒体传送
 */
const val URI_CAST_TRANSFER = "/channel/cast/transfer"

/**
 * 退出投屏界面
 */
const val URI_CAST_EXIT = "/channel/cast/exit"

/**
 * 发送消息
 */
const val URI_SEND_MESSAGE = "/channel/msg"

const val RESULT_SUCCESS = 0
const val RESULT_FAILED = -1
const val RESULT_FAILED_UNKNOWN = RESULT_FAILED
const val RESULT_FAILED_INVALID_TOKEN = -2
const val RESULT_FAILED_RECEIVER_OFFLINE = -3
const val RESULT_FAILED_SENDER_OFFLINE = -4
const val RESULT_FAILED_RECEIVER_TIMEOUT = -5
const val RESULT_FAILED_SENDER_TIMEOUT = -6
const val RESULT_FAILED_MESSAGE_PARSER_MISSING = -7


const val RESULT_MESSAGE_SUCCESS = "success"
const val RESULT_MESSAGE_FAILED = "failed"
const val RESULT_MESSAGE_INVALID_TOKEN = "invalid token"

const val MIME_TYPE_JSON = "application/json"

const val LOG_DISABLE = false
const val FILE_SERVER_PORT = 18050
const val MESSAGE_SERVER_PORT = 18030

/**
 * 心跳周期
 */
const val INTERVAL_HEART_BEAT = 10000L
const val MAX_SERVER_HEART_BEAT_LOST = 3
const val CLIENT_TIMEOUT = INTERVAL_HEART_BEAT * MAX_SERVER_HEART_BEAT_LOST

const val LINK_SERVICE_TYPE = "_lanlink._tcp."
const val LINK_SERVICE_RECEIVER = "LanLink"

/**
 * 退出投屏界面
 */
const val CONTROL_EXIT_CAST = 1

const val MSG_UI_ACTIVITY_REGISTER = 0

const val MSG_WORKER_HEART_BEAT = 1000
const val MSG_WORKER_SERVER_CONNECT = MSG_WORKER_HEART_BEAT + 1
const val MSG_WORKER_SERVER_DISCONNECT = MSG_WORKER_HEART_BEAT + 2
const val MSG_WORKER_CHECK_CLIENT_TIMEOUT = MSG_WORKER_HEART_BEAT + 3
