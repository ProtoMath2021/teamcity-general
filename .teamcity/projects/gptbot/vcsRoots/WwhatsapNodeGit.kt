package projects.gptbot.vcsRoots

import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object WwhatsappNodeGit : GitVcsRoot({
    id("Gptbot_WwhatsappNodeGit")
    name = "git@github.com:dev4team-ai/wwhatsapp-node.git"
    url = "git@github.com:dev4team-ai/wwhatsapp-node.git"
    branch = "refs/heads/main"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "wwhatsapp-node"
    }
})