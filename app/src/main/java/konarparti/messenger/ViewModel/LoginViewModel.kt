package konarparti.messenger.ViewModel

import androidx.lifecycle.viewModelScope
import konarparti.messenger.Web.LoginRequest
import konarparti.messenger.Web.RetrofitInstance
import konarparti.messenger.Web.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import androidx.lifecycle.ViewModel
import konarparti.messenger.MainActivity
import konarparti.messenger.R
import konarparti.messenger.Web.SharedPreferencesHelper

class LoginViewModel(private val tokenManager: TokenManager, application: MainActivity) : ViewModel() {
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState

    private val context = application

    fun login(name: String, pwd: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val response = RetrofitInstance.api.login(LoginRequest(name, pwd))
                if (response != "") {
                    tokenManager.saveToken(response)
                    SharedPreferencesHelper.saveToken(context, response)
                    SharedPreferencesHelper.setLogin(context, name)
                    _uiState.value = LoginUiState.Success
                } else {
                    val errorMessage = context.getString(R.string.invalid_credentials)
                    _uiState.value = LoginUiState.Error(errorMessage)
                }
            } catch (e: IOException) {
                _uiState.value = LoginUiState.Error(context.getString(R.string.network_error))
            } catch (e: HttpException) {
                if(e.code() == 401)
                    _uiState.value = LoginUiState.Error(context.getString(R.string.invalid_credentials))
                else
                    _uiState.value = LoginUiState.Error(context.getString(R.string.server_error))
            }
        }
    }

    fun logout() {
        viewModelScope.launch{
            _uiState.value = LoginUiState.Idle
            tokenManager.clearToken()
            SharedPreferencesHelper.clearToken(context)
        }
    }
}

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()
    object Success : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
