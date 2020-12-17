package com.yhl.lanlink.log

import android.util.Log
import com.yhl.lanlink.LOG_DISABLE

class Logger {

    companion object {

        fun v(tag: String, msg: String) {
            if (LOG_DISABLE) {
                return
            }
            Log.v(tag, msg)
        }

        fun d(tag: String, msg: String) {
            if (LOG_DISABLE) {
                return
            }
            Log.d(tag, msg)
        }

        fun i(tag: String, msg: String) {
            if (LOG_DISABLE) {
                return
            }
            Log.i(tag, msg)
        }

        fun w(tag: String, msg: String) {
            if (LOG_DISABLE) {
                return
            }
            Log.w(tag, msg)
        }

        fun e(tag: String, msg: String) {
            if (LOG_DISABLE) {
                return
            }
            Log.e(tag, msg)
        }
    }
}