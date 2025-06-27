package projects.protonmath.backend

import jetbrains.buildServer.configs.kotlin.*
import projects.protonmath.backend.vcsRoots.*
import projects.protonmath.backend.buildTypes.*

object BackendProject : Project({
    name = "Backend"

    vcsRoot(CoreApiGit)

    buildType(Build)
})
