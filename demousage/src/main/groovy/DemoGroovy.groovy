import io.rsug.abyrvalg.Config
import io.rsug.abyrvalg.ConfigKt

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class DemoGroovy {
    static void main(String[] args) throws Exception {
//        args = new String[]{"tmp/abyrvalg.conf"}
        // здесь только чтение конфига из файла
        // пример
        if (args.length < 1) {
            System.err.println("""Start: DemoGroovy.bat abyrvalg.conf""")
            System.exit(-1)
            return
        }
        Path cfgpath = Paths.get(args[0])
        Config config
        if (Files.isRegularFile(cfgpath)) {
            String text = Files.newBufferedReader(cfgpath).text
            config = ConfigKt.parseHoconFromString(text)
        } else {
            System.err.println("Error: Unable to get config: ${cfgpath.toAbsolutePath()}")
            System.exit(-1)
            return
        }
        config.createClient()
        config.init()
    }
}
