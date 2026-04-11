package xyz.jasenon.classtimetable.util

import com.google.gson.Gson

/**
 * gson序列化反序列化工具类
 */
class GsonUtil {

    companion object {

        val gson = Gson();

        /**
         * 从实体转化为byteArray
         */
        fun toByteArray(any: Any): ByteArray{
            return gson.toJson(any).toByteArray(Charsets.UTF_8)
        }

        /**
         * 从byteArray转化为实体
         */
        fun <T> to(byteArray: ByteArray, clazz: Class<T>): T{
            val json = byteArray.toString(Charsets.UTF_8)
            return gson.fromJson(json, clazz);
        }

        /**
         * 将 ByteArray 反序列化为实体
         * 使用 inline + reified 简化调用：GsonUtil.to<User>(bytes)
         */
        inline fun <reified T> to(byteArray: ByteArray): T {
            val json = byteArray.toString(Charsets.UTF_8)
            return gson.fromJson(json, T::class.java)
        }

    }


}