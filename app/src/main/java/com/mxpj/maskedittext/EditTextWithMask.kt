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

    var unformattedValue = ""

    var suffix = ""

    //# 2 1    /     2 0
    var mask = ""

    var maskPlaceholderSymbol = '#'

    init {
        addInputFilter()
        for((index, sym) in mask.withIndex()){
            if(sym == maskPlaceholderSymbol){
                symbolPositions.add(index)
            }
        }

        /*
        var cumulativeRelativePositions = ArrayList<Int>()
        var symbolPositionsPointer = 0
        for(index in mask.indices){
            cumulativeRelativePositions.add(index)
            if(symbolPositions[symbolPositionsPointer] == index){
                relativePositions.add(cumulativeRelativePositions)
                cumulativeRelativePositions = ArrayList()
                symbolPositionsPointer++
            }
        }

         */

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
                //println("relative pos start ${relativePositions[dstart]}")
                relativePositions[dstart]
            }
            else {
                //println(relativePositions.last() + (dstart - mask.length))
                println("dstart $dstart mask ${mask.lastIndex}")
                println("relative pos start ${relativePositions.last() + (dstart - mask.lastIndex)}")
                relativePositions.last() + (dstart - max(mask.lastIndex, mask.length))
            }
            val endRel = if(dend < relativePositions.size) {
                //println("relative pos end ${relativePositions[dend]}")
                relativePositions[dend]
            }
            else {
                //println(relativePositions.last() + (dend - mask.length))
                //println("relative pos end ${relativePositions.last() + (dend - mask.lastIndex)}")
                relativePositions.last() + (dend - max(mask.lastIndex, mask.length))
            }
            if(dstart < dend /* && symbolPositions.contains(dend)*/){
                unformattedValue = unformattedValue.substring(0, startRel) + unformattedValue.substring(endRel, unformattedValue.length)
            }
            else {
                unformattedValue = unformattedValue.substring(0, startRel) + source + unformattedValue.substring(endRel, unformattedValue.length)
            }
            //println(unformattedValue)
            source ?: ""
        }
        filters = filters.clone().plus(inputFilter)
    }

    private fun addTextChangedListener() {
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(text: CharSequence?, cursorPosition: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                this@EditTextWithMask.removeTextChangedListener(this)
                turnOffFilterFlag = true
                val mask = applyMask()
                println(mask)
                s?.let {
                    s.replace(0, s.length, mask)
                }
                turnOffFilterFlag = false
                this@EditTextWithMask.addTextChangedListener(this)
            }
        })
    }

    private fun applyMask(): String {
        if(unformattedValue.isEmpty()) return ""
        val newString = mask.toCharArray()
        //val addedCharIndexes = ArrayList<Int>()
        var newStringSuffix = unformattedValue
        for((unformattedPosition, formattedPosition) in symbolPositions.withIndex()){
            if(unformattedValue.length <= unformattedPosition) break
            newString[formattedPosition] = unformattedValue[unformattedPosition]
            newStringSuffix = newStringSuffix.substring(1)
            //addedCharIndexes.add(unformattedPosition)
        }
        println("unformatted $unformattedValue")
        if(newStringSuffix.isNotEmpty() /*unformattedValue.length > newString.size*/){
            return String(newString).plus(
                newStringSuffix
            )
        }
        return String(newString).split(maskPlaceholderSymbol)[0]
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