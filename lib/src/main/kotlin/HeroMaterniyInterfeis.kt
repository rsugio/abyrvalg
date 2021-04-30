import io.rsug.abyrvalg.Config
import k1.*

class HeroMaterniyInterfeis(val config: Config, val pi: Config.PI) {
    fun mappingTest(swcvGuid: String, namespace: String, name: String, bodyXml: String): String {
        val bodyEscaped = bodyXml
        val mapping = TestExecutionRequest(
            HmRef(
                HmVC(swcvGuid, "S"),
                HmKey("MAPPING", null, mutableListOf(name, namespace))
            ), TestExecutionRequest.TestData(bodyEscaped,
                TestExecutionRequest.Parameters(
                    TestExecutionRequest.TestParameterInfo(
                        TestExecutionRequest.HIParameters(
                            TestExecutionRequest.Properties(
                                mutableListOf(TestExecutionRequest.Property("TimeSent", ""))
                            )),
                        TestExecutionRequest.HIParameters(TestExecutionRequest.Properties(
                            mutableListOf() //TestExecutionRequest.Property("name", "value"))
                        )
                        )
                    )
                ),
                null, //TestExecutionRequest.TestParameters("AAA", 1, 2),
                3)
        )
        val req = request("4054f934fcd811eab42d54f36eecaf14", "executemappingmethod",
            "mappingtestservice", mapping.composeXml())
        return req.printXml()
    }
}