package translator

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import java.util.regex.Pattern

/**
 * Công cụ dịch file strings.xml sang nhiều ngôn ngữ sử dụng phương pháp miễn phí
 * Lưu ý: Chỉ phù hợp cho dự án nhỏ với lượng văn bản ít
 */
class StringTranslator(
    private val sourceFile: File,
    private val languages: List<Pair<String, String>>,
    private val outputDirectory: File
) {
    private val nonTranslatableStrings = mutableSetOf<String>()

    fun translate() {
        println("Bắt đầu quá trình dịch")
        println("File nguồn: ${sourceFile.absolutePath}")
        println("Ngôn ngữ hỗ trợ: ${languages.joinToString(", ") { "${it.first} (${it.second})" }}")

        val sourceDoc = loadXmlDocument(sourceFile)
        val stringNodes = sourceDoc.getElementsByTagName("string")

        // Tìm các chuỗi được đánh dấu không cần dịch
        findNonTranslatableStrings(stringNodes)

        // Dịch cho từng ngôn ngữ
        languages.forEach { (langCode, langName) ->
            translateToLanguage(langCode, langName, sourceDoc)
        }

        println("Dịch thuật hoàn tất thành công!")
    }

    private fun findNonTranslatableStrings(stringNodes: NodeList) {
        for (i in 0 until stringNodes.length) {
            val node = stringNodes.item(i)
            if (node is Element) {
                val translatable = node.getAttribute("translatable")
                if (translatable == "false") {
                    nonTranslatableStrings.add(node.getAttribute("name"))
                }
            }
        }
    }

    private fun translateToLanguage(langCode: String, langName: String, sourceDoc: Document) {
        println("Đang dịch sang $langName ($langCode)...")

        // Tạo bản sao của document gốc
        val targetDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        val resources = targetDoc.createElement("resources")
        targetDoc.appendChild(resources)

        // Xử lý tất cả các node string
        val stringNodes = sourceDoc.getElementsByTagName("string")
        for (i in 0 until stringNodes.length) {
            val sourceNode = stringNodes.item(i) as Element
            val nameAttr = sourceNode.getAttribute("name")

            // Bỏ qua các chuỗi không cần dịch
            if (nameAttr in nonTranslatableStrings) {
                val targetNode = targetDoc.createElement("string")
                targetNode.setAttribute("name", nameAttr)
                targetNode.setAttribute("translatable", "false")
                targetNode.textContent = sourceNode.textContent
                resources.appendChild(targetNode)
                continue
            }

            // Lấy nội dung cần dịch
            val sourceText = sourceNode.textContent
            if (sourceText.isBlank()) {
                val targetNode = targetDoc.createElement("string")
                targetNode.setAttribute("name", nameAttr)
                targetNode.textContent = sourceText
                resources.appendChild(targetNode)
                continue
            }

            try {
                // Bảo vệ các định dạng đặc biệt trước khi dịch
                val (textToTranslate, placeholders) = preprocessText(sourceText)

                // Trường hợp đặc biệt: Chuỗi chứa nhiều dấu nháy - giữ nguyên chuỗi gốc
                if (placeholders.entries.count { it.key.startsWith("{{QUOTE_") } > 1 && nameAttr == "sound") {
                    println("  - Giữ nguyên chuỗi đặc biệt '$nameAttr'")
                    val targetNode = targetDoc.createElement("string")
                    targetNode.setAttribute("name", nameAttr)
                    targetNode.textContent = sourceText
                    resources.appendChild(targetNode)
                    continue
                }

                // Dịch văn bản với cơ chế thử lại
                println("  - Đang dịch '$nameAttr'")
                var translatedText = ""
                var retryCount = 0
                var success = false

                while (!success && retryCount < 3) {
                    try {
                        translatedText = translateWithFreeApi(textToTranslate, "en", langCode)
                        success = true
                    } catch (e: Exception) {
                        retryCount++
                        if (retryCount < 3) {
                            println("    Thử lại lần $retryCount sau lỗi: ${e.message}")
                            Thread.sleep(1000) // Đợi 1 giây trước khi thử lại
                        } else {
                            println("    Không thể dịch sau 3 lần thử. Giữ nguyên chuỗi gốc.")
                            translatedText = textToTranslate
                        }
                    }
                }

                // Khôi phục các placeholder
                val finalText = postprocessText(translatedText, placeholders)

                // Tạo node mới
                val targetNode = targetDoc.createElement("string")
                targetNode.setAttribute("name", nameAttr)
                targetNode.textContent = finalText
                resources.appendChild(targetNode)

                // Tránh gửi quá nhiều request liên tiếp
                Thread.sleep(1500)

            } catch (e: Exception) {
                println("  ! Lỗi khi dịch '$nameAttr': ${e.message}")
                // Nếu lỗi, giữ nguyên chuỗi gốc
                val targetNode = targetDoc.createElement("string")
                targetNode.setAttribute("name", nameAttr)
                targetNode.textContent = sourceText
                resources.appendChild(targetNode)
            }
        }

        // Lưu file đã dịch
        val langDir = when {
            langCode == "zh-CN" -> File(outputDirectory, "values-zh-rCN")
            langCode == "zh-TW" -> File(outputDirectory, "values-zh-rTW")
            // Xử lý các định dạng XX-rYY
            langCode.contains("-") -> {
                val parts = langCode.split("-")
                if (parts.size == 2) {
                    File(outputDirectory, "values-${parts[0]}-r${parts[1]}")
                } else {
                    File(outputDirectory, "values-$langCode")
                }
            }
            else -> File(outputDirectory, "values-$langCode")
        }

        if (!langDir.exists()) {
            langDir.mkdirs()
        }

        val outputFile = File(langDir, "strings.xml")
        writeXmlDocument(targetDoc, outputFile)
        println("Đã tạo ${outputFile.absolutePath}")
    }

    private fun translateWithFreeApi(text: String, fromLang: String, toLang: String): String {
        if (text.isBlank()) return text

        try {
            val encodedText = URLEncoder.encode(text, "UTF-8")

            // Chuyển đổi định dạng ngôn ngữ
            var apiLangCode = toLang
            if (toLang.contains("-r")) {
                // Chuyển từ định dạng xx-rYY thành xx-YY
                apiLangCode = toLang.replace("-r", "-")
            } else if (toLang.contains("-")) {
                // Đã đúng định dạng xx-YY cho API
                apiLangCode = toLang
            }

            val urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$fromLang&tl=$apiLangCode&dt=t&q=$encodedText"

            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            try {
                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val reader = InputStreamReader(conn.inputStream, StandardCharsets.UTF_8)
                    val response = reader.readText()

                    // Phân tích kết quả JSON đơn giản
                    // Format: [[["translated text","original text",null,null,1]],null,"en"]
                    val pattern = Pattern.compile("\\[\\[\\[\"(.*?)\",\"")
                    val matcher = pattern.matcher(response)

                    if (matcher.find()) {
                        return matcher.group(1)
                    }
                    return text
                } else {
                    println("Lỗi khi gọi API: HTTP $responseCode")
                    return text
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            println("Lỗi khi dịch: ${e.message}")
            return text
        }
    }

    private fun preprocessText(text: String): Pair<String, Map<String, String>> {
        var processedText = text
        val placeholders = mutableMapOf<String, String>()
        var count = 0

        // Xử lý dấu nháy kép đã escape (phải xử lý trước tiên)
        val escapedQuoteRegex = Regex("\\\\\"")
        escapedQuoteRegex.findAll(processedText).forEach { match ->
            val placeholder = "{{ESCAPED_QUOTE_${count}}}"
            placeholders[placeholder] = match.value
            processedText = processedText.replace(match.value, placeholder)
            count++
        }

        // Xử lý các escape characters khác
        val escapeRegex = Regex("\\\\[nrt'\\\\]")
        escapeRegex.findAll(processedText).forEach { match ->
            val placeholder = "{{ESCAPE_${count}}}"
            placeholders[placeholder] = match.value
            processedText = processedText.replace(match.value, placeholder)
            count++
        }

        // Xử lý dấu ba chấm (ellipsis)
        val ellipsisRegex = Regex("\\.{3}")
        ellipsisRegex.findAll(processedText).forEach { match ->
            val placeholder = "{{ELLIPSIS_${count}}}"
            placeholders[placeholder] = match.value
            processedText = processedText.replace(match.value, placeholder)
            count++
        }

        // Xử lý các định dạng như %s, %d, v.v.
        val formatRegex = Regex("%(\\d+\\$)?[a-zA-Z]")
        formatRegex.findAll(processedText).forEach { match ->
            val placeholder = "{{PLACEHOLDER_${count}}}"
            placeholders[placeholder] = match.value
            processedText = processedText.replace(match.value, placeholder)
            count++
        }

        // Xử lý thẻ HTML
        val htmlRegex = Regex("<[^>]+>")
        htmlRegex.findAll(processedText).forEach { match ->
            val placeholder = "{{HTML_${count}}}"
            placeholders[placeholder] = match.value
            processedText = processedText.replace(match.value, placeholder)
            count++
        }

        // Xử lý XML entities
        val entityRegex = Regex("&[a-zA-Z0-9#]+;")
        entityRegex.findAll(processedText).forEach { match ->
            val placeholder = "{{ENTITY_${count}}}"
            placeholders[placeholder] = match.value
            processedText = processedText.replace(match.value, placeholder)
            count++
        }

        // Xử lý dấu nháy sau khi đã xử lý các loại khác
        val quoteRegex = Regex("(['\"])")
        val tempProcessed = processedText
        quoteRegex.findAll(tempProcessed).forEach { match ->
            val placeholder = "{{QUOTE_${count}}}"
            placeholders[placeholder] = match.value
            processedText = processedText.replace(match.value, placeholder)
            count++
        }

        return Pair(processedText, placeholders)
    }

    private fun postprocessText(translatedText: String, placeholders: Map<String, String>): String {
        var result = translatedText

        // Chuyển đổi tất cả placeholder thành chữ thường để xử lý thống nhất
        val allPlaceholders = placeholders.keys.map { it.lowercase() to placeholders[it] }.toMap()
        val lowerResult = result.lowercase()

        // Bước 1: Xử lý tất cả các placeholder không phải dấu nháy
        val nonQuotePlaceholders = placeholders.entries
            .filter { !it.key.startsWith("{{QUOTE_", ignoreCase = true) && !it.key.startsWith("{{ESCAPED_QUOTE_", ignoreCase = true) }
            .sortedByDescending { it.key.length } // Xử lý placeholder dài trước

        for ((placeholder, original) in nonQuotePlaceholders) {
            // Xử lý biến thể chữ hoa/thường
            val lowerPlaceholder = placeholder.lowercase()
            val upperPlaceholder = placeholder.uppercase()
            val titlePlaceholder = placeholder.capitalize()
            
            result = result.replace(placeholder, original)
                .replace(lowerPlaceholder, original)
                .replace(upperPlaceholder, original)
                .replace(titlePlaceholder, original)
            
            // Xử lý các biến thể với dấu gạch ngang thay vì gạch dưới
            val dashVariant = placeholder.replace("_", "-")
            result = result.replace(dashVariant, original)
                .replace(dashVariant.lowercase(), original)
                .replace(dashVariant.uppercase(), original)
        }

        // Bước 2: Xử lý dấu nháy kép đã escape
        val escapedQuotePlaceholders = placeholders.entries
            .filter { it.key.startsWith("{{ESCAPED_QUOTE_", ignoreCase = true) }
            .sortedByDescending { it.key.length }
        
        for ((placeholder, original) in escapedQuotePlaceholders) {
            // Xử lý placeholder chính
            result = result.replace(placeholder, original)
            
            // Xử lý các biến thể có thể xảy ra
            val variations = listOf(
                placeholder.lowercase(),
                placeholder.uppercase(),
                placeholder.capitalize(),
                placeholder.replace("_", "-"),
                placeholder.replace("_", "-").lowercase(),
                placeholder.replace("ESCAPED_QUOTE", "Escaped_quote"),
                placeholder.replace("ESCAPED_QUOTE", "escaped_quote"),
                placeholder.replace("ESCAPED_QUOTE", "Escape_quote"),
                placeholder.replace("ESCAPED_QUOTE", "escape_quote"),
                placeholder.replace("ESCAPED_QUOTE", "eccaped_quote"),
                placeholder.replace("ESCAPED_QUOTE", "ecaped_quote"),
                placeholder.replace("ESCAPED_QUOTE", "encaped_quote")
            )
            
            for (variation in variations) {
                result = result.replace(variation, original)
            }
        }
        
        // Bước 3: Xử lý dấu nháy thường
        val quotePlaceholders = placeholders.entries
            .filter { it.key.startsWith("{{QUOTE_", ignoreCase = true) }
            .sortedByDescending { it.key.length }

        for ((placeholder, original) in quotePlaceholders) {
            // Xử lý placeholder chính
            result = result.replace(placeholder, original)
            
            // Xử lý các biến thể có thể xảy ra
            val variations = listOf(
                placeholder.lowercase(),
                placeholder.uppercase(),
                placeholder.capitalize(),
                placeholder.replace("_", "-"),
                placeholder.replace("_", "-").lowercase(),
                placeholder.replace("QUOTE", "quote"),
                placeholder.replace("QUOTE", "Quote"),
                placeholder.replace("QUOTE", "evave"),
                placeholder.replace("QUOTE", "Evave")
            )
            
            for (variation in variations) {
                result = result.replace(variation, original)
            }
        }

        // Bước 4: Xử lý các placeholder không được thay thế
        // Dấu nháy kép escape còn sót lại
        val escapedQuoteRegex = Regex("\\{\\{(?:ESCAPED_QUOTE|[Ee]scaped[_-][Qq]uote|[Ee]ccaped[_-][Qq]uote|[Ee]ncaped[_-][Qq]uote|[Ee]caped[_-][Qq]uote|[Ee]scape[_-](?:[Qq]uote)?)[_-]?\\d+\\}\\}")
        result = escapedQuoteRegex.replace(result, "\\\"")
        
        // Dấu escape khác còn sót lại
        val escapeRegex = Regex("\\{\\{(?:ESCAPE|[Ee]scape)[_-]\\d+\\}\\}")
        result = escapeRegex.replace(result, "\\\\")
        
        // Dấu nháy thường còn sót lại
        val quoteRegex = Regex("\\{\\{(?:QUOTE|[Qq]uote|[Ee]vave)[_-]\\d+\\}\\}")
        result = quoteRegex.replace(result, "\"")
        
        // Ellipsis còn sót lại
        val ellipsisRegex = Regex("\\{\\{(?:ELLIPSIS|[Ee]llipsis)[_-]\\d+\\}\\}")
        result = ellipsisRegex.replace(result, "…")

        // Bước 5: Xử lý các encoding đặc biệt và escape characters
        result = result.replace("\\u0027", "'")
            .replace("\\u0022", "\"")
            .replace("\\u003c", "<")
            .replace("\\u003e", ">")
            .replace("\\u0026", "&")
            .replace("\\\\'", "'")
            .replace("\\\\\"", "\"")
            
        // Xử lý các escape kép
        result = result.replace("\\\\\"", "\\\"")
            .replace("\\\\'", "\\'")

        // Bước 6: Xử lý các biểu diễn của ellipsis
        result = result.replace("...", "…")
            .replace("&#8230;", "…")
            .replace("\u2026", "…")
            .replace("\\u2026", "…")
            .replace("\u2025", "…")
            .replace("\\.{3}", "…")

        return result
    }

    private fun loadXmlDocument(file: File): Document {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        return builder.parse(file)
    }

    private fun writeXmlDocument(doc: Document, file: File) {
        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")

        // Xử lý tất cả các string nodes
        val nodeList = doc.getElementsByTagName("string")
        for (i in 0 until nodeList.length) {
            val node = nodeList.item(i) as Element
            val content = node.textContent
            
            // Xử lý các placeholder còn sót lại
            var newContent = content
            
            // 1. Xử lý placeholder còn sót cho dấu nháy kép escape
            val escapedQuotePlaceholderRegex = Regex("\\{\\{(?:ESCAPED_QUOTE|[Ee]scaped[_-][Qq]uote|[Ee]ccaped[_-][Qq]uote|[Ee]ncaped[_-][Qq]uote|[Ee]caped[_-][Qq]uote|[Ee]scape[_-](?:[Qq]uote)?)[_-]?\\d+\\}\\}")
            newContent = escapedQuotePlaceholderRegex.replace(newContent, "\\\"")
            
            // 2. Xử lý placeholder còn sót cho các dấu escape khác
            val escapePlaceholderRegex = Regex("\\{\\{(?:ESCAPE|[Ee]scape)[_-]\\d+\\}\\}")
            newContent = escapePlaceholderRegex.replace(newContent, "\\\\")
            
            // 3. Xử lý placeholder cho dấu nháy thường
            val quotePlaceholderRegex = Regex("\\{\\{(?:QUOTE|[Qq]uote|[Ee]vave)[_-]\\d+\\}\\}")
            newContent = quotePlaceholderRegex.replace(newContent, "\"")
            
            // 4. Đảm bảo dấu nháy đơn được escape đúng
            if (newContent.contains("'") && !newContent.contains("\\'")) {
                newContent = newContent.replace("'", "\\'")
            }
            
            // 5. Đảm bảo dấu nháy kép được escape đúng
            if (newContent.contains("\"") && !newContent.contains("\\\"")) {
                newContent = newContent.replace("\"", "\\\"")
            }
            
            // 6. Sửa lỗi escape kép
            newContent = newContent.replace("\\\\\"", "\\\"")
                .replace("\\\\'", "\\'")
            
            node.textContent = newContent
        }

        val source = DOMSource(doc)
        val result = StreamResult(FileOutputStream(file))
        transformer.transform(source, result)
    }

    // Extension function để hỗ trợ capitalize
    private fun String.capitalize(): String {
        return this.lowercase().replaceFirstChar { 
            if (it.isLowerCase()) it.titlecase() else it.toString() 
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            var sourceFile: File? = null
            var languages: List<Pair<String, String>>? = null
            var outputDir: File? = null

            // Xử lý tham số dòng lệnh
            var i = 0
            while (i < args.size) {
                when (args[i]) {
                    "--sourcefile" -> {
                        sourceFile = File(args[i + 1])
                        i += 2
                    }
                    "--languages" -> {
                        languages = args[i + 1].split(",").map {
                            val parts = it.split(":")
                            // Chuyển đổi định dạng nếu cần
                            var langCode = parts[0]
                            // Nếu người dùng nhập sai định dạng (như pt-rBR thay vì pt-BR)
                            if (langCode.contains("-r")) {
                                println("Chú ý: Chuyển đổi mã ngôn ngữ ${parts[0]} thành định dạng chuẩn cho API.")
                                langCode = langCode.replace("-r", "-")
                            }
                            langCode to parts[1]
                        }
                        i += 2
                    }
                    "--outdir" -> {
                        outputDir = File(args[i + 1])
                        i += 2
                    }
                    else -> {
                        i++
                    }
                }
            }

            // Kiểm tra tham số bắt buộc
            if (sourceFile == null || !sourceFile.exists()) {
                println("Lỗi: File nguồn không tồn tại hoặc không được chỉ định")
                printUsage()
                return
            }

            if (languages == null || languages.isEmpty()) {
                println("Lỗi: Không có ngôn ngữ nào được chỉ định")
                printUsage()
                return
            }

            if (outputDir == null) {
                println("Lỗi: Thư mục đầu ra không được chỉ định")
                printUsage()
                return
            }

            // Tạo và chạy translator
            val translator = StringTranslator(
                sourceFile,
                languages,
                outputDir
            )

            translator.translate()
        }

        private fun printUsage() {
            println("""
                Sử dụng: java -jar StringTranslator.jar [options]
                Các tùy chọn:
                  --sourcefile <path>           Đường dẫn đến file strings.xml nguồn
                  --languages <lang:name,...>   Danh sách ngôn ngữ cần dịch sang
                                                Ví dụ: "fr:French,es:Spanish,de:German"
                                                Lưu ý: Sử dụng định dạng "xx-YY" cho mã ngôn ngữ với khu vực
                                                (ví dụ: "pt-BR", "zh-CN", không phải "pt-rBR")
                  --outdir <path>               Đường dẫn đến thư mục đầu ra (thường là res/)
            """.trimIndent())
        }
    }
}