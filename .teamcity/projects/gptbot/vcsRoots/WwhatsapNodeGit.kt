package projects.gptbot.vcsRoots

import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object WwhatsappNodeGit : GitVcsRoot({
    id("Gptbot_WwhatsappNodeGit")
    name = "git@github.com:dev4team-ai/wwhatsapp-node.git"
    url = "git@github.com:dev4team-ai/wwhatsapp-node.git"
    branch = "%teamcity.build.branch%"
    branchSpec = """
        +:refs/heads/*
        +:refs/tags/*
    """.trimIndent()
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "wwhatsapp-node"
    }
})