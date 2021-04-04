import io.ktor.client.*
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
val xmlSoap = k5.xml()

@Serializable
data class Config(
    val httpLogLevel: LogLevel = LogLevel.INFO,
    val pi: Map<String, PI> = mutableMapOf(),
    val tenants: Map<String, Tenant> = mutableMapOf(),
) {
    @Transient
    lateinit var client: HttpClient

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

class CheckCapabilitiesCPI(val config: Config, val nick: String) {
    val tenant: Config.Tenant

    init {
        tenant = config.tenants[nick] ?: error("Не могу найти $nick в конфиге, шары залил что-ли уже с утра?!")
    }

    fun exec(): StringBuilder {
        val caps = StringBuilder()
        var ok = false
        runBlocking {
            tenant.loginIfNeeded(config.client)
            var resp: HttpResponse = config.client.get("${tenant.tmn}/api/v1/\$metadata") {
                tenant.defaultHeaders(this, ContentType.Application.Xml)
            }
            ok = resp.status == HttpStatusCode.OK && resp.contentType()!!.match(ContentType.Application.Xml)
            caps.append("/api/v1 : rc=${resp.status.value} API=${if (ok) "Поддерживается" else "НеПоддерживается"}\n")

            resp = config.client.get("${tenant.tmn}/itspaces/odata/1.0/workspace.svc/\$metadata") {
                tenant.defaultHeaders(this, ContentType.Application.Xml)
            }
            ok = resp.status == HttpStatusCode.OK && resp.contentType()!!.match(ContentType.Application.Xml)
            caps.append("/itspaces/odata/1.0/workspace.svc : rc=${resp.status.value} API=${if (ok) "Поддерживается" else "НеПоддерживается"}\n")

            resp =
                config.client.get("${tenant.tmn}/itspaces/Operations/com.sap.it.op.tmn.commands.dashboard.webui.CapabilityListCommand") {
                    tenant.defaultHeaders(this)
                }
            ok = resp.status == HttpStatusCode.OK && resp.contentType()!!.match(ContentType.Application.Json)
            caps.append("/itspaces/Operations/com.sap.it.op.tmn.commands.dashboard.webui.CapabilityListCommand : rc=${resp.status.value} API=${if (ok) "Поддерживается" else "НеПоддерживается"}\n")
            if (ok) {
                caps.append(resp.readText()).append("\n")
            }

            resp = config.client.get("${tenant.tmn}/itspaces/api/1.0/configurations") {
                tenant.defaultHeaders(this)
            }
            ok = resp.status == HttpStatusCode.OK && resp.contentType()!!.match(ContentType.Application.Json)
            caps.append("/itspaces/api/1.0/configurations : rc=${resp.status.value} API=${if (ok) "Поддерживается" else "НеПоддерживается"}\n")
            if (resp.contentType()!!.match(ContentType.Application.Json)) {
                val ks = k3.OPConfigurationKeyValue.getMap(resp.readText())
                ks.entries.forEach {
                    caps.append("\t${it.key} = ${it.value}\n")
                }
            }
        }
        return caps
    }
}

class CommunicationChannelsPO(val config: Config, val nick: String) {
    val po: Config.PI

    init {
        po = config.pi[nick] ?: error("Милостивый государь мой, не можно отыскать пэошку $nick в конфиге")
    }

    fun getList(): MutableList<CommunicationChannelID> {
        val req = Envelope(CommunicationChannelQueryRequest())
        val xml = xmlSoap.encodeToString(req)
        lateinit var list: MutableList<CommunicationChannelID>
        runBlocking {
            val resp = po.postSOAP(config.client, CommunicationChannelQueryRequest.getUrl(po.host), xml)
            val ccqr: Envelope<CommunicationChannelQueryResponse> = xmlSoap.decodeFromString(resp.readText())
            list = ccqr.data.channels
        }
        return list
    }

    fun readChannels(lst: MutableList<CommunicationChannelID>): List<CommunicationChannel> {
        val req2 = Envelope(
            CommunicationChannelReadRequest("User", lst)
        )
        val xml2 = xmlSoap.encodeToString(req2)
        lateinit var ccs: List<CommunicationChannel>
        runBlocking {
            val resp2 = po.postSOAP(config.client, CommunicationChannelReadRequest.getUrl(po.host), xml2)
            val text2 = resp2.readText()
            val ccrr: Envelope<CommunicationChannelReadResponse> = xmlSoap.decodeFromString(text2)
            ccs = ccrr.data.channels
        }
        return ccs
    }
}

/**
 * Скачивает пакеты и артефакты из среды разработки, выдаёт отчёт
 */
class DownloadWorkspace(config: Config, nick: String) {
    val tenant: Config.Tenant
    lateinit var workspace: Workspace

    init {
        tenant = config.tenants[nick] ?: error("Non enim album: $nick")
    }

    suspend fun exec(client: HttpClient) {
        tenant.loginIfNeeded(client)
        this.workspace = Workspace(tenant)
        this.workspace.retrieve(client)
    }
}

class ServiceEndpointsCPI(val config: Config, val nick: String) {
    val tenant: Config.Tenant

    init {
        tenant = config.tenants[nick] ?: error("Ну куды, куды по помытому и с грязным тенантом: $nick")
    }

    fun extract(): List<ServiceEndpoint> {
        lateinit var lst: List<ServiceEndpoint>
        runBlocking {
            tenant.loginIfNeeded(config.client)
            val resp = tenant.getJson(config.client, ServiceEndpoints.getUrl(tenant.tmn, true))
            val ss = ServiceEndpoints.parse(resp.readText())
            require(ss.d.__next == "", { "//TODO - listing" })
            lst = ss.d.results
        }
        return lst
    }
}
