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

import io.ktor.client.call.UnsupportedContentTypeException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.utils.HttpClientDefaultPool
import io.ktor.content.OutgoingContent
import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom
import io.ktor.util.moveToByteArray
import io.ktor.util.toMap
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.io.writer
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.internal.http.HttpMethod
import java.nio.ByteBuffer

internal class OkHttpRequestProducer internal constructor(
    private val requestData: HttpRequestData,
    private val body: OutgoingContent,
    private val dispatcher: CoroutineDispatcher,
    private val context: CompletableDeferred<Unit>
) {
    private val requestChannel = Channel<ByteBuffer>(1)

    init {
        when(body) {
            is OutgoingContent.ByteArrayContent -> requestChannel.offer(ByteBuffer.wrap(body.bytes()))
            is OutgoingContent.ProtocolUpgrade -> throw UnsupportedContentTypeException(body)
            is OutgoingContent.NoContent -> requestChannel.close()
            is OutgoingContent.ReadChannelContent -> prepareBody(body.readFrom())
            is OutgoingContent.WriteChannelContent -> {
                val writer = writer(Unconfined, autoFlush = true) { body.writeTo(channel) }
                prepareBody(writer.channel)
            }
        }
    }

    internal suspend fun setup(): Request {
        val builder = Request.Builder()

        // URL
        builder.url(URLBuilder().takeFrom(requestData.url).buildString())

        // Headers
        for((header, values) in requestData.headers.toMap()) {
            for(value in values) {
                builder.addHeader(header, value)
            }
        }

        // Method & Body
        val mediaType = body.contentType?.let { MediaType.parse(it.toString()) }
        val method = requestData.method.value
        val body = if(!requestChannel.isClosedForReceive && HttpMethod.permitsRequestBody(method)) {
            RequestBody.create(mediaType, requestChannel.receive().moveToByteArray())
        } else null
        requestChannel.close()
        builder.method(method, body)

        // Build
        return builder.build()
    }

    private fun prepareBody(bodyChannel: ByteReadChannel): Job {
        val result = launch(dispatcher + context) {
            while(!bodyChannel.isClosedForRead) {
                val buffer = HttpClientDefaultPool.borrow()
                try {
                    while(bodyChannel.readAvailable(buffer) != -1 && buffer.remaining() > 0) {}
                    buffer.flip()
                    requestChannel.send(buffer)
                } catch (cause: Throwable) {
                    HttpClientDefaultPool.recycle(buffer)
                    throw cause
                }
            }
        }

        result.invokeOnCompletion { cause ->
            if(cause !== null) {
                context.completeExceptionally(cause)
                requestChannel.close(cause)
            } else {
                context.complete(Unit)
            }
        }

        return result
    }
}
