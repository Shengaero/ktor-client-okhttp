# ktor-client-okhttp

This library is a [ktor-client](https://ktor.io/) engine wrapper
for [okhttp](https://github.com/square/okhttp).

Like other ktor-client engines, all that is required is plugging it into
a `HttpClient` like so:

```kotlin
val client = HttpClient(OkHttp)
```

### Gradle Dependency

```gradle
repositories {
    jcenter()
}

dependencies {
    compile "me.kgustave:ktor-client-okhttp:$ktorClientOkHttpVersion"
}
```

### Maven Dependency

```xml
<repositories>
  <repository>
    <id>jcenter</id>
    <name>jcenter-bintray</name>
    <url>https://jcenter.bintray.com</url>
  </repository>
</repositories>
```

```xml
<dependencies>
  <dependency>
    <groupId>me.kgustave</groupId>
    <artifactId>ktor-client-okhttp</artifactId>
    <version>${ktorClientOkHttpVersion}</version>
    <type>pom</type>
  </dependency>
</dependencies>
```

### License

`ktor-client-okhttp` is licensed under the [Apache 2.0 License](https://github.com/Shengaero/ktor-client-okhttp/tree/master/LICENSE)

```
Copyright 2018 Kaidan Gustave

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
