package projects.gptbot.buildTypes

import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildFeatures.vcsLabeling
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs
import projects.gptbot.vcsRoots.WwhatsappNodeVcs

object WwhatsappNode : BuildType({
    id("GptbotProject_WwhatsappNode")
    name = "wwhatsapp-node"

    // Set default parameter for semantic version
    params {
        param("env.SEMANTIC_VERSION", "0.0.0-dev")  // More predictable default
        param("env.USE_FALLBACK_VERSION", "false")  // Initialize fallback flag
        param("env.SKIP_SEMANTIC_RELEASE", "false")  // Initialize skip flag
        param("env.VERSION_SOURCE", "default")  // Track version source
        param("teamcity.build.branch", "refs/heads/main")  // Configurable build branch, defaults to main
    }

    vcs {
        root(WwhatsappNodeVcs)

        cleanCheckout = true
    }

    steps {
        // Step 1: Check if building from exact Git tag
        script {
            name = "Check for Exact Git Tag"
            scriptContent = """
                #!/bin/bash
                set -euo pipefail
                
                echo "=== Checking for exact Git tag ==="
                echo "Current commit: $(git rev-parse HEAD)"
                
                if TAG_VERSION=$(git describe --tags --exact-match HEAD 2>/dev/null); then
                    echo "🏷️  Building from exact tag: ${'$'}TAG_VERSION"
                    
                    # Remove 'v' prefix if present (v1.2.3 -> 1.2.3)
                    SEMANTIC_VERSION=${'$'}{TAG_VERSION#v}
                    
                    # Validate semantic version format
                    if [[ ${'$'}SEMANTIC_VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.-]+)?(\+[a-zA-Z0-9.-]+)?${'$'} ]]; then
                        echo "✅ Valid semantic version from tag: ${'$'}SEMANTIC_VERSION"
                        echo "##teamcity[setParameter name='env.SEMANTIC_VERSION' value='${'$'}SEMANTIC_VERSION']"
                        echo "##teamcity[setParameter name='env.VERSION_SOURCE' value='git-tag']"
                        echo "##teamcity[setParameter name='env.SKIP_SEMANTIC_RELEASE' value='true']"
                    else
                        echo "⚠️  Tag has invalid semantic version format: ${'$'}SEMANTIC_VERSION"
                        echo "##teamcity[setParameter name='env.SKIP_SEMANTIC_RELEASE' value='false']"
                    fi
                else
                    echo "📝 No exact tag found, will use semantic-release"
                    echo "##teamcity[setParameter name='env.SKIP_SEMANTIC_RELEASE' value='false']"
                fi
            """.trimIndent()
        }
        
        // Step 2: Generate version using semantic-release (conditional)
        script {
            name = "Generate Version with semantic-release"
            conditions {
                equals("env.SKIP_SEMANTIC_RELEASE", "false")
            }
            scriptContent = """
                #!/bin/bash
                set -euo pipefail
                
                echo "=== Generating version using semantic-release ==="
                echo "Current branch ref: %teamcity.build.branch%"
                
                # Extract branch name from refs
                BRANCH_REF="%teamcity.build.branch%"
                BRANCH_NAME=${'$'}{BRANCH_REF#refs/heads/}
                BRANCH_NAME=${'$'}{BRANCH_NAME#refs/tags/}
                echo "Branch name: ${'$'}BRANCH_NAME"
                
                # Check if Node.js and npm are available
                if ! command -v node >/dev/null 2>&1; then
                    echo "⚠️  Node.js not found, will use fallback generation"
                    echo "##teamcity[setParameter name='env.USE_FALLBACK_VERSION' value='true']"
                    exit 0
                elif ! command -v npm >/dev/null 2>&1; then
                    echo "⚠️  npm not found, will use fallback generation"
                    echo "##teamcity[setParameter name='env.USE_FALLBACK_VERSION' value='true']"
                    exit 0
                fi
                
                # Create temporary directory for semantic-release
                TEMP_DIR=$(mktemp -d)
                trap 'cd "${'$'}OLDPWD" && rm -rf "${'$'}TEMP_DIR"' EXIT
                echo "📦 Setting up semantic-release in: ${'$'}TEMP_DIR"
                
                # Setup semantic-release configuration
                cd "${'$'}TEMP_DIR"
                ln -sf "${'$'}OLDPWD/.git" .git
                
                cat > package.json << 'EOF'
{
  "name": "wwhatsapp-node-version",
  "version": "0.0.0-development",
  "private": true,
  "repository": {
    "type": "git"
  }
}
EOF
                
                cat > .releaserc.json << 'EOF'
{
  "branches": [
    "main",
    "master",
    {
      "name": "develop",
      "prerelease": "beta"
    },
    {
      "name": "release/*",
      "prerelease": "rc"
    },
    {
      "name": "*",
      "prerelease": "alpha"
    }
  ],
  "plugins": [
    [
      "@semantic-release/commit-analyzer",
      {
        "preset": "conventionalcommits"
      }
    ],
    [
      "@semantic-release/release-notes-generator",
      {
        "preset": "conventionalcommits"
      }
    ]
  ],
  "dryRun": true
}
EOF
                
                echo "🔍 Running semantic-release to determine next version..."
                
                # Set CI environment for semantic-release
                export CI=true
                export CONTINUOUS_INTEGRATION=true
                export GITHUB_ACTIONS=false
                
                # Run semantic-release with timeout
                if OUTPUT=$(timeout 60 npx semantic-release --dry-run 2>&1); then
                    echo "✅ semantic-release completed successfully"
                    
                    # Extract version from output (try multiple patterns)
                    VERSION=""
                    VERSION=$(echo "${'$'}OUTPUT" | grep -oE "The next release version is [0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.-]+)?" | grep -oE "[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.-]+)?" | head -1 || echo "")
                    
                    if [ -z "${'$'}VERSION" ]; then
                        VERSION=$(echo "${'$'}OUTPUT" | grep -oE "Published release [0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.-]+)?" | grep -oE "[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.-]+)?" | head -1 || echo "")
                    fi
                    
                    if [ -z "${'$'}VERSION" ]; then
                        VERSION=$(echo "${'$'}OUTPUT" | grep -oE "## \[[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.-]+)?\]" | grep -oE "[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.-]+)?" | head -1 || echo "")
                    fi
                    
                    if [ -n "${'$'}VERSION" ]; then
                        echo "🏷️  semantic-release determined version: ${'$'}VERSION"
                        echo "##teamcity[setParameter name='env.SEMANTIC_VERSION' value='${'$'}VERSION']"
                        echo "##teamcity[setParameter name='env.VERSION_SOURCE' value='semantic-release']"
                        echo "##teamcity[setParameter name='env.USE_FALLBACK_VERSION' value='false']"
                    else
                        echo "⚠️  Could not extract version from semantic-release output"
                        echo "📄 Output preview:"
                        echo "${'$'}OUTPUT" | head -10
                        echo "##teamcity[setParameter name='env.USE_FALLBACK_VERSION' value='true']"
                    fi
                else
                    echo "⚠️  semantic-release failed or timed out"
                    echo "📄 Error output:"
                    echo "${'$'}OUTPUT" | tail -5
                    echo "##teamcity[setParameter name='env.USE_FALLBACK_VERSION' value='true']"
                fi
            """.trimIndent()
        }
        
        // Step 3: Generate fallback version (conditional or if needed)
        script {
            name = "Generate Fallback Version"
            conditions {
                equals("env.USE_FALLBACK_VERSION", "true")
            }
            scriptContent = """
                #!/bin/bash
                set -euo pipefail
                
                echo "=== Generating fallback version ==="
                echo "Current version: %env.SEMANTIC_VERSION%"
                echo "Fallback triggered: %env.USE_FALLBACK_VERSION%"
                
                # Extract branch name
                BRANCH_REF="%teamcity.build.branch%"
                BRANCH_NAME=${'$'}{BRANCH_REF#refs/heads/}
                BRANCH_NAME=${'$'}{BRANCH_NAME#refs/tags/}
                echo "Branch name: ${'$'}BRANCH_NAME"
                
                # Get commit hash
                COMMIT_HASH=$(git rev-parse --short HEAD)
                echo "Commit hash: ${'$'}COMMIT_HASH"
                
                # Get latest semantic version tag as base
                LATEST_TAG=$(git tag -l | grep -E '^v?[0-9]+\.[0-9]+\.[0-9]+${'$'}' | sort -V | tail -1 || echo "")
                if [ -n "${'$'}LATEST_TAG" ]; then
                    BASE_VERSION=${'$'}{LATEST_TAG#v}
                    echo "Using base version from tag: ${'$'}BASE_VERSION"
                else
                    BASE_VERSION="0.1.0"
                    echo "Using default base version: ${'$'}BASE_VERSION"
                fi
                
                # Clean branch name for version identifier
                CLEAN_BRANCH_NAME=$(echo "${'$'}BRANCH_NAME" | sed 's/[^a-zA-Z0-9.-]/-/g' | tr '[:upper:]' '[:lower:]' | sed 's/^-*//; s/-*${'$'}//')
                
                # Generate version based on branch type (fixed pattern matching)
                case "${'$'}BRANCH_NAME" in
                    "main"|"master")
                        SEMANTIC_VERSION="${'$'}BASE_VERSION-dev.%build.number%+${'$'}COMMIT_HASH"
                        echo "🔧 Main/master branch version: ${'$'}SEMANTIC_VERSION"
                        ;;
                    "develop")
                        SEMANTIC_VERSION="${'$'}BASE_VERSION-beta.%build.number%+${'$'}COMMIT_HASH"
                        echo "🧪 Develop branch version: ${'$'}SEMANTIC_VERSION"
                        ;;
                    "release/"*)
                        SEMANTIC_VERSION="${'$'}BASE_VERSION-rc.%build.number%+${'$'}COMMIT_HASH"
                        echo "🚀 Release branch version: ${'$'}SEMANTIC_VERSION"
                        ;;
                    *)
                        SEMANTIC_VERSION="${'$'}BASE_VERSION-${'$'}CLEAN_BRANCH_NAME.%build.number%+${'$'}COMMIT_HASH"
                        echo "🌿 Feature branch version: ${'$'}SEMANTIC_VERSION"
                        ;;
                esac
                
                echo "##teamcity[setParameter name='env.SEMANTIC_VERSION' value='${'$'}SEMANTIC_VERSION']"
                echo "##teamcity[setParameter name='env.VERSION_SOURCE' value='fallback']"
            """.trimIndent()
        }
        
        // Step 4: Set version metadata (always runs)
        script {
            name = "Set Version Metadata"
            scriptContent = """
                #!/bin/bash
                set -euo pipefail
                
                echo "=== Setting version metadata ==="
                
                SEMANTIC_VERSION="%env.SEMANTIC_VERSION%"
                echo "Current semantic version: ${'$'}SEMANTIC_VERSION"
                
                # Check if we still have the default version (no previous step succeeded)
                if [ "${'$'}SEMANTIC_VERSION" = "dev-%build.number%" ]; then
                    echo "⚠️  No version was generated by previous steps, creating emergency fallback"
                    
                    # Extract branch name for emergency fallback
                    BRANCH_REF="%teamcity.build.branch%"
                    BRANCH_NAME=${'$'}{BRANCH_REF#refs/heads/}
                    BRANCH_NAME=${'$'}{BRANCH_NAME#refs/tags/}
                    COMMIT_HASH=$(git rev-parse --short HEAD)
                    
                    # Clean branch name
                    CLEAN_BRANCH_NAME=$(echo "${'$'}BRANCH_NAME" | sed 's/[^a-zA-Z0-9.-]/-/g' | tr '[:upper:]' '[:lower:]' | sed 's/^-*//; s/-*${'$'}//')
                    
                    if [ "${'$'}BRANCH_NAME" = "main" ] || [ "${'$'}BRANCH_NAME" = "master" ]; then
                        SEMANTIC_VERSION="0.1.0-emergency.%build.number%+${'$'}COMMIT_HASH"
                    else
                        SEMANTIC_VERSION="0.1.0-emergency-${'$'}CLEAN_BRANCH_NAME.%build.number%+${'$'}COMMIT_HASH"
                    fi
                    
                    echo "##teamcity[setParameter name='env.SEMANTIC_VERSION' value='${'$'}SEMANTIC_VERSION']"
                    echo "##teamcity[setParameter name='env.VERSION_SOURCE' value='emergency']"
                fi
                
                # Final validation of version format
                if [[ ! ${'$'}SEMANTIC_VERSION =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9.-]+)?(\+[a-zA-Z0-9.-]+)?${'$'} ]]; then
                    echo "❌ Invalid final version format: ${'$'}SEMANTIC_VERSION"
                    echo "🔄 Creating final emergency fallback"
                    COMMIT_HASH=$(git rev-parse --short HEAD)
                    SEMANTIC_VERSION="0.1.0-invalid.%build.number%+${'$'}COMMIT_HASH"
                    echo "##teamcity[setParameter name='env.SEMANTIC_VERSION' value='${'$'}SEMANTIC_VERSION']"
                    echo "##teamcity[setParameter name='env.VERSION_SOURCE' value='emergency-invalid']"
                fi
                
                echo "✅ Final semantic version: ${'$'}SEMANTIC_VERSION"
                
                # Extract base version (major.minor.patch)
                BASE_VERSION=${'$'}{SEMANTIC_VERSION%%-*}
                IFS='.' read -ra VERSION_PARTS <<< "${'$'}BASE_VERSION"
                
                echo "##teamcity[setParameter name='env.VERSION_MAJOR' value='${'$'}{VERSION_PARTS[0]:-0}']"
                echo "##teamcity[setParameter name='env.VERSION_MINOR' value='${'$'}{VERSION_PARTS[1]:-0}']"
                echo "##teamcity[setParameter name='env.VERSION_PATCH' value='${'$'}{VERSION_PARTS[2]:-0}']"
                
                # Extract pre-release identifier if present
                if [[ "${'$'}SEMANTIC_VERSION" == *"-"* ]]; then
                    PRERELEASE=${'$'}{SEMANTIC_VERSION#*-}
                    PRERELEASE=${'$'}{PRERELEASE%+*}
                    echo "##teamcity[setParameter name='env.VERSION_PRERELEASE' value='${'$'}PRERELEASE']"
                    echo "Pre-release: ${'$'}PRERELEASE"
                else
                    echo "##teamcity[setParameter name='env.VERSION_PRERELEASE' value='']"
                fi
                
                # Extract build metadata if present
                if [[ "${'$'}SEMANTIC_VERSION" == *"+"* ]]; then
                    BUILD_METADATA=${'$'}{SEMANTIC_VERSION#*+}
                    echo "##teamcity[setParameter name='env.VERSION_BUILD' value='${'$'}BUILD_METADATA']"
                    echo "Build metadata: ${'$'}BUILD_METADATA"
                else
                    echo "##teamcity[setParameter name='env.VERSION_BUILD' value='']"
                fi
                
                # Check if version is Docker-compatible (no + characters in main version)
                DOCKER_VERSION=${'$'}{SEMANTIC_VERSION//+/-}  # Replace + with - for Docker compatibility
                if [ "${'$'}DOCKER_VERSION" != "${'$'}SEMANTIC_VERSION" ]; then
                    echo "🐳 Docker-compatible version: ${'$'}DOCKER_VERSION"
                    echo "##teamcity[setParameter name='env.DOCKER_VERSION' value='${'$'}DOCKER_VERSION']"
                else
                    echo "##teamcity[setParameter name='env.DOCKER_VERSION' value='${'$'}SEMANTIC_VERSION']"
                fi
                
                echo "✅ Version metadata set successfully"
                echo "   Version: ${'$'}SEMANTIC_VERSION"
                echo "   Docker Version: ${'$'}DOCKER_VERSION"
                echo "   Source: %env.VERSION_SOURCE%"
                echo "   Major: ${'$'}{VERSION_PARTS[0]:-0}"
                echo "   Minor: ${'$'}{VERSION_PARTS[1]:-0}"
                echo "   Patch: ${'$'}{VERSION_PARTS[2]:-0}"
            """.trimIndent()
        }

        // Docker build step - uses Docker-compatible semantic version
        dockerCommand {
            name = "build image"
            commandType = build {
                source = file {
                    path = "Dockerfile.optimized"
                }
                // Use Docker-compatible version (+ replaced with -)
                namesAndTags = """
                    protonmath/wwhatsapp-node:%env.DOCKER_VERSION%
                    protonmath/wwhatsapp-node:latest
                """.trimIndent()
            }
        }
        
        dockerCommand {
            name = "publish"
            commandType = push {
                namesAndTags = "protonmath/wwhatsapp-node:%env.DOCKER_VERSION%"
            }
        }
        
        // Publish latest tag only for main branch
        dockerCommand {
            name = "publish latest tag"
            commandType = push {
                namesAndTags = "protonmath/wwhatsapp-node:latest"
            }
            conditions {
                matches("teamcity.build.branch", "refs/heads/(main|master)")
            }
        }
    }

    triggers {
        vcs {
            branchFilter = ""
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
            vcsRootId = "${WwhatsappNodeVcs.id}"
            labelingPattern = "v%env.SEMANTIC_VERSION%"  // Add 'v' prefix for Git tags
            successfulOnly = true
            branchFilter = """
                +:refs/heads/main
                +:refs/heads/master
            """.trimIndent()
        }
    }

    requirements {
        // Require Docker capability for building and pushing images
        exists("docker.server.version")
        // Prefer nodejs-build agents but allow any agent with Docker
        equals("env.AGENT_TYPE", "nodejs-build")
    }
})
