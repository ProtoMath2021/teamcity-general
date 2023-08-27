import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildFeatures.vcsLabeling
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.kotlinScript
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.projectFeatures.dockerRegistry
import jetbrains.buildServer.configs.kotlin.triggers.vcs

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2022.10"

project {

    subProject(Projectexp)
    subProject(Proton)
}


object Projectexp : Project({
    name = "projectexp"
})


object Proton : Project({
    name = "proton"

    subProject(Proton_Front_2)
    subProject(Proton_Front)
    subProject(Proton_Backend_2)
})


object Proton_Backend_2 : Project({
    name = "backend (1)"

    buildType(Proton_Backend_2_Publish)
    buildType(Proton_Backend_2_Deploy)
    buildType(Proton_Backend_2_Build)

    params {
        password("db-pass", "zxxa604baaec7c27caf057cd168a37737e8", display = ParameterDisplay.HIDDEN)
        text("db-port", "5432", label = "db", description = "db", readOnly = true, allowEmpty = true)
        param("db-user", "proton-db-user")
        param("db-host", "proton-postgresql")
        param("db-name", "proton")
    }

    features {
        dockerRegistry {
            id = "PROJECT_EXT_3"
            name = "protonmath_Docker"
            userName = "protonmath"
            password = "zxxc0ce9241d87412ebc5a4bd5fa5ffed29"
        }
    }
    buildTypesOrder = arrayListOf(Proton_Backend_2_Build, Proton_Backend_2_Publish, Proton_Backend_2_Deploy)
})

object Proton_Backend_2_Build : BuildType({
    name = "build"

    artifactRules = "+:out => out"
    publishArtifacts = PublishMode.SUCCESSFUL

    params {
        param("BRANCH", "%vcsroot.ProtomathCoreApiGit.branch%")
        param("CURRENT_TAG", "")
        param("HEAD_TAG", "")
        param("teamcity.git.fetchAllHeads", "true")
        param("LAST_TAG", "")
        param("SKIP_PUBLISH", "false")
    }

    vcs {
        root(AbsoluteId("ProtomathCoreApiGit"))
    }

    steps {
        script {
            name = "get git tags"
            scriptContent = """
                (git describe --exact-match --tags HEAD 2>/dev/null || echo null) | xargs -I {} echo \"##teamcity[setParameter name=\'HEAD_TAG\' value=\'{}\']\"
                (git describe --abbrev=0 --tags ${'$'}(git rev-list --tags --max-count=1) 2>/dev/null || echo null) | xargs -I {} echo \"##teamcity[setParameter name=\'LAST_TAG\' value=\'{}\']\"
            """.trimIndent()
        }
        kotlinScript {
            name = "Get current tag"
            content = """
                fun String.incMinor(): String {
                    val parts = this.split(".")
                    if (parts.size != 3 || !parts[0].startsWith("v")) {
                        throw IllegalArgumentException("Invalid version string: BuildStep(name = 'Get current tag', type = 'kotlinScript')")
                    }
                    val major = parts[0]
                    val minor = parts[1]
                    val patch = parts[2]
                    return "${'$'}major.${'$'}minor.${'$'}{patch.toInt().inc()}"
                }
                fun getCurrentTag(branch: String, head: String, last: String, buildNumber: String): String {
                    println("branch: ${'$'}branch, head: ${'$'}head, last: ${'$'}last, buildNumber: ${'$'}buildNumber")
                    when (branch) {
                        "master" -> {
                            if (last.isEmpty() || last == "null") return "v0.0.1"
                            if (head == "null") return last.incMinor()
                            println("Head tag alreade exist: ${'$'}head")
                            return head
                        }
                        else -> {
                            if (last.isEmpty()) return "v0.0.1-${'$'}branch-1"
                            return "${'$'}{last.incMinor()}-${'$'}branch-${'$'}buildNumber"
                        }
                    }
                }
                val branch: String = "%vcsroot.ProtomathCoreApiGit.branch%"
                val buildNumber = "%build.number%"
                val head = "%HEAD_TAG%"
                val last = "%LAST_TAG%"
                val currentTag = getCurrentTag(branch, head, last, buildNumber)
                println("##teamcity[setParameter name='CURRENT_TAG' value='${'$'}currentTag']")
            """.trimIndent()
        }
        gradle {
            tasks = "clean assemble"
            jdkHome = "%env.JDK_17%"
        }
        kotlinScript {
            name = "Check build condition"
            content = """
                val branch = "%BRANCH%"
                val head = "%HEAD_TAG%"
                val last = "%LAST_TAG%"
                
                when (branch) {
                    "master" -> if (head==last) println("##teamcity[setParameter name='SKIP_PUBLISH' value='true']")
                    else -> println("##teamcity[setParameter name='SKIP_PUBLISH' value='false']")
                }
                println("%SKIP_PUBLISH%")
            """.trimIndent()
        }
    }
})

object Proton_Backend_2_Deploy : BuildType({
    name = "deploy"

    enablePersonalBuilds = false
    type = BuildTypeSettings.Type.DEPLOYMENT
    buildNumberPattern = "%DEPLOY_TAG%"
    maxRunningBuilds = 1

    params {
        param("CURRENT_TAG", "${Proton_Backend_2_Publish.depParamRefs["CURRENT_TAG"]}")
        param("CERT_URL", "http://keycloak/realms/protonmath/protocol/openid-connect/certs")
        text("DEPLOY_TAG", "", display = ParameterDisplay.PROMPT, allowEmpty = true)
    }

    steps {
        kotlinScript {
            name = "check deploy CURRENT_TAG"
            content = """
                val currentTag = "%CURRENT_TAG%"
                val deployTag = "%DEPLOY_TAG%"
                
                if (deployTag.isBlank() && currentTag.isBlank()) throw IllegalArgumentException("CURRENT_TAG cannot be blank")
                
                if (deployTag.isBlank()) {
                    println("##teamcity[setParameter name='DEPLOY_TAG' value='${'$'}currentTag']")
                }
                println("%DEPLOY_TAG%")
            """.trimIndent()
        }
        script {
            name = "delete helm release before deploy"
            enabled = false
            scriptContent = """
                helm uninstall backend -n protonmath 2>/dev/null
                helm list -n protonmath 2>/dev/null
            """.trimIndent()
        }
        script {
            name = "deploy helm"
            workingDir = ".pipeline/helm/backend-app"
            scriptContent = """
                helm upgrade -i --namespace protonmath \
                	--set app.version=%DEPLOY_TAG% \
                    --set database.host=%db-host% \
                    --set database.port=%db-port% \
                    --set database.user=%db-user% \
                    --set database.name=%db-name% \
                    --set database.password=%db-pass% \
                    --set keycloak.certUrl=%CERT_URL% \
                	backend .
            """.trimIndent()
        }
    }

    triggers {
        vcs {
            enabled = false
            branchFilter = """
                +:<default>
                +:release/ *
            """.trimIndent()
        }
    }

    dependencies {
        snapshot(Proton_Backend_2_Publish) {
            onDependencyFailure = FailureAction.IGNORE
        }
    }
})

object Proton_Backend_2_Publish : BuildType({
    name = "publish"

    buildNumberPattern = "%CURRENT_TAG%"

    params {
        param("BRANCH", "${Proton_Backend_2_Build.depParamRefs["BRANCH"]}")
        param("CURRENT_TAG", "${Proton_Backend_2_Build.depParamRefs["CURRENT_TAG"]}")
        param("HEAD_TAG", "${Proton_Backend_2_Build.depParamRefs["HEAD_TAG"]}")
        param("LAST_TAG", "${Proton_Backend_2_Build.depParamRefs["LAST_TAG"]}")
        param("SKIP_PUBLISH", "${Proton_Backend_2_Build.depParamRefs["SKIP_PUBLISH"]}")
    }

    vcs {
        root(AbsoluteId("ProtomathCoreApiGit"))
    }

    steps {
        gradle {
            name = "create dockerfile"

            conditions {
                equals("SKIP_PUBLISH", "false")
            }
            tasks = "createDockerfile"
            jdkHome = "%env.JDK_17%"
        }
        dockerCommand {
            name = "build image"

            conditions {
                equals("SKIP_PUBLISH", "false")
            }
            commandType = build {
                source = file {
                    path = "Dockerfile"
                }
                namesAndTags = "protonmath/proton-backend:%CURRENT_TAG%"
            }
        }
        dockerCommand {
            name = "push image"

            conditions {
                equals("SKIP_PUBLISH", "false")
            }
            commandType = push {
                namesAndTags = "protonmath/proton-backend:%CURRENT_TAG%"
            }
        }
    }

    features {
        dockerSupport {
            cleanupPushedImages = true
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_3"
            }
        }
        vcsLabeling {
            vcsRootId = "ProtomathCoreApiGit"
            labelingPattern = "%CURRENT_TAG%"
            successfulOnly = true
        }
    }

    dependencies {
        dependency(Proton_Backend_2_Build) {
            snapshot {
                reuseBuilds = ReuseBuilds.NO
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                cleanDestination = true
                artifactRules = "+:./out/*.* => ./artifact"
            }
        }
    }
})


object Proton_Front : Project({
    name = "front"
    archived = true
})


object Proton_Front_2 : Project({
    name = "front (1)"

    buildType(Proton_Front_2_BuildDocker)
    buildType(Proton_Front_2_DeployKuber)

    features {
        dockerRegistry {
            id = "PROJECT_EXT_4"
            name = "protonmath_Docker"
            userName = "protonmath"
            password = "zxxc0ce9241d87412ebc5a4bd5fa5ffed29"
        }
    }
})

object Proton_Front_2_BuildDocker : BuildType({
    name = "Build docker"

    params {
        param("teamcity.git.fetchAllHeads", "true")
        param("DEPLOY_TAG", "v%build.number%")
    }

    vcs {
        root(AbsoluteId("ProtomathUiNewGit"))
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
                commandArgs = ""
            }
        }
    }

    features {
        dockerSupport {
            cleanupPushedImages = true
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_4"
            }
        }
        vcsLabeling {
            vcsRootId = "ProtomathUiNewGit"
            labelingPattern = "%DEPLOY_TAG%"
            successfulOnly = true
            branchFilter = "master"
        }
    }
})

object Proton_Front_2_DeployKuber : BuildType({
    name = "Deploy kuber"

    params {
        param("BASE_URL", "http://protonmath.ru/api")
        param("AUTH_BASE_URL", "https://auth.protonmath.ru")
        text("DEPLOY_TAG", "", display = ParameterDisplay.PROMPT, allowEmpty = false)
    }

    steps {
        script {
            name = "deploy helm"
            workingDir = "helm/frontend-app"
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
