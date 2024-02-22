package ProtonMath_Frontend_StudentUi

import ProtonMath_Frontend_StudentUi.buildTypes.*
import ProtonMath_Frontend_StudentUi.vcsRoots.*
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project

object Project : Project({
    id("ProtonMath_Frontend_StudentUi")
    name = "student-ui"

    vcsRoot(ProtonMath_Frontend_StudentUi_GitGithubComProtoMath2021protomathStudentUiGit)

    buildType(ProtonMath_Frontend_StudentUi_Build)
})
