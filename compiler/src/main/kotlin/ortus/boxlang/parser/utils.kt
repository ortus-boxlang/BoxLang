package ortus.boxlang.parser

import org.apache.commons.io.ByteOrderMark
import org.apache.commons.io.input.BOMInputStream
import java.io.File
import java.io.InputStream

fun File.inputStreamWithoutBOM(): InputStream = this.inputStream().withoutBOM()

fun InputStream.withoutBOM(): InputStream = BOMInputStream.builder()
	.setInputStream(this)
	.setByteOrderMarks(ByteOrderMark.UTF_8)
	.setInclude(false)
	.get()
