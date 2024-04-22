package ProtonMath

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.Project

object Project : Project({
    id("ProtonMath")
    name = "ProtonMath"

    subProject(ProtonMath_Frontend.Project)
    subProject(ProtonMath_Backend.Project)
})
