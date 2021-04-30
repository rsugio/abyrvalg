import io.ktor.client.statement.*
import io.rsug.abyrvalg.Config
import k1.*
import kotlinx.coroutines.runBlocking

class HeroMaterniyInterfeis(val config: Config, val pi: Config.PI) {
    fun mappingTest(swcvGuid: String, namespace: String, name: String, bodyXml: String): String {
        return ""
    }

    fun query(qu: GeneralQueryRequest, hmreq: HmRequest): QueryResult? {
        val y = hmreq.render(HmRequest.input("QUERY_REQUEST_XML", qu.compose()), "GENERIC", "QUERY")
        var qr: QueryResult? = null
        runBlocking {
            val resp = post("/rep/query/int?container=any", y)
            if (resp.isException()) {
                System.err.println(resp.getCoreException())
            } else {
                require(resp.get("MethodFault")?.value?.size == 0)
                val outputXml = resp.get("MethodOutput")!!.get("Return")!!.value!!.get(0) as String
                qr = QueryResult.parseUnescapedXml(outputXml)
            }
        }
        return qr
    }

    suspend fun post(hmires: String = "/rep/query/int?container=any", hmInst: HmInstance): HmInstance {
        val req = hmInst.printXml()
//        println(req)
        val resp = pi.postHMI(config.client, hmires, req)
        val respXml = resp.readText()
//        println(respXml)
        return HmInstance.parse(respXml)
    }
}