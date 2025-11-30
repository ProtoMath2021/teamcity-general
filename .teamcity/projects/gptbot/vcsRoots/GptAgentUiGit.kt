package projects.gptbot.vcsRoots

import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object GptAgentUiGit : GitVcsRoot({
    id("Gptbot_GptAgentUiGit")
    name = "git@github.com:dev4team-ai/gpt-agent-ui.git"
    url = "git@github.com:dev4team-ai/gpt-agent-ui.git"
    branch = "refs/heads/master"
    branchSpec = "*"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "gpt-agent-ui"
    }
})
