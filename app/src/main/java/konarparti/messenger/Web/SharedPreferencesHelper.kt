package konarparti.messenger.Web

import android.content.Context
import android.content.SharedPreferences

object SharedPreferencesHelper {

    private const val PREFS_NAME = "konarparti_prefs"
    private const val KEY_TOKEN = "token"
    private const val LOGIN_TOKEN = "login"

    fun getLogin(context: Context): String? {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(LOGIN_TOKEN, null)
    }

    fun setLogin(context: Context, login: String) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(LOGIN_TOKEN, login)
        editor.apply()
    }

    fun getToken(context: Context): String? {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(KEY_TOKEN, null)
    }

    fun saveToken(context: Context, token: String) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(KEY_TOKEN, token)
        editor.apply()
    }

    fun clearToken(context: Context) {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove(KEY_TOKEN)
        editor.apply()
    }
}
