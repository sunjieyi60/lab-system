package xyz.jasenon.classtimetable.ui.dialog

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * 开门方式UI提供者工厂
 * 
 * 使用单例模式管理不同开门方式的UI提供者
 * 支持动态注册和获取UI提供者
 */
object DoorOpenUiProviderFactory {
    private const val TAG = "DoorOpenUiProviderFactory"
    
    private val factory: MutableMap<DoorOpeningType, DoorOpeningUIProvider> =
        ConcurrentHashMap()

    /**
     * 注册UI提供者
     * 
     * @param type 开门方式类型
     * @param provider UI提供者实例
     */
    fun register(type: DoorOpeningType, provider: DoorOpeningUIProvider) {
        val previous = factory.put(type, provider)
        if (previous != null) {
            Log.w(TAG, "DoorOpenType: $type 已存在，将被覆盖")
        }
        Log.d(TAG, "注册成功 - DoorOpenType: $type, Provider: ${provider::class.simpleName}")
    }

    /**
     * 获取UI提供者
     * 
     * @param type 开门方式类型
     * @return UI提供者实例
     * @throws RuntimeException 如果类型未注册
     */
    fun get(type: DoorOpeningType): DoorOpeningUIProvider {
        return factory[type] 
            ?: throw RuntimeException("DoorOpenType: $type 尚未注册!")
    }
    
    /**
     * 检查是否已注册
     * 
     * @param type 开门方式类型
     * @return 是否已注册
     */
    fun isRegistered(type: DoorOpeningType): Boolean {
        return factory.containsKey(type)
    }
    
    /**
     * 取消注册
     * 
     * @param type 开门方式类型
     */
    fun unregister(type: DoorOpeningType) {
        factory.remove(type)
        Log.d(TAG, "取消注册 - DoorOpenType: $type")
    }
}
