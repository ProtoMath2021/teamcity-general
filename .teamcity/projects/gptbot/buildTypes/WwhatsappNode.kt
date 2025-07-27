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
        param("env.SEMANTIC_VERSION", "dev-%build.number%")  // Default fallback
        param("teamcity.build.branch", "refs/heads/main")  // Configurable build branch, defaults to main
    }

    vcs {
        root(WwhatsappNodeVcs)

        cleanCheckout = true
    }

    steps {
        // Extract semantic version from Git tags with commit-based version increment
        script {
            name = "Extract Git Tag Version"
            scriptContent = """
                #!/bin/bash
                set -e
                
                echo "=== Git Tag Version Extraction with Conventional Commits ==="
                echo "Current branch ref: %teamcity.build.branch%"
                echo "Current commit: $(git rev-parse HEAD)"
                
                # Extract branch name from refs (remove refs/heads/ or refs/tags/ prefix)
                BRANCH_REF="%teamcity.build.branch%"
                BRANCH_NAME=${'$'}{BRANCH_REF#refs/heads/}
                BRANCH_NAME=${'$'}{BRANCH_NAME#refs/tags/}
                echo "Branch name: ${'$'}BRANCH_NAME"
                
                # Function to increment version based on type
                increment_version() {
                    local version=${'$'}1
                    local bump_type=${'$'}2
                    
                    # Parse current version (remove 'v' prefix if present)
                    version=${'$'}{version#v}
                    
                    # Split version into parts
                    IFS='.' read -ra VERSION_PARTS <<< "${'$'}version"
                    local major=${'$'}{VERSION_PARTS[0]:-0}
                    local minor=${'$'}{VERSION_PARTS[1]:-0}
                    local patch=${'$'}{VERSION_PARTS[2]:-0}
                    
                    case ${'$'}bump_type in
                        "major")
                            major=${'$'}((major + 1))
                            minor=0
                            patch=0
                            ;;
                        "minor")
                            minor=${'$'}((minor + 1))
                            patch=0
                            ;;
                        "patch")
                            patch=${'$'}((patch + 1))
                            ;;
                    esac
                    
                    echo "${'$'}major.${'$'}minor.${'$'}patch"
                }
                
                # Function to analyze commit messages and determine version bump
                analyze_commits() {
                    local since_ref=${'$'}1
                    local has_breaking=false
                    local has_feat=false
                    local has_fix=false
                    
                    echo "🔍 Analyzing commits since ${'$'}since_ref..."
                    
                    # Get all commit messages since the reference
                    while IFS= read -r commit_msg; do
                        echo "  📝 Commit: ${'$'}commit_msg"
                        
                        # Check for breaking changes (BREAKING CHANGE or release:)
                        if [[ ${'$'}commit_msg =~ ^release:|^BREAKING[[:space:]]CHANGE:|!: ]]; then
                            has_breaking=true
                            echo "    🔥 BREAKING CHANGE detected"
                        # Check for features (feat:)
                        elif [[ ${'$'}commit_msg =~ ^feat(\(.+\))?[[:space:]]*: ]]; then
                            has_feat=true
                            echo "    ✨ Feature detected"
                        # Check for fixes and other changes
                        elif [[ ${'$'}commit_msg =~ ^(fix|docs|style|refactor|perf|test|chore|ci|build)(\(.+\))?[[:space:]]*: ]]; then
                            has_fix=true
                            echo "    🐛 Fix/Other change detected"
                        fi
                    done < <(git log --format="%s" ${'$'}{since_ref}..HEAD 2>/dev/null || echo "")
                    
                    # Determine version bump priority: major > minor > patch
                    if [ "${'$'}has_breaking" = true ]; then
                        echo "major"
                    elif [ "${'$'}has_feat" = true ]; then
                        echo "minor"
                    elif [ "${'$'}has_fix" = true ]; then
                        echo "patch"
                    else
                        echo "patch"  # Default to patch for any changes
                    fi
                }
                
                # Check if current commit has a tag
                if git describe --tags --exact-match HEAD 2>/dev/null; then
                    # We're building from a tagged commit (release)
                    TAG_VERSION=$(git describe --tags --exact-match HEAD 2>/dev/null)
                    # Remove 'v' prefix if present (v1.2.3 -> 1.2.3)
                    SEMANTIC_VERSION=${'$'}{TAG_VERSION#v}
                    echo "✅ Found exact Git tag: ${'$'}TAG_VERSION"
                    echo "📦 Release version: ${'$'}SEMANTIC_VERSION"
                    
                elif git describe --tags --abbrev=0 2>/dev/null; then
                    # We have tags in history, analyze commits to increment version
                    LATEST_TAG=$(git describe --tags --abbrev=0 2>/dev/null)
                    
                    # Filter to get only semantic version tags (v1.2.3 format), ignore pre-release tags
                    SEMANTIC_TAG=$(git tag -l | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+${'$'}' | sort -V | tail -1)
                    
                    if [ -n "${'$'}SEMANTIC_TAG" ]; then
                        # Use the latest semantic version tag as reference
                        REFERENCE_TAG="${'$'}SEMANTIC_TAG"
                        echo "📋 Latest semantic tag: ${'$'}REFERENCE_TAG"
                    else
                        # No semantic tags found, treat as fresh repository
                        echo "📋 No semantic version tags found (found: ${'$'}LATEST_TAG)"
                        COMMIT_HASH=$(git rev-parse --short HEAD)
                        
                        # Check if current commit is a release commit in fresh repository
                        HAS_RELEASE_COMMIT=false
                        CURRENT_COMMIT_MSG=$(git log -1 --format="%s" HEAD 2>/dev/null || echo "")
                        if [[ ${'$'}CURRENT_COMMIT_MSG =~ ^release: ]]; then
                            HAS_RELEASE_COMMIT=true
                            echo "🎯 RELEASE commit detected in repository without semantic tags: ${'$'}CURRENT_COMMIT_MSG"
                        fi
                        
                        if [ "${'$'}BRANCH_NAME" = "main" ] || [ "${'$'}BRANCH_NAME" = "master" ]; then
                            if [ "${'$'}HAS_RELEASE_COMMIT" = true ]; then
                                # Release commit in repo without semantic tags - clean version with 'v' prefix
                                SEMANTIC_VERSION="v0.1.0"
                                echo "🆕 No semantic tags found, creating initial release version: ${'$'}SEMANTIC_VERSION"
                            else
                                # No release commit - regular initial version
                                SEMANTIC_VERSION="0.1.0"
                                echo "🆕 No semantic tags found, using initial version: ${'$'}SEMANTIC_VERSION"
                            fi
                        else
                            CLEAN_BRANCH_NAME=$(echo "${'$'}BRANCH_NAME" | sed 's/[^a-zA-Z0-9.-]/-/g' | tr '[:upper:]' '[:lower:]')
                            SEMANTIC_VERSION="0.1.0-${'$'}CLEAN_BRANCH_NAME.%build.number%-${'$'}COMMIT_HASH"
                            echo "🆕 No semantic tags found, using initial version: ${'$'}SEMANTIC_VERSION"
                        fi
                        
                        # Exit this branch since we handled the no-semantic-tags case
                        echo "=== Final Result ==="
                        echo "🏷️  Semantic Version: ${'$'}SEMANTIC_VERSION"
                        echo "##teamcity[setParameter name='env.SEMANTIC_VERSION' value='${'$'}SEMANTIC_VERSION']"
                        
                        # Also set individual version parts for reference
                        IFS='.' read -ra VERSION_PARTS <<< "${'$'}{SEMANTIC_VERSION%%-*}"
                        echo "##teamcity[setParameter name='env.VERSION_MAJOR' value='${'$'}{VERSION_PARTS[0]:-0}']"
                        echo "##teamcity[setParameter name='env.VERSION_MINOR' value='${'$'}{VERSION_PARTS[1]:-0}']"
                        echo "##teamcity[setParameter name='env.VERSION_PATCH' value='${'$'}{VERSION_PARTS[2]:-0}']"
                        exit 0
                    fi
                    
                    COMMIT_HASH=$(git rev-parse --short HEAD)
                    COMMITS_SINCE_TAG=$(git rev-list ${'$'}{REFERENCE_TAG}..HEAD --count)
                    
                    echo "🔢 Commits since tag: ${'$'}COMMITS_SINCE_TAG"
                    
                    if [ ${'$'}COMMITS_SINCE_TAG -gt 0 ]; then
                        # Analyze commits to determine version bump
                        BUMP_TYPE=$(analyze_commits "${'$'}REFERENCE_TAG")
                        NEW_VERSION=$(increment_version "${'$'}REFERENCE_TAG" "${'$'}BUMP_TYPE")
                        
                        echo "📈 Version bump type: ${'$'}BUMP_TYPE"
                        echo "🔄 Next version would be: ${'$'}NEW_VERSION"
                        
                        # Check if any commit since last tag contains 'release:' message
                        HAS_RELEASE_COMMIT=false
                        while IFS= read -r commit_msg; do
                            if [[ ${'$'}commit_msg =~ ^release: ]]; then
                                HAS_RELEASE_COMMIT=true
                                echo "🎯 RELEASE commit detected: ${'$'}commit_msg"
                                break
                            fi
                        done < <(git log --format="%s" ${'$'}{REFERENCE_TAG}..HEAD 2>/dev/null || echo "")
                        
                        if [ "${'$'}HAS_RELEASE_COMMIT" = true ]; then
                            # Release commit found - use clean version with 'v' prefix
                            SEMANTIC_VERSION="v${'$'}NEW_VERSION"
                            echo "🏷️  Release version (clean): ${'$'}SEMANTIC_VERSION"
                        else
                            # No release commit - use pre-release format
                            if [ "${'$'}BRANCH_NAME" = "main" ] || [ "${'$'}BRANCH_NAME" = "master" ]; then
                                # Main branch: use next version + pre-release info
                                SEMANTIC_VERSION="${'$'}NEW_VERSION-alpha.${'$'}COMMITS_SINCE_TAG-${'$'}COMMIT_HASH"
                            else
                                # Feature branch: include branch name
                                CLEAN_BRANCH_NAME=$(echo "${'$'}BRANCH_NAME" | sed 's/[^a-zA-Z0-9.-]/-/g' | tr '[:upper:]' '[:lower:]')
                                SEMANTIC_VERSION="${'$'}NEW_VERSION-${'$'}CLEAN_BRANCH_NAME.${'$'}COMMITS_SINCE_TAG-${'$'}COMMIT_HASH"
                            fi
                            echo "🚧 Pre-release version: ${'$'}SEMANTIC_VERSION"
                        fi
                    else
                        # No commits since tag, use the tag version
                        SEMANTIC_VERSION=${'$'}{REFERENCE_TAG#v}
                    fi
                    
                else
                    # No tags in repository, start with 0.1.0
                    COMMIT_HASH=$(git rev-parse --short HEAD)
                    
                    # Check if current commit is a release commit in fresh repository
                    HAS_RELEASE_COMMIT=false
                    CURRENT_COMMIT_MSG=$(git log -1 --format="%s" HEAD 2>/dev/null || echo "")
                    if [[ ${'$'}CURRENT_COMMIT_MSG =~ ^release: ]]; then
                        HAS_RELEASE_COMMIT=true
                        echo "🎯 RELEASE commit detected in fresh repository: ${'$'}CURRENT_COMMIT_MSG"
                    fi
                    
                    if [ "${'$'}BRANCH_NAME" = "main" ] || [ "${'$'}BRANCH_NAME" = "master" ]; then
                        if [ "${'$'}HAS_RELEASE_COMMIT" = true ]; then
                            # Release commit in fresh repo - clean version with 'v' prefix
                            SEMANTIC_VERSION="v0.1.0"
                            echo "🆕 No tags found, creating initial release version: ${'$'}SEMANTIC_VERSION"
                        else
                            # No release commit - regular initial version
                            SEMANTIC_VERSION="0.1.0"
                            echo "🆕 No tags found, using initial version: ${'$'}SEMANTIC_VERSION"
                        fi
                    else
                        CLEAN_BRANCH_NAME=$(echo "${'$'}BRANCH_NAME" | sed 's/[^a-zA-Z0-9.-]/-/g' | tr '[:upper:]' '[:lower:]')
                        SEMANTIC_VERSION="0.1.0-${'$'}CLEAN_BRANCH_NAME.%build.number%-${'$'}COMMIT_HASH"
                        echo "🆕 No tags found, using initial version: ${'$'}SEMANTIC_VERSION"
                    fi
                fi
                
                echo "=== Final Result ==="
                echo "🏷️  Semantic Version: ${'$'}SEMANTIC_VERSION"
                echo "##teamcity[setParameter name='env.SEMANTIC_VERSION' value='${'$'}SEMANTIC_VERSION']"
                
                # Also set individual version parts for reference
                IFS='.' read -ra VERSION_PARTS <<< "${'$'}{SEMANTIC_VERSION%%-*}"
                echo "##teamcity[setParameter name='env.VERSION_MAJOR' value='${'$'}{VERSION_PARTS[0]:-0}']"
                echo "##teamcity[setParameter name='env.VERSION_MINOR' value='${'$'}{VERSION_PARTS[1]:-0}']"
                echo "##teamcity[setParameter name='env.VERSION_PATCH' value='${'$'}{VERSION_PARTS[2]:-0}']"
            """.trimIndent()
        }

        // Docker build step - uses semantic version (Docker-compatible)
        dockerCommand {
            name = "build image"
            commandType = build {
                source = file {
                    path = "Dockerfile.optimized"
                }
                // Use semantic version (now Docker-compatible)
                namesAndTags = """
                    protonmath/wwhatsapp-node:%env.SEMANTIC_VERSION%
                    protonmath/wwhatsapp-node:latest
                """.trimIndent()
            }
        }
        
        dockerCommand {
            name = "publish"
            commandType = push {
                namesAndTags = "protonmath/wwhatsapp-node:%env.SEMANTIC_VERSION%"
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
            labelingPattern = "v%env.SEMANTIC_VERSION%"
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
        // equals("env.AGENT_TYPE", "nodejs-build")
    }
})
