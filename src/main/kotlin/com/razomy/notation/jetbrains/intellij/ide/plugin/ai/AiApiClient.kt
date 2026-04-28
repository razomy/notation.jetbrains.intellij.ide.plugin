package com.razomy.notation.jetbrains.intellij.ide.plugin.ai

import com.google.gson.Gson
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

object AiApiClient {
    // ВРЕМЕННЫЙ ПЕРЕКЛЮЧАТЕЛЬ ДЛЯ ТЕСТИРОВАНИЯ UI
    private const val MOCK_MODE = true

    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
    private val gson = Gson()

    private const val API_URL_CHAT = "http://localhost:8080/api/chat"
    private const val API_URL_HISTORY = "http://localhost:8080/api/history"

    data class ApiRequest(val prompt: String, val code: String? = null)
    data class ApiResponse(val messages: List<String>)

    fun sendRequest(prompt: String, selectedCode: String? = null): List<String> {
        if (MOCK_MODE) {
            Thread.sleep(800) // Имитация задержки сети
            if (selectedCode != null) {
                // Мок для Inline Action (обязательно возвращаем <newcode>)
                return listOf(
                    "You: $prompt",
                    "AI: Sure, I modified your code based on '$prompt'. Here is the result:\n<newcode>\n// Modified by AI\n$selectedCode\n// End of modification\n</newcode>"
                )
            } else {
                // Мок для обычного чата
                return listOf(
                    "You: $prompt",
                    "AI: This is a mock response to your request: '$prompt'.\n\n```java\nSystem.out.println(\"Hello from mock AI\");\n```"
                )
            }
        }

        return try {
            val requestBody = gson.toJson(ApiRequest(prompt, selectedCode))
            val request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL_CHAT))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                gson.fromJson(response.body(), ApiResponse::class.java).messages
            } else {
                listOf("Error: Status ${response.statusCode()}")
            }
        } catch (e: Exception) {
            listOf("Error: ${e.message}")
        }
    }

    fun fetchInitialChat(): List<String> {
        if (MOCK_MODE) {
            Thread.sleep(500)
            return listOf("System: Welcome to Local AI Chat (Mock Mode enabled).")
        }

        return try {
            val request = HttpRequest.newBuilder().uri(URI.create(API_URL_HISTORY)).GET().build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) gson.fromJson(response.body(), ApiResponse::class.java).messages else emptyList()
        } catch (e: Exception) {
            listOf("System: Failed to load history.")
        }
    }
}