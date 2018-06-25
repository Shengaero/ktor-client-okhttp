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

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory

/**
 * Factory for [HttpClientEngine]s using [okhttp](https://github.com/square/okhttp)
 *
 * @author Kaidan Gustave
 */
object OkHttp: HttpClientEngineFactory<OkHttpEngineConfig> {
    override fun create(block: OkHttpEngineConfig.() -> Unit): HttpClientEngine {
        return OkHttpEngine(OkHttpEngineConfig().also(block))
    }
}
