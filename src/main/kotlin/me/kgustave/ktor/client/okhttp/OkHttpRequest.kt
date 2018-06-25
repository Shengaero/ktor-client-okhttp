/*
 * Copyright 2018 Kaidan Gustave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.kgustave.ktor.client.okhttp

import io.ktor.client.call.HttpClientCall
import io.ktor.client.request.HttpRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.response.HttpResponse
import io.ktor.content.OutgoingContent
import io.ktor.util.Attributes
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import okhttp3.OkHttpClient
import okhttp3.Response
import java.util.*

internal class OkHttpRequest internal constructor(
    override val call: HttpClientCall,
    private val client: OkHttpClient,
    private val requestData: HttpRequestData,
    private val dispatcher: CoroutineDispatcher
): HttpRequest {
    override val attributes = Attributes().apply(requestData.attributes)
    override val content = requireNotNull(requestData.body as? OutgoingContent) { "Invalid request body: ${requestData.body}" }
    override val executionContext = requestData.executionContext
    override val method get() = requestData.method
    override val url get() = requestData.url
    override val headers get() = requestData.headers

    suspend fun execute(): HttpResponse {
        val producer = OkHttpRequestProducer(requestData, content, dispatcher, executionContext)
        val request = producer.setup()
        val date = Date()
        val response = suspendCancellableCoroutine<Response> { cont ->
            OkHttpRequestCallback(cont).also {
                client.newCall(request).enqueue(it)
            }
        }
        return OkHttpResponse(call, date, executionContext, response)
    }
}
