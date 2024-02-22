package patches.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.BuildType
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.ui.*

/*
This patch script was generated by TeamCity on settings change in UI.
To apply the patch, create a buildType with id = 'TeamcityGeneral_ProtonMath_Frontend_StudentUi_Build'
in the project with id = 'TeamcityGeneral_ProtonMath_Frontend_StudentUi', and delete the patch script.
*/
create(RelativeId("TeamcityGeneral_ProtonMath_Frontend_StudentUi"), BuildType({
    id("TeamcityGeneral_ProtonMath_Frontend_StudentUi_Build")
    name = "build"

    vcs {
        root(RelativeId("TeamcityGeneral_ProtonMath_Frontend_StudentUi_GitGithubComProtoMath2021protomathStudentUiGit"))
    }

    steps {
        script {
            name = "test"
            scriptContent = "ls -al"
        }
    }
}))
