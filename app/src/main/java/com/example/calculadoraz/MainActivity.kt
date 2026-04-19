package com.example.calculadoraz

import android.content.*
import android.content.res.Configuration
import android.graphics.pdf.PdfDocument
import android.os.*
import android.text.InputFilter
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {

    private val historial = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("config", MODE_PRIVATE)
        val lang = prefs.getString("lang", "es")!!
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    private fun erf(x: Double): Double {
        val sign = if (x >= 0) 1 else -1
        val t = 1.0 / (1.0 + 0.3275911 * kotlin.math.abs(x))
        val y = 1.0 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t
                - 0.284496736) * t + 0.254829592) * t * kotlin.math.exp(-x * x)
        return sign * y
    }

    private fun animarBoton(view: View) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(80)
            .withEndAction {
                view.animate().scaleX(1f).scaleY(1f).duration = 80
            }
    }

    private fun guardarHistorial() {
        val prefs = getSharedPreferences("historial", MODE_PRIVATE)
        prefs.edit().putStringSet("data", historial.toSet()).apply()
    }

    private fun cargarHistorial() {
        val prefs = getSharedPreferences("historial", MODE_PRIVATE)
        val data = prefs.getStringSet("data", emptySet())!!
        historial.clear()
        historial.addAll(data)
    }

    private fun exportarPDF(texto: String) {

        if (texto.isBlank() || texto == "Z = --") {
            Toast.makeText(this, "No hay resultado para exportar", Toast.LENGTH_SHORT).show()
            return
        }

        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(300, 600, 1).create()
        val page = pdf.startPage(pageInfo)

        val canvas = page.canvas
        val paint = android.graphics.Paint()

        paint.textSize = 16f
        canvas.drawText("Resultado Z:", 10f, 30f, paint)
        canvas.drawText(texto, 10f, 60f, paint)

        pdf.finishPage(page)

        val file = File(getExternalFilesDir(null), "resultadoZ.pdf")
        pdf.writeTo(FileOutputStream(file))
        pdf.close()

        Toast.makeText(this, "PDF guardado:\n${file.path}", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etX = findViewById<EditText>(R.id.etX)
        val etMedia = findViewById<EditText>(R.id.etMedia)
        val etDesv = findViewById<EditText>(R.id.etDesviacion)
        val btnCalc = findViewById<Button>(R.id.btnCalcular)
        val btnTabla = findViewById<Button>(R.id.btnVerTabla)
        val btnIdioma = findViewById<Button>(R.id.btnIdioma)
        val btnCopiar = findViewById<Button>(R.id.btnCopiar)
        val btnCompartir = findViewById<Button>(R.id.btnCompartir)
        val slider = findViewById<SeekBar>(R.id.sliderZ)
        val tvRes = findViewById<TextView>(R.id.tvResultado)
        val grafica = findViewById<GraficaGaussView>(R.id.graficaGauss)
        val listHistorial = findViewById<ListView>(R.id.listHistorial)

        val filtro = InputFilter.LengthFilter(10)
        listOf(etX, etMedia, etDesv).forEach {
            it.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            it.filters = arrayOf(filtro)
        }

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, historial)
        listHistorial.adapter = adapter

        cargarHistorial()
        adapter.notifyDataSetChanged()

        grafica.iniciarAnimacion()

        btnCalc.setOnClickListener {
            animarBoton(it)

            val x = etX.text.toString().toDoubleOrNull()
            val m = etMedia.text.toString().toDoubleOrNull()
            val d = etDesv.text.toString().toDoubleOrNull()

            if (x == null || m == null || d == null || d <= 0) {
                Toast.makeText(this, "Datos inválidos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val z = (x - m) / d
            val prob = 0.5 * (1 + erf(z / sqrt(2.0)))

            val resultado = "Z=%.4f | Prob=%.4f".format(z, prob)

            tvRes.text = resultado
            grafica.dibujarZ(z.toFloat())

            historial.add(0, resultado)
            adapter.notifyDataSetChanged()
            guardarHistorial()
        }

        btnCompartir.setOnClickListener {
            animarBoton(it)
            Toast.makeText(this, "Mantén presionado para exportar PDF", Toast.LENGTH_SHORT).show()
        }

        btnCompartir.setOnLongClickListener {
            animarBoton(it)
            exportarPDF(tvRes.text.toString())
            true
        }

        btnCopiar.setOnClickListener {
            animarBoton(it)
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Z", tvRes.text))
            Toast.makeText(this, "Copiado", Toast.LENGTH_SHORT).show()
        }

        btnTabla.setOnClickListener {
            animarBoton(it)
            startActivity(Intent(this, TablaZActivity::class.java))
        }

        btnIdioma.setOnClickListener {
            animarBoton(it)
            val opciones = arrayOf("Español", "English", "Français", "Português")
            AlertDialog.Builder(this)
                .setTitle("Idioma")
                .setItems(opciones) { _, which ->
                    val lang = listOf("es","en","fr","pt")[which]
                    getSharedPreferences("config", MODE_PRIVATE)
                        .edit().putString("lang", lang).apply()
                    recreate()
                }.show()
        }

        slider.max = 800
        slider.progress = 400

        slider.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val z = (progress - 400) / 100f
                grafica.dibujarZ(z)
                tvRes.text = "Z=%.2f".format(z)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
}