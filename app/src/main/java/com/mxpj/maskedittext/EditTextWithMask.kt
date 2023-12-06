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

    private var hasMaskSymbols: Boolean = false

    private var action = Action.ADD

    private enum class Action {
        ADD,
        DELETE
    }

    private var isMaskPlaceholderLast = false

    var unformattedValue = ""

    var suffix = ""

    var mask = "(miron) (##)"

    var maskPlaceholderSymbol = '#'

    init {
        //mask += maskPlaceholderSymbol
        addInputFilter()
        for((index, sym) in mask.withIndex()){
            if(sym == maskPlaceholderSymbol){
                symbolPositions.add(index)
            }
        }

        var symbolPositionsPointer = 0
        for(char in mask){
            isMaskPlaceholderLast = false
            relativePositions.add(symbolPositionsPointer)
            if(char == maskPlaceholderSymbol){
                symbolPositionsPointer++
                hasMaskSymbols = true
                isMaskPlaceholderLast = true
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
                relativePositions.last() + (dstart - getRelativeOffset())
            }

            val endRel = if(dend < relativePositions.size) {
                relativePositions[dend]
            }
            else {
                println(relativePositions.last())
                println(dend)
                println(getRelativeOffset())
                relativePositions.last() + (dend - getRelativeOffset())
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

    private fun getRelativeOffset(): Int {
        return if(!hasMaskSymbols || !isMaskPlaceholderLast){
            mask.length
        }
        else{
            mask.lastIndex
        }
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
                val cursorPosition = cursor
                s?.let {
                    s.replace(0, s.length, mask)
                }
                if(action == Action.DELETE && (s?.length ?: 0) > 0){
                    this@EditTextWithMask.setSelection(cursorPosition)
                }
                turnOffFilterFlag = false
                this@EditTextWithMask.addTextChangedListener(this)
            }
        })
    }

    private fun applyMask(cursorPosition: Int): String {
        //if(unformattedValue.isEmpty()) return ""
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
        return if(action == Action.ADD) {
            String(newString).split(maskPlaceholderSymbol)[0]
        } else {
            val newStringWithoutPlaceholders = String(newString)
                .split(maskPlaceholderSymbol)[0]
            val offset = getOffsetForFormattedPosition(newStringWithoutPlaceholders)
            newStringWithoutPlaceholders
                .substring(0,max(lastFormattedPosition + offset, cursorPosition))
        }
    }

    private fun getOffsetForFormattedPosition(
        formattedString: String,
    ): Int {
        return if(mask.isEmpty()){
            if(formattedString.length > 1) 1 else 0
        }
        else {
            if(unformattedValue.isNotEmpty()) 1 else 0
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