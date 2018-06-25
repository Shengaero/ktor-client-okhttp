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

import io.ktor.cio.executor
import io.ktor.client.call.HttpClientCall
import io.ktor.client.call.HttpEngineCall
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.HttpRequestData
import io.ktor.client.utils.HTTP_CLIENT_DEFAULT_DISPATCHER
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.ExecutorService

class OkHttpEngine internal constructor(override val config: OkHttpEngineConfig): HttpClientEngine {
    override val dispatcher = config.dispatcher ?: HTTP_CLIENT_DEFAULT_DISPATCHER
    private val client = config.okClient ?: config.okClientBuilder.run {
        config.interceptors.forEach { this.addInterceptor(it) }
        config.sslContext?.let { sslContext ->
            config.trustManager?.let { trustManager ->
                this.sslSocketFactory(sslContext.socketFactory, trustManager)
            }
        }
        config.authenticator?.let { this.authenticator(it) }
        (dispatcher.executor() as? ExecutorService)?.let { this.dispatcher(Dispatcher(it)) }
        this.followSslRedirects(config.followSslRedirects)
        return@run this.build()
    }

    override suspend fun execute(call: HttpClientCall, data: HttpRequestData): HttpEngineCall {
        val request = OkHttpRequest(call, client, data, dispatcher)
        val response = request.execute()
        return HttpEngineCall(request, response)
    }

    override fun close() {
        client.dispatcher().executorService().shutdown()
    }
}
