package xyz.jasenon.classtimetable.ui.dialog

import android.util.Log

/**
 * 开门方式UI提供者初始化器
 * 
 * 负责在应用启动时注册所有可用的UI提供者
 * 建议在 Application.onCreate() 或 MainActivity.onCreate() 中调用
 */
object DoorOpenUiProviderInitializer {
    private const val TAG = "DoorOpenUiProviderInitializer"
    
    /**
     * 初始化并注册所有UI提供者
     * 
     * 这个方法会注册所有已实现的开门方式UI提供者
     * 如果某个提供者还未实现，可以注释掉对应的注册代码
     */
    fun initialize() {
        Log.d(TAG, "开始初始化开门方式UI提供者...")
        
        // 注册密码开门UI提供者
        DoorOpenUiProviderFactory.register(
            DoorOpeningType.PASSWORD,
            PwdOpenUiProvider(
                title = "密码开门",
                description = "请输入6位数字密码"
            )
        )
        
        // 注册人脸识别开门UI提供者
        DoorOpenUiProviderFactory.register(
            DoorOpeningType.FACE_RECOGNITION,
            FaceOpenUiProvider(
                title = "人脸识别开门",
                description = "请面向摄像头进行人脸识别"
            )
        )
        // 
        // DoorOpenUiProviderFactory.register(
        //     DoorOpeningType.QR_CODE,
        //     QRCodeUiProvider(...)
        // )
        // 
        // DoorOpenUiProviderFactory.register(
        //     DoorOpeningType.CARD,
        //     CardUiProvider(...)
        // )
        
        Log.d(TAG, "开门方式UI提供者初始化完成")
    }
}



