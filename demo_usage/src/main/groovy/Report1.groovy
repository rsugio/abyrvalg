import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil
import io.rsug.abyrvalg.Config
import io.rsug.abyrvalg.ServiceEndpointsCPI
import k3.EntryPoint
import k3.ServiceEndpoint
import k5.CommunicationChannel
import k5.CommunicationChannelDirectionEnum
import k5.CommunicationChannelID

import java.nio.file.Files
import java.nio.file.Paths

class Report__POReceiverToCPIendpoint {
    List<CommunicationChannel> senders = []
    List<CommunicationChannel> receivers = []
    List<Item> items = []
}

class Item {
    CommunicationChannel ccrecv
    String extractedUrl
    ServiceEndpoint serviceEndpoint
    EntryPoint entryPoint
    String remark
}

/**
 * Это отчёт который показывает какой коммуникационный канал ресивер вызывает какой поток в одном из CPI
 * @param config
 * @return
 */
static report1(Config config, Config.PI pi, Config.Tenant cpiru, Config.Tenant cpieu) {
    // Получаем список коммуникационных каналов и отбираем только по компоненте CPI
    CommunicationChannelsPI ccs = new CommunicationChannelsPI(config, pi)
    List<CommunicationChannelID> lst = ccs.getList(), lst2 = []
    lst.each { cc ->
        if (cc.componentID.contains("CPI")) {
            lst2.add(cc)
        }
    }

    // Читаем перечень всех ендпоинтов из российского и европейского CPI
    ServiceEndpointsCPI ru = new ServiceEndpointsCPI(config, cpiru)
    List<ServiceEndpoint> ruse = ru.extract()
    ServiceEndpointsCPI eu = new ServiceEndpointsCPI(config, cpieu)
    List<ServiceEndpoint> euse = eu.extract()

    // Читаем детали по каналам
    List<CommunicationChannel> ccd = ccs.readChannels(lst2)

    Report__POReceiverToCPIendpoint rep = new Report__POReceiverToCPIendpoint()
    // Делаем отчёт

    rep.senders = ccd.findAll { it.direction == CommunicationChannelDirectionEnum.Sender }
    rep.receivers = ccd.findAll { it.direction == CommunicationChannelDirectionEnum.Receiver }
    rep.receivers.each { cd ->
        String url = null
        if (cd.messageProtocol == "REST") {
            url = cd.adapterSpecificAttribute.find { it.name == "URLPattern" }.value
        } else if (cd.messageProtocol in ["SOAP", "XI"])
            url = cd.adapterSpecificAttribute.find { it.name == "XMBWS.TargetURL" }.value
        if (url) {
            Item x = new Item()
            x.ccrecv = cd
            x.extractedUrl = url
            ruse.each { se ->
                def ex = se.entryPoints.results.find { it.Url == url }
                if (ex) {
                    x.serviceEndpoint = se
                    x.entryPoint = ex
                    x.remark = "Российский ЦОД"
                }
            }
            euse.each { se ->
                def ex = se.entryPoints.results.find { it.Url == url }
                if (ex) {
                    x.serviceEndpoint = se
                    x.entryPoint = ex
                    x.remark = "Европейский ЦОД"
                }
            }
            rep.items.add(x)
        }
    }
    // Выводим отчёт в HTML
    StreamingMarkupBuilder mb = new StreamingMarkupBuilder()
    Writable rez = mb.bind {
        html {
            'head' {
                'title'("Отчёт1 - сбойка канал-поток между PO и CPI")
            }
            'body' {
                'h1'('1)Сбойка канал-поток между PO и CPI')
                'h2'("Сендеры на стороне PO")
                'p'("Для каналов-сендеров пока не получается определить вызывающий их поток в CPI," +
                        "так как надо парсить IFLW, учитывать конфигурируемые параметры. Но это //TODO!")
                'ul' {
                    rep.senders.each { x ->
                        'li'("${x.channel.partyID}|${x.channel.componentID}|${x.channel.channelID}")
                    }
                }

                'h2'("Вызовы из PO в CPI")
                'table'(border: 1) {
                    'tr' {
                        'td'("Канал")
                        'td'('URL')
                        'td'('Поток в CPI')
                        'td'('Комментарий')
                    }
                    rep.items.each { x ->
                        'tr' {
                            'td'("${x.ccrecv.channel.partyID}|${x.ccrecv.channel.componentID}|${x.ccrecv.channel.channelID}")
                            'td'(x.extractedUrl)
                            'td'(x.entryPoint?.name ?: "Поток не найден")
                            'td'(x.remark)
                        }
                    }
                }
            }
        }
    }
    Writer w = Files.newBufferedWriter(Paths.get("report1.html"))
    w.write(XmlUtil.serialize(rez))
    w.close()
}
