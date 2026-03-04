load("@rules_java//java:defs.bzl", "java_binary", "java_library", "java_plugin")

package(default_visibility = ["//visibility:public"])

java_plugin(
    name = "dagger_compiler_plugin",
    processor_class = "dagger.internal.codegen.ComponentProcessor",
    deps = [
        "@maven//:com_google_dagger_dagger_compiler",
    ],
)

java_library(
    name = "dagger",
    exported_plugins = [":dagger_compiler_plugin"],
    exports = [
        "@maven//:com_google_dagger_dagger",
        "@maven//:jakarta_inject_jakarta_inject_api",
    ],
)
