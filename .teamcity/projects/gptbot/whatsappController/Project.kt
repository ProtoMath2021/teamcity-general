package projects.gptbot.whatsappController

import jetbrains.buildServer.configs.kotlin.*
import projects.gptbot.whatsappController.vcsRoots.*
import projects.gptbot.whatsappController.buildTypes.*

object WhatsappControllerProject : Project({
    name = "WhatsappController"

    vcsRoot(WhatsappControllerGit)

    buildType(Build)
})
