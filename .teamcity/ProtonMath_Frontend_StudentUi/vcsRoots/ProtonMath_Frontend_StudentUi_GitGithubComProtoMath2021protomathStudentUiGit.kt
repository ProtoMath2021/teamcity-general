package ProtonMath_Frontend_StudentUi.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object ProtonMath_Frontend_StudentUi_GitGithubComProtoMath2021protomathStudentUiGit : GitVcsRoot({
    name = "git@github.com:ProtoMath2021/protomath-student-ui.git"
    url = "git@github.com:ProtoMath2021/protomath-student-ui.git"
    branch = "refs/heads/main"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "proton-front-student-ui"
    }
})
