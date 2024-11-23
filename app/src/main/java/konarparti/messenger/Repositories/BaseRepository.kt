package konarparti.messenger.Repositories

import android.util.Log
import konarparti.messenger.Base.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

open class BaseRepository {

    suspend fun <T> apiCall(
        callback: suspend () -> T
    ): Resource<T> {
        return withContext(Dispatchers.IO) {
            try {
                Resource.Success(callback())
            } catch (e: Throwable) {
                Log.d("api", e.message.toString())
                Resource.Error(e.message!!)
            }
        }
    }
}