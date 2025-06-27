package projects.protonmath.frontend.teacherui

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildFeatures.vcsLabeling
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

object TeacherUiProject : Project({
    name = "teacher-ui"

    vcsRoot(TeacherUiGit)

    buildType(BuildDocker)
    buildType(Deploy)
})

object TeacherUiGit : GitVcsRoot({
    id("ProtonMath_Frontend_TeacherUi_Git")
    name = "git@github.com:ProtoMath2021/protomath-ui-new.git"
    url = "git@github.com:ProtoMath2021/protomath-ui-new.git"
    branch = "refs/heads/master"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "proton-front-teamcity"
    }
})

object BuildDocker : BuildType({
    id("ProtonMath_Frontend_TeacherUi_BuildDocker")
    name = "build_docker"

    params {
        param("DEPLOY_TAG", "v%build.number%")
    }

    vcs {
        root(TeacherUiGit)
    }

    steps {
        dockerCommand {
            name = "build image"
            commandType = build {
                source = file {
                    path = "Dockerfile"
                }
                namesAndTags = "protonmath/proton-ui:%DEPLOY_TAG%"
            }
        }
        dockerCommand {
            name = "push image"
            commandType = push {
                namesAndTags = "protonmath/proton-ui:%DEPLOY_TAG%"
            }
        }
    }

    features {
        dockerSupport {
            cleanupPushedImages = true
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_11"
            }
        }
        vcsLabeling {
            vcsRootId = "${TeacherUiGit.id}"
            labelingPattern = "%DEPLOY_TAG%"
            successfulOnly = true
            branchFilter = ""
        }
    }
})

object Deploy : BuildType({
    id("ProtonMath_Frontend_TeacherUi_Deploy")
    name = "deploy"

    params {
        param("BASE_URL", "https://testinfra.online/api")
        param("AUTH_BASE_URL", "https://auth.devinfra.ru")
        text("DEPLOY_TAG", "", display = ParameterDisplay.PROMPT, allowEmpty = true)
    }

    vcs {
        root(TeacherUiGit)
    }

    steps {
        script {
            name = "deply helm"
            workingDir = ".helm"
            scriptContent = """
                helm upgrade -i --namespace protonmath \
                                    --set app.version=%DEPLOY_TAG% \
                                    --set app.baseUrl=%BASE_URL% \
                                    --set app.authBaseUrl=%AUTH_BASE_URL% \
                                	frontend .
            """.trimIndent()
        }
    }
})
