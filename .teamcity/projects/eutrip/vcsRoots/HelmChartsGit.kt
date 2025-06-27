package projects.eutrip.vcsRoots

import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object HelmChartsGit : GitVcsRoot({
    id("Eutrip_HelmChartsGit")
    name = "git@github.com:ProtoMath2021/eutrip-helm-charts.git"
    url = "git@github.com:ProtoMath2021/eutrip-helm-charts.git"
    branch = "refs/heads/master"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "eutrip-helm-teamcity"
    }
})
