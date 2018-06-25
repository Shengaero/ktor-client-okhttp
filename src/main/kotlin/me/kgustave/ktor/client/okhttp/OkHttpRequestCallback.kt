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

import kotlinx.coroutines.experimental.CancellableContinuation
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.channels.Channel
import okhttp3.*
import java.io.IOException

internal class OkHttpRequestCallback
internal constructor(private val continuation: CancellableContinuation<Response>): Callback {

    override fun onFailure(call: Call, e: IOException) {
        continuation.resumeWithException(e)
    }

    override fun onResponse(call: Call, response: Response) {
        if(call.isCanceled) {
            continuation.cancel(CancellationException("Call cancelled!"))
            return
        }
        continuation.resume(response)
    }
}
