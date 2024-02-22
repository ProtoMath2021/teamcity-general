package ProtonMath_Frontend_StudentUi.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script

object ProtonMath_Frontend_StudentUi_Build : BuildType({
    name = "build"

    vcs {
        root(ProtonMath_Frontend_StudentUi.vcsRoots.ProtonMath_Frontend_StudentUi_GitGithubComProtoMath2021protomathStudentUiGit)
    }

    steps {
        script {
            name = "test"
            scriptContent = "ls -al"
        }
    }
})
