package projects.protonmath.backend.vcsRoots

import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object CoreApiGit : GitVcsRoot({
    id("ProtonMath_Backend_CoreApiGit")
    name = "git@github.com:ProtoMath2021/protomath-core-api.git"
    url = "git@github.com:ProtoMath2021/protomath-core-api.git"
    branch = "refs/heads/master"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "proton-back-teamcity"
    }
})
