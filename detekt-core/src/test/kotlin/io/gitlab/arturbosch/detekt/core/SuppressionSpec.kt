package io.gitlab.arturbosch.detekt.core

import io.github.detekt.test.utils.compileContentForTest
import io.github.detekt.test.utils.compileForTest
import io.github.detekt.test.utils.resourceAsPath
import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Location
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.internal.isSuppressedBy
import io.gitlab.arturbosch.detekt.core.rules.visitFile
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import io.gitlab.arturbosch.detekt.test.yamlConfig
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.lastBlockStatementOrThis
import org.jetbrains.kotlin.resolve.BindingContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class SuppressionSpec {

    @Nested
    inner class `detekt findings can be suppressed with @Suppress or @SuppressWarnings` {

        @Test
        fun `should not be suppressed by a @Deprecated annotation`() {
            assertThat(isSuppressedBy("Deprecated", "This should no longer be used")).isFalse()
        }

        @Test
        fun `should not be suppressed by a @Suppress annotation for another rule`() {
            assertThat(isSuppressedBy("Suppress", "NotATest")).isFalse()
        }

        @Test
        fun `should not be suppressed by a @SuppressWarnings annotation for another rule`() {
            assertThat(isSuppressedBy("SuppressWarnings", "NotATest")).isFalse()
        }

        @Test
        fun `should be suppressed by a @Suppress annotation for the rule`() {
            assertThat(isSuppressedBy("Suppress", "Test")).isTrue()
        }

        @Test
        fun `should be suppressed by a @SuppressWarnings annotation for the rule`() {
            assertThat(isSuppressedBy("SuppressWarnings", "Test")).isTrue()
        }

        @Test
        fun `should be suppressed by a @SuppressWarnings annotation for 'all' rules`() {
            assertThat(isSuppressedBy("Suppress", "all")).isTrue()
        }

        @Test
        fun `should be suppressed by a @SuppressWarnings annotation for 'ALL' rules`() {
            assertThat(isSuppressedBy("SuppressWarnings", "ALL")).isTrue()
        }

        @Test
        fun `should not be suppressed by a @Suppress annotation with a Checkstyle prefix`() {
            assertThat(isSuppressedBy("Suppress", "Checkstyle:Test")).isFalse()
        }

        @Test
        fun `should not be suppressed by a @SuppressWarnings annotation with a Checkstyle prefix`() {
            assertThat(isSuppressedBy("SuppressWarnings", "Checkstyle:Test")).isFalse()
        }

        @Test
        fun `should be suppressed by a @Suppress annotation with a 'Detekt' prefix`() {
            assertThat(isSuppressedBy("Suppress", "Detekt:Test")).isTrue()
        }

        @Test
        fun `should be suppressed by a @SuppressWarnings annotation with a 'Detekt' prefix`() {
            assertThat(isSuppressedBy("SuppressWarnings", "Detekt:Test")).isTrue()
        }

        @Test
        fun `should be suppressed by a @Suppress annotation with a 'detekt' prefix`() {
            assertThat(isSuppressedBy("Suppress", "detekt:Test")).isTrue()
        }

        @Test
        fun `should be suppressed by a @SuppressWarnings annotation with a 'detekt' prefix`() {
            assertThat(isSuppressedBy("SuppressWarnings", "detekt:Test")).isTrue()
        }

        @Test
        fun `should be suppressed by a @Suppress annotation with a 'detekt' prefix with a dot`() {
            assertThat(isSuppressedBy("Suppress", "detekt.Test")).isTrue()
        }

        @Test
        fun `should be suppressed by a @SuppressWarnings annotation with a 'detekt' prefix with a dot`() {
            assertThat(isSuppressedBy("SuppressWarnings", "detekt.Test")).isTrue()
        }

        @Test
        fun `should not be suppressed by a @Suppress annotation with a 'detekt' prefix with a wrong separator`() {
            assertThat(isSuppressedBy("Suppress", "detekt/Test")).isFalse()
        }

        @Test
        fun `should not be suppressed by a @SuppressWarnings annotation with a 'detekt' prefix with a wrong separator`() {
            assertThat(isSuppressedBy("SuppressWarnings", "detekt/Test")).isFalse()
        }

        @Test
        fun `should be suppressed by a @Suppress annotation with an alias`() {
            assertThat(isSuppressedBy("Suppress", "alias")).isTrue()
        }

        @Test
        fun `should be suppressed by a @SuppressWarnings annotation with an alias`() {
            assertThat(isSuppressedBy("SuppressWarnings", "alias")).isTrue()
        }
    }

    @Nested
    inner class `different suppression scenarios` {

        @Test
        fun `rule should be suppressed`() {
            val ktFile = compileForTest(resourceAsPath("/suppression/SuppressedObject.kt"))
            val rule = TestRule()
            rule.visitFile(ktFile)
            assertThat(rule.expected).isNotNull()
        }

        @Test
        fun `findings are suppressed`() {
            val ktFile = compileForTest(resourceAsPath("/suppression/SuppressedElements.kt"))
            val ruleSet = RuleSet("Test", listOf(TestLM(), TestLPL()))
            val findings = ruleSet.visitFile(ktFile, BindingContext.EMPTY)
            assertThat(findings.size).isZero()
        }

        @Test
        fun `rule should be suppressed by ALL`() {
            val ktFile = compileForTest(resourceAsPath("/suppression/SuppressedByAllObject.kt"))
            val rule = TestRule()
            rule.visitFile(ktFile)
            assertThat(rule.expected).isNotNull()
        }

        @Test
        fun `rule should be suppressed by detekt prefix in uppercase with dot separator`() {
            val ktFile = compileContentForTest(
                """
            @file:Suppress("Detekt.ALL")
            object SuppressedWithDetektPrefix {

                fun stuff() {
                    println("FAILED TEST")
                }
            }
                """.trimIndent()
            )
            val rule = TestRule()
            rule.visitFile(ktFile)
            assertThat(rule.expected).isNotNull()
        }

        @Test
        fun `rule should be suppressed by detekt prefix in lowercase with colon separator`() {
            val ktFile = compileContentForTest(
                """
            @file:Suppress("detekt:ALL")
            object SuppressedWithDetektPrefix {

                fun stuff() {
                    println("FAILED TEST")
                }
            }
                """.trimIndent()
            )
            val rule = TestRule()
            rule.visitFile(ktFile)
            assertThat(rule.expected).isNotNull()
        }

        @Test
        fun `rule should be suppressed by detekt prefix in all caps with colon separator`() {
            val ktFile = compileContentForTest(
                """
            @file:Suppress("DETEKT:ALL")
            object SuppressedWithDetektPrefix {

                fun stuff() {
                    println("FAILED TEST")
                }
            }
                """.trimIndent()
            )
            val rule = TestRule()
            rule.visitFile(ktFile)
            assertThat(rule.expected).isNotNull()
        }
    }

    @Nested
    inner class `suppression based on aliases from config property` {

        @Test
        fun `allows to declare`() {
            val ktFile = compileContentForTest(
                """
            @file:Suppress("detekt:MyTest")
            object SuppressedWithDetektPrefixAndCustomConfigBasedPrefix {

                fun stuff() {
                    println("FAILED TEST")
                }
            }
                """.trimIndent()
            )
            val rule = TestRule(TestConfig(mutableMapOf("aliases" to "[MyTest]")))
            rule.visitFile(ktFile)
            assertThat(rule.expected).isNotNull()
        }
    }

    @Nested
    inner class `suppression's via rule set id` {

        val code = """
            fun lpl(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int) = Unit
        """.trimIndent()

        val config = yamlConfig("/suppression/ruleset-suppression.yml").subConfig("complexity")

        @Test
        fun `reports without a suppression`() {
            assertThat(TestLPL(config).lint(code)).isNotEmpty()
        }

        @Test
        fun `reports with wrong suppression`() {
            assertThat(TestLPL(config).lint("""@Suppress("wrong_name_used")$code""")).isNotEmpty()
        }

        @Test
        fun `suppresses by rule set id`() {
            assertCodeIsSuppressed("""@Suppress("complexity")$code""", config)
        }

        @Test
        fun `suppresses by rule set id and detekt prefix`() {
            assertCodeIsSuppressed("""@Suppress("detekt.complexity")$code""", config)
        }

        @Test
        fun `suppresses by rule id`() {
            assertCodeIsSuppressed("""@Suppress("LongParameterList")$code""", config)
        }

        @Test
        fun `suppresses by combination of rule set and rule id`() {
            assertCodeIsSuppressed("""@Suppress("complexity.LongParameterList")$code""", config)
        }

        @Test
        fun `suppresses by combination of detekt prefix, rule set and rule id`() {
            assertCodeIsSuppressed("""@Suppress("detekt:complexity:LongParameterList")$code""", config)
        }

        @Nested
        inner class `MultiRule` {

            @Suppress("DEPRECATION")
            private lateinit var subject: io.gitlab.arturbosch.detekt.api.MultiRule

            @BeforeEach
            fun setupSubject() {
                subject = AMultiRule(config)
            }

            @ParameterizedTest
            @ValueSource(
                strings = [
                    "complexity",
                    "LongParameterList",
                    "detekt.complexity.LongParameterList"
                ]
            )
            fun `is suppressed by rule id`(ruleId: String) {
                assertThat(subject.lint("""@Suppress("$ruleId")$code""")).isEmpty()
            }
        }
    }
}

