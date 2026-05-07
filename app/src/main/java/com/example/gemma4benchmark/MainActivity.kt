package com.example.gemma4benchmark

import android.app.Activity
import android.app.AlertDialog
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Debug
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var modelStatusView: TextView
    private lateinit var outputView: TextView
    private lateinit var loadButton: Button
    private lateinit var translateButton: Button
    private lateinit var colors: AppColors
    private val buttonByMode = LinkedHashMap<PromptMode, Button>()

    private var engine: Engine? = null
    private var isLoaded = false
    private var isBusy = false

    // Translation state
    private var currentEnglishCleaned: String? = null
    private var currentEnglishFormatted: String? = null
    private var currentEnglishMode: PromptMode? = null
    private var showingTranslation = false

    private val modelFile: File
        get() = File(filesDir, "gemma4_litert_benchmark/gemma-4-E2B-it.litertlm")

    // ── Theme colors ───────────────────────────────────────────────────────────

    private data class AppColors(
        val background: Int,
        val surface: Int,
        val surfaceStroke: Int,
        val primaryText: Int,
        val secondaryText: Int,
        val cardText: Int,
        val sectionLabel: Int,
        val badgeFill: Int,
        val badgeText: Int,
        val badgeStroke: Int,
        val disclaimerFill: Int,
        val disclaimerText: Int,
        val disclaimerStroke: Int,
    )

    private fun isDarkMode(): Boolean {
        val mask = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mask == Configuration.UI_MODE_NIGHT_YES
    }

    private fun resolveColors(): AppColors = if (isDarkMode()) AppColors(
        background      = Color.rgb(15,  17,  23),
        surface         = Color.rgb(28,  31,  42),
        surfaceStroke   = Color.rgb(55,  62,  80),
        primaryText     = Color.rgb(220, 228, 242),
        secondaryText   = Color.rgb(160, 172, 196),
        cardText        = Color.rgb(200, 210, 230),
        sectionLabel    = Color.rgb(120, 160, 215),
        badgeFill       = Color.rgb(18,  45,  32),
        badgeText       = Color.rgb(110, 190, 140),
        badgeStroke     = Color.rgb(35,  85,  60),
        disclaimerFill  = Color.rgb(22,  25,  35),
        disclaimerText  = Color.rgb(130, 142, 168),
        disclaimerStroke= Color.rgb(50,  56,  75),
    ) else AppColors(
        background      = Color.rgb(247, 249, 252),
        surface         = Color.WHITE,
        surfaceStroke   = Color.rgb(221, 226, 235),
        primaryText     = Color.rgb(20,  37,  63),
        secondaryText   = Color.rgb(45,  55,  72),
        cardText        = Color.rgb(30,  41,  59),
        sectionLabel    = Color.rgb(30,  60,  100),
        badgeFill       = Color.rgb(225, 244, 233),
        badgeText       = Color.rgb(24,  70,  48),
        badgeStroke     = Color.rgb(162, 213, 182),
        disclaimerFill  = Color.rgb(240, 244, 250),
        disclaimerText  = Color.rgb(80,  90,  110),
        disclaimerStroke= Color.rgb(200, 210, 228),
    )

    // ── Lifecycle ──────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        colors = resolveColors()
        window.decorView.setBackgroundColor(colors.background)
        buildUi()
        updateModelStatus("Unloaded")
        log("demo_screen_ready=true dark_mode=${isDarkMode()}")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            engine?.close()
        } catch (t: Throwable) {
            log("engine_close_error=${t::class.java.name}: ${t.message}")
        }
    }

    // ── UI construction ────────────────────────────────────────────────────────

    private fun buildUi() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
            setBackgroundColor(colors.background)
        }
        applySafeTopInset(content)

        content.addView(header("Offline Scientific Planning Explainer"))
        content.addView(badge("Hackathon offline AI demo — not the full calculator product"))
        content.addView(body("Deterministic result first. Offline Gemma 4 explanation second. The deterministic result is the source of truth."))
        content.addView(badgeGrid())

        modelStatusView = card("Model Status", "")
        content.addView(modelStatusView)

        content.addView(card("Prepared Deterministic Sample-Size Workflow", payloadText()))

        loadButton = Button(this).apply {
            text = "Load Gemma 4 Model"
            setOnClickListener { loadModel() }
        }
        content.addView(loadButton)

        content.addView(sectionLabel("Guided Explanations"))
        content.addView(body("Each button produces a different bounded explanation. No open chat. No freeform input."))

        addEnglishButton(content, PromptMode.SIMPLE)
        addEnglishButton(content, PromptMode.P1P2_SOURCES)
        addEnglishButton(content, PromptMode.STUDY_POWER)
        addEnglishButton(content, PromptMode.VERIFY)

        translateButton = Button(this).apply {
            text = "Translate"
            isEnabled = false
            setOnClickListener { handleTranslateToggle() }
        }
        content.addView(translateButton)

        content.addView(disclaimer(
            "This tool helps learners and researchers understand deterministic sample-size planning " +
            "outputs and prepare better questions for supervisors or statisticians. " +
            "Not a statistical advisor, medical advisor, or replacement for professional review."
        ))

        outputView = card("Output", initialOutputText())
        content.addView(outputView)

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(colors.background)
            addView(content)
        }
        setContentView(scrollView)
    }

    private fun applySafeTopInset(content: LinearLayout) {
        content.setOnApplyWindowInsetsListener { view, insets ->
            val topInset = insets.systemWindowInsetTop
            view.setPadding(28, maxOf(28, topInset + 18), 28, 28)
            insets
        }
        content.requestApplyInsets()
    }

    private fun addEnglishButton(content: LinearLayout, mode: PromptMode) {
        val button = Button(this).apply {
            text = mode.displayLabel
            isEnabled = false
            setOnClickListener { explainWithGemma(mode) }
        }
        buttonByMode[mode] = button
        content.addView(button)
    }

    // ── Translate toggle ───────────────────────────────────────────────────────

    private fun handleTranslateToggle() {
        if (isBusy || !isLoaded) return
        if (showingTranslation) {
            // Show English — no Gemma call, restore stored output
            showingTranslation = false
            outputView.textDirection = View.TEXT_DIRECTION_LOCALE
            outputView.text = currentEnglishFormatted ?: "No English output stored."
            updateButtonStates()
            log("show_english=true mode=${currentEnglishMode?.logName}")
        } else {
            val english = currentEnglishCleaned
            if (english == null) {
                outputView.text = "Output\n\nGenerate an English explanation first, then translate."
                return
            }
            showLanguagePicker(english)
        }
    }

    private fun showLanguagePicker(englishContent: String) {
        data class LangItem(val label: String, val subtitle: String?, val enabled: Boolean)
        val options = listOf(
            LangItem("Kurdish (Experimental)", "Validation in progress", enabled = false),
            LangItem("Arabic", null, enabled = true),
        )

        val adapter = object : BaseAdapter() {
            override fun getCount() = options.size
            override fun getItem(pos: Int) = options[pos]
            override fun getItemId(pos: Int) = pos.toLong()
            override fun isEnabled(pos: Int) = options[pos].enabled

            override fun getView(pos: Int, convertView: View?, parent: ViewGroup?): View {
                val item = options[pos]
                val row = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(56, 28, 56, 28)
                }
                row.addView(TextView(this@MainActivity).apply {
                    text = item.label
                    textSize = 16f
                    setTextColor(if (item.enabled) colors.cardText else colors.secondaryText)
                })
                item.subtitle?.let {
                    row.addView(TextView(this@MainActivity).apply {
                        text = it
                        textSize = 12f
                        setTextColor(colors.disclaimerText)
                        setPadding(0, 4, 0, 0)
                    })
                }
                return row
            }
        }

        val listView = ListView(this)
        listView.adapter = adapter

        val dialog = AlertDialog.Builder(this)
            .setTitle("Translate to")
            .setView(listView)
            .show()

        listView.setOnItemClickListener { _, _, pos, _ ->
            if (options[pos].enabled) {
                dialog.dismiss()
                translateToArabic(englishContent)
            }
        }
    }

    // ── Model loading ──────────────────────────────────────────────────────────

    private fun loadModel() {
        if (isBusy || isLoaded) return
        val file = modelFile
        updateModelStatus("Loading")
        setBusy(true)
        log("load_requested=true")
        logModelReadState()

        if (!file.exists() || !file.canRead()) {
            updateModelStatus("Error: model path is not readable")
            setBusy(false)
            log("RESULT=FAIL model path is not readable by app")
            return
        }

        Thread {
            try {
                val loadStart = SystemClock.elapsedRealtime()
                log("load_start_ms=$loadStart backend=GPU memory_before=${memorySummary()}")

                val config = EngineConfig(
                    modelPath = file.absolutePath,
                    backend = Backend.GPU(),
                    cacheDir = cacheDir.path,
                )
                val loadedEngine = Engine(config)
                loadedEngine.initialize()
                val loadMs = SystemClock.elapsedRealtime() - loadStart

                engine = loadedEngine
                isLoaded = true
                log("RESULT=MODEL_LOADED load_ms=$loadMs memory_after=${memorySummary()}")
                runOnUiThread {
                    updateModelStatus("Loaded in ${loadMs} ms")
                    setBusy(false)
                }
            } catch (t: Throwable) {
                log("RESULT=LOAD_FAIL ${t::class.java.name}: ${t.message} memory=${memorySummary()}")
                runOnUiThread {
                    updateModelStatus("Error: ${t::class.java.simpleName}")
                    setBusy(false)
                }
            }
        }.start()
    }

    // ── English generation ─────────────────────────────────────────────────────

    private fun explainWithGemma(mode: PromptMode) {
        val activeEngine = engine ?: return
        if (isBusy || !isLoaded) return

        showingTranslation = false
        currentEnglishCleaned = null
        currentEnglishFormatted = null
        currentEnglishMode = mode

        setBusy(true, activeMode = mode)
        outputView.textDirection = View.TEXT_DIRECTION_LOCALE
        outputView.text = "Output — ${mode.displayLabel}\n\nGemma 4 is generating offline on-device...\n\nNo network required."
        log("generation_requested=${mode.logName}")

        Thread {
            try {
                val genStart = SystemClock.elapsedRealtime()
                val response = activeEngine.createConversation().use { c ->
                    c.sendMessage(buildPrompt(mode)).toString()
                }
                val genMs = SystemClock.elapsedRealtime() - genStart
                val cleaned = cleanResponse(response)
                val formatted = "Output — ${mode.displayLabel}\n\n$cleaned\n\nGeneration time: ${genMs} ms | Offline on-device"

                log("RESULT=EXPLANATION_READY mode=${mode.logName} generation_ms=$genMs output_chars=${response.length} memory=${memorySummary()}")

                runOnUiThread {
                    currentEnglishCleaned = cleaned
                    currentEnglishFormatted = formatted
                    outputView.textDirection = View.TEXT_DIRECTION_LOCALE
                    outputView.text = formatted
                    setBusy(false)
                }
            } catch (t: Throwable) {
                log("RESULT=GENERATION_FAIL mode=${mode.logName} ${t::class.java.name}: ${t.message}")
                runOnUiThread {
                    outputView.text = "Output\n\nError: ${t::class.java.simpleName}\n${t.message.orEmpty()}"
                    setBusy(false)
                }
            }
        }.start()
    }

    // ── Arabic translation ─────────────────────────────────────────────────────

    private fun translateToArabic(englishContent: String) {
        val activeEngine = engine ?: return
        if (isBusy || !isLoaded) return

        setBusy(true, isTranslating = true)
        outputView.textDirection = View.TEXT_DIRECTION_LOCALE
        outputView.text = "Output (Arabic / عربي)\n\nGemma 4 is translating offline on-device...\n\nNo network required."
        log("generation_requested=arabic source_mode=${currentEnglishMode?.logName}")

        Thread {
            try {
                val genStart = SystemClock.elapsedRealtime()
                val response = activeEngine.createConversation().use { c ->
                    c.sendMessage(buildTranslationPrompt(englishContent)).toString()
                }
                val genMs = SystemClock.elapsedRealtime() - genStart
                val cleaned = cleanResponse(response)
                val formatted = "Output (Arabic / عربي)\n\n$cleaned\n\nGeneration time: ${genMs} ms | Offline on-device"

                log("RESULT=EXPLANATION_READY mode=arabic source_mode=${currentEnglishMode?.logName} generation_ms=$genMs output_chars=${response.length} memory=${memorySummary()}")

                runOnUiThread {
                    showingTranslation = true
                    outputView.textDirection = View.TEXT_DIRECTION_RTL
                    outputView.text = formatted
                    setBusy(false)
                }
            } catch (t: Throwable) {
                log("RESULT=GENERATION_FAIL mode=arabic ${t::class.java.name}: ${t.message}")
                runOnUiThread {
                    showingTranslation = false
                    outputView.text = "Output\n\nError: ${t::class.java.simpleName}\n${t.message.orEmpty()}"
                    setBusy(false)
                }
            }
        }.start()
    }

    // ── Kurdish translation ────────────────────────────────────────────────────

    private fun translateToKurdish(englishContent: String) {
        val activeEngine = engine ?: return
        if (isBusy || !isLoaded) return

        setBusy(true, isTranslating = true)
        outputView.textDirection = View.TEXT_DIRECTION_LOCALE
        outputView.text = "Output (Kurdish — Experimental / کوردی)\n\nGemma 4 is translating offline on-device...\n\nNo network required."
        log("generation_requested=kurdish source_mode=${currentEnglishMode?.logName}")

        Thread {
            try {
                val genStart = SystemClock.elapsedRealtime()
                val response = activeEngine.createConversation().use { c ->
                    c.sendMessage(buildKurdishTranslationPrompt(englishContent)).toString()
                }
                val genMs = SystemClock.elapsedRealtime() - genStart
                val cleaned = cleanResponse(response)
                val formatted = "Output (Kurdish — Experimental / کوردی)\n\n$cleaned\n\nGeneration time: ${genMs} ms | Offline on-device | Experimental"

                log("RESULT=EXPLANATION_READY mode=kurdish source_mode=${currentEnglishMode?.logName} generation_ms=$genMs output_chars=${response.length} memory=${memorySummary()}")

                runOnUiThread {
                    showingTranslation = true
                    outputView.textDirection = View.TEXT_DIRECTION_RTL
                    outputView.text = formatted
                    setBusy(false)
                }
            } catch (t: Throwable) {
                log("RESULT=GENERATION_FAIL mode=kurdish ${t::class.java.name}: ${t.message}")
                runOnUiThread {
                    showingTranslation = false
                    outputView.text = "Output\n\nError: ${t::class.java.simpleName}\n${t.message.orEmpty()}"
                    setBusy(false)
                }
            }
        }.start()
    }

    // ── State management ───────────────────────────────────────────────────────

    private fun updateModelStatus(state: String) {
        val file = modelFile
        modelStatusView.text = """
            Model Status

            State: $state
            Runtime: LiteRT-LM Android
            Backend: GPU
            Exists: ${file.exists()}
            Readable: ${file.canRead()}
            Size: ${if (file.exists()) file.length() else -1} bytes
            Path: app internal files/gemma4_litert_benchmark/gemma-4-E2B-it.litertlm
        """.trimIndent()
        loadButton.isEnabled = !isBusy && !isLoaded && file.exists() && file.canRead()
        updateButtonStates()
    }

    private fun setBusy(busy: Boolean, activeMode: PromptMode? = null, isTranslating: Boolean = false) {
        isBusy = busy
        loadButton.isEnabled = !busy && !isLoaded && modelFile.exists() && modelFile.canRead()

        if (busy) {
            buttonByMode.forEach { (mode, button) ->
                button.isEnabled = false
                button.text = if (mode == activeMode) "Generating..." else mode.displayLabel
            }
            translateButton.isEnabled = false
            translateButton.text = when {
                isTranslating -> "Translating..."
                showingTranslation -> "Show English"
                else -> "Translate"
            }
        } else {
            updateButtonStates()
        }
    }

    private fun updateButtonStates() {
        buttonByMode.forEach { (mode, button) ->
            button.text = mode.displayLabel
            button.isEnabled = !isBusy && isLoaded
        }
        val hasEnglish = currentEnglishCleaned != null
        translateButton.isEnabled = !isBusy && isLoaded && hasEnglish
        translateButton.text = if (showingTranslation) "Show English" else "Translate"
    }

    // ── Prompts ────────────────────────────────────────────────────────────────

    private fun buildTranslationPrompt(englishContent: String): String = """
        Translate the following English explanation into clear, simple Arabic.
        Rules you must follow:
        - Keep all numbers exactly as they appear. Do not change any numeric values.
        - Do not add new facts, assumptions, or recommendations.
        - Do not add medical advice or clinical recommendations.
        - Write entirely in Arabic.
        - Use the section labels below exactly.

        English explanation to translate:
        $englishContent

        Structure your Arabic output with these labeled sections only:
        ما الذي تم حسابه
        ماذا تعني الأرقام
        ملاحظة مهمة
    """.trimIndent()

    private fun buildKurdishTranslationPrompt(englishContent: String): String = """
        Translate the following English explanation into clear, simple Kurdish (Sorani).
        Rules you must follow:
        - Keep all numbers exactly as they appear. Do not change any numeric values.
        - Do not add new facts, assumptions, or recommendations.
        - Do not add medical advice or clinical recommendations.
        - Write in Kurdish Sorani script.
        - Use the section labels below exactly, translated into Kurdish Sorani.

        English explanation to translate:
        $englishContent

        Structure your Kurdish output with these labeled sections (translate section headings into Kurdish):
        What Was Calculated
        What the Numbers Mean
        Important Note
    """.trimIndent()

    private fun buildPrompt(mode: PromptMode): String {
        val system = """
            You explain deterministic sample-size calculator results for an offline Android hackathon demo.
            The deterministic payload below is the only source of truth.
            Rules you must follow:
            - Do not recalculate or override any numeric results.
            - Do not invent assumptions, citations, or data sources.
            - Do not provide professional medical, legal, or statistical advice.
            - Do not validate study design or claim clinical sufficiency.
            - Keep the answer concise, structured, and mobile-readable.
            - Use short sections with clear labels. Avoid raw markdown symbols like ** or ##.

            Deterministic payload:
            {
              "schemaVersion": 1,
              "moduleId": "statistics.two_proportion_sample_size",
              "calculationId": "two_independent_proportions_sample_size",
              "source": "deterministic_engine",
              "aiRole": "explain_only",
              "inputs": [
                {"id":"p1","label":"Group 1 adverse-outcome planning estimate","value":0.167,"unit":"proportion"},
                {"id":"p2","label":"Group 2 improved-care planning target","value":0.08,"unit":"proportion"},
                {"id":"alpha","label":"Significance level (Type I error rate)","value":0.05},
                {"id":"power","label":"Statistical power","value":0.80}
              ],
              "outputs": [
                {"id":"sample_size_per_group","label":"Sample size per group","value":221},
                {"id":"total_sample_size","label":"Total sample size","value":442}
              ],
              "planningContext": {
                "p1Note":"Inspired by published adverse-outcome rates in regional observational studies. Planning estimate only.",
                "p2Note":"Hypothetical improved-care planning target for demonstration. Not from the same study.",
                "method":"Two-proportion z-test with alpha and beta. Uses the assumed difference between p1 and p2."
              },
              "assumptions": [
                {"id":"independent_groups","text":"The two groups are independent."},
                {"id":"planning_estimates","text":"p1 and p2 are planning estimates. Their quality affects the sample-size result."},
                {"id":"no_dropout_adjustment","text":"These numbers do not include dropout or non-response adjustment."}
              ],
              "limitations": [
                {"id":"not_study_validation","text":"This does not validate study design or clinical sufficiency."},
                {"id":"not_medical_advice","text":"This is not medical advice. Values require justification before a real study."}
              ],
              "aiBoundary": {"mayRecalculate":false,"mayOverride":false,"mayAddClinicalAdvice":false}
            }
        """.trimIndent()
        return "$system\n\nGuided action:\n${mode.instruction}"
    }

    private fun cleanResponse(response: String): String = response
        .replace(Regex("[*_`]+"), "")
        .replace(Regex("^#+\\s*", RegexOption.MULTILINE), "")
        .replace(Regex("^\\s*[-•]\\s+", RegexOption.MULTILINE), "  ")
        .replace("\r\n", "\n")
        .trim()

    // ── Payload text ───────────────────────────────────────────────────────────

    private fun payloadText(): String = """
        Tool: Two-Proportion Sample Size

        p1 (planning estimate): 0.167
        p2 (planning target):   0.08
        alpha:                  0.05
        power:                  0.80

        Deterministic output:
        Sample size per group:  221
        Total sample size:      442

        Planning context:
        p1 is a planning estimate inspired by published adverse-outcome rates
        in regional observational studies. p2 is a hypothetical improved-care
        planning target for demonstration purposes only. Both are planning
        assumptions, not clinical recommendations.

        Boundary:
        This demo does not validate study design, diagnose, treat, or provide
        clinical recommendations. Values should be justified before any real study.
    """.trimIndent()

    // ── UI helpers ─────────────────────────────────────────────────────────────

    private fun header(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(colors.primaryText)
            setPadding(0, 0, 0, 10)
        }

    private fun sectionLabel(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(colors.sectionLabel)
            setPadding(0, 14, 0, 6)
        }

    private fun body(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(colors.secondaryText)
            setPadding(0, 0, 0, 14)
        }

    private fun disclaimer(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(colors.disclaimerText)
            setPadding(18, 12, 18, 12)
            background = roundedBackground(colors.disclaimerFill, colors.disclaimerStroke, 12f)
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            params.setMargins(0, 8, 0, 18)
            layoutParams = params
        }

    private fun badge(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(colors.badgeText)
            setPadding(18, 10, 18, 10)
            background = roundedBackground(colors.badgeFill, colors.badgeStroke, 18f)
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            params.setMargins(0, 0, 0, 10)
            layoutParams = params
        }

    private fun badgeGrid(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(badge("Gemma4-E2B  |  LiteRT-LM  |  GPU backend"))
            addView(badge("Runs fully offline on-device — no network"))
            addView(badge("Deterministic result is the source of truth"))
            setPadding(0, 0, 0, 6)
        }

    private fun card(title: String, text: String): TextView =
        TextView(this).apply {
            this.text = if (text.isEmpty()) title else "$title\n\n$text"
            textSize = 14f
            setTextColor(colors.cardText)
            setPadding(22, 20, 22, 20)
            background = roundedBackground(colors.surface, colors.surfaceStroke, 14f)
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            )
            params.setMargins(0, 0, 0, 18)
            layoutParams = params
        }

    private fun roundedBackground(fill: Int, stroke: Int, radius: Float): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fill)
            setStroke(2, stroke)
            cornerRadius = radius
        }

    // ── Instrumentation ────────────────────────────────────────────────────────

    private fun initialOutputText(): String = """
        Ready for Gemma 4 explanation.

        Load the model above, then choose a guided explanation button.
        Each button produces a different bounded explanation.

        The deterministic result above is the source of truth.
        Gemma 4 explains — it does not recalculate.

        This is an offline hackathon demo.
        Not professional medical or statistical advice.
    """.trimIndent()

    private fun memorySummary(): String {
        val rt = Runtime.getRuntime()
        val javaUsed = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024
        val nativeHeap = Debug.getNativeHeapAllocatedSize() / 1024 / 1024
        return "java_used=${javaUsed}MB,native_heap=${nativeHeap}MB"
    }

    private fun log(message: String) {
        android.util.Log.i("Gemma4Benchmark", "DEMO: $message")
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(Date())

    private fun logModelReadState() {
        val file = modelFile
        log("timestamp=${timestamp()}")
        log("device_model=${android.os.Build.MODEL}")
        log("android_version=${android.os.Build.VERSION.RELEASE}")
        log("files_dir=${filesDir.absolutePath}")
        log("model_exists=${file.exists()} model_can_read=${file.canRead()} model_size=${if (file.exists()) file.length() else -1}")
    }

    // ── Prompt modes ───────────────────────────────────────────────────────────

    private enum class PromptMode(
        val logName: String,
        val displayLabel: String,
        val instruction: String,
    ) {
        SIMPLE(
            logName = "simple",
            displayLabel = "Explain Simply",
            instruction = """
                Give a short, beginner-friendly explanation of what the deterministic result means.
                Use plain language. Avoid formulas. Keep it under 150 words.
                Structure your answer with these labeled sections (no markdown symbols):

                What Was Calculated
                What the Numbers Mean
                Why It Matters
                Important Note

                Do not add new numbers. Do not recalculate. Do not provide professional advice.
            """.trimIndent(),
        ),

        P1P2_SOURCES(
            logName = "p1p2_sources",
            displayLabel = "Where Do p1 and p2 Come From?",
            instruction = """
                Explain what p1 and p2 represent as planning estimates in a sample-size calculation.
                Describe the types of sources researchers commonly use to justify such values.
                Structure your answer with these labeled sections (no markdown symbols):

                What Are p1 and p2?
                Common Sources for Planning Estimates
                Why Source Quality Matters
                What You Should Do Before a Real Study

                Common source types to mention (do not invent citations or specific studies):
                published observational studies, pilot data, disease or outcome registries,
                local hospital records, population statistics, expert consensus assumptions.

                State clearly: p1 and p2 in this payload are planning estimates.
                Their quality directly affects the sample-size result.
                Values must be justified before any real study.
                Do not recommend one mandatory source. Do not claim clinical definitiveness.
            """.trimIndent(),
        ),

        STUDY_POWER(
            logName = "study_power",
            displayLabel = "Explain Study Power",
            instruction = """
                Explain alpha, beta, and statistical power as used in this sample-size calculation.
                Structure your answer with these labeled sections (no markdown symbols):

                Alpha — Type I Error
                Beta — Type II Error
                Power = 1 - Beta
                What Power 0.80 Means Here
                What Power Does Not Guarantee

                Use simple language. Explain:
                - alpha (0.05) is the acceptable false-positive risk
                - beta (0.20 for power 0.80) is the false-negative risk
                - power is the probability of detecting the assumed difference if it is real
                - power 0.80 means an 80 percent chance of detecting that difference under these assumptions
                - power does not guarantee study success or clinical significance

                Do not add new numbers. Do not recalculate. Do not provide professional advice.
            """.trimIndent(),
        ),

        VERIFY(
            logName = "verify",
            displayLabel = "What Should I Verify?",
            instruction = """
                Help the learner or researcher prepare better questions for their supervisor or statistician.
                List safe preparatory checks relevant to this sample-size planning result.
                Structure your answer with these labeled sections (no markdown symbols):

                Before Using This Estimate
                Key Planning Checks
                Design and Population Checks
                Who to Consult

                Checks to include (as practical questions):
                - Where did p1 and p2 come from? Are they from comparable populations?
                - Is the assumed difference between p1 and p2 realistic for the study context?
                - Does the study population match the source of the planning estimates?
                - What are the inclusion and exclusion criteria?
                - Has dropout, refusal, or non-response been accounted for?
                - What is the study design type (RCT, cohort, case-control)?
                - Is a two-proportion test the right method, or is another method needed?
                - Has a supervisor or qualified statistician reviewed the assumptions?

                Do not validate study design. Do not make clinical decisions. Do not replace professional review.
            """.trimIndent(),
        ),
    }
}
