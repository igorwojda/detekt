package io.gitlab.arturbosch.detekt.rules.naming

import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.assertThat
import io.gitlab.arturbosch.detekt.test.compileAndLint
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.regex.PatternSyntaxException

class FunctionNamingSpec {

    @Test
    fun `allows FunctionName as alias for suppressing`() {
        val code = """
        @Suppress("FunctionName")
        fun MY_FUN() {}
        """.trimIndent()
        assertThat(FunctionNaming().compileAndLint(code)).isEmpty()
    }

    @Test
    fun `allows anonymous functions`() {
        val code = """
        val f: (Int) -> Int = fun(i: Int): Int {
            return i + i
        }
        """.trimIndent()
        assertThat(FunctionNaming().compileAndLint(code)).isEmpty()
    }

    @Test
    fun `ignores functions in classes matching excludeClassPattern`() {
        val code = """
        class WhateverTest {
            fun SHOULD_NOT_BE_FLAGGED() {}
        }
        """.trimIndent()
        val config = TestConfig(mapOf(FunctionNaming.EXCLUDE_CLASS_PATTERN to ".*Test$"))
        assertThat(FunctionNaming(config).compileAndLint(code)).isEmpty()
    }

    @Test
    fun `flags functions inside functions`() {
        val code = """
        class C : I {
            override fun shouldNotBeFlagged() {
                fun SHOULD_BE_FLAGGED() { }
            }
        }
        interface I { fun shouldNotBeFlagged() }
        """.trimIndent()
        assertThat(FunctionNaming().compileAndLint(code)).hasStartSourceLocation(3, 13)
    }

    @Test
    fun `ignores overridden functions by default`() {
        val code = """
        class C : I {
            override fun SHOULD_NOT_BE_FLAGGED() {}
        }
        interface I { @Suppress("FunctionNaming") fun SHOULD_NOT_BE_FLAGGED() }
        """.trimIndent()
        assertThat(FunctionNaming().compileAndLint(code)).isEmpty()
    }

    @Test
    fun `does not report when the function name is identical to the type of the result`() {
        val code = """
        interface Foo
        private class FooImpl : Foo

        fun Foo(): Foo = FooImpl()
        """.trimIndent()
        val config = TestConfig(mapOf(FunctionNaming.IGNORE_OVERRIDDEN to "false"))
        assertThat(FunctionNaming(config).compileAndLint(code)).isEmpty()
    }

    @Test
    fun `flags functions with bad names inside overridden functions by default`() {
        val code = """
        class C : I {
            override fun SHOULD_BE_FLAGGED() {
                fun SHOULD_BE_FLAGGED() {}
            }
        }
        interface I { @Suppress("FunctionNaming") fun SHOULD_BE_FLAGGED() }
        """.trimIndent()
        assertThat(FunctionNaming().compileAndLint(code)).hasStartSourceLocation(3, 13)
    }

    @Test
    fun `doesn't ignore overridden functions if ignoreOverridden is false`() {
        val code = """
        class C : I {
            override fun SHOULD_BE_FLAGGED() {}
        }
        interface I { fun SHOULD_BE_FLAGGED() }
        """.trimIndent()
        val config = TestConfig(mapOf(FunctionNaming.IGNORE_OVERRIDDEN to "false"))
        assertThat(FunctionNaming(config).compileAndLint(code)).hasStartSourceLocations(
            SourceLocation(2, 18),
            SourceLocation(4, 19)
        )
    }

    @Test
    fun `doesn't allow functions with backtick`() {
        val code = """
            fun `7his is a function name _`() = Unit
        """.trimIndent()
        assertThat(FunctionNaming().compileAndLint(code)).hasStartSourceLocations(SourceLocation(1, 5))
    }

    @Test
    fun `should use custom name for method`() {
        val config = TestConfig(mapOf(FunctionNaming.FUNCTION_PATTERN to "^`.+`$"))
        assertThat(
            FunctionNaming(config).compileAndLint(
                """
        class Foo {
            fun `name with back ticks`(){
              
            }
        }
                """.trimIndent()
            )
        ).isEmpty()
    }

    @Test
    fun shouldExcludeClassesFromFunctionNaming() {
        val code = """
        class Bar {
            fun MYFun() {}
        }

        object Foo {
            fun MYFun() {}
        }
        """.trimIndent()
        val config = TestConfig(mapOf(FunctionNaming.EXCLUDE_CLASS_PATTERN to "Foo|Bar"))
        assertThat(FunctionNaming(config).compileAndLint(code)).isEmpty()
    }

    @Test
    fun `should report a function name that begins with a backtick, capitals, and spaces`() {
        val subject = FunctionNaming()
        val code = "fun `Hi bye`() = 3"
        subject.compileAndLint(code)
        assertThat(subject.findings).hasSize(1)
    }

    @Nested
    inner class `exclude class pattern function regex code cases` {
        private val excludeClassPatternFunctionRegexCode = """
            class Bar {
                fun MYFun() {}
            }

            object Foo {
                fun MYFun() {}
            }
        """.trimIndent()

        @Test
        fun shouldFailWithInvalidRegexFunctionNaming() {
            val config = TestConfig(mapOf(FunctionNaming.EXCLUDE_CLASS_PATTERN to "*Foo"))
            assertThatExceptionOfType(PatternSyntaxException::class.java).isThrownBy {
                FunctionNaming(config).compileAndLint(excludeClassPatternFunctionRegexCode)
            }
        }

        @Test
        fun shouldNotFailWithInvalidRegexWhenDisabledFunctionNaming() {
            val configRules = mapOf(
                "active" to "false",
                FunctionNaming.EXCLUDE_CLASS_PATTERN to "*Foo"
            )
            val config = TestConfig(configRules)
            assertThat(FunctionNaming(config).compileAndLint(excludeClassPatternFunctionRegexCode)).isEmpty()
        }
    }

    @Test
    fun `should not detect any`() {
        val code = """
            data class D(val i: Int, val j: Int)
            fun doStuff() {
                val (_, HOLY_GRAIL) = D(5, 4)
            }
        """.trimIndent()
        assertThat(FunctionNaming().compileAndLint(code)).isEmpty()
    }
}
