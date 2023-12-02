package com.mxpj.maskedittext

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.*
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import java.lang.Integer.max
import java.util.LinkedList
import java.util.Queue

class EditTextWithMask(
    context: Context,
    attributeSet: AttributeSet
): AppCompatEditText(context, attributeSet) {

    private val textPaint = TextPaint()

    private val symbolPositions = ArrayList<Int>()

    private val relativePositions = ArrayList<Int>()

    private var turnOffFilterFlag = false

    private var action = Action.ADD

    private enum class Action {
        ADD,
        DELETE
    }

    var unformattedValue = ""

    var suffix = ""

    var mask = "miron pidoras (####) i huesos"

    var maskPlaceholderSymbol = '#'

    init {
        addInputFilter()
        for((index, sym) in mask.withIndex()){
            if(sym == maskPlaceholderSymbol){
                symbolPositions.add(index)
            }
        }

        var symbolPositionsPointer = 0
        for(char in mask){
            relativePositions.add(symbolPositionsPointer)
            if(char == maskPlaceholderSymbol){
                symbolPositionsPointer++
            }
        }
        if(relativePositions.isEmpty()){
            relativePositions.add(0)
        }

        addTextChangedListener()
    }

    private fun addInputFilter() {
        val inputFilter = InputFilter { source, start, end, dest, dstart, dend ->
            if(turnOffFilterFlag) return@InputFilter source ?: ""
            val startRel = if(dstart < relativePositions.size) {
                relativePositions[dstart]
            }
            else {
                relativePositions.last() + (dstart - max(mask.lastIndex, mask.length))
            }

            val endRel = if(dend < relativePositions.size) {
                relativePositions[dend]
            }
            else {
                relativePositions.last() + (dend - max(mask.lastIndex, mask.length))
            }

            unformattedValue = if(dstart < dend){
                action = Action.DELETE
                unformattedValue.substring(0, startRel) + unformattedValue.substring(endRel, unformattedValue.length)
            } else {
                action = Action.ADD
                unformattedValue.substring(0, startRel) + source + unformattedValue.substring(endRel, unformattedValue.length)
            }
            source ?: ""
        }
        filters = filters.clone().plus(inputFilter)
    }

    private fun addTextChangedListener() {
        addTextChangedListener(object : TextWatcher {
            var cursor: Int = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(text: CharSequence?, cursorPosition: Int, before: Int, count: Int) {
                cursor = cursorPosition
            }

            override fun afterTextChanged(s: Editable?) {
                this@EditTextWithMask.removeTextChangedListener(this)
                turnOffFilterFlag = true
                val mask = applyMask(cursor)
                println(mask)
                s?.let {
                    s.replace(0, s.length, mask)
                }
                turnOffFilterFlag = false
                this@EditTextWithMask.addTextChangedListener(this)
            }
        })
    }

    private fun applyMask(cursorPosition: Int): String {
        if(unformattedValue.isEmpty()) return ""
        val newString = mask.toCharArray()
        var newStringSuffix = unformattedValue
        var lastFormattedPosition = 0
        for((unformattedPosition, formattedPosition) in symbolPositions.withIndex()){
            if(unformattedValue.length <= unformattedPosition) break
            newString[formattedPosition] = unformattedValue[unformattedPosition]
            newStringSuffix = newStringSuffix.substring(1)
            lastFormattedPosition = formattedPosition
        }
        if(newStringSuffix.isNotEmpty()){
            return String(newString).plus(
                newStringSuffix
            )
        }
        if(action == Action.ADD) {
            return String(newString).split(maskPlaceholderSymbol)[0]
        }
        else {
            return String(newString)
                .split(maskPlaceholderSymbol)[0]
                .substring(0,max(lastFormattedPosition, cursorPosition))
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val suffixXPosition = textPaint.measureText(text.toString()) + paddingLeft + letterSpacing
        canvas?.drawText(suffix, suffixXPosition, baseline.toFloat(), textPaint)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        textPaint.color = currentTextColor
        textPaint.textSize = textSize
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.typeface = typeface
        textPaint.letterSpacing = letterSpacing
    }
}