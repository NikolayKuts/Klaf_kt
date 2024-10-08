package plugins.telegramAppDistribution

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentDisposition
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import java.io.File

class TelegramApi(
    private val httpClient: HttpClient
) {

    companion object {

        private const val BASE_URL = "https://api.telegram.org/bot"
        private const val SEND_DOCUMENT_ENDPOINT = "sendDocument"
        private const val CHAT_ID_PARAMETER_KEY = "chat_id"
        private const val DOCUMENT_PARAMETER_KEY = "document"
    }

    suspend fun uploadFile(file: File, token: String, chatId: String) {
        val url = "$BASE_URL$token/$SEND_DOCUMENT_ENDPOINT"
        val headers = Headers.build {
            append(
                name = HttpHeaders.ContentDisposition,
                value = "${ContentDisposition.Parameters.FileName}=\"${file.name}\""
            )
        }
        val partData = formData {
            append(
                key = DOCUMENT_PARAMETER_KEY,
                value = file.readBytes(),
                headers = headers
            )
        }

        val response = httpClient.post(urlString = url) {
            parameter(CHAT_ID_PARAMETER_KEY, chatId)
            setBody(body = MultiPartFormDataContent(parts = partData))
        }

        println("TelegramApi. Upload file response: ${response.bodyAsText()}")
    }
}