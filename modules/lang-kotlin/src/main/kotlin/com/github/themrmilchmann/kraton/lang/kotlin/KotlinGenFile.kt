package com.github.themrmilchmann.kraton.lang.kotlin

import com.github.themrmilchmann.kraton.*

fun Profile.kotlinFile(

    init: KotlinFile.() -> Unit
) = KotlinFile()
    .also(init)
    .run {  }


class KotlinFile {

}