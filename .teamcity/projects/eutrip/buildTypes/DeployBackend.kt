package projects.eutrip.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import projects.eutrip.vcsRoots.AnsibleHostGit

object DeployBackend : BuildType({
    id("Eutrip_DeployBackend")
    name = "Deploy"

    enablePersonalBuilds = false
    type = BuildTypeSettings.Type.DEPLOYMENT
    maxRunningBuilds = 1

    params {
        param("env.PATH", "/usr/bin:/usr/sbin:/usr/local/sbin:/usr/local/bin:/home/buildagent/.local/bin")
        checkbox("prod", "false", label = "prod", display = ParameterDisplay.PROMPT,
                  checked = "true", unchecked = "false")
        text("ui_version", "", label = "ui_version", display = ParameterDisplay.PROMPT, allowEmpty = true)
    }

    vcs {
        root(AnsibleHostGit)
    }

    steps {
        script {
            name = "requirements"
            scriptContent = "ansible-galaxy install -r requirements.yml"
        }
        script {
            name = "deploy playbook"
            scriptContent = "ansible-playbook -i ./projects/eutrip/inventory/dev/inv ./projects/eutrip/playbooks/play-app.yaml -e ansible_user=cd_tech_agent -e ansible_host=85.30.208.151 -e ansible_port=2207 -e ui_version=dev-%ui_version% -vvv"
        }
        script {
            name = "deploy playbook prod"

            conditions {
                equals("prod", "true")
            }
            scriptContent = "ansible-playbook -i ./projects/eutrip/inventory/prod/inv ./projects/eutrip/playbooks/play-app.yaml -e ansible_user=infra -e ansible_host=103.137.248.112 -e ansible_port=22 -e ui_version=prod-%ui_version% -vvv"
        }
    }

    triggers {
        finishBuildTrigger {
            buildType = "${Backend.id}"
            successfulOnly = true
        }
    }

    requirements {
        equals("teamcity.agent.name", "Agent 2-1")
    }
})
