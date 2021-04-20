import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil
import io.rsug.abyrvalg.Config
import io.rsug.abyrvalg.WorkspaceCPI
import k3.IntegrationDesigntimeArtifact
import k3.IntegrationPackage

import java.nio.file.Files
import java.nio.file.Paths

/**
 * Сравнение двух CPI Workspace
 * @param config
 * @return
 */
static report(Config config, Config.Tenant cpileft, Config.Tenant cpiright) {
    WorkspaceCPI left = new WorkspaceCPI(config, cpileft)
    left.retrieve()
    WorkspaceCPI right = new WorkspaceCPI(config, cpiright)
    right.retrieve()

    // Выводим отчёт в HTML
    StreamingMarkupBuilder mb = new StreamingMarkupBuilder()
    Writable rez = mb.bind {
        html {
            'head' {
                'title'("Отчёт4")
            }
            'body' {
                'h1'('Отчёт4')
                'h2'("Workspaces")
                'table'(border: 1) {
                    'tr' {
                        'td'("Артефакт")
                        'td'(cpileft.nick)
                        'td'(cpiright.nick)
                        'td'('Комментарий')
                    }
                    left.packages.findAll { it.name != "delme" }
                            .forEach { pack ->
                                IntegrationPackage rpack = right.packages.find { it.name == pack.name }
                                'tr' {
                                    'td'("Пакет: $pack.name", colspan: 4, color: "green")
                                }
                                pack.ida.each { ida ->
                                    IntegrationDesigntimeArtifact rida = rpack?.ida.find { it.name == ida.name }
                                    String verdict = ""
                                    if (ida.version == "Active") {
                                        verdict = "Слева черновик"
                                    } else if (rida == null) {
                                        verdict = "Потока справа нет"
                                    } else if (ida.version != rida.version)
                                        verdict = "Разница версий"
                                    if (verdict) {
                                        'tr' {
                                            'td'(ida.name)
                                            'td'(ida.version)
                                            td(rida?.version ?: "")
                                            td(verdict)
                                        }
                                    }

                                }
                            }
                }
            }
        }
    }
    Writer w = Files.newBufferedWriter(Paths.get("report4.html"))
    w.write(XmlUtil.serialize(rez))
    w.close()
}

