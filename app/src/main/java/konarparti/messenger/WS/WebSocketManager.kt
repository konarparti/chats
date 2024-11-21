package konarparti.messenger.WS

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class WebSocketManager(private val token: String) {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null


    //StudiOTHECiAidea password
    fun connect() {
        val request = Request.Builder()
            .url("wss://faerytea.name:8008/ws/<username>?token=$token")
            .build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                handleNewMessage(text)
            }
        })
    }

    private fun handleNewMessage(messageJson: String) {
        // Обновление сообщений в локальной базе данных
    }

    fun disconnect() {
        webSocket?.close(1000, "Client disconnected")
    }
}
