package com.z299studio.pbfree.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.*

class PassGenViewModel : ViewModel() {

    var upperCase = true
        set(value) {
            field = value
            if (!value && !this.lowerCase && !this.numbers && !this.chars) {
                field = true
            }
        }

    var lowerCase = true
        set(value) {
            field = value
            if (!value && !this.upperCase && !this.numbers && !this.chars) {
                field = true
            }
        }


    var numbers = true
        set(value) {
            field = value
            if (!value && !this.lowerCase && !this.upperCase && !this.chars) {
                field = true
            }
        }

    var chars = true
        set(value) {
            field = value
            if (!value && !this.lowerCase && !this.upperCase && !this.numbers) {
                field = true
            }
        }


    var length = 12
        set(value) {
            field = value
            if (value < 4) {
                field = 4
            }
            this.password.value = randomString(this)
            this.lengthText.value = field.toString()
        }

    var identifiable = true
    var phoneFriendly = false
    val password = MutableLiveData("")
    val lengthText = MutableLiveData(this.length.toString())
    val confirmed = MutableLiveData(false)
    var cancelValue = ""
    var index = -1

    companion object {
        private val RANDOM = Random()

        fun randomString(options: PassGenViewModel): String {
            val oA2Z = "ABCDEFGHJKLMNPQRSTUVWXYZ"
            val a2z = "abcdefghijkmnoprstuvwxyz"
            val digits = "23456789"
            val c1 = ".,?!'\"@&$();:/-"
            val c2 = "[]{}#%^*+=_\\~<>"
            val all = StringBuilder()
            var count = 0
            val result = CharArray(options.length)
            if (options.upperCase) {
                result[count++] = oA2Z.random()
                all.append(oA2Z)
            }
            if (options.lowerCase) {
                result[count++] = a2z.random()
                all.append(a2z)
            }
            if (options.numbers) {
                result[count++] = digits.random()
                all.append(digits)
            }
            if (options.chars) {
                result[count++] = c1.random()
                all.append(c1)
            }
            if (options.chars && !options.phoneFriendly) {
                all.append(c2)
            }
            if (!options.identifiable) {
                if (options.numbers) {
                    all.append("10")
                }
                if (options.upperCase) {
                    all.append("OI")
                }
                if (options.lowerCase) {
                    all.append("l")
                }
                if (options.chars) {
                    all.append("| ")
                }
            }
            val candidates = all.toString()
            while (count < options.length) {
                result[count++] = candidates.random()
            }
            return String(result.sortedWith { _, _ -> RANDOM.nextInt(3) - 1 }.toCharArray())
        }
    }
}