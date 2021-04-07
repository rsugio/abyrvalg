import io.rsug.abyrvalg.Config
import io.rsug.abyrvalg.Config.Companion.parseHocon
import io.rsug.abyrvalg.WorkspaceCPI
import k6.IFlowBpmnDefinitions
import kotlinx.serialization.ExperimentalSerializationApi
import nl.adaptivity.xmlutil.serialization.UnknownXmlFieldException
import org.junit.Before
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipInputStream

class Tests {
    val tmpDir = Paths.get("tmp")
    private lateinit var config: Config

    fun mkdir(where: Path, what: String): Path {
        val x = where.resolve(what)
        if (!Files.isDirectory(x))
            Files.createDirectory(x)
        return x
    }

    @ExperimentalSerializationApi
    @Before
    fun config() {
        val text = String(Files.newInputStream(tmpDir.resolve("abyrvalg.conf")).readAllBytes(), StandardCharsets.UTF_8)
        config = parseHocon(text)
        config.createClient()
        config.init()
    }

    @Test
    fun downloadWorkspaceCPI() {
        config.tenants.values
            .filter { it.nick == "rutest" }
            .forEach { tenant ->
                val tdir = mkdir(tmpDir, tenant.nick)
                val wk = WorkspaceCPI(config, tenant)
                wk.retrieve()
                wk.packages.forEach { pack ->
                    val pdir = mkdir(tdir, pack.Id)
                    pack.ida.forEach { ida ->
                        val z = pdir.resolve("${ida.Id}__version=${ida.Version}.zip")
                        if (!Files.isRegularFile(z)) {
                            println("${ida.Id} ${ida.Version}")
                            val bytes = wk.download(ida)
                            Files.write(z, bytes)
                        }
                    }
                }
            }
    }

    @Test
    fun massParseIflw() {
        var cnt = 0
        config.tenants.values.forEach { tenant ->
            Files.newDirectoryStream(tmpDir.resolve(tenant.nick)).forEach { pack ->
                Files.newDirectoryStream(pack, "*.zip").forEach { zipath ->
                    val zis = ZipInputStream(Files.newInputStream(zipath))
                    var ze = zis.nextEntry
                    while (ze != null) {
                        if (ze.name.startsWith("src/main/resources/scenarioflows/integrationflow/")) {
                            val iflw = String(zis.readAllBytes(), StandardCharsets.UTF_8)
                            try {
                                IFlowBpmnDefinitions.parse(iflw)
                            } catch (e: Exception) {
                                val w = Files.newBufferedWriter(pack.resolve("$cnt.iflw"))
                                w.write(iflw)
                                w.close()
                                System.err.println("error: $zipath")
                                System.err.println(e)
                            }
                        }
                        zis.closeEntry()
                        ze = zis.nextEntry
                    }
                    zis.close()
                    cnt++
                }
            }
        }
        println("Total IFLW processed: $cnt")
    }
}