package projects.eutrip.vcsRoots

import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object AnsibleHostGit : GitVcsRoot({
    id("Eutrip_AnsibleHostGit")
    name = "git@github.com:ProtoMath2021/ansible-host.git"
    url = "git@github.com:ProtoMath2021/ansible-host.git"
    branch = "refs/heads/master"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "eutrip_cd"
    }
})
