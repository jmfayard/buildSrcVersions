import io.kotest.matchers.shouldBe
import kotlin.test.Test

class DecamelizeTest {
    @Test
    fun `split_hello_world`() {
        decamelize("helloWorld") shouldBe "hello_world"
        decamelize("helloWorld", separator = "-") shouldBe "hello-world"
    }
}
