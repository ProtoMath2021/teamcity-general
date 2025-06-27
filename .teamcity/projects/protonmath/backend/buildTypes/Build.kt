package projects.protonmath.backend.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildFeatures.vcsLabeling
import jetbrains.buildServer.configs.kotlin.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.buildSteps.nodeJS
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import projects.protonmath.backend.vcsRoots.CoreApiGit

object Build : BuildType({
    id("ProtonMath_Backend_Build")
    name = "build"

    params {
        param("env.NPM_TOKEN", "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")
        password("env.GH_TOKEN", "zxx8b917490812b4af1dee4d6c6a628840b20fec2d360dfb6bc18284b59651a4c2a47f1c6a8d404c9a4775d03cbe80d301b")
        text("CURRENT_TAG_EXPERT", "", allowEmpty = true)
    }

    vcs {
        root(CoreApiGit)
    }

    steps {
        script {
            name = "hosts"
            scriptContent = """echo "`mkdir -p ~/.ssh && chmod 700 ~/.ssh && ssh-keyscan -t rsa github.com >> ~/.ssh/known_hosts`""""
        }
        nodeJS {
            name = "getVer"
            shellScript = """
                npm install
                npm install @semantic-release/git @semantic-release/changelog -D
                npm update semantic-release @semantic-release/* --save-dev
                
                echo HELP1
                
                git config --global --add safe.directory "${'$'}(pwd)"
                
                echo HELP 
                echo %build.number%
                echo %env.GH_TOKEN%
                echo "`pwd`"
                echo "`ls -la`"
                echo "`ls -la .git/`"
                echo "getVer1" 
                
                
                
                npx semantic-release --debug --no-ci
                
                echo "`ls -la`"
            """.trimIndent()
            dockerImage = "node"
            dockerPull = true
        }
        script {
            name = "setVer"
            scriptContent = """
                echo "`ls -la`"
                git fetch --tags
                latest_tag=${'$'}(git tag --sort=version:refname | grep -v '^backend' | tail -n 1)
                echo "${'$'}latest_tag"
                echo "setVer  HELP1"
                # Set a build parameter using a service message
                echo "##teamcity[setParameter name='CURRENT_TAG_EXPERT' value='${'$'}latest_tag']"
                echo "setVer HELP2"
                
                find ./out -type f -name "*.jar" -exec rm {} \;
            """.trimIndent()
        }
        gradle {
            name = "build"
            tasks = "clean assemble"
            jdkHome = "%env.JDK_17_0%"
        }
        script {
            name = "renameJar"
            scriptContent = """
                echo "`ls -la ./out/`"
                echo "HELP renameJar"
                echo "%CURRENT_TAG_EXPERT%"
                mv ./out/*.jar ./out/amogus-%CURRENT_TAG_EXPERT%.jar
                echo "`ls -la ./out/`"
                echo "is renamed?"
                find ./out -type f ! -name "*.jar" -exec rm {} \;
                
                echo "`ls -la ./out/`"
            """.trimIndent()
        }
    }

    features {
        vcsLabeling {
            vcsRootId = "${CoreApiGit.id}"
            labelingPattern = "%env.TEAMCITY_PROJECT_NAME%-%build.vcs.number%"
            branchFilter = ""
        }
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_11"
            }
        }
    }
})
