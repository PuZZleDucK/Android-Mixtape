package com.example.androidmixtape.compat

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModernAppIdentityContractTest {
    private val root = generateSequence(File(requireNotNull(System.getProperty("user.dir")))) { it.parentFile }
        .first { File(it, "settings.gradle.kts").isFile }
    private val buildScript = File(root, "app/build.gradle.kts").readText()

    @Test
    fun modernIsTheOnlyAppVariant() {
        assertTrue(buildScript.contains("create(\"modern\")"))
        assertFalse(buildScript.contains("create(\"legacy\")"))
        assertFalse(buildScript.contains("minSdk = 19"))
        assertTrue(buildScript.contains("minSdk = 24"))
        assertFalse(File(root, "app/src/legacy").exists())
        assertFalse(File(root, "app/src/androidTestLegacy").exists())
    }

    @Test
    fun productionApplicationIdUsesOwnedDomain() {
        assertTrue(buildScript.contains("applicationId = \"org.puzzleduck.mixtape\""))
        val deployScript = File(root, "kunlun-sync.sh").readText()
        assertTrue(deployScript.contains("KUNLUN_PACKAGE_NAME:-org.puzzleduck.mixtape"))
        // Kotlin's internal namespace is independent of the installed package ID.
        assertTrue(deployScript.contains("com.example.androidmixtape.MainActivity"))
    }
}
