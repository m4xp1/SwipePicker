package one.xcorp.swipepicker

import android.text.InputFilter
import android.text.Spanned
import java.util.*

class TimeInputFilter(val is24Hour: Boolean = false) : InputFilter {

    private val input = StringBuilder()
    private val legalTree by lazy { generateLegalTree() }

    override fun filter(
            source: CharSequence, start: Int, end: Int,
            dest: Spanned, dstart: Int, dend: Int): CharSequence? {
        input.replace(0, input.length, dest.toString())
        input.replace(dstart, dend, source.subSequence(start, end).toString())

        var node: Node? = legalTree
        for (char in input) {
            node = node?.canReach(char)
            if (node == null) {
                return ""
            }
        }
        return null
    }

    private fun generateLegalTree(): Node {
        // The root of the tree doesn't contain any numbers.
        val result = Node()

        if (is24Hour) {
            // We'll be re-using these nodes, so we'll save them.
            val delimiter = generateDelimiterWithMinutes()

            // The first digit may be 0-1.
            var firstDigit = Node('0', '1')
            result.addChild(firstDigit)

            // When the first digit is 0-1, the second digit may be 0-9.
            var secondDigit = Node('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
            firstDigit.addChild(secondDigit)
            // We may now be followed by the delimiter and first minute digit.
            secondDigit.addChild(delimiter)

            // When the first digit is 0-1, we may now be followed by the delimiter.
            firstDigit.addChild(delimiter)

            // The first digit may be 2.
            firstDigit = Node('2')
            result.addChild(firstDigit)

            // When the first digit is 2, the second digit may be 0-3.
            secondDigit = Node('0', '1', '2', '3')
            firstDigit.addChild(secondDigit)
            // We must now be followed by the delimiter and first minute digit.
            secondDigit.addChild(delimiter)

            // When the first digit is 2, the second digit may be delimiter.
            firstDigit.addChild(delimiter)

            // The first digit may be 3-9.
            firstDigit = Node('3', '4', '5', '6', '7', '8', '9')
            result.addChild(firstDigit)
            // We must now be followed by the delimiter and first minute digit
            firstDigit.addChild(delimiter)
        } else {
            // We'll need to use the AM/PM node a lot. Set up AM and PM.
            val ampm = Node(' ')
            val firstChar = Node('A', 'a', 'P', 'p')
            ampm.addChild(firstChar)
            val secondChar = Node('M', 'm')
            firstChar.addChild(secondChar)

            // We'll be re-using these nodes, so we'll save them.
            val delimiter = generateDelimiterWithMinutes(ampm)

            // The first hour digit may be 1.
            var firstDigit = Node('1')
            result.addChild(firstDigit)
            // We'll allow quick input of on-the-hour times.
            firstDigit.addChild(ampm)
            // When the first digit is 1, the second digit may be delimiter.
            firstDigit.addChild(delimiter)

            // When the first digit is 1, the second digit may be 0-2.
            val secondDigit = Node('0', '1', '2')
            firstDigit.addChild(secondDigit)
            // Also for quick input of on-the-hour times.
            secondDigit.addChild(ampm)
            // When the second digit is 0-2, the third digit may be delimiter.
            secondDigit.addChild(delimiter)

            // The hour digit may be 2-9.
            firstDigit = Node('2', '3', '4', '5', '6', '7', '8', '9')
            result.addChild(firstDigit)
            // We'll allow quick input of on-the-hour-times.
            firstDigit.addChild(ampm)
            // When the first digit is 2-9, the second digit may be delimiter.
            firstDigit.addChild(delimiter)
        }

        return result
    }

    private fun generateDelimiterWithMinutes(postfix: Node? = null): Node {
        val delimiter = Node(':')

        val minuteFirstDigit = Node('0', '1', '2', '3', '4', '5')
        val minuteSecondDigit = Node('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')

        // The delimiter must be followed by the first digit.
        delimiter.addChild(minuteFirstDigit)
        // The first digit must be followed by the second digit.
        minuteFirstDigit.addChild(minuteSecondDigit)

        if (postfix != null) {
            minuteSecondDigit.addChild(postfix)
        }

        return delimiter
    }

    private inner class Node(vararg chars: Char) {

        private val legalChars = chars
        private val children = ArrayList<Node>()

        fun contains(char: Char) = char in legalChars

        fun canReach(char: Char): Node? = children.find { it.contains(char) }

        fun addChild(child: Node): Node {
            children.add(child)
            return this
        }
    }
}