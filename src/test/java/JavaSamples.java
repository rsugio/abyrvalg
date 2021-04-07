import io.rsug.abyrvalg.CheckCapabilitiesCPI;
import io.rsug.abyrvalg.Config;
import io.rsug.abyrvalg.ServiceEndpointsCPI;
import io.rsug.abyrvalg.WorkspaceCPI;
import k3.EntryPoint;
import k3.ServiceEndpoint;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class JavaSamples {
    private Path tmpDir = Paths.get("tmp");
    private Config config;

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

    @Test
    public void serviceEndpointsCPI() {
        for (Config.Tenant ten : config.getTenants().values()) {
            ServiceEndpointsCPI endpoints = new ServiceEndpointsCPI(config, ten);
            List<ServiceEndpoint> lst = endpoints.extract();
            System.out.println("\n" + ten.getNick());
            for (ServiceEndpoint se : lst) {
                System.out.println(se.getId());
                for (EntryPoint ep : se.getEntryPoints().getResults()) {
                    assert !ep.getUrl().isEmpty();
                }
            }
        }
    }

    @Test
    public void check() {
        for (Config.Tenant ten : config.getTenants().values()) {
            WorkspaceCPI wksp = new WorkspaceCPI(config, ten);
            wksp.retrieve();

        }
    }

}
