package projects.protonmath

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.projectFeatures.dockerRegistry
import projects.protonmath.backend.BackendProject
import projects.protonmath.frontend.FrontendProject

object ProtonMathProject : Project({
    name = "ProtonMath"

    features {
        dockerRegistry {
            id = "PROJECT_EXT_11"
            name = "Docker Registry"
            userName = "protonmath"
            password = "zxx5e244335597ac1e30345c418695c01fe083c51d26ee56b10396789d1aa194f59a07c29f646e3eb09"
        }
    }

    subProject(FrontendProject)
    subProject(BackendProject)
})
