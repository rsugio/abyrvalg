import io.rsug.abyrvalg.Config
import k1.HmInstance
import k1.TestExecutionRequest
import k1.XiObj
import k1.request

class HeroMaterniyInterfeis(val config: Config, val pi: Config.PI) {
    fun mappingTest(swcvGuid: String, namespace: String, name: String, bodyXml: String): String {
        val bodyEscaped = bodyXml
        val mapping = TestExecutionRequest(
            TestExecutionRequest.Ref(
                TestExecutionRequest.VC(swcvGuid, "S"),
                TestExecutionRequest.Key("MAPPING", mutableListOf(name, namespace))
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