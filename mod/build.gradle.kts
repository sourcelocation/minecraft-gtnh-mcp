plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

val distributableJar = tasks.named("reobfJar")

tasks.register<Copy>("stageModJar") {
    dependsOn(distributableJar)
    from(distributableJar.map { it.outputs.files })
    into(layout.projectDirectory.dir("../dist"))
    rename { "gtnh-mcp-mod.jar" }
}
