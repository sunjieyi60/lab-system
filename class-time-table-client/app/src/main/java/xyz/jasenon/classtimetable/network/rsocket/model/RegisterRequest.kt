import xyz.jasenon.classtimetable.network.Const
import xyz.jasenon.classtimetable.network.Const.Route.DEVICE_REGISTER
import xyz.jasenon.classtimetable.network.rsocket.model.RSocketRequestable

data class RegisterRequest(
    val uuid: String,
    val laboratoryId: Long? = null
) : RSocketRequestable, Const.Route {
    override fun getRoute(): String = DEVICE_REGISTER
}