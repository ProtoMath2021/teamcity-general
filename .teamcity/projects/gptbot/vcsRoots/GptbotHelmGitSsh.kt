package projects.gptbot.vcsRoots

import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object GptbotHelmGitSsh : GitVcsRoot({
    id("GptbotProject_GptbotHelmGitSsh")
    name = "gptbot-helm-git-ssh"
    url = "git@github.com:dev4team-ai/gptbot-helm.git"
    branch = "main"
    authMethod = uploadedKey {
        uploadedKey = "gpt-helm-updater"
    }
})
