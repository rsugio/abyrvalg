import groovy.util.GroovyScriptEngine
import groovy.util.ScriptException
import io.ktor.util.*
import io.rsug.abyrvalg.Config
import kotlinx.serialization.ExperimentalSerializationApi
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

@ExperimentalSerializationApi
@KtorExperimentalAPI
fun main(args: Array<String>) {
    if (args.size < 2) {
        System.err.println("""Запуск: АБЫРВАЛГ конфиг.кфг [может_быть_папка/]кмд.груви""")
        exitProcess(-1)
    }
    val cfgpath = Paths.get(args[0])
    val config = if (Files.isRegularFile(cfgpath)) {
        val text = Files.newBufferedReader(cfgpath).readText()
        Config.parseHocon(text)
    } else {
        System.err.println("ЕГГОГ: Не могу найти конфиг: $cfgpath")
        throw NoSuchFileException(cfgpath.toFile())
    }

    val pwd = Paths.get(".").toRealPath().toUri().toURL()
    val scrpath = Paths.get(args[1])
    if (!Files.isRegularFile(scrpath)) {
        System.err.println("ЕГГОГ: Не могу найти скрипт: $scrpath")
        throw NoSuchFileException(scrpath.toFile())
    }
    config.createClient()
    config.init()
    val engine = GroovyScriptEngine(arrayOf(pwd))
    try {
        val clazz = engine.loadScriptByName(scrpath.toString())
        val meth = clazz.getDeclaredMethod("abyrvalg", config.javaClass)
        meth.invoke(clazz, config)
    } catch (e: ScriptException) {
        // как только возникнет, понять как выдавать причину
        e.printStackTrace()
        System.err.println(e)
        exitProcess(-1)
    } catch (e: NoSuchMethodException) {
        System.err.println(e)
        System.err.println("Возможная причина - должен быть объявлен: static void abyrvalg(Config cfg)")
        exitProcess(-1)
    } catch (e: IllegalArgumentException) {
        System.err.println(e)
        System.err.println("Возможная причина - метод void abyrvalg(Config) объявлен не как статический")
        exitProcess(-1)
    } catch (e: java.lang.reflect.InvocationTargetException) {
        e.printStackTrace()
        exitProcess(-1)
    } finally {
        config.client.close()
    }
    when (java.time.Instant.now().epochSecond % 5) {
        0L -> println("\n\nВердикт: Абырвалг доволен, приходите завтра")
        1L -> println("\n\nО-о-о!... Етит твою мать, профессор!!!")
        3L -> println("\n\nОкончательная бумажка. Фактическая! Настоящая!! Броня!!!")
        4L -> println("\n\nДай папиросочку, у тебя брюки в полосочку!")
    }
    exitProcess(0)
}
/**
Вариант с передачей конфига в байндинге:
val binding = Binding()
binding.setVariable("config", config)
val dsl = engine.createScript(args[1], binding)
dsl.run()
 */


/**
fun parseD(x: String): Long {
assert(x != "" && x.startsWith("/Date(") && x.endsWith(")/"))
val millis = x.substring(6, x.length - 2).toLong()
return millis
}

val mskTZ = ZoneId.of("Europe/Moscow")
val msk = Clock.system(mskTZ)
val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssz").withZone(mskTZ)
val stx = Regex("&%24skiptoken=\\d+")
 */


