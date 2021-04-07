import io.rsug.abyrvalg.CheckCapabilitiesCPI;
import io.rsug.abyrvalg.Config;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JavaSamples {
    Path tmpDir = Paths.get("tmp");
    Config config;

    @Before
    public void config() throws IOException {
        String text = new String(Files.newInputStream(tmpDir.resolve("abyrvalg.conf")).readAllBytes(), StandardCharsets.UTF_8);

        config = Config.Companion.parseHocon(text);
        config.createClient();
        config.init();
    }

    @Test
    public void checkCapabilitiesCPI() {
        for (Config.Tenant ten : config.getTenants().values()) {
            CheckCapabilitiesCPI check = new CheckCapabilitiesCPI(config, ten);
            System.out.println("\n" + ten.getNick() + "\n");
            System.out.println(check.exec() + "\n");
        }
    }


}
