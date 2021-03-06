/*
 *    Copyright 2020 Criteo
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.criteo.publisher.model

import com.criteo.publisher.mock.MockedDependenciesRule
import com.criteo.publisher.util.JsonSerializer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import javax.inject.Inject

class RemoteConfigResponseTest {

  @Rule
  @JvmField
  val mockedDependenciesRule = MockedDependenciesRule()

  @Inject
  private lateinit var jsonSerializer: JsonSerializer

  @Test
  fun read_GivenEmptyJson_ReturnEmptyObject() {
    val json = """{
    }""".trimIndent()

    val response = readFromString(json)

    assertThat(response.killSwitch).isNull()
    assertThat(response.androidDisplayUrlMacro).isNull()
    assertThat(response.androidAdTagUrlMode).isNull()
    assertThat(response.androidAdTagDataMacro).isNull()
    assertThat(response.androidAdTagDataMode).isNull()
  }

  @Test
  fun read_GivenJsonWithUnknownProperty_IgnoreThemAndReturnEmptyObject() {
    val json = """{
      "unknownPropertyThatShouldBeIgnored": ""
    }""".trimIndent()

    val response = readFromString(json)

    assertThat(response.killSwitch).isNull()
    assertThat(response.androidDisplayUrlMacro).isNull()
    assertThat(response.androidAdTagUrlMode).isNull()
    assertThat(response.androidAdTagDataMacro).isNull()
    assertThat(response.androidAdTagDataMode).isNull()
  }

  @Test
  fun read_GivenFullJson_ReturnFullObject() {
    val json = """{
      "killSwitch": true,
      "AndroidDisplayUrlMacro": "%%macroUrl%%",
      "AndroidAdTagUrlMode": "<html />",
      "AndroidAdTagDataMacro": "%%macroData%%",
      "AndroidAdTagDataMode": "<body />",
      "csmEnabled": true
    }""".trimIndent()

    val response = readFromString(json)

    assertThat(response.killSwitch).isTrue()
    assertThat(response.androidDisplayUrlMacro).isEqualTo("%%macroUrl%%")
    assertThat(response.androidAdTagUrlMode).isEqualTo("<html />")
    assertThat(response.androidAdTagDataMacro).isEqualTo("%%macroData%%")
    assertThat(response.androidAdTagDataMode).isEqualTo("<body />")
    assertThat(response.csmEnabled).isTrue()
  }

  @Test
  fun read_GivenBooleanWithNumber_ThrowIOException() {
    val json = """{
      "killSwitch": 1
    }""".trimIndent()

    assertThatCode {
      readFromString(json)
    }.isInstanceOf(IOException::class.java)
  }

  @Test
  fun read_GivenBooleanWithTrueString_ReturnTrue() {
    val json = """{
      "killSwitch": "true"
    }""".trimIndent()

    val response = readFromString(json)

    assertThat(response.killSwitch).isTrue()
  }

  @Test
  fun read_GivenBooleanWithNotTheTrueString_ReturnFalse() {
    val json = """{
      "killSwitch": "anything"
    }""".trimIndent()

    val response = readFromString(json)

    assertThat(response.killSwitch).isFalse()
  }

  @Test
  fun read_GivenIllFormedJson_ThrowIOException() {
    assertThatCode {
      readFromString("{")
    }.isInstanceOf(IOException::class.java)
  }

  @Test
  fun read_GivenEmptyString_ThrowIOException() {
    assertThatCode {
      readFromString("")
    }.isInstanceOf(IOException::class.java)
  }

  private fun readFromString(json: String): RemoteConfigResponse {
    return ByteArrayInputStream(json.toByteArray(Charsets.UTF_8)).use {
      jsonSerializer.read(RemoteConfigResponse::class.java, it)
    }
  }

}