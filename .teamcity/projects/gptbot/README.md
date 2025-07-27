# Automated Semantic Versioning with Conventional Commits

This document explains the automated semantic versioning system implemented in the TeamCity build configuration for the `wwhatsapp-node` project. The system automatically determines version increments based on commit message patterns following the [Conventional Commits](https://www.conventionalcommits.org/) specification.

## 🎯 Overview

The build system analyzes commit messages since the last Git tag to automatically determine the appropriate semantic version increment, eliminating manual version management while ensuring consistent versioning practices.

## 📋 Commit Message Patterns

### Version Increment Rules

| Commit Pattern | Version Impact | Example | Version Change |
|----------------|----------------|---------|----------------|
| `release: <message>` | **Clean Release** (vX.Y.Z) | `release: version 1.3.0` | `1.2.3` → `v1.3.0` |
| `BREAKING CHANGE:` | **Major** (X.0.0) | `feat!: remove deprecated endpoints` | `1.2.3` → `2.0.0-alpha.1` |
| `feat: <message>` | **Minor** (x.Y.0) | `feat: add user authentication` | `1.2.3` → `1.3.0-alpha.1` |
| `fix: <message>` | **Patch** (x.y.Z) | `fix: resolve login timeout` | `1.2.3` → `1.2.4-alpha.1` |
| `docs:`, `style:`, etc. | **Patch** (x.y.Z) | `docs: update API documentation` | `1.2.3` → `1.2.4-alpha.1` |

### Supported Conventional Commit Types

#### Clean Release Version (Release Commits)
- `release:` - **Triggers clean version format with 'v' prefix**
- **Automatically creates Git tags** via VCS labeling feature
- **Used for final releases** - no alpha/pre-release suffix

#### Major Version Increment (Breaking Changes)
- `BREAKING CHANGE:` - API breaking changes  
- `feat!:` - Breaking feature changes (with exclamation mark)
- **Creates pre-release versions** with alpha suffix until tagged

#### Minor Version Increment (New Features)
- `feat:` - New features
- `feat(scope):` - Scoped new features
- **Creates pre-release versions** with alpha suffix until tagged

#### Patch Version Increment (Bug Fixes & Maintenance)
- `fix:` - Bug fixes
- `docs:` - Documentation changes
- `style:` - Code style changes (formatting, missing semicolons, etc.)
- `refactor:` - Code refactoring without functionality changes
- `perf:` - Performance improvements
- `test:` - Adding or updating tests
- `chore:` - Maintenance tasks, dependency updates
- `ci:` - CI/CD configuration changes
- `build:` - Build system or external dependency changes

## 🔄 Versioning Logic Flow

### 1. Tagged Commit (Release Build)
```bash
# Current commit has a Git tag (created automatically by VCS labeling)
git tag v1.2.3
git push origin v1.2.3

# TeamCity Output:
# ✅ Found exact Git tag: v1.2.3
# 📦 Release version: 1.2.3
# 🏷️ Docker images (main branch):
#    - protonmath/wwhatsapp-node:1.2.3
#    - protonmath/wwhatsapp-node:latest ✅
```

### 2. Release Commit (Clean Version)
```bash
# Latest tag: v1.2.3
# New commits with release trigger:
git commit -m "feat: add dark mode support"
git commit -m "fix: resolve theme switching bug"
git commit -m "release: version 1.3.0 with dark mode"

# TeamCity Analysis:
# 🔍 Analyzing commits since v1.2.3...
#   📝 Commit: feat: add dark mode support
#     ✨ Feature detected
#   📝 Commit: fix: resolve theme switching bug
#     🐛 Fix/Other change detected
#   📝 Commit: release: version 1.3.0 with dark mode
#     🎯 RELEASE commit detected
# 📈 Version bump type: minor (feat detected)
# 🔄 Next version would be: 1.3.0
# 🏷️ Release version (clean): v1.3.0
# 🏷️ VCS labeling will create Git tag: v1.3.0
```

### 3. Development Build (Pre-release)
```bash
# Latest tag: v1.2.3
# New commits without release trigger:
git commit -m "feat: add user preferences"
git commit -m "fix: resolve minor bug"

# TeamCity Analysis:
# 🔍 Analyzing commits since v1.2.3...
#   📝 Commit: feat: add user preferences
#     ✨ Feature detected
#   📝 Commit: fix: resolve minor bug
#     🐛 Fix/Other change detected
# 📈 Version bump type: minor (feat detected)
# 🔄 Next version would be: 1.3.0
# 🚧 Pre-release version: 1.3.0-alpha.2-a1b2c3d
```

### 4. Fresh Repository (No Tags)
```bash
# No tags in repository
# With release commit:
git commit -m "release: initial version 0.1.0"
# TeamCity Output: v0.1.0

# Without release commit:
git commit -m "initial: setup project"
# TeamCity Output: 0.1.0
```

## 🌟 Version Format Examples

### Clean Release Versions (Release Commits)
- `v1.0.0` - Initial release with `release:` commit
- `v1.2.0` - Minor release with `release:` commit
- `v1.2.3` - Patch release with `release:` commit
- **Triggers VCS labeling** to create Git tags automatically

### Pre-Release Versions (Development)

#### Main/Master Branch (No Release Commit)
- `1.3.0-alpha.5-a1b2c3d` - 5 commits ahead of v1.2.3, next minor version
- `2.0.0-alpha.3-b2c3d4e` - 3 commits ahead with breaking changes

#### Feature Branches
- `1.3.0-feature-auth.2-c3d4e5f` - Feature branch with 2 commits
- `1.2.4-bugfix-login.1-d4e5f6a` - Bug fix branch with 1 commit

### Fresh Repository Versions
- `v0.1.0` - Initial release with `release:` commit
- `0.1.0` - Initial version without `release:` commit

## 🚀 Development Workflow

### For Feature Development
```bash
# 1. Create feature branch
git checkout -b feature/user-dashboard
git push -u origin feature/user-dashboard

# 2. Make commits with conventional messages
git commit -m "feat: add user dashboard layout"
git commit -m "feat: implement dashboard widgets"
git commit -m "fix: resolve responsive design issues"
git commit -m "test: add dashboard component tests"

# 3. TeamCity automatically builds:
# Version: 1.3.0-feature-user-dashboard.4-f1a2b3c
# Docker: protonmath/wwhatsapp-node:1.3.0-feature-user-dashboard.4-f1a2b3c
# ❌ No 'latest' tag (feature branch)
# (Next version would be 1.3.0 due to 'feat:' commits)
```

### For Bug Fixes
```bash
# 1. Create bug fix branch
git checkout -b bugfix/memory-leak
git push -u origin bugfix/memory-leak

# 2. Make commits
git commit -m "fix: resolve memory leak in session management"
git commit -m "test: add memory usage tests"

# 3. TeamCity automatically builds:
# Version: 1.2.4-bugfix-memory-leak.2-a3b4c5d
# Docker: protonmath/wwhatsapp-node:1.2.4-bugfix-memory-leak.2-a3b4c5d
# ❌ No 'latest' tag (feature branch)
# (Next version would be 1.2.4 due to 'fix:' commits)
```

### For Releases
```bash
# 1. Merge to main/master
git checkout main
git merge feature/user-dashboard
git push origin main

# 2. TeamCity builds pre-release (no release commit):
# Version: 1.3.0-alpha.6-g2h3i4j
# Docker: protonmath/wwhatsapp-node:1.3.0-alpha.6-g2h3i4j
# Docker: protonmath/wwhatsapp-node:latest ✅ (main branch)

# 3. Ready to release - add release commit:
git commit -m "release: version 1.3.0 with user dashboard and improvements"
git push origin main

# 4. TeamCity builds clean release:
# Version: v1.3.0 (clean format)
# Docker: protonmath/wwhatsapp-node:v1.3.0
# Docker: protonmath/wwhatsapp-node:latest ✅ (main branch)
# VCS Labeling: Creates Git tag v1.3.0 automatically

# 5. Subsequent builds from tag:
# Version: 1.3.0 (without 'v' prefix for exact tag builds)
```

## 🔍 Priority System

When multiple commit types exist since the last tag, the system uses this priority:

1. **Major** (Breaking changes) - Highest priority
2. **Minor** (Features) - Medium priority  
3. **Patch** (Fixes/Other) - Lowest priority

### Example: Mixed Commits
```bash
# Commits since v1.2.3:
git commit -m "fix: resolve login bug"          # Patch
git commit -m "feat: add user preferences"     # Minor  
git commit -m "release: version 1.3.0"         # Release trigger

# Result: Clean release version (v1.3.0) due to 'release:' commit
# VCS labeling automatically creates Git tag v1.3.0
```

```bash
# Commits since v1.2.3:
git commit -m "feat: add new feature"          # Minor
git commit -m "fix: resolve bug"              # Patch
git commit -m "BREAKING CHANGE: remove old API" # Major

# Result: Major pre-release version (2.0.0-alpha.3-abc123) due to breaking change
# To create clean release: add 'release: version 2.0.0' commit
```

## 🏷️ Docker Image Tagging Strategy

### Clean Release Images (Release Commits)
```bash
# For release commits on main/master branch:
protonmath/wwhatsapp-node:v1.2.3     # Clean version with 'v' prefix
protonmath/wwhatsapp-node:latest     # ✅ Latest tag included

# VCS labeling automatically creates Git tag: v1.2.3
```

### Development Images

#### Main/Master Branch (Pre-release)
```bash
# Main branch builds without release commit:
protonmath/wwhatsapp-node:1.3.0-alpha.5-a1b2c3d
protonmath/wwhatsapp-node:latest  # ✅ Latest tag included
```

#### Exact Tag Builds
```bash
# Building from existing Git tags:
protonmath/wwhatsapp-node:1.2.3      # No 'v' prefix for exact tag builds
protonmath/wwhatsapp-node:latest     # ✅ Latest tag included
```

#### Feature/Development Branches
```bash
# Feature branch builds:
protonmath/wwhatsapp-node:1.3.0-feature-auth.2-b2c3d4e  
# ❌ No latest tag - only semantic version tag
```

### Latest Tag Policy
- **`latest` tag is ONLY created for main/master branch builds**
- **Feature branches receive only semantic version tags**
- This ensures `latest` always points to the most recent main branch build
- Prevents feature branch builds from overwriting production-ready images

### Release Process
1. **Development**: Make commits with conventional messages
2. **Pre-release**: Merge to main → gets `X.Y.Z-alpha.N-hash` format
3. **Release**: Add `release: version X.Y.Z` commit → gets `vX.Y.Z` format
4. **Auto-tagging**: VCS labeling creates Git tag automatically
5. **Tagged builds**: Use exact tag → gets `X.Y.Z` format (no 'v' prefix)

## 📊 TeamCity Build Output

### Successful Analysis Example
```
=== Git Tag Version Extraction with Conventional Commits ===
Current branch: main
Current commit: a1b2c3d4e5f6789...

📋 Latest tag: v1.2.3
🔢 Commits since tag: 3
🔍 Analyzing commits since v1.2.3...
  📝 Commit: feat: add user authentication
    ✨ Feature detected
  📝 Commit: fix: resolve login timeout
    🐛 Fix/Other change detected  
  📝 Commit: docs: update API documentation
    🐛 Fix/Other change detected
📈 Version bump type: minor
🔄 Next version would be: 1.3.0
🚧 Pre-release version: 1.3.0-alpha.3+a1b2c3d

=== Final Result ===
🏷️ Semantic Version: v1.3.0 (clean release format)
```

## 🎯 Benefits

### ✅ Automated Version Management
- No manual version updates required
- Consistent versioning across all builds
- Reduces human error in version assignment

### ✅ Semantic Versioning Compliance
- Follows [SemVer](https://semver.org/) specification
- Clear distinction between breaking, feature, and patch changes
- Predictable version increments

### ✅ Git Integration
- Uses Git tags as source of truth for releases
- Includes commit hashes for traceability
- Branch-aware versioning for development workflows

### ✅ CI/CD Integration
- Seamless integration with TeamCity
- Automatic Docker image tagging
- Build parameters for additional automation

## 🛠️ Advanced Features

### Version Parameters
The build sets additional parameters for advanced use cases:
```bash
env.SEMANTIC_VERSION=1.3.0-alpha.3+a1b2c3d
env.VERSION_MAJOR=1
env.VERSION_MINOR=3  
env.VERSION_PATCH=0
```

### Branch-Specific Behavior
- **Main/Master**: Clean pre-release versions (`-alpha.N`)
- **Feature branches**: Include sanitized branch name (`-feature-name.N`)
- **All branches**: Include git commit hash (`+hash`)

## 📝 Best Practices

### Commit Message Guidelines
```bash
# ✅ Good examples:
git commit -m "feat: add user authentication system"
git commit -m "fix: resolve memory leak in session handler"
git commit -m "docs: update API documentation for v2"
git commit -m "release: version 1.3.0 with authentication and fixes"

# ❌ Avoid:
git commit -m "update stuff"
git commit -m "fixes"
git commit -m "WIP"
```

### Release Management
1. **Use feature branches** for development
2. **Merge to main** when features are complete  
3. **Add `release:` commit** when ready for clean version
4. **VCS labeling automatically creates Git tags** for release versions
5. **Use descriptive commit messages** following conventional format
6. **Review version increments** before adding release commits

## 🔧 Configuration Details

The versioning logic is implemented in the TeamCity build step `Extract Git Tag Version` using bash scripting with:

- **Git tag analysis** for current version detection
- **Commit message parsing** with regex patterns
- **Version increment calculation** based on semver rules
- **Branch-aware naming** for development versions
- **Conditional Docker tagging** - `latest` tag only for main/master branches
- **Parameter injection** for TeamCity integration

### Build Steps Overview

1. **Extract Git Tag Version** - Analyzes commits and sets semantic version
   - **Release commit detection** - Sets clean `vX.Y.Z` format for `release:` commits
   - **Pre-release formatting** - Sets `X.Y.Z-alpha.N-hash` for development
2. **Build Image** - Creates Docker image with semantic version tag
3. **Build Latest Tag** - Creates `latest` tag image (main/master only)
4. **Publish** - Pushes semantic version tag to registry
5. **Publish Latest Tag** - Pushes `latest` tag (main/master only)
6. **VCS Labeling** - Automatically creates Git tags for clean release versions

This system provides a robust, automated approach to semantic versioning that scales with your development workflow while maintaining consistency and traceability, with production-safe Docker image management and **automated Git tag creation**.
