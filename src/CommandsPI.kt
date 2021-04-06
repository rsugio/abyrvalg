import io.ktor.client.statement.*
import io.rsug.abyrvalg.Config
import io.rsug.abyrvalg.xmlSoap
import k5.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class CommunicationChannelsPI(val config: Config, val pi: Config.PI) {
    fun getList(): MutableList<CommunicationChannelID> {
        val req = Envelope(CommunicationChannelQueryRequest())
        val xml = xmlSoap.encodeToString(req)
        lateinit var list: MutableList<CommunicationChannelID>
        runBlocking {
            val resp = pi.postSOAP(config.client, CommunicationChannelQueryRequest.getUrl(pi.host), xml)
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
            val resp2 = pi.postSOAP(config.client, CommunicationChannelReadRequest.getUrl(pi.host), xml2)
            val text2 = resp2.readText()
            val ccrr: Envelope<CommunicationChannelReadResponse> = xmlSoap.decodeFromString(text2)
            ccs = ccrr.data.channels
        }
        return ccs
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
        val lookups: MutableList<CommunicationChannelID> = mutableListOf()
    )

    class ChannelInfo(
        val channel: CommunicationChannelID,
        val usagesICo: MutableList<IntegratedConfigurationID> = mutableListOf()
    )
}

class IntegratedConfigurationsPI(val config: Config, val pi: Config.PI) {
    fun getList(): MutableList<IntegratedConfigurationID> {
        val req = Envelope(IntegratedConfigurationQueryRequest())
        val xml = xmlSoap.encodeToString(req)
        lateinit var list: MutableList<IntegratedConfigurationID>
        runBlocking {
            val resp = pi.postSOAP(config.client, IntegratedConfigurationQueryRequest.getUrl750(pi.host), xml)
            val xs: Envelope<IntegratedConfigurationQueryResponse> = xmlSoap.decodeFromString(resp.readText())
            list = xs.data.IntegratedConfigurationID
        }
        return list
    }

    fun readICos(lst: MutableList<IntegratedConfigurationID>): MutableList<IntegratedConfiguration> {
        val req2 = Envelope(
            IntegratedConfigurationReadRequest("User", lst)
        )
        val xml2 = xmlSoap.encodeToString(req2)
        lateinit var ccs: MutableList<IntegratedConfiguration>
        runBlocking {
            val resp2 = pi.postSOAP(config.client, IntegratedConfigurationReadRequest.getUrl750(pi.host), xml2)
            val text2 = resp2.readText()
            val xs: Envelope<IntegratedConfiguration750ReadResponse> = xmlSoap.decodeFromString(text2)
            ccs = xs.data.IntegratedConfiguration
        }
        return ccs
    }

    fun whereUsedChannels(
        channels: MutableList<CommunicationChannelID>,
        icos: MutableList<IntegratedConfigurationID>
    ): ICOWhereUsedList {
        val portion: Int = 200
        val wu = ICOWhereUsedList()

        val lst = mutableListOf<IntegratedConfigurationID>()
        val rez = mutableListOf<IntegratedConfiguration>()
        icos.forEach {
            if (lst.size == portion) {
                readICos(lst).forEach { ico ->
                    whereUsed(ico, wu)
                }
                rez.addAll(this.readICos(lst))
                lst.clear()
            }
            lst.add(it)
        }
        readICos(lst).forEach { ico ->
            whereUsed(ico, wu)
        }
        channels.forEach{
            if (!wu.channels.containsKey(it)) {
                wu.channels[it] = mutableListOf()
            }
        }
        return wu
    }

    private fun whereUsed(
        ico: IntegratedConfiguration,
        wu: ICOWhereUsedList
    ): ICOWhereUsedList.IcoInfo {
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
        return icoInfo
    }
}
