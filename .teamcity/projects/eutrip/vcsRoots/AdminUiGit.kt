package projects.eutrip.vcsRoots

import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object AdminUiGit : GitVcsRoot({
    id("Eutrip_AdminUiGit")
    name = "git@github.com:ProtoMath2021/eutrip-admin-ui.git"
    url = "git@github.com:ProtoMath2021/eutrip-admin-ui.git"
    branch = "refs/heads/master"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "eutrip-front-teamcity"
    }
})
