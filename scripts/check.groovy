import Config

assert config != null && config instanceof Config
'PO CPI demonstration'(config)

/**
 * Проверка входа в системы
 * @param config
 * @return
 */
static 'PO CPI demonstration'(Config config) {
    config.tenants.each {nick, tenant ->
        println ("$nick это $tenant")
//        println(new CheckCapabilitiesCPI(config, nick).exec())
    }
    config.пэошки.each {nick, po ->
        println ("$nick это $po")

    }
}
//    DownloadWorkspace dw = new DownloadWorkspace(config, "rutest", Paths.get("."))
//    host.execute(dw)
//    DownloadWorkspace deu = new DownloadWorkspace(config, "eutest", Paths.get("."))
//    if (deu!=null) {
//        host.execute(deu)
//    }
//    dw.workspace.packages.each {pack ->
//        println(pack.Id)
//        pack.ida.each {IntegrationDesigntimeArtifact ida ->
//            println("\tIDA = ${ida.name}, ${ida.version}")
//        }
//        pack.vmda.each { ValueMappingDesigntimeArtifact vmda ->
//            println("\tVM = ${vmda.name}")
//        }
//    }
//}
//
//
//
//static String dumpOut(clz) {
//    def log = "\n\nclass ${clz.class.canonicalName} " << ""
//    log << "\n\t\tannotatedInterfaces=${clz.getClass().annotatedInterfaces}"
//    log << "\n\t\tannotatedSuperclass=${clz.getClass().annotatedSuperclass}"
//    log << "\n"
//    log << "{\n\t//properties:\n"
//    clz.metaClass.properties.each { prop ->
//        String mod = java.lang.reflect.Modifier.toString(prop.modifiers)
//        log << "\t$mod ${prop.type.name} ${prop.name};\n"
//    }
//    log << "\n\t//methods:\n"
//    clz.metaClass.methods.each { method ->
//        String mod = java.lang.reflect.Modifier.toString(method.modifiers)
//        log << "\t$mod ${method.returnType.name} ${method.name}( ${method.parameterTypes*.name.join(', ')} );\n"
//    }
//    log << "}\n"
//    return log
//}