package projects.eutrip.vcsRoots

import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object CoreApiGit : GitVcsRoot({
    id("Eutrip_CoreApiGit")
    name = "git@github.com:ProtoMath2021/eutrip-core-api.git"
    url = "git@github.com:ProtoMath2021/eutrip-core-api.git"
    branch = "refs/heads/main"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "eutrip-back-teamcity"
    }
})
