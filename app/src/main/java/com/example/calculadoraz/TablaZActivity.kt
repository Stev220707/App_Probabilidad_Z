package com.example.calculadoraz

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.exp
import kotlin.math.abs

class TablaZActivity : AppCompatActivity() {

    private lateinit var tabla: TableLayout
    private lateinit var buscador: EditText
    private lateinit var scrollVertical: ScrollView

    private val celdas = mutableListOf<Triple<Double, TextView, Int>>()
    // Z, vista, posición Y

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabla_z)

        tabla = findViewById(R.id.tablaZ)
        buscador = findViewById(R.id.etBuscarZ)
        scrollVertical = findViewById(R.id.vScroll)

        crearEncabezado()
        llenarTabla()

        // 🔍 BUSCADOR SEGURO (NO CRASHEA)
        buscador.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val texto = s.toString()

                if (texto.isEmpty()) return

                val valor = texto.toDoubleOrNull()
                if (valor != null) {
                    buscarYResaltar(valor)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    // 🔥 ENCABEZADO
    private fun crearEncabezado() {
        val fila = TableRow(this)

        fila.addView(crearCelda("Z", Color.CYAN))

        for (i in 0..9) {
            fila.addView(crearCelda("0.$i", Color.CYAN))
        }

        tabla.addView(fila)
    }

    // 🔥 TABLA COMPLETA
    private fun llenarTabla() {

        var zBase = -3.9
        var posicionY = 0

        while (zBase <= 3.9) {

            val fila = TableRow(this)

            fila.addView(crearCelda(String.format("%.1f", zBase), Color.YELLOW))

            for (i in 0..9) {

                val z = zBase + i * 0.01
                val valor = cdf(z)

                val tv = crearCelda(String.format("%.4f", valor), Color.WHITE)

                // 📍 GUARDAR CELDA
                celdas.add(Triple(z, tv, posicionY))

                tv.setOnClickListener {
                    animarSeleccion(tv)
                    Toast.makeText(this, "Z=%.2f\nP=%.4f".format(z, valor), Toast.LENGTH_SHORT).show()
                }

                fila.addView(tv)
            }

            tabla.addView(fila)
            posicionY += 120 // altura aproximada fila
            zBase += 0.1
        }
    }

    // 🔍 BUSCAR Y HACER SCROLL
    private fun buscarYResaltar(valor: Double) {

        var mejor: Triple<Double, TextView, Int>? = null
        var minDiff = Double.MAX_VALUE

        for (celda in celdas) {
            val diff = abs(celda.first - valor)
            if (diff < minDiff) {
                minDiff = diff
                mejor = celda
            }
        }

        mejor?.let {
            animarSeleccion(it.second)

            // 🔥 SCROLL AUTOMÁTICO
            scrollVertical.post {
                scrollVertical.smoothScrollTo(0, it.third)
            }
        }
    }

    // ✨ ANIMACIÓN PRO
    private fun animarSeleccion(tv: TextView) {
        val anim = ValueAnimator.ofObject(
            ArgbEvaluator(),
            Color.TRANSPARENT,
            Color.CYAN,
            Color.TRANSPARENT
        )
        anim.duration = 600
        anim.addUpdateListener {
            tv.setBackgroundColor(it.animatedValue as Int)
        }
        anim.start()
    }

    private fun crearCelda(texto: String, color: Int): TextView {
        return TextView(this).apply {
            text = texto
            setTextColor(color)
            setPadding(16, 16, 16, 16)
            textSize = 14f
        }
    }

    // 🔥 CDF
    private fun cdf(x: Double): Double {
        val t = 1.0 / (1.0 + 0.2316419 * kotlin.math.abs(x))
        val d = 0.3989423 * exp(-x * x / 2)
        val prob = d * t * (0.3193815 + t * (-0.3565638 + t * (1.781478 + t * (-1.821256 + t * 1.330274))))
        return if (x > 0) 1 - prob else prob
    }
}