package io.rsug.abyrvalg

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import k3.*
import kotlinx.coroutines.runBlocking

/**
 * Проверка, в какие АПИ удаётся тыкнутся а в какие нет
 */
class CheckCapabilitiesCPI(val config: Config, val tenant: Config.Tenant) {
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

/**
 * Вытаскиваем ендпоинты, кроме processDirect и http -- только там где они в сервисе есть
 */
class ServiceEndpointsCPI(val config: Config, val tenant: Config.Tenant) {
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

/**
 * Скачиваем пакеты и вложенные артефакты через /api/v1.0
 */
class WorkspaceCPI(val config: Config, val tenant: Config.Tenant) {
    val packages: MutableList<IntegrationPackage> = mutableListOf()

    fun retrieve() {
        // 1. Получить список всех пакетов
        runBlocking {
            tenant.loginIfNeeded(config.client)
            val resp = tenant.getJson(config.client, IntegrationPackages.getUrl(tenant.tmn))
            val d = IntegrationPackages.parse(resp.readText())
            require(d.d.__next == "", { "Too many packages! Listing is not implemented yet" })
            d.d.results.forEach { pack ->
                packages.add(pack)
                val respida = tenant.getJson(config.client, pack.IntegrationDesigntimeArtifacts.getUri())
                val respvm = tenant.getJson(config.client, pack.ValueMappingDesigntimeArtifacts.getUri())

                IntegrationDesigntimeArtifacts.parse(respida.readText()).d.results.forEach { it ->
                    pack.ida.add(it)
                }
                ValueMappingDesigntimeArtifacts.parse(respvm.readText()).d.results.forEach { it ->
                    pack.vmda.add(it)
                }
            }
        }
        return
    }

    fun download(ida: IntegrationDesigntimeArtifact):ByteArray? {
        return null
    }
}

