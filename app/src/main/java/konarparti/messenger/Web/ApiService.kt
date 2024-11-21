package konarparti.messenger.Web


import konarparti.messenger.Base.Message
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("/channel/{channelName}")
    suspend fun getMessages(
        @Path("channelName") channelName: String,
        @Query("limit") limit: Int = 20,
        @Query("lastKnownId") lastKnownId: String? = null
    ): List<Message>

    @POST("/login")
    suspend fun login(@Body request: LoginRequest): String

    @GET("/channels")
    suspend fun getChannels(): List<String>
}
