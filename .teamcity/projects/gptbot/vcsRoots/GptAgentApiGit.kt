package projects.gptbot.vcsRoots

import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object GptAgentApiGit : GitVcsRoot({
    id("Gptbot_GptAgentApiGit")
    name = "git@github.com:dev4team-ai/gpt-agent-api.git"
    url = "git@github.com:dev4team-ai/gpt-agent-api.git"
    branch = "refs/heads/master"
    branchSpec = "*"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "gpt-agent-api"
    }
})
