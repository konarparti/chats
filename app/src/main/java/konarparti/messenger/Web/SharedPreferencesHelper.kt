package konarparti.messenger.Web

import android.content.Context
import android.content.SharedPreferences

object SharedPreferencesHelper {

    private const val PREFS_NAME = "my_app_prefs"
    private const val KEY_TOKEN = "token"

    // Получаем токен из SharedPreferences
    fun getToken(context: Context): String? {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(KEY_TOKEN, null)
    }

    // Сохраняем токен в SharedPreferences
    fun saveToken(context: Context, token: String) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(KEY_TOKEN, token)
        editor.apply()
    }

    // Удаляем токен
    fun clearToken(context: Context) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove(KEY_TOKEN)
        editor.apply()
    }
}
