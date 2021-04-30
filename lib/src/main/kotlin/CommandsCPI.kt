package io.rsug.abyrvalg

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
        var ok: Boolean
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
                val ks = OPConfigurationKeyValue.getMap(resp.readText())
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
    val designArtifacts: MutableList<IntegrationDesigntimeArtifact> = mutableListOf()
    val designVMG: MutableList<ValueMappingDesigntimeArtifact> = mutableListOf()
    val runtimeArtifacts: MutableList<IntegrationRuntimeArtifact> = mutableListOf()

    fun retrieve() {
        runBlocking {
            extract()
        }
    }

    suspend fun extract() {
        // Получить список всех пакетов
        tenant.loginIfNeeded(config.client)
        packages.clear()
        designArtifacts.clear()
        designVMG.clear()
        runtimeArtifacts.clear()

        val resp = tenant.getJson(config.client, IntegrationPackages.getUrl(tenant.tmn))
        val d = IntegrationPackages.parse(resp.readText())
        require(d.d.__next == "", { "Too many packages! Listing is not implemented yet" })
        d.d.results.forEach { pack ->
            // по каждому пакету накачать потоки и VMG
            packages.add(pack)
            val respida = tenant.getJson(config.client, pack.IntegrationDesigntimeArtifacts.getUri())
            val respvm = tenant.getJson(config.client, pack.ValueMappingDesigntimeArtifacts.getUri())

            IntegrationDesigntimeArtifacts.parse(respida.readText(), pack).d.results.forEach { dt ->
                pack.ida.add(dt)
                designArtifacts.add(dt)
            }
            ValueMappingDesigntimeArtifacts.parse(respvm.readText(), pack).d.results.forEach { vm ->
                pack.vmda.add(vm)
                designVMG.add(vm)
            }
        }
        // Получить список рантайма
        val ira = tenant.getJson(config.client, IntegrationRuntimeArtifacts.getUrl(tenant.tmn))
        val e = IntegrationRuntimeArtifacts.parse(ira.readText())
        require(e.d.__next == "", { "Too many packages! Listing is not implemented yet" })
        e.d.results.forEach { rt ->
            runtimeArtifacts.add(rt)
            when (rt.Type) {
                IntegrationArtifactTypeEnum.INTEGRATION_FLOW -> {
                    rt.designtimeArtifact = designArtifacts.find { it.Id == rt.Id }
                }
                IntegrationArtifactTypeEnum.VALUE_MAPPING -> {
                    rt.designtimeVMG = designVMG.find { it.Id == rt.Id }
                }
                else -> TODO("ERROR")
            }
            // для каждого в рантайме ищем пакет, дизайн-тайм и инфу об ошибках
            if (rt.Status != CpiDeployedStatus.STARTED) {
                TODO("Добавить скачивание ошибки")
            }

        }
    }

    suspend fun downloads(ida: IntegrationDesigntimeArtifact): ByteArray {
        require(ida.__metadata.media_src.isNotEmpty())
        val resp: HttpResponse = config.client.get(ida.__metadata.media_src) {
            tenant.defaultHeaders(this)
        }
        require(resp.status == HttpStatusCode.OK)
        require(resp.contentType() != null && resp.contentType()!!.match(ContentType.Application.Zip))
        return resp.readBytes()
    }

    fun download(ida: IntegrationDesigntimeArtifact): ByteArray {
        return runBlocking {
            downloads(ida)
        }
    }
}

