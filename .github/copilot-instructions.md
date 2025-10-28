# TeamCity DSL Configuration Guide

## Project Architecture

This is a **TeamCity Configuration as Code** repository using Kotlin DSL to manage CI/CD pipelines for multiple projects. The structure follows TeamCity's hierarchical project organization with three main product lines: **Eutrip**, **ProtonMath**, and **Gptbot**.

### Core Structure
- **Root settings**: `.teamcity/settings.kts` - Main entry point with global Docker registry and project references
- **Project hierarchy**: Each project in `projects/{name}/Project.kt` defines VCS roots, build types, and sub-projects
- **Modular organization**: Separate `vcsRoots/` and `buildTypes/` folders for reusable components
- **Patches**: Runtime configuration changes stored in `patches/` - apply these to main DSL files, then delete

## Essential Development Patterns

### Build Type Structure
All build types follow this pattern:
```kotlin
object Backend : BuildType({
    id("Project_Backend")
    name = "Backend"
    
    vcs { root(CoreApiGit) }
    
    steps {
        gradle { tasks = "bootBuildImage"; jdkHome = "%env.JDK_21%" }
        dockerCommand { /* tag and push */ }
    }
    
    features {
        dockerSupport { loginToRegistry = on { dockerRegistryId = "PROJECT_EXT_3" } }
    }
})
```

### VCS Root Authentication
Projects use **SSH keys** for GitHub authentication stored in TeamCity:
- `uploadedKey = "project-specific-key-name"` (e.g., `eutrip-back-teamcity`, `gpt-agent-api`)
- Branch patterns: `refs/heads/main` for new projects, `refs/heads/master` for legacy

### Docker Registry Integration
Global Docker Hub registry (`PROJECT_EXT_3`) used across all builds with standardized image naming:
- Format: `protonmath/{service-name}:{version}`
- Version patterns: `%build.number%`, `latest`, `dev-%build.counter%`, `prod-%build.counter%`

### Environment-Specific Deployments
Projects distinguish dev/prod environments through:
- **Build parameters**: Different API URLs (`env.REACT_APP_API_URL`)
- **Naming conventions**: `dev-*` vs `prod-*` tags
- **Conditional steps**: Using `conditions { equals("prod", "true") }`

## Project-Specific Patterns

### Eutrip: Traditional VM Deployment
- **Backend**: Spring Boot → Docker → direct deployment
- **Frontend**: React builds with environment-specific API URLs
- **Deployment**: Ansible playbooks for VM provisioning (`DeployBackend` type)
- **Agent requirements**: Specific agent names (`"Agent 2-1"`)

### ProtonMath: Kubernetes-Native
- **Complex frontend hierarchy**: Multiple UIs (student, teacher v1/v2) as separate sub-projects
- **Semantic versioning**: Uses `semantic-release` with custom jar renaming (`amogus-%CURRENT_TAG_EXPERT%.jar`)
- **Keycloak integration**: Extensive OAuth parameters for authentication
- **Helm deployments**: Direct Kubernetes deployment via `.helm` directories

### Gptbot: Modern GitOps
- **GitHub App integration**: Automated deployments with webhook integration
- **Helm-based GitOps**: Updates `image-versions.yaml` in separate repo for deployment
- **Multi-cluster support**: `developing`, `staging`, `production` environments
- **Complex version handling**: Branch-based versioning with Docker tag safety

## Critical Developer Workflows

### DSL Development & Testing
```bash
# Debug TeamCity DSL locally
mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate
# Attach debugger to port 8000 for breakpoints
```

### Environment Variables by Project
- **JDK requirements**: `%env.JDK_21%` (Gptbot), `%env.JDK_17_0%` (ProtonMath)
- **Node.js builds**: Use `dockerImage = "node"` with `dockerPull = true`
- **Agent targeting**: `equals("env.AGENT_TYPE", "nodejs-build")` for frontend builds

### Common Troubleshooting
- **Patches folder**: Contains UI-generated changes - apply to main DSL files and delete patches
- **Missing SSH keys**: Check `uploadedKey` names match TeamCity's uploaded key identifiers
- **Docker registry failures**: Verify `dockerRegistryId` matches project features (usually `PROJECT_EXT_3` or `PROJECT_EXT_11`)
- **VCS branch mismatches**: Ensure `branch = "refs/heads/main"` matches actual repo default branch

## Integration Points

### External Dependencies
- **Custom TeamCity server**: `teamcity.devinfra.ru` for plugins and artifacts
- **Private Docker registry**: ProtonMath uses separate registry (`PROJECT_EXT_11`)
- **GitHub authentication**: Mix of SSH keys and OAuth tokens depending on project
- **Keycloak**: ProtonMath frontend builds require extensive OAuth configuration

### Cross-Project Communication
- **Shared Docker registry**: Root-level configuration inherited by most projects
- **Build triggers**: `finishBuildTrigger` chains backend→deployment builds
- **Parameter inheritance**: Environment variables passed through project hierarchy

## File Navigation Tips
- Start with `settings.kts` to understand project structure
- Check `Project.kt` files for build type and VCS root organization  
- Look in `buildTypes/` for specific CI/CD implementation details
- Review `patches/` for recent manual changes that need DSL integration