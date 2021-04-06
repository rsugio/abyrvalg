package io.rsug.abyrvalg

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.cookies.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import k3.ServiceEndpoint
import k3.ServiceEndpoints
import k5.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.util.*

enum class AuthEnum { Basic }

// в xmlSoap много полиморфизма внутри SOAP-Envelope
val xmlSoap = xml()

@Serializable
data class Config(
    val httpLogLevel: LogLevel = LogLevel.INFO,
    val pi: Map<String, PI> = mutableMapOf(),
    val tenants: Map<String, Tenant> = mutableMapOf(),
) {
    @Transient
    lateinit var client: HttpClient

    fun createClient() {
        client = HttpClient(CIO) {
            engine {
                threadsCount = 2
            }
            install(HttpCookies) {
                storage = AcceptAllCookiesStorage()
            }
            install(Logging) {
//                logger = httpLogger
                level = httpLogLevel
            }
        }
    }
    fun init() {
        runBlocking {
            tenants.forEach {
                val ten = it.value
                ten.nick = it.key
                if (ten.autologin) {
                    ten.login(client)
                }
                println("INFO: ${ten.nick} autologged=${ten.logged}") //TODO - logger
            }
        }
    }

    @Serializable
    data class PI(
        val sid: String,
        val host: String,
        val login: Login
    ) {
        var _auth: String = ""

        init {
            require(login.auth == AuthEnum.Basic)
            _auth = "Basic " +
                    Base64.getEncoder().encodeToString("${login.login}:${login.password}".encodeToByteArray())
        }

        suspend fun postSOAP(clnt: HttpClient, url: String, bodySOAPXML: String): HttpResponse {
            lateinit var resp: HttpResponse
            resp = clnt.post(url) {
                header("Authorization", _auth)
                body = TextContent(bodySOAPXML, ContentType.Text.Xml)
            }
            requireXml(resp)
            return resp
        }
    }

    @Serializable
    data class Tenant(
        val tmn: String,
        val login: Login,
        val autologin: Boolean = false
    ) {
        var nick: String = ""
        var logged: Boolean = false

        private var _auth: String = ""
        var xcsrftoken: String = "Fetch"
            set(new) {
                field = new
            }

        suspend fun login(clnt: HttpClient) {
            require(login.auth == AuthEnum.Basic)
            _auth =
                "Basic " + Base64.getEncoder().encodeToString("${login.login}:${login.password}".encodeToByteArray())
            val resp: HttpResponse = clnt.head("${tmn}/api/v1/") {
                header("Authorization", _auth)
                header("X-CSRF-TOKEN", xcsrftoken)
            }
            require(resp.status.value == 200)
            xcsrftoken = resp.headers.get("X-CSRF-TOKEN") ?: error("No x-csrf-token header found during login")
            logged = true
        }

        suspend fun loginIfNeeded(client: HttpClient) {
            if (!logged) login(client)
            require(logged)
        }

        fun defaultHeaders(
            b: HttpRequestBuilder,
            accept: ContentType = ContentType.Application.Json
        ) {
            b.header("Authorization", _auth)
            b.header("X-CSRF-Token", xcsrftoken)
            b.accept(accept)
        }

        suspend fun getJson(clnt: HttpClient, url: String): HttpResponse {
            val resp: HttpResponse = clnt.get(url) {
                accept(ContentType.Application.Json)
                defaultHeaders(this)
            }
            requireJson(resp)
            return resp
        }

    }

    @Serializable
    class Login(
        val auth: AuthEnum = AuthEnum.Basic,
        val login: String? = null,
        val password: String? = null
    )
}


fun requireJson(resp: HttpResponse) {
    require(resp.status.equals(HttpStatusCode.OK))
    require(resp.contentType()!!.match(ContentType.Application.Json))
}

fun requireXml(resp: HttpResponse) {
    require(resp.status.equals(HttpStatusCode.OK))
    require(
        resp.contentType()!!.match(ContentType.Application.Xml) ||
                resp.contentType()!!.match(ContentType.Text.Xml)
    )
}

