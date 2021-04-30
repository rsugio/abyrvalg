import kotlin.test.Test
import io.rsug.abyrvalg.Config
import io.rsug.abyrvalg.parseHoconFromPath
import k1.GeneralQueryRequest
import k1.HmRequest
import kotlinx.serialization.ExperimentalSerializationApi
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.div

/**
 * Конфиг берётся из $PROJECT/tmp/abyrvalg.conf и соединение устанавливается с реальными системами
 * По умолчанию никаких деструктивных методов здесь быть не должно
 */
@OptIn(ExperimentalSerializationApi::class)
class OnlineTests {
    val config: Config
    val tmp = Paths.get("../tmp").toAbsolutePath()
    val cfgPath = tmp / "abyrvalg.conf"

    init {
        assert(Files.isRegularFile(cfgPath), { "$cfgPath not found" })
        config = parseHoconFromPath(cfgPath, tmp)
        config.createClient()
        config.init()
    }

    @Test
    fun pi_query() {
        // для каждого пиая
        config.pi.values
            .filter { it.sid == "DPH" }
            .forEach { pi ->
                println("PI SID=${pi.sid}")
                val hmi = HeroMaterniyInterfeis(config, pi)
                val qu = GeneralQueryRequest.ofArg(listOf("workspace"),
                    GeneralQueryRequest.elementary("WS_ORDER", "EQ", GeneralQueryRequest.Simple(-1)),
                    "", "RA_WORKSPACE_ID", "WS_NAME", "VENDOR", "NAME", "VERSION", "CAPTION", "WS_TYPE")
                val req = HmRequest("dummy", "7.0", "0")
                val resp = hmi.query(qu, req)
                println(resp?.toTable())

            }
    }
}