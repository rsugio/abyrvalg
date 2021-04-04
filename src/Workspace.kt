import io.ktor.client.*
import io.ktor.client.statement.*
import k3.IntegrationDesigntimeArtifacts
import k3.IntegrationPackage
import k3.IntegrationPackages
import k3.ValueMappingDesigntimeArtifacts


class Workspace(
    val tenant: Config.Tenant,
    val packages: MutableList<IntegrationPackage> = mutableListOf()
) {
    suspend fun retrieve(clnt: HttpClient) {
        // 1. Получить список всех пакетов
        val resp = tenant.getJson(clnt, IntegrationPackages.getUrl(tenant.tmn))
        val d = IntegrationPackages.parse(resp.readText())
        require(d.d.__next == "", { "Too many packages! Listing is not implemented yet" })
        d.d.results.forEach { pack ->
            packages.add(pack)
            val respida = tenant.getJson(clnt, pack.IntegrationDesigntimeArtifacts.getUri())
            val respvm = tenant.getJson(clnt, pack.ValueMappingDesigntimeArtifacts.getUri())

            IntegrationDesigntimeArtifacts.parse(respida.readText()).d.results.forEach { it ->
                pack.ida.add(it)
            }
            ValueMappingDesigntimeArtifacts.parse(respvm.readText()).d.results.forEach { it ->
                pack.vmda.add(it)
            }
        }
    }
}