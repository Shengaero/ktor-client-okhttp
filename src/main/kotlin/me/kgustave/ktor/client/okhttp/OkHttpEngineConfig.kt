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

import io.ktor.client.engine.HttpClientEngineConfig
import okhttp3.*
import javax.net.ssl.X509TrustManager

class OkHttpEngineConfig internal constructor(): HttpClientEngineConfig() {
    var okClient: OkHttpClient? = null
    var okClientBuilder = OkHttpClient.Builder()
    val interceptors = mutableSetOf<(Interceptor.Chain) -> Response>()
    var trustManager: X509TrustManager? = null
    var followSslRedirects = false
    var authenticator: ((Route, Response) -> Request?)? = null
}
