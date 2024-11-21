package konarparti.messenger.Repositories

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
                Resource.Error(e.message!!)
            }
        }
    }
}