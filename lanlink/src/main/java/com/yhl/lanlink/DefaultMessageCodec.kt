package com.yhl.lanlink

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

class DefaultMessageCodec<T>(clazz: Class<T>, private val tag: String) : MessageCodec<T>() {

    constructor(clazz: Class<T>) : this(clazz, clazz.name)

    private var gson = GsonBuilder().setLenient().create()
    private var adapter: TypeAdapter<T> = gson.getAdapter(clazz)

    override fun getMessageType(): String {
        return tag
    }

    override fun encode(value: T): ByteArray {
        val buffer = Buffer()
        val writer = OutputStreamWriter(buffer.outputStream(), Charset.forName("UTF-8"))
        val jsonWriter = gson.newJsonWriter(writer)
        adapter.write(jsonWriter, value)
        jsonWriter.close()
        return buffer.readByteArray()
    }

    override fun decode(data: ByteArray): T {
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