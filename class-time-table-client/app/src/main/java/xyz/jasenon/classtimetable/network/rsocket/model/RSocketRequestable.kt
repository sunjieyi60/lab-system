package xyz.jasenon.classtimetable.network.rsocket.model

import xyz.jasenon.classtimetable.util.GsonUtil

/**
 * 可请求实体接口
 *
 * 实现该接口的类表示一个具体的 RSocket 业务请求。
 * 它可以被 [xyz.jasenon.classtimetable.util.GsonUtil] 自动包装为 [RSocketRequest]。
 */
interface RSocketRequestable {
    /**
     * 获取该请求对应的 RSocket 路由地址
     * 通常定义在 [xyz.jasenon.classtimetable.network.Const.Route] 中。
     */
    fun getRoute(): String

    /**
     * 实现了该接口的方法可以使用模板方法快捷包装
     */
    fun convert(metadata: Map<String, String> = HashMap()): RSocketRequest{
        return RSocketRequest(
            route = getRoute(),
            payload = GsonUtil.toByteArray(this),
            metadata = metadata
        )
    }
}
