pluginManagement {
    repositories {
        // 本地构建加速：阿里云镜像优先，原仓库兜底。
        //
        // 但 GitHub Actions 的 runner 在美国，访问 maven.aliyun.com 会返回 502
        // （实测 com.google.devtools.ksp.gradle.plugin-2.3.10.jar → 502 Bad Gateway）。
        // Gradle 遇到 5xx 会把这个仓库标记为失效（"Repository maven is disabled"），
        // **不会**回退到 google()/mavenCentral()，整个配置阶段直接失败。
        // 所以 CI 上必须跳过镜像，走官方仓库。
        if (System.getenv("GITHUB_ACTIONS").isNullOrEmpty()) {
            maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
        }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 同上：CI 上跳过阿里云镜像
        if (System.getenv("GITHUB_ACTIONS").isNullOrEmpty()) {
            maven { url = uri("https://maven.aliyun.com/repository/google") }
            maven { url = uri("https://maven.aliyun.com/repository/public") }
        }
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "NeriPlayer"
include(":app")
fun includeOwnedLibrary(path: String) {
    include(path)
    project(path).projectDir = file("modules/${path.drop(1).replace(':', '/')}")
}

val ownedLibraryPaths = file("gradle/owned-modules.txt").readLines().filter { it.isNotBlank() }
ownedLibraryPaths.forEach(::includeOwnedLibrary)
ownedLibraryPaths.filter { it.count { character -> character == ':' } > 1 }
    .map { it.substringBeforeLast(':') }.distinct().forEach { parent ->
        project(parent).projectDir = file("modules/${parent.drop(1).replace(':', '/')}")
    }
include(":ksp-annotations")
include(":ksp-processor")
include(":accompanist-lyrics-core")
include(":accompanist-lyrics-ui")
include(":hidden-api")
includeBuild("build-logic")

project(":accompanist-lyrics-core").projectDir = file("np-submodule/accompanist-lyrics-core")
project(":accompanist-lyrics-ui").projectDir = file("np-submodule/accompanist-lyrics-ui/src")
