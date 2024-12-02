package konarparti.messenger.Web


import konarparti.messenger.Base.Message
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("/channel/{channelName}")
    suspend fun getMessages(
        @Path("channelName") channelName: String,
        @Query("lastKnownId") lastKnownId: Int = 0,
        @Query("limit") limit: Int = 100,
        @Query("reverse") reverse: Boolean = false
    ): List<Message>

    @POST("/login")
    suspend fun login(@Body request: LoginRequest): String

    @GET("/channels")
    suspend fun getChannels(): List<String>

    @POST("/messages")
    @Headers("Content-Type: application/json;charset=UTF-8")
    suspend fun sendMessage(
        @Header("X-Auth-Token") token: String,
        @Body message: Message
    ): Response<Int>
}
