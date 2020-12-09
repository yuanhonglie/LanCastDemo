package com.yhl.lanlink.data

import com.google.gson.GsonBuilder
import com.google.gson.JsonIOException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonToken
import com.yhl.lanlink.interfaces.MessageCodec
import okio.Buffer
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.Charset

class TaskInfoCodec: MessageCodec() {
    private var gson = GsonBuilder().setLenient().create()
    private var adapter: TypeAdapter<TaskInfo>

    init {
        adapter = gson.getAdapter(TaskInfo::class.java)
    }

    override fun getMessageType(): String {
        return TaskInfo::class.qualifiedName ?: "TaskInfo"
    }

    override fun encode(msg: Any): ByteArray {
        val value = msg as TaskInfo
        val buffer = Buffer()
        val writer = OutputStreamWriter(buffer.outputStream(), Charset.forName("UTF-8"))
        val jsonWriter = gson.newJsonWriter(writer)
        adapter.write(jsonWriter, value)
        jsonWriter.close()
        return buffer.readByteArray()
    }

    override fun decode(data: ByteArray): TaskInfo {
        val input = ByteArrayInputStream(data)
        return input.use { input ->
            val reader = InputStreamReader(input)
            val jsonReader = gson.newJsonReader(reader)
            val result = adapter.read(jsonReader)
            if (jsonReader.peek() != JsonToken.END_DOCUMENT) {
                throw JsonIOException("JSON document was not fully consumed.")
            }
            result
        }
    }
}