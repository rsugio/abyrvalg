package io.rsug.abyrvalg

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigResolveOptions
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.cookies.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.hocon.Hocon
import java.net.URL
import java.util.*

enum class AuthEnum { Basic }

@Serializable
data class Config(
    val httpLogLevel: LogLevel = LogLevel.INFO,
    val pi: Map<String, PI> = mutableMapOf(),
    val tenants: Map<String, Tenant> = mutableMapOf(),
    val elasticsearch: ElasticSearch = ElasticSearch(),
) {
    @Transient
    lateinit var client: HttpClient

    @Transient
    lateinit var elastic: Any

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
//            this.install(ElasticFeature)
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
        if (elasticsearch.autologin) {
//            elastic = elasticsearch.login()
        }
    }

    @Serializable
    data class PI(
        val sid: String,
        val host: String,
        val login: Login,
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

        suspend fun post(clnt: HttpClient, url: String, bodyS: String, contentType: ContentType): HttpResponse {
            lateinit var resp: HttpResponse
            resp = clnt.post(url) {
                header("Authorization", _auth)
                body = TextContent(bodyS, contentType)
            }
            return resp
        }
    }

    @Serializable
    data class Tenant(
        val tmn: String,
        val login: Login,
        val autologin: Boolean = false,
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
            accept: ContentType = ContentType.Application.Json,
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
        val password: String? = null,
    )

    @Serializable
    class ElasticSearch(
        val url: String = "http://localhost:9200",
        val autologin: Boolean = false,
        val repository: String = "abyrvalg",
        val user: String = "",
        val password: String = "",
        val useSniffer: Boolean = false,
    ) {
        fun login(): Any? {
            val u = URL(url)

            return null
        }
    }

    companion object {
        @ExperimentalSerializationApi
        fun parseHoconFromString(text: String): Config {
            val resolved = ConfigFactory.parseString(text).resolve(ConfigResolveOptions.defaults())
            return Hocon.decodeFromConfig(serializer(), resolved)
        }
        @OptIn(ExperimentalSerializationApi::class)
        fun parseHoconFromResource(resn: String): Config {
            val cfg = ConfigFactory.parseResources(resn)
            val resolved = cfg.resolve(ConfigResolveOptions.defaults())
            return Hocon.decodeFromConfig(serializer(), resolved)
        }
    }

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

