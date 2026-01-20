import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.projectFeatures.buildReportTab
import jetbrains.buildServer.configs.kotlin.projectFeatures.dockerRegistry

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

version = "2025.03"

project {
    description = "Contains all other projects"

    features {
        buildReportTab {
            id = "PROJECT_EXT_1"
            title = "Code Coverage"
            startPage = "coverage.zip!index.html"
        }
        dockerRegistry {
            id = "PROJECT_EXT_3"
            name = "Docker Registry"
            userName = "protonmath"
            password = "zxxc0ce9241d87412ebc5a4bd5fa5ffed29"
        }
        dockerRegistry {
            id = "PROJECT_EXT_4"
            name = "Private Registry"
            url = "http://registry.INTERNAL:5000"
        }
    }

    cleanup {
        baseRule {
            preventDependencyCleanup = false
        }
    }

    subProject(projects.eutrip.EutripProject)
    subProject(projects.protonmath.ProtonMathProject)
    subProject(projects.gptbot.GptbotProject)
}
