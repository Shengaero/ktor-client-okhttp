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
import io.ktor.client.response.HttpResponse
import io.ktor.http.*
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.io.ByteReadChannel
import kotlinx.coroutines.experimental.io.EmptyByteReadChannel
import okhttp3.Protocol.*
import okhttp3.Response
import java.time.Instant
import java.util.*

internal class OkHttpResponse internal constructor(
    override val call: HttpClientCall,
    override val requestTime: Date,
    override val executionContext: CompletableDeferred<Unit>,
    private val okResponse: Response
): HttpResponse {
    override val status = HttpStatusCode.fromValue(okResponse.code())
    override val content = okResponse.body()?.bytes()?.let { ByteReadChannel(it) } ?: EmptyByteReadChannel

    override val responseTime = run {
        val dateHeader = okResponse.header(HttpHeaders.Date)
        if(dateHeader !== null) {
            return@run Date.from(Instant.from(httpDateFormat.parse(dateHeader)))!!
        }
        return@run Date()
    }

    override val version = run {
        when(okResponse.protocol()) {
            HTTP_1_0 -> HttpProtocolVersion.fromValue("http/1.0", 1, 0)
            HTTP_1_1 -> HttpProtocolVersion.HTTP_1_1
            HTTP_2 -> HttpProtocolVersion.HTTP_2_0
            else -> throw UnsupportedOperationException(
                "Cannot handle protocol response version '${okResponse.protocol()}'!"
            )
        }
    }

    override val headers = Headers.build {
        for((header, values) in okResponse.headers().toMultimap()) {
            for(value in values) {
                this.append(header, value)
            }
        }
    }

    override fun close() {
        okResponse.close()
    }
}
