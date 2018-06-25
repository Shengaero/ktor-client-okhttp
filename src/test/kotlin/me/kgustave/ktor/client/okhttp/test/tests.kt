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
package me.kgustave.ktor.client.okhttp.test

import com.google.gson.GsonBuilder
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.response.HttpResponse
import io.ktor.client.tests.utils.clientTest
import io.ktor.client.tests.utils.config
import io.ktor.client.tests.utils.test
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.gson.GsonConverter
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import me.kgustave.ktor.client.okhttp.OkHttp
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.slf4j.event.Level
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

data class House(val address: String, val tenants: List<Person>)

data class Person(val name: String, val age: Int)

private fun Application.configure() {
    install(CORS)

    install(CallLogging) {
        level = Level.DEBUG
    }

    install(StatusPages) {
        exception<Throwable> {
            call.respond(HttpStatusCode.BadRequest)
        }
    }

    install(ContentNegotiation) {
        register(ContentType.Application.Json, GsonConverter(GsonBuilder().apply(GsonBuilder::configure).create()))
    }

    routing {
        route("/test") {
            val houses = hashMapOf<String, House>()
            route("/housing") {
                post {
                    val address = call.request.queryParameters["address"]
                    houses += if(address === null) {
                        val house = call.receive<House>()
                        house.address to house
                    } else {
                        val house = requireNotNull(houses[address])
                        address to house.copy(tenants = house.tenants + call.receive<Person>())
                    }

                    call.respond(HttpStatusCode.Created)
                }
                get {
                    val address = requireNotNull(call.request.queryParameters["address"])
                    val house = requireNotNull(houses[address])
                    call.response.status(HttpStatusCode.OK)
                    call.respond(house)
                }
            }
            post("/headers") {
                val key1 = call.request.headers["Key1"]
                val key2 = call.request.headers.getAll("Key2") ?: emptyList()
                if(key1 != "KeyOne" || "KeyTwo" !in key2 || "Key2" !in key2) {
                    return@post call.respond(HttpStatusCode.BadRequest)
                }
                with(call.response.headers) {
                    append("Key3", "KeyThree")
                    append("Key3", "Key3")
                    append("Key4", "Key4")
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}

private fun GsonBuilder.configure() {
    serializeNulls()
    disableHtmlEscaping()
}

@TestInstance(PER_CLASS) class OkHttpClientEngineTests {
    private companion object {
        private const val Host = "0.0.0.0"
        private const val Port = 9090
    }

    private lateinit var service: NettyApplicationEngine

    @BeforeAll fun initialize() {
        this.service = embeddedServer(Netty, host = Host, port = Port, module = Application::configure)
        this.service.start()
    }

    @Test fun `Json Content`() = clientTest(OkHttp) {
        config {
            install(JsonFeature) {
                serializer = GsonSerializer(GsonBuilder::configure)
            }
        }

        test { client ->
            val first = client.post<HttpResponse>(port = Port, path = "/test/housing") {
                contentType(ContentType.Application.Json)
                body = House(address = "123 OkHttp", tenants = listOf(
                    Person(
                        name = "James",
                        age = 22
                    ),
                    Person(
                        name = "Peter",
                        age = 26
                    )
                ))
            }

            assertEquals(HttpStatusCode.Created, first.status)

            val mark = Person(name = "Mark", age = 24)
            val second = client.post<HttpResponse>(port = Port, path = "/test/housing") {
                url { parameters["address"] = "123 OkHttp" }
                contentType(ContentType.Application.Json)
                body = mark
            }

            assertEquals(HttpStatusCode.Created, second.status)

            val house = client.get<House>(port = Port, path = "/test/housing") {
                url { parameters["address"] = "123 OkHttp" }
                contentType(ContentType.Application.Json)
            }

            assertTrue(mark in house.tenants)
            assertEquals(3, house.tenants.size)
        }
    }

    @Test fun `Request Headers And Response Headers`() = clientTest(OkHttp) {
        test { client ->
            val response = client.post<HttpResponse>(port = Port, path = "/test/headers", body = "") {
                headers.append("Key1", "KeyOne")
                headers.append("Key2", "KeyTwo")
                headers.append("Key2", "Key2")
            }

            assertEquals(HttpStatusCode.OK, response.status)
            val keyThrees = assertNotNull(response.headers.getAll("Key3"))
            assertTrue("KeyThree" in keyThrees)
            assertTrue("Key3" in keyThrees)
            assertEquals("Key4", response.headers["Key4"])
        }
    }

    @AfterAll fun destroy() {
        if(!::service.isInitialized) {
            service.stop(0L, 0L, TimeUnit.MILLISECONDS)
        }
    }
}
