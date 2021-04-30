import io.rsug.abyrvalg.AuthEnum;
import io.rsug.abyrvalg.Config;
import io.rsug.abyrvalg.ConfigKt;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DemoJava {
    public static void main(String[] args) throws Exception {
        Config config;
        if (args.length > 0) {
            // Чтение конфига из файла
            Path cfgpath = Paths.get(args[0]);
            if (Files.isRegularFile(cfgpath)) {
                config = ConfigKt.parseHoconFromPath(cfgpath);
            } else {
                System.err.println("Error: Unable to get config: " + cfgpath.toAbsolutePath());
                System.exit(-1);
                return;
            }
        } else {
            // Построение конфига в коде, пример
            config = new Config();
            Config.Login login = new Config.Login(AuthEnum.Basic, "piuser", "initinit123");
            Config.PI dpi = new Config.PI("DPI", "http://host:50000", login);
            config.getPi().put("DPI", dpi);
            Config.Login suser = new Config.Login(AuthEnum.Basic, "S999999999", "initinit123");
            Config.Tenant ten = new Config.Tenant("https://e459999-tmn.hci.ru1.hana.ondemand.com", suser, true);
            config.getTenants().put("test", ten);
            System.out.println("Config created dynamically");
            System.out.println(config);
        }
        config.createClient();
        config.init();
    }
}