private fun assertCodeIsSuppressed(code: String, config: Config) {
    val findings = TestLPL(config).lint(code)
    assertThat(findings).isEmpty()
}

private fun isSuppressedBy(annotation: String, argument: String): Boolean {
    val annotated = """
            @$annotation("$argument")
            class Test
    """.trimIndent()
    val file = compileContentForTest(annotated)
    val annotatedClass = file.children.first { it is KtClass } as KtAnnotated
    return annotatedClass.isSuppressedBy("Test", setOf("alias"))
}

private class AMultiRule(config: Config) :
    @Suppress("DEPRECATION")
    io.gitlab.arturbosch.detekt.api.MultiRule() {
    override val rules: List<Rule> = listOf(TestLPL(config))
}

private class TestRule(config: Config = Config.empty) : Rule(config) {
    override val issue = Issue("Test", Severity.CodeSmell, "", Debt.TWENTY_MINS)
    var expected: String? = "Test"
    override fun visitClassOrObject(classOrObject: KtClassOrObject) {
        expected = null
    }
}

private class TestLM : Rule() {
    override val issue = Issue("LongMethod", Severity.CodeSmell, "", Debt.TWENTY_MINS)
    override fun visitNamedFunction(function: KtNamedFunction) {
        val start = Location.startLineAndColumn(function.funKeyword!!).line
        val end = Location.startLineAndColumn(function.lastBlockStatementOrThis()).line
        val offset = end - start
        if (offset > 10) report(CodeSmell(issue, Entity.from(function), message = ""))
    }
}

private class TestLPL(config: Config = Config.empty) : Rule(config) {
    override val issue = Issue("LongParameterList", Severity.CodeSmell, "", Debt.TWENTY_MINS)
    override fun visitNamedFunction(function: KtNamedFunction) {
        val size = function.valueParameters.size
        if (size > 5) report(CodeSmell(issue, Entity.from(function), message = ""))
    }
}
