package xyz.jasenon.classtimetable.network.handler

import android.util.Log
import org.json.JSONArray
import xyz.jasenon.classtimetable.dto.CourseScheduleDto
import xyz.jasenon.classtimetable.network.observe.RemoteDataObservable
import xyz.jasenon.classtimetable.protocol.CommandType
import xyz.jasenon.classtimetable.protocol.SmartBoardPacket
import xyz.jasenon.classtimetable.constants.WeekType

/**
 * 课表响应包处理器
 * 解析 TIMETABLE_RESP payload，通过 RemoteDataObservable 更新课表数据，观察者自动刷新页面。
 */
class TimetablePacketHandler : PacketHandler {

    override fun handle(packet: SmartBoardPacket): Boolean {
        if (packet.cmdType != CommandType.TIMETABLE_RESP) return false
        val payload = packet.payload ?: return true
        try {
            val json = String(payload, Charsets.UTF_8)
            val list = parseTimetableJson(json)
            RemoteDataObservable.updateTimetable(list)
            Log.d(TAG, "Timetable updated, count=${list.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Parse timetable failed", e)
        }
        return true
    }

    private fun parseTimetableJson(json: String): List<CourseScheduleDto> {
        val list = mutableListOf<CourseScheduleDto>()
        val arr = JSONArray(json)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                CourseScheduleDto(
                    id = obj.optLong("id", 0L).takeIf { it > 0 },
                    courseId = obj.optLong("courseId", 0L).takeIf { it > 0 },
                    courseName = obj.optString("courseName").takeIf { it.isNotEmpty() },
                    teacherId = obj.optLong("teacherId", 0L).takeIf { it > 0 },
                    teacherName = obj.optString("teacherName").takeIf { it.isNotEmpty() },
                    laboratoryId = obj.optLong("laboratoryId", 0L).takeIf { it > 0 },
                    laboratoryName = obj.optString("laboratoryName").takeIf { it.isNotEmpty() },
                    weekType = obj.optString("weekType").let { when (it) {
                        "SINGLE" -> WeekType.Single
                        "DOUBLE" -> WeekType.Double
                        "BOTH" -> WeekType.Both
                        else -> null
                    } },
                    startWeek = obj.optInt("startWeek", 0).takeIf { it > 0 },
                    endWeek = obj.optInt("endWeek", 0).takeIf { it > 0 },
                    weekdays = obj.optJSONArray("weekdays")?.let { a ->
                        MutableList<Int?>(a.length()) { i -> a.optInt(i).takeIf { v -> v != 0 } }
                    },
                    startSection = obj.optInt("startSection", 0).takeIf { it > 0 },
                    endSection = obj.optInt("endSection", 0).takeIf { it > 0 }
                )
            )
        }
        return list
    }

    companion object {
        private const val TAG = "TimetablePacketHandler"
    }
}
