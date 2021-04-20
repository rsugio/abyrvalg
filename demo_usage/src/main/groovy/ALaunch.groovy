import io.rsug.abyrvalg.Config

import java.nio.file.Path

class ALaunch {
    static void abyrvalg(Config cfg) {
        println("ALaunch.start with $cfg")
//        Report1.report1(cfg, "pitest", "rutest", "eutest")
//        Report3.report(cfg, cfg.pi["qpi"])
//        Report4.report(cfg, cfg.tenants.get("rutest"), cfg.tenants.get("ruprod"))
    }

    static void main(String[] args) {
        String cwd = Path.of("").toAbsolutePath().toString()
        println("ALaunch.main with current directory: $cwd")
        ApplicationKt.main("tmp/abyrvalg.conf", "demo_usage/src/main/groovy/ALaunch.groovy")
    }
}