//    val h1: HttpResponse = client.head("$tmn/api/v1/")
//    token = h1.headers.get("X-CSRF-Token") ?: ""
//    var mpl: MessageProcessingLogs = client.get(
//        MessageProcessingLogs.getUrl(tmn)
//                + "?\$filter=IntegrationFlowName eq 'Replicate_B2B_Order_From_S4HANA_To_SAP_Commerce_Cloud' and LogEnd gt datetime'2021-02-01T00:00:00' and LogEnd lt datetime'2021-03-31T00:00:00'"
////            "&\$expand=CustomHeaderProperties,AdapterAttributes"
//    )
//    var __next = mpl.d.__next
//    mpl.d.results.forEach {
//    }
//    while (__next != "") {
//        __next = "$tmn/api/v1/$__next"
//        println(__next)
//        mpl = client.get(__next)
//        __next = stx.replace(mpl.d.__next, "")
//    }
//
//    client.close()
//
//
//    val webflows: List<WebIFlow> = client.get(WebIFlow.getUrl(tmn))
//    webflows.forEach {
//        println("${it.id} - ${it.type} - ${it.bundleName}")
//        if (it.type == IntegrationArtifactTypeEnum.INTEGRATION_FLOW) {
//            val webflow: WebIFlowSingle = client.get(WebIFlowSingle.getUrl(tmn, it.id))
//        }
//    }
//
//    val d1: MessageProcessingLogSingle = client.get(MessageProcessingLogSingle.getUrl(tmn, messageGuid, true))
//    val d2: MPLAttachments = client.get(MPLAttachments.getUrl(tmn, messageGuid))
//    println(d1.d.AdapterAttributes.results)
//    println(d1.d.CustomHeaderProperties.results)
//    d2.d.results.forEach { a3 ->
////        println("${a3.Name} size=${a3.PayloadSize} ${a3.TimeStamp} ${a3.ContentType}")
//    }
//
//    val z: MplDetailCommand = client.get(MplDetailCommand.getUrl(tmn, messageGuid))
////    println(z.messageGuid)
////    println(z.mplData)
////    println(z.lastError)
//    z.mplAttachments.forEach { a2 ->
//        val a2c: HttpResponse = client.get(a2.getUrl(tmn))
//        val a2a = a2c.readBytes()
////        println("${a2.attachmentName} size=${a2.payloadSize} downloaded=${a2a.size} ${a2.timestamp} ${a2.contentType}")
//    }
//
//    if (testMany) {
//        val x1: ServiceEndpoints = client.get(ServiceEndpoints.getUrl(tmn, true))
//        x1.d.results.forEach {
//            println(it.Name)
//            it.EntryPoints.results.forEach { ep ->
//                println("\t${ep.Url}")
//            }
//            it.ApiDefinitions.results.forEach { ad ->
//                println("\t\t${ad.Name} = ${ad.Url}")
//            }
//        }
////    val iras: IntegrationRuntimeArtifacts = client.get(IntegrationRuntimeArtifacts.getUrl(tmn))
////    val x21: LogFiles = client.get(LogFiles.getUrl(tmn))
////    val x2: LogFileArchives = client.get(LogFileArchives.getUrl(tmn))
////    val x3: IntegrationPackages = client.get(IntegrationPackages.getUrl(tmn))
////    val x4: ValueMappingDesigntimeArtifacts = client.get(ValueMappingDesigntimeArtifacts.getUrl(tmn))
////    val x5: UserCredentials = client.get(UserCredentials.getUrl(tmn))
////    val mpl: MessageProcessingLogs = client.get(MessageProcessingLogs.getUrl(tmn))
////    val mse: MessageStoreEntries = client.get(MessageStoreEntries.getUrl(tmn, "AGBVsuUVg4t6AL3duuy39rKvkl0w"))
//        val getNodesCommand: OPGetNodesCommand = client.get(OPGetNodesCommand.getUrl(tmn))
//        getNodesCommand.nodes.forEach {
//        }
//
//        val packages: IntegrationPackages = client.get(IntegrationPackages.getUrl(tmn))
//        val ira: IntegrationRuntimeArtifacts = client.get(IntegrationRuntimeArtifacts.getUrl(tmn))
//        ira.d.results.forEach { ra ->
////        val packcontent: HttpResponse = client.get(ra.__metadata.media_src)
////        println("${ra.Id} = ${packcontent.readBytes().size}")
//        }
//
//        val cfg: List<OPConfigurationKeyValue> = client.get(OPConfigurationKeyValue.getUrl(tmn))
//        val tenant = mutableMapOf<String, String>()
//        cfg.filter { it.key in arrayOf("tenantId", "tenantName", "buildNumber") }
//            .forEach {
//                require(it.value is JsonPrimitive)
////                tenant.put(it.key, it.value?.content ?: "")
//            }
//        println(tenant)
//
//        val iclc: OPIntegrationComponentsListCommand = client.get(OPIntegrationComponentsListCommand.getUrl(tmn))
//        iclc.artifactInformations.forEach {
//            //println("${it.id}")
//        }
//
////    val dl: OPDownloadContentCommand = client.get("$tmn/itspaces/Operations/com.sap.it.nm.commands.deploy.DownloadContentCommand?artifactIds=6ef77148-d236-41a3-b9ea-e6f703e7f774&tenantId=j68386e77")
////    Paths.get("tmp.zip").writeBytes(dl.artifacts[0].content)
//
//        packages.d.results.forEach { pack ->
//            if (pack.Id.startsWith("!NLMK")) {
//                val ida: IntegrationDesigntimeArtifacts = client.get(pack.IntegrationDesigntimeArtifacts.getUri())
//                val packcontent: HttpResponse = client.get(pack.__metadata.media_src) {
//                    expectSuccess = false
//                }
//                if (packcontent.status == HttpStatusCode.OK) {
//                    val packzip = packcontent.readBytes()
//                    println("* ${pack.Id} ${packzip.size}*")
//                } else {
//                    println("* ${pack.Id} error=${packcontent.status.value}")
//                }
//
//                ida.d.results.forEach { flow ->
//                    require(flow.__metadata.media_src != null)
////            val flowcontent: HttpResponse = client.get(flow.__metadata.media_src)
////            val zip = flowcontent.readBytes()
////            println("\t${flow.Id} = ${zip.size}")
////            val conf: Configurations = client.get(flow.Configurations.getUri())
////            val rscs: Resources = client.get(flow.Resources.getUri())
//                }
//            }
//        }
//    }

/**
var g2: ODataMPL = client.get(u)
var next = g2.d.getOrDefault("__next", "") as String
//    println(next)
var results = g2.d["results"] as List<LinkedTreeMap<String, Any>>?
d2(results, client, attnames)
while (next != "") {
g2 = client.get() {
url("$uri/$next")
}
results = g2.d["results"] as List<LinkedTreeMap<String, Any>>?
d2(results, client, attnames)
next = g2.d.getOrDefault("__next", "") as String
next = stx.replace(next, "")
//        println(results?.size)
//        println(next)
}

suspend fun d1(results: List<LinkedTreeMap<String, Any>>?, client: HttpClient, names: List<String>): Unit {
println(results?.size)
results?.forEach { x: LinkedTreeMap<String, Any> ->
val MessageGuid = x["MessageGuid"] as String
val attUrl = ((x["Attachments"] as LinkedTreeMap<*, *>)
["__deferred"] as LinkedTreeMap<*, *>)["uri"] as String
println("$MessageGuid = $attUrl")
val rpx = rp.resolve(MessageGuid)
if (!Files.isDirectory(rpx)) Files.createDirectory(rpx)
val rsp: ODataMPL = client.get(attUrl)
var attr = rsp.d["results"] as List<LinkedTreeMap<String, Any>>?
attr?.forEach { y: LinkedTreeMap<String, Any> ->
val Name = y["Name"] as String
val Id = y["Id"] as String
val media_src = (y["__metadata"] as LinkedTreeMap<*, *>)["media_src"] as String
println("$Id - $Name - $media_src")
if (names.contains(Name)) {
val attach: HttpResponse = client.get(media_src)
val nx = rpx.resolve(Name)
val wc = nx.toFile().writeChannel()
wc.writeFully(attach.readBytes())
wc.close()
}
}
}
}

suspend fun d2(results: List<LinkedTreeMap<String, Any>>?, client: HttpClient, names: List<String>): Unit {
//    println(results?.size)
results?.forEach { x: LinkedTreeMap<String, Any> ->
val MessageGuid = x["MessageGuid"] as String
val ApplicationMessageId = x["ApplicationMessageId"] as String
val Status = x["Status"] as String
val LogStart = x["LogStart"] as String
val sdt = dtf.format(Instant.ofEpochMilli(parseD(LogStart)))

val errUrl =
((x["ErrorInformation"] as LinkedTreeMap<*, *>)["__deferred"] as LinkedTreeMap<*, *>)["uri"] as String
println("$MessageGuid,$ApplicationMessageId,$Status,$sdt")
}
}
 **/
