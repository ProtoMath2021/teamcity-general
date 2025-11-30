# TeamCity DSL Builder

## Quick Start

### Option 1: Build with multi-stage Dockerfile (self-contained)

```bash
cd .teamcity
docker build -t teamcity-dsl-build .
docker run -v ${PWD}:/teamcity-dsl teamcity-dsl-build
```

### Option 2: Use pre-built image (recommended for CI/CD)

**Build and push the pre-built image once:**

```bash
cd .teamcity
docker build -f Dockerfile.prebuilt -t protonmath/teamcity-dsl-builder:latest .
docker push protonmath/teamcity-dsl-builder:latest
```

**Use in any project:**

```bash
# Generate configs
docker run --rm -v ${PWD}:/teamcity-dsl protonmath/teamcity-dsl-builder:latest

# Just compile (validate syntax)
docker run --rm -v ${PWD}:/teamcity-dsl protonmath/teamcity-dsl-builder:latest compile -B

# Debug with verbose output
docker run --rm -v ${PWD}:/teamcity-dsl protonmath/teamcity-dsl-builder:latest compile teamcity-configs:generate -X
```

## Output

Generated configs will be placed in `target/generated-configs/` directory.

## PowerShell (Windows)

```powershell
# Build
docker build -f Dockerfile.prebuilt -t protonmath/teamcity-dsl-builder:latest .

# Run
docker run --rm -v "${PWD}:/teamcity-dsl" protonmath/teamcity-dsl-builder:latest
```
