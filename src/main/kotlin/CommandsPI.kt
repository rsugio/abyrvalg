//@file:Suppress("unused")

import io.ktor.client.statement.*
import io.rsug.abyrvalg.Config
import io.rsug.abyrvalg.xmlSoap
import k5.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class CommunicationChannelsPI(val config: Config, val pi: Config.PI) {
    fun getList() = runBlocking { getListSuspend() }

    suspend fun getListSuspend(): List<CommunicationChannelID> {
        val req = Envelope(CommunicationChannelQueryRequest())
        val xml = xmlSoap.encodeToString(req)
        val resp = pi.postSOAP(config.client, CommunicationChannelQueryRequest.getUrl(pi.host), xml)
        val ccqr: Envelope<CommunicationChannelQueryResponse> = xmlSoap.decodeFromString(resp.readText())
        return ccqr.data.channels
    }

    fun readChannels(lst: List<CommunicationChannelID>) = runBlocking { readChannelsSuspend(lst) }

    suspend fun readChannelsSuspend(lst: List<CommunicationChannelID>): List<CommunicationChannel> {
        val req2 = Envelope(
            CommunicationChannelReadRequest("User", lst)
        )
        val xml2 = xmlSoap.encodeToString(req2)
        val resp2 = pi.postSOAP(config.client, CommunicationChannelReadRequest.getUrl(pi.host), xml2)
        val text2 = resp2.readText()
        val ccrr: Envelope<CommunicationChannelReadResponse> = xmlSoap.decodeFromString(text2)
        return ccrr.data.channels
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
        val req = Envelope(IntegratedConfigurationQueryRequest())
        val xml = xmlSoap.encodeToString(req)
        lateinit var list: MutableList<IntegratedConfigurationID>

        val url = if (use750)
            IntegratedConfigurationQueryRequest.getUrl750(pi.host)
        else
            IntegratedConfigurationQueryRequest.getUrl(pi.host)

        val resp = pi.postSOAP(config.client, url, xml)
        val xs: Envelope<IntegratedConfigurationQueryResponse> = xmlSoap.decodeFromString(resp.readText())
        return xs.data.IntegratedConfigurationID
    }

    fun readICos(lst: List<IntegratedConfigurationID>) = runBlocking {
        parseIcoSoap(readICosSuspendString(lst))
    }

    suspend fun readICosSuspendString(lst: List<IntegratedConfigurationID>): String {
        val req2 = Envelope(
            IntegratedConfigurationReadRequest("User", lst)
        )
        val xml2 = xmlSoap.encodeToString(req2)
        val url = if (use750)
            IntegratedConfigurationReadRequest.getUrl750(pi.host)
        else
            IntegratedConfigurationReadRequest.getUrl(pi.host)

        return pi.postSOAP(config.client, url, xml2).readText()
    }

    fun parseIcoSoap(payloadXmlSoap: String) =
        if (use750) {
            val xs: Envelope<IntegratedConfiguration750ReadResponse> = xmlSoap.decodeFromString(payloadXmlSoap)
            xs.data.IntegratedConfiguration
        } else {
            val xs: Envelope<IntegratedConfigurationReadResponse> = xmlSoap.decodeFromString(payloadXmlSoap)
            xs.data.IntegratedConfiguration
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
