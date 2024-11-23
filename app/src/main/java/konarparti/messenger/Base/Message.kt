package konarparti.messenger.Base

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Message(
    val id: Int,
    val from: String,
    val to: String,
    val data: Data,
    val time: String,
)

@JsonClass(generateAdapter = true)
data class Data(
    val Image: Image? = null,
    val Text: Text? = null,
)

@JsonClass(generateAdapter = true)
data class Image(
    val link: String = "",
)

@JsonClass(generateAdapter = true)
data class Text(
    val text: String = "",
)