//@file:Suppress("unused")

import io.ktor.client.statement.*
import io.ktor.http.*
import io.rsug.abyrvalg.Config
import k1.NotSoComplexQuery
import k5.*
import kotlinx.coroutines.runBlocking
import java.util.*

class CommunicationChannelsPI(val config: Config, val pi: Config.PI) {
    fun getList() = runBlocking { getListSuspend() }

    suspend fun getListSuspend(): List<CommunicationChannelID> {
        val xml = CommunicationChannelQueryRequest().composeSOAP()
        val resp = pi.postSOAP(config.client, CommunicationChannelQueryRequest.getUrl(pi.host), xml)
        val ccqr = CommunicationChannelQueryResponse.parse(resp.readText())
        return ccqr.channels
    }

    fun readChannels(lst: List<CommunicationChannelID>) = runBlocking { readChannelsSuspend(lst) }

    suspend fun readChannelsSuspend(lst: List<CommunicationChannelID>): List<CommunicationChannel> {
        val xml = CommunicationChannelReadRequest("User", lst).composeSOAP()
        val resp = pi.postSOAP(config.client, CommunicationChannelReadRequest.getUrl(pi.host), xml).readText()
        return CommunicationChannelReadResponse.parse(resp).channels
    }
}


class ICOWhereUsedList(
    val channels: MutableMap<CommunicationChannelID, MutableList<IntegratedConfigurationID>> = mutableMapOf(),
    val icos: MutableList<IcoInfo> = mutableListOf(),
) {
    class IcoInfo(
        val ico: IntegratedConfigurationID,
        val sender: CommunicationChannelID,
        val receivers: MutableList<CommunicationChannelID> = mutableListOf(),
        val lookups: MutableList<CommunicationChannelID> = mutableListOf(),
    )

    class ChannelInfo(
        val channel: CommunicationChannelID,
        val usagesICo: MutableList<IntegratedConfigurationID> = mutableListOf(),
    )
}

class IntegratedConfigurationsPI(val config: Config, val pi: Config.PI, val use750: Boolean = true) {
    // размеры чанков какими нарезать
    private val portionCc = 200
    private val portionIco = 200

    fun getList() = runBlocking { getListSuspend() }

    suspend fun getListSuspend(): List<IntegratedConfigurationID> {
        val xml = IntegratedConfigurationQueryRequest().composeSOAP()

        val url = if (use750)
            IntegratedConfigurationQueryRequest.getUrl750(pi.host)
        else
            IntegratedConfigurationQueryRequest.getUrl(pi.host)

        val resp = pi.postSOAP(config.client, url, xml)
        val xs = IntegratedConfigurationQueryResponse.parse(resp.readText())
        return xs.IntegratedConfigurationID
    }

    fun readICos(lst: List<IntegratedConfigurationID>) = runBlocking {
        parseIcoSoap(readICosSuspendString(lst))
    }

    suspend fun readICosSuspendString(lst: List<IntegratedConfigurationID>): String {
        val xml = IntegratedConfigurationReadRequest("User", lst).composeSOAP()
        val url = if (use750)
            IntegratedConfigurationReadRequest.getUrl750(pi.host)
        else
            IntegratedConfigurationReadRequest.getUrl(pi.host)

        return pi.postSOAP(config.client, url, xml).readText()
    }

    fun parseIcoSoap(payloadXmlSoap: String) =
        if (use750) {
            IntegratedConfiguration750ReadResponse.parse(payloadXmlSoap).IntegratedConfiguration
        } else {
            IntegratedConfigurationReadResponse.parse(payloadXmlSoap).IntegratedConfiguration
        }

    fun whereUsedChannels(
        channels: List<CommunicationChannelID>,
        icos: List<IntegratedConfigurationID>,
    ) = runBlocking { whereUsedChannelsSuspend(channels, icos) }

    suspend fun whereUsedChannelsSuspend(
        channels: List<CommunicationChannelID>,
        icos: List<IntegratedConfigurationID>,
    ): ICOWhereUsedList {

        val wu = ICOWhereUsedList()
        icos.chunked(portionIco).forEach { icoSmall ->
            parseIcoSoap(readICosSuspendString(icoSmall)).forEach { ico ->
                val ccSender = ico.InboundProcessing.CommunicationChannel
                val ccReceivers = mutableListOf<CommunicationChannelID>()
                ico.OutboundProcessing.forEach { op ->
                    ccReceivers.add(op.CommunicationChannel)
                }
                val icoInfo = ICOWhereUsedList.IcoInfo(ico.IntegratedConfigurationID, ccSender, ccReceivers)
                wu.icos.add(icoInfo)
                ccReceivers.forEach {
                    if (wu.channels.containsKey(it)) {
                        wu.channels[it]!!.add(ico.IntegratedConfigurationID)
                    } else {
                        wu.channels[it] = mutableListOf(ico.IntegratedConfigurationID)
                    }
                }
                if (wu.channels[ccSender] == null) {
                    wu.channels[ccSender] = mutableListOf(ico.IntegratedConfigurationID)
                } else {
                    wu.channels[ccSender]!!.add(ico.IntegratedConfigurationID)
                }
            }
        }
        return wu
//    val lst = mutableListOf<IntegratedConfigurationID>()
//    val rez = mutableListOf<IntegratedConfiguration>()
//    icos.forEach
//    {
//        channels.forEach {
//            if (!wu.channels.containsKey(it)) {
//                wu.channels[it] = mutableListOf()
//            }
//        }
//        return wu
//    }
    }
}

class SimpleQueryPI(val config: Config, val pi: Config.PI) {
//    fun getList() = runBlocking { getListSuspend() }

    suspend fun getList(entity: String): List<Map<String, String>> {
        val req = NotSoComplexQuery.repQuery(entity)
        val ct = ContentType.parse(NotSoComplexQuery.getContentType())
        val url = NotSoComplexQuery.getUrlRep(pi.host)

        val resp = pi.post(config.client, url, req, ct)
        val n = NotSoComplexQuery(Scanner(resp.readText()))
        return n.lines
    }
}
