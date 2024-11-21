package konarparti.messenger.Web

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenManager.token.firstOrNull() }
        val request = chain.request().newBuilder()
            .addHeader("Content-Type", "application/json")
        token?.let {
            request.addHeader("X-Auth-Token", it)
        }
        return chain.proceed(request.build())
    }
}
