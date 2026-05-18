package ee.vstepa.jetbrains.plugins.renpy.service

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.io.File
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class RenPyNavigationService(private val project: Project) {
    private val labelCache = ConcurrentHashMap<String, Pair<String, Int>>()
    private val screenCache = ConcurrentHashMap<String, Pair<String, Int>>()
    private val variableCache = ConcurrentHashMap<String, Pair<String, Int>>()
    private var isInitialized = false

    fun initIndex() {
        val basePath = project.basePath ?: return

        // 1. Try loading from navigation.json first
        loadFromJson(basePath)

        // 2. Run fallback scanner to catch labels/screens missing from the JSON
        buildFallbackIndex(basePath)

        isInitialized = true
    }

    private fun loadFromJson(basePath: String) {
        val navFile = File(basePath, "game/saves/navigation.json")
        if (!navFile.exists()) return

        try {
            val content = navFile.readText()
            val json = Gson().fromJson(content, JsonObject::class.java)
            val location = json.getAsJsonObject("location") ?: return

            // Only parse labels
            location.getAsJsonObject("label")?.entrySet()?.forEach { (name, data) ->
                val array = data.asJsonArray
                val path = array.get(0).asString
                val line = array.get(1).asInt
                labelCache[name] = Pair(path, line)
            }

            // Only parse screens
            location.getAsJsonObject("screen")?.entrySet()?.forEach { (name, data) ->
                val array = data.asJsonArray
                val path = array.get(0).asString
                val line = array.get(1).asInt
                screenCache[name] = Pair(path, line)
            }
        } catch (e: Exception) {
            // Silently fail on malformed JSON
        }
    }

    private fun buildFallbackIndex(basePath: String) {
        val gameDir = File(basePath, "game")
        if (!gameDir.exists()) return

        val labelRegex = Regex("""^\s*label\s+([a-zA-Z0-9_]+)""")
        val screenRegex = Regex("""^\s*screen\s+([a-zA-Z0-9_]+)""")
        val varRegex = Regex("""^\s*(?:define|default)\s+([a-zA-Z0-9_]+)""")

        gameDir.walkTopDown().filter { it.isFile && it.extension == "rpy" }.forEach { file ->
            val relativePath = file.relativeTo(File(basePath)).path.replace("\\", "/")

            try {
                file.useLines { lines ->
                    lines.forEachIndexed { index, line ->
                        val labelMatch = labelRegex.find(line)
                        if (labelMatch != null) {
                            val labelName = labelMatch.groupValues[1]
                            if (!labelCache.containsKey(labelName)) {
                                labelCache[labelName] = Pair(relativePath, index + 1)
                            }
                        }

                        val screenMatch = screenRegex.find(line)
                        if (screenMatch != null) {
                            val screenName = screenMatch.groupValues[1]
                            if (!screenCache.containsKey(screenName)) {
                                screenCache[screenCache.keys.toString()] = Pair(relativePath, index + 1)
                            }
                        }

                        // Check for Variables
                        val varMatch = varRegex.find(line)
                        if (varMatch != null) {
                            val varName = varMatch.groupValues[1]
                            if (!variableCache.containsKey(varName)) {
                                variableCache[varName] = Pair(relativePath, index + 1)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Skip unreadable files
            }
        }
    }

    fun getLabelLocation(name: String): Pair<String, Int>? {
        if (!isInitialized) initIndex()
        return labelCache[name]
    }

    fun getScreenLocation(name: String): Pair<String, Int>? {
        if (!isInitialized) initIndex()
        return screenCache[name]
    }

    fun getAllLabels(): Set<String> {
        if (!isInitialized) initIndex()
        return labelCache.keys
    }

    fun getAllScreens(): Set<String> {
        if (!isInitialized) initIndex()
        return screenCache.keys
    }

    fun getAllVariables(): Set<String> {
        if (!isInitialized) initIndex()
        return variableCache.keys
    }
}