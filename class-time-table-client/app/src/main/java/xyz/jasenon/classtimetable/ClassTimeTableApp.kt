package xyz.jasenon.classtimetable

import android.app.Application
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.LogLevel
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.AndroidPrinter
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ClassTimeTableApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 初始化 XLog
        val config = LogConfiguration.Builder()
            .logLevel(LogLevel.ALL) // 默认开启所有日志，或者根据需要调整
            .tag("ClassTimeTable")
            .build()
        
        XLog.init(config, AndroidPrinter())
    }
}
