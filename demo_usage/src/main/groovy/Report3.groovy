import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil
import io.rsug.abyrvalg.Config
import k5.CommunicationChannelID
import k5.IntegratedConfigurationID

import java.nio.file.Files
import java.nio.file.Paths

/**
 * Это отчёт по икошкам и каналам
 * @param config
 * @return
 */
static report(Config config, Config.PI pi) {
    CommunicationChannelsPI ccpi = new CommunicationChannelsPI(config, pi)
    List<CommunicationChannelID> cclist = ccpi.getList()
    IntegratedConfigurationsPI ico = new IntegratedConfigurationsPI(config, pi)
    List<IntegratedConfigurationID> icolist = ico.getList()
    ICOWhereUsedList wu = ico.whereUsedChannels(cclist, icolist)
    // Выводим отчёт в HTML
    StreamingMarkupBuilder mb = new StreamingMarkupBuilder()
    Writable rez = mb.bind {
        html {
            'head' {
                'title'("Отчёт3")
                style(type: "text/css", """.unused{color:red}
tr.unused td{background-color: yellow}""")
                
            }
            'body' {
                'h1'("Отчёт3 $pi.sid")
                'h2'("Список каналов")
                'table'(border: 1) {
                    'tr' {
                        'td'("Канал")
                        'td'('Икошки')
                    }
                    wu.channels.each { k, v ->
                        if (v.empty) {
                            'tr'(class: "unused") {
                                'td'("$k.partyID|$k.componentID|$k.channelID")
                                'td'("")
                            }
                        } else v.eachWithIndex { IntegratedConfigurationID d, Integer ix ->
                            'tr' {
                                if (v.size() > 1 && ix == 0)
                                    'td'(rowspan: v.size(), "$k.partyID|$k.componentID|$k.channelID")
                                else if (v.size() == 1) {
                                    'td'("$k.partyID|$k.componentID|$k.channelID")
                                }
                                'td'("$d.senderPartyID|$d.senderComponentID|{$d.interfaceNamespace}$d.interfaceName|$d.receiverPartyID|$d.receiverComponentID")
                            }
                        }
                    }
                } // список каналов
                'h2'("Список икошек")
                'table'(border: 1) {
                    'tr' {
                        'td'("Икошка")
                        'td'('Сендер')
                        'td'('Ресиверы')
                    }
                    wu.icos.each { info ->
                        IntegratedConfigurationID d = info.ico
                        def k = info.sender
                        List<String> var = []
                        info.receivers.eachWithIndex { CommunicationChannelID r, int i ->
                            var.add("$r.partyID|$r.componentID|$r.channelID")
                        }
                        var.eachWithIndex { String entry, int ix ->
                            'tr' {
                                if (var.size() > 1 && ix == 0) {
                                    'td'(rowspan: var.size(), "$d.senderPartyID|$d.senderComponentID|{$d.interfaceNamespace}$d.interfaceName|$d.receiverPartyID|$d.receiverComponentID")
                                    'td'(rowspan: var.size(), "$k.partyID|$k.componentID|$k.channelID")
                                } else if (var.size() == 1) {
                                    'td'("$d.senderPartyID|$d.senderComponentID|{$d.interfaceNamespace}$d.interfaceName|$d.receiverPartyID|$d.receiverComponentID")
                                    'td'("$k.partyID|$k.componentID|$k.channelID")
                                }
                                'td'(entry)
                            }
                        }
                    }
                } // ICO table
            }
        }
    }
    Writer w = Files.newBufferedWriter(Paths.get("report3.html"))
    w.write(XmlUtil.serialize(rez))
    w.close()
}
