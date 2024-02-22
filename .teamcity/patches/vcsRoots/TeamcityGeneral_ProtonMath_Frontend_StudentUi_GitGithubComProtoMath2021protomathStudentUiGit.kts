package patches.vcsRoots

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.ui.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

/*
This patch script was generated by TeamCity on settings change in UI.
To apply the patch, create a vcsRoot with id = 'TeamcityGeneral_ProtonMath_Frontend_StudentUi_GitGithubComProtoMath2021protomathStudentUiGit'
in the project with id = 'TeamcityGeneral_ProtonMath_Frontend_StudentUi', and delete the patch script.
*/
create(RelativeId("TeamcityGeneral_ProtonMath_Frontend_StudentUi"), GitVcsRoot({
    id("TeamcityGeneral_ProtonMath_Frontend_StudentUi_GitGithubComProtoMath2021protomathStudentUiGit")
    name = "git@github.com:ProtoMath2021/protomath-student-ui.git"
    url = "git@github.com:ProtoMath2021/protomath-student-ui.git"
    branch = "refs/heads/main"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "proton-front-student-ui"
    }
}))

