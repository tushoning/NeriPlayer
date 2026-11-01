plugins {
    id("build-logic.android.library")
}

android {
    namespace = "moe.ouom.neriplayer.nativebridge"

    defaultConfig {
        // 默认只编 armeabi-v7a（电视是 32 位）；需要别的 ABI 用 -PtargetAbis=arm64-v8a,armeabi-v7a
        val targetAbis = (project.findProperty("targetAbis") as String?)
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: listOf("armeabi-v7a")
        ndk {
            abiFilters += targetAbis
        }
        externalNativeBuild {
            cmake {
                cppFlags += listOf("-fexceptions", "-frtti")
                arguments += "-DANDROID_STL=c++_static"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}
