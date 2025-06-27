package projects.eutrip

import jetbrains.buildServer.configs.kotlin.*
import projects.eutrip.vcsRoots.*
import projects.eutrip.buildTypes.*

object EutripProject : Project({
    name = "Eutrip"

    vcsRoot(AnsibleHostGit)
    vcsRoot(CoreApiGit)
    vcsRoot(AdminUiGit)
    vcsRoot(HelmChartsGit)

    buildType(FrontendProd)
    buildType(DeployBackend)
    buildType(Frontend)
    buildType(Backend)
})
