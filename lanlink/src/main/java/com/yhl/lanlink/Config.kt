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
const val URI_MEDIA_TRANSFER = "/media/transfer"

const val RESULT_SUCCESS = 0
const val RESULT_FAILED_UNKNOWN = -1
const val RESULT_FAILED_SERVER_TIMEOUT = -2
const val RESULT_FAILED_INVALID_TOKEN = -3
const val RESULT_FAILED = RESULT_FAILED_UNKNOWN

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
const val INTERVAL_HEART_BEAT = 5000L
const val MAX_SERVER_HEART_BEAT_LOST = 3

const val LINK_SERVICE_TYPE = "_lanlink._tcp."
const val LINK_SERVICE_RECEIVER = "LanLink"


const val MSG_UI_ACTIVITY_REGISTER = 0

const val MSG_WORKER_HEART_BEAT = 1000
const val MSG_WORKER_SERVER_CONNECT = MSG_WORKER_HEART_BEAT + 1
const val MSG_WORKER_SERVER_DISCONNECT = MSG_WORKER_HEART_BEAT + 2
