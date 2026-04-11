package xyz.jasenon.classtimetable.network.rsocket.model

import kotlinx.serialization.Serializable
import xyz.jasenon.classtimetable.config.DeviceProfile
import xyz.jasenon.classtimetable.config.DeviceRuntimeConfig

@Serializable
data class RegisterResponse(
    val uuid : String,
    val config : DeviceRuntimeConfig?,
    val laboraotryId: Long?
){

}
