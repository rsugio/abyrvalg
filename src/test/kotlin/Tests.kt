import io.rsug.abyrvalg.Config
import io.rsug.abyrvalg.Config.Companion.parseHocon
import io.rsug.abyrvalg.WorkspaceCPI
import k6.IFlowBpmnDefinitions
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.Before
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipInputStream
import kotlin.io.path.writer

class Tests {
    private val tmpDir = Paths.get("tmp")
    private lateinit var config: Config

    private fun mkdir(where: Path, what: String): Path {
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
    fun verifyConfig() {
        println(config.elasticsearch.url)
    }

    @Test
    fun analyzeWorkspace() {
        config.tenants.values
            .filter { it.nick == "eutest" }
            .forEach { tenant ->
                val wk = WorkspaceCPI(config, tenant)
                wk.retrieve()
                wk.runtimeArtifacts.forEach { rt ->
                    println("${rt.Id} ${rt.Version} -> ${rt.designtimeArtifact ?: rt.designtimeVMG}")
                }
            }
    }

    @Test
    fun downloadWorkspaceCPI() {
        config.tenants.values
//            .filter { it.nick == "eutest" }   // <-- если хотим не всё протестить
            .forEach { tenant ->
                val wk = WorkspaceCPI(config, tenant)
                wk.retrieve()
                val tdir = mkdir(tmpDir, tenant.nick)
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

    @Test
    fun simpleQuery1() {
        config.pi.values
            .filter { it.sid == "QPH" }
            .forEach {
                val sq = SimpleQueryPI(config, it)
                runBlocking {
                    val n = sq.getList("XI_TRAFO")
                    n.filter { it["Description"]!!.isNotEmpty() }
                        .forEach {
                            println("""${it["Name"]} = ${it["Description"]}""")
                        }
                }
            }
    }

    @Test
    fun massParseICo() {
        var cnt = 0
        val download = false
        config.pi.values
            .filter { it.sid == "QPH" }
            .forEach { pi ->
                val pidir = tmpDir.resolve(pi.sid)
                val icos = IntegratedConfigurationsPI(config, pi, true)
                if (download) {
                    runBlocking {
                        icos.getListSuspend().chunked(200).forEach { ico200 ->
                            val txt = icos.readICosSuspendString(ico200)
                            val wr = Files.newBufferedWriter(pidir.resolve("ICO750_${cnt++}.xml"))
                            wr.write(txt)
                            wr.close()
                        }
                    }
                } else {
                    Files.newDirectoryStream(pidir, "ICO750_*.xml").forEach { pack ->
                        val xmlSoap = Files.newBufferedReader(pack).readText()
                        icos.parseIcoSoap(xmlSoap)
                        println("$pack done")
                    }
                }
            }
    }

}