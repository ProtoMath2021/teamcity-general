import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildFeatures.vcsLabeling
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.kotlinScript
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.projectFeatures.dockerRegistry
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

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

    vcsRoot(ProtomathTeamcityPipeline_GitGithubComProtoMath2021projectExpertBackendGit)
    vcsRoot(ProtomathUiNewGit)
    vcsRoot(ProtomathCoreApiGit)

    template(ProtomathTeamcityPipeline_BackendTemplate)

    features {
        dockerRegistry {
            id = "PROJECT_EXT_5"
            name = "Docker Registry"
            userName = "protonmath"
            password = "zxxc0ce9241d87412ebc5a4bd5fa5ffed29"
        }
    }

    subProject(ProtomathTeamcityPipeline_Projectexp)
    subProject(ProtomathTeamcityPipeline_Proton)
}

object ProtomathTeamcityPipeline_BackendTemplate : Template({
    id("BackendTemplate")
    name = "backend-template"
})

object ProtomathCoreApiGit : GitVcsRoot({
    id = AbsoluteId("ProtomathCoreApiGit")
    name = "git@github.com:ProtoMath2021/protomath-core-api.git"
    url = "git@github.com:ProtoMath2021/protomath-core-api.git"
    branch = "master"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "proton-back-teamcity"
    }
    param("secure:password", "")
})

object ProtomathTeamcityPipeline_GitGithubComProtoMath2021projectExpertBackendGit : GitVcsRoot({
    id("GitGithubComProtoMath2021projectExpertBackendGit")
    name = "git@github.com:ProtoMath2021/project-expert-backend.git"
    url = "git@github.com:ProtoMath2021/project-expert-backend.git"
    branch = "refs/heads/master"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "prexp-back-teamcity"
    }
    param("secure:password", "")
})

object ProtomathUiNewGit : GitVcsRoot({
    id = AbsoluteId("ProtomathUiNewGit")
    name = "git@github.com:ProtoMath2021/protomath-ui-new.git"
    url = "git@github.com:ProtoMath2021/protomath-ui-new.git"
    branch = "refs/heads/master"
    authMethod = uploadedKey {
        userName = "git"
        uploadedKey = "proton-front-teamcity"
    }
})


object ProtomathTeamcityPipeline_Projectexp : Project({
    id("Projectexp")
    name = "projectexp"

    subProject(ProtomathTeamcityPipeline_Projectexp_Backend)
})


object ProtomathTeamcityPipeline_Projectexp_Backend : Project({
    id("Projectexp_Backend")
    name = "backend"

    buildType(ProtomathTeamcityPipeline_Projectexp_Backend_Build)
    buildType(ProtomathTeamcityPipeline_Projectexp_Backend_Publish)
})

object ProtomathTeamcityPipeline_Projectexp_Backend_Build : BuildType({
    id("Projectexp_Backend_Build")
    name = "build"

    artifactRules = "+:out => out"
    publishArtifacts = PublishMode.SUCCESSFUL

    vcs {
        root(ProtomathTeamcityPipeline_GitGithubComProtoMath2021projectExpertBackendGit)
    }

    steps {
        gradle {
            tasks = "clean assemble"
            jdkHome = "%env.JDK_17_0%"
        }
    }
})

object ProtomathTeamcityPipeline_Projectexp_Backend_Publish : BuildType({
    id("Projectexp_Backend_Publish")
    name = "publish"

    params {
        text("CURRENT_TAG", "", display = ParameterDisplay.PROMPT, allowEmpty = true)
        param("SKIP_PUBLISH", "false")
    }

    vcs {
        root(ProtomathTeamcityPipeline_GitGithubComProtoMath2021projectExpertBackendGit)
    }

    steps {
        gradle {
            tasks = "createDockerfile"
            jdkHome = "%env.JDK_17_0%"
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
                namesAndTags = "protonmath/projectexp-backend:%CURRENT_TAG%"
            }
        }
        dockerCommand {
            name = "push image"

            conditions {
                equals("SKIP_PUBLISH", "false")
            }
            commandType = push {
                namesAndTags = "protonmath/projectexp-backend:%CURRENT_TAG%"
            }
        }
    }

    dependencies {
        dependency(ProtomathTeamcityPipeline_Projectexp_Backend_Build) {
            snapshot {
            }

            artifacts {
                cleanDestination = true
                artifactRules = "+:./out/*.* => ./artifact"
            }
        }
    }
})


object ProtomathTeamcityPipeline_Proton : Project({
    id("Proton")
    name = "proton"

    subProject(ProtomathTeamcityPipeline_Proton_Front_2)
    subProject(ProtomathTeamcityPipeline_Proton_Backend_2)
})


object ProtomathTeamcityPipeline_Proton_Backend_2 : Project({
    id("Proton_Backend_2")
    name = "backend"

    buildType(ProtomathTeamcityPipeline_Proton_Backend_2_Publish)
    buildType(ProtomathTeamcityPipeline_Proton_Backend_2_Deploy)
    buildType(ProtomathTeamcityPipeline_Proton_Backend_2_Build)

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
    buildTypesOrder = arrayListOf(ProtomathTeamcityPipeline_Proton_Backend_2_Build, ProtomathTeamcityPipeline_Proton_Backend_2_Publish, ProtomathTeamcityPipeline_Proton_Backend_2_Deploy)
})

object ProtomathTeamcityPipeline_Proton_Backend_2_Build : BuildType({
    templates(ProtomathTeamcityPipeline_BackendTemplate)
    id("Proton_Backend_2_Build")
    name = "build"

    artifactRules = "+:out => out"
    publishArtifacts = PublishMode.SUCCESSFUL

    params {
        param("BRANCH", "${ProtomathCoreApiGit.paramRefs["branch"]}")
        param("CURRENT_TAG", "")
        param("HEAD_TAG", "")
        param("teamcity.git.fetchAllHeads", "true")
        param("LAST_TAG", "")
        param("SKIP_PUBLISH", "false")
    }

    vcs {
        root(ProtomathCoreApiGit)
    }

    steps {
        script {
            name = "get git tags"
            id = "RUNNER_1"
            scriptContent = """
                (git describe --exact-match --tags HEAD 2>/dev/null || echo null) | xargs -I {} echo \"##teamcity[setParameter name=\'HEAD_TAG\' value=\'{}\']\"
                (git describe --abbrev=0 --tags ${'$'}(git rev-list --tags --max-count=1) 2>/dev/null || echo null) | xargs -I {} echo \"##teamcity[setParameter name=\'LAST_TAG\' value=\'{}\']\"
            """.trimIndent()
        }
        kotlinScript {
            name = "Get current tag"
            id = "RUNNER_2"
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
                val branch: String = "${ProtomathCoreApiGit.paramRefs["branch"]}"
                val buildNumber = "%build.number%"
                val head = "%HEAD_TAG%"
                val last = "%LAST_TAG%"
                val currentTag = getCurrentTag(branch, head, last, buildNumber)
                println("##teamcity[setParameter name='CURRENT_TAG' value='${'$'}currentTag']")
            """.trimIndent()
        }
        gradle {
            id = "RUNNER_3"
            tasks = "clean assemble"
            jdkHome = "%env.JDK_17%"
        }
        kotlinScript {
            name = "Check build condition"
            id = "RUNNER_4"
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

object ProtomathTeamcityPipeline_Proton_Backend_2_Deploy : BuildType({
    templates(ProtomathTeamcityPipeline_BackendTemplate)
    id("Proton_Backend_2_Deploy")
    name = "deploy"

    enablePersonalBuilds = false
    type = BuildTypeSettings.Type.DEPLOYMENT
    buildNumberPattern = "%DEPLOY_TAG%"
    maxRunningBuilds = 1

    params {
        param("CURRENT_TAG", "${ProtomathTeamcityPipeline_Proton_Backend_2_Publish.depParamRefs["CURRENT_TAG"]}")
        param("CERT_URL", "http://keycloak/realms/protonmath/protocol/openid-connect/certs")
        text("DEPLOY_TAG", "", display = ParameterDisplay.PROMPT, allowEmpty = true)
    }

    vcs {
        root(ProtomathCoreApiGit)
    }

    steps {
        kotlinScript {
            name = "check deploy CURRENT_TAG"
            id = "RUNNER_1"
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
            id = "RUNNER_2"
            enabled = false
            scriptContent = """
                helm uninstall backend -n protonmath 2>/dev/null
                helm list -n protonmath 2>/dev/null
            """.trimIndent()
        }
        script {
            name = "deploy helm"
            id = "RUNNER_3"
            workingDir = ".helm"
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
            id = "TRIGGER_1"
            enabled = false
            branchFilter = """
                +:<default>
                +:release/ *
            """.trimIndent()
        }
    }

    dependencies {
        snapshot(ProtomathTeamcityPipeline_Proton_Backend_2_Publish) {
            onDependencyFailure = FailureAction.IGNORE
        }
    }
})

object ProtomathTeamcityPipeline_Proton_Backend_2_Publish : BuildType({
    templates(ProtomathTeamcityPipeline_BackendTemplate)
    id("Proton_Backend_2_Publish")
    name = "publish"

    buildNumberPattern = "%CURRENT_TAG%"

    params {
        param("BRANCH", "${ProtomathTeamcityPipeline_Proton_Backend_2_Build.depParamRefs["BRANCH"]}")
        param("CURRENT_TAG", "${ProtomathTeamcityPipeline_Proton_Backend_2_Build.depParamRefs["CURRENT_TAG"]}")
        param("HEAD_TAG", "${ProtomathTeamcityPipeline_Proton_Backend_2_Build.depParamRefs["HEAD_TAG"]}")
        param("LAST_TAG", "${ProtomathTeamcityPipeline_Proton_Backend_2_Build.depParamRefs["LAST_TAG"]}")
        param("SKIP_PUBLISH", "${ProtomathTeamcityPipeline_Proton_Backend_2_Build.depParamRefs["SKIP_PUBLISH"]}")
    }

    vcs {
        root(ProtomathCoreApiGit)
    }

    steps {
        gradle {
            name = "create dockerfile"
            id = "RUNNER_1"

            conditions {
                equals("SKIP_PUBLISH", "false")
            }
            tasks = "createDockerfile"
            jdkHome = "%env.JDK_17%"
        }
        dockerCommand {
            name = "build image"
            id = "RUNNER_2"

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
            id = "RUNNER_3"

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
            id = "BUILD_EXT_1"
            cleanupPushedImages = true
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_3"
            }
        }
        vcsLabeling {
            id = "BUILD_EXT_2"
            vcsRootId = "${ProtomathCoreApiGit.id}"
            labelingPattern = "%CURRENT_TAG%"
            successfulOnly = true
        }
    }

    dependencies {
        dependency(ProtomathTeamcityPipeline_Proton_Backend_2_Build) {
            snapshot {
                reuseBuilds = ReuseBuilds.NO
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                id = "ARTIFACT_DEPENDENCY_1"
                cleanDestination = true
                artifactRules = "+:./out/*.* => ./artifact"
            }
        }
    }
})


object ProtomathTeamcityPipeline_Proton_Front_2 : Project({
    id("Proton_Front_2")
    name = "front"

    buildType(ProtomathTeamcityPipeline_Proton_Front_2_BuildDocker)
    buildType(ProtomathTeamcityPipeline_Proton_Front_2_DeployKuber)

    features {
        dockerRegistry {
            id = "PROJECT_EXT_4"
            name = "protonmath_Docker"
            userName = "protonmath"
            password = "zxxc0ce9241d87412ebc5a4bd5fa5ffed29"
        }
    }
})

object ProtomathTeamcityPipeline_Proton_Front_2_BuildDocker : BuildType({
    id("Proton_Front_2_BuildDocker")
    name = "Build docker"

    params {
        param("teamcity.git.fetchAllHeads", "true")
        param("DEPLOY_TAG", "v%build.number%")
    }

    vcs {
        root(ProtomathUiNewGit)
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
            vcsRootId = "${ProtomathUiNewGit.id}"
            labelingPattern = "%DEPLOY_TAG%"
            successfulOnly = true
            branchFilter = "master"
        }
    }
})

object ProtomathTeamcityPipeline_Proton_Front_2_DeployKuber : BuildType({
    id("Proton_Front_2_DeployKuber")
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
