package ProtonMath_Frontend

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project

object Project : Project({
    id("ProtonMath_Frontend")
    name = "Frontend"

    subProject(ProtonMath_Frontend_StudentUi.Project)
})
