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
 * Công cụ dịch chọn lọc các chuỗi cụ thể trong file strings.xml
 * Chỉ dịch những string được chỉ định, giữ nguyên các string khác từ file hiện có
 */
class SelectiveStringTranslator(
    private val sourceFile: File,             // File strings.xml nguồn
    private val stringsToTranslate: Set<String>,  // Danh sách các string name cần dịch
    private val languages: List<Pair<String, String>>, // Ngôn ngữ cần dịch
    private val outputDirectory: File         // Thư mục đầu ra
) {
    private val nonTranslatableStrings = mutableSetOf<String>()

    fun translate() {
        println("Bắt đầu quá trình dịch chọn lọc")
        println("File nguồn: ${sourceFile.absolutePath}")
        println("Số lượng chuỗi cần dịch: ${stringsToTranslate.size}")
        println("Các chuỗi cần dịch: ${stringsToTranslate.joinToString(", ")}")
        println("Ngôn ngữ hỗ trợ: ${languages.joinToString(", ") { "${it.first} (${it.second})" }}")

        val sourceDoc = loadXmlDocument(sourceFile)
        val stringNodes = sourceDoc.getElementsByTagName("string")

        // Tìm các chuỗi được đánh dấu không cần dịch
        findNonTranslatableStrings(stringNodes)

        // Dịch cho từng ngôn ngữ
        languages.forEach { (langCode, langName) ->
            translateToLanguage(langCode, langName, sourceDoc)
        }

        println("Dịch thuật chọn lọc hoàn tất thành công!")
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
        println("Đang dịch chọn lọc sang $langName ($langCode)...")

        // Xác định thư mục ngôn ngữ
        val langDir = when {
            langCode == "zh-CN" -> File(outputDirectory, "values-zh-rCN")
            langCode == "zh-TW" -> File(outputDirectory, "values-zh-rTW")
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

        // Kiểm tra xem có tệp dịch hiện có không
        val existingFile = File(langDir, "strings.xml")
        val existingDoc = if (existingFile.exists()) {
            try {
                loadXmlDocument(existingFile)
            } catch (e: Exception) {
                println("  ! Không thể đọc file hiện có: ${e.message}")
                null
            }
        } else {
            null
        }

        // Tạo tài liệu mới
        val targetDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        val resources = targetDoc.createElement("resources")
        targetDoc.appendChild(resources)

        // Lấy danh sách các chuỗi từ tệp hiện có (nếu có)
        val existingStrings = mutableMapOf<String, String>()
        if (existingDoc != null) {
            val existingNodes = existingDoc.getElementsByTagName("string")
            for (i in 0 until existingNodes.length) {
                val node = existingNodes.item(i) as Element
                val name = node.getAttribute("name")
                val content = node.textContent
                existingStrings[name] = content
            }
            println("  - Đã tìm thấy ${existingStrings.size} chuỗi trong file hiện có")
        }

        // Xử lý tất cả các node string từ tệp nguồn
        val stringNodes = sourceDoc.getElementsByTagName("string")
        for (i in 0 until stringNodes.length) {
            val sourceNode = stringNodes.item(i) as Element
            val nameAttr = sourceNode.getAttribute("name")
            val sourceText = sourceNode.textContent

            val targetNode = targetDoc.createElement("string")
            targetNode.setAttribute("name", nameAttr)

            // Xử lý các thuộc tính khác nếu có
            val translatable = sourceNode.getAttribute("translatable")
            if (translatable.isNotEmpty()) {
                targetNode.setAttribute("translatable", translatable)
            }

            if (nameAttr in stringsToTranslate && nameAttr !in nonTranslatableStrings) {
                // Chuỗi cần dịch
                println("  - Đang dịch '$nameAttr'")
                try {
                    // Bảo vệ các định dạng đặc biệt trước khi dịch
                    val (textToTranslate, placeholders) = preprocessText(sourceText)

                    // Dịch văn bản với cơ chế thử lại
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
                    targetNode.textContent = finalText

                    // Tránh gửi quá nhiều request liên tiếp
                    Thread.sleep(1000)
                } catch (e: Exception) {
                    println("  ! Lỗi khi dịch '$nameAttr': ${e.message}")
                    // Nếu lỗi, sử dụng bản dịch hiện có hoặc giữ nguyên chuỗi gốc
                    targetNode.textContent = existingStrings[nameAttr] ?: sourceText
                }
            } else {
                // Chuỗi không cần dịch, sử dụng bản dịch hiện có hoặc giữ nguyên chuỗi gốc
                targetNode.textContent = existingStrings[nameAttr] ?: sourceText
            }

            resources.appendChild(targetNode)
        }

        // Lưu file đã dịch
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

        // Lưu trữ nguyên bản của chuỗi gốc
        val originalText = text

        // Phát hiện ngôn ngữ từ tên thread
        val currentThread = Thread.currentThread()
        val threadName = currentThread.name
        val targetLang = threadName.substringAfterLast("-", "")

        // Phát hiện các mẫu sở hữu cách phổ biến
        val possessivePatterns = listOf(
            Regex("(app)'s\\s+(global\\s+configuration|configuration)"),
            Regex("(app)'s\\s+(settings)"),
            Regex("(application)'s\\s+(global\\s+configuration|configuration)"),
            Regex("(application)'s\\s+(settings)"),
            Regex("(device)'s\\s+(configuration|settings)"),
            Regex("(user)'s\\s+(configuration|settings)")
        )

        // Phát hiện mẫu trong warning_setting
        val overridePattern = Regex("will\\s+override\\s+(?:the\\s+)?(?:app|application)'s\\s+(?:global\\s+)?configuration")

        // Xử lý dấu nháy kép đã escape (phải xử lý trước tiên)
        val escapedQuoteRegex = Regex("\\\\\"")
        escapedQuoteRegex.findAll(processedText).forEach { match ->
            val placeholder = "{{ESCAPED_QUOTE_${count}}}"
            placeholders[placeholder] = match.value
            processedText = processedText.replace(match.value, placeholder)
            count++
        }

        // Thêm mới: Xử lý đặc biệt cho sở hữu cách (apostrophe - 's)
        val possessiveRegex = Regex("\\\\'s")
        possessiveRegex.findAll(processedText).forEach { match ->
            val placeholder = "{{APOSTROPHE_S_${count}}}"
            placeholders[placeholder] = match.value
            processedText = processedText.replace(match.value, placeholder)
            count++
        }

        // Lưu trữ tham chiếu đến chuỗi ban đầu
        placeholders["{{ORIGINAL_TEXT}}"] = originalText

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

        // Xác định ngôn ngữ từ tên thư mục
        val langCode = when {
            file.absolutePath.contains("values-vi") -> "vi"
            file.absolutePath.contains("values-zh") -> "zh"
            file.absolutePath.contains("values-ja") -> "ja"
            file.absolutePath.contains("values-ko") -> "ko"
            file.absolutePath.contains("values-th") -> "th"
            file.absolutePath.contains("values-bn") -> "bn"
            file.absolutePath.contains("values-hi") -> "hi"
            file.absolutePath.contains("values-mr") -> "mr"
            file.absolutePath.contains("values-ru") -> "ru"
            file.absolutePath.contains("values-zu") -> "zu"
            file.absolutePath.contains("values-ar") -> "ar"
            file.absolutePath.contains("values-he") -> "he"
            file.absolutePath.contains("values-ur") -> "ur"
            file.absolutePath.contains("values-fa") -> "fa"
            else -> {
                val valuesPattern = Regex("values-([a-zA-Z]{2}(?:-r[A-Z]{2})?)")
                val matcher = valuesPattern.find(file.absolutePath)
                matcher?.groupValues?.getOrNull(1) ?: ""
            }
        }

        // Xử lý tất cả các string nodes
        val nodeList = doc.getElementsByTagName("string")
        for (i in 0 until nodeList.length) {
            val node = nodeList.item(i) as Element
            val content = node.textContent
            val nameAttr = node.getAttribute("name")

            // Xử lý chuỗi
            var newContent = content
            
            // Bỏ tất cả placeholder
            newContent = newContent.replace(Regex("\\{\\{[^}]*\\}\\}"), "")
            
            // Loại bỏ các dấu nháy kép escape kép nếu có
            newContent = newContent.replace("\\\\\"", "\\\"")
            // Trường hợp đặc biệt cho các ngôn ngữ có vấn đề
            if (langCode.lowercase() in listOf("bn", "hi", "mr", "ru", "zu", "ar")) {
                // Escape tất cả dấu nháy kép
                if (newContent.contains("\"")) {
                    newContent = newContent.replace("\"", "\\\"")
                }
            } else {
                // Xử lý chuỗi bình thường cho các ngôn ngữ không có vấn đề
                if (newContent.contains("\"") && !newContent.contains("\\\"")) {
                    newContent = newContent.replace("\"", "\\\"")
                }
            }
            
            // Cuối cùng sửa lỗi escape kép
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
            var stringsToTranslate: Set<String>? = null
            var languages: List<Pair<String, String>>? = null
            var outputDir: File? = null

            println("Tham số đầu vào: ${args.joinToString(" ")}")
            
            // Xử lý trường hợp đọc tham số từ file
            val processedArgs = if (args.size == 1 && args[0].startsWith("@")) {
                val paramFile = File(args[0].substring(1))
                if (paramFile.exists()) {
                    try {
                        println("Đọc tham số từ file: ${paramFile.absolutePath}")
                        paramFile.readLines().filter { it.isNotBlank() }.toTypedArray()
                    } catch (e: Exception) {
                        println("Lỗi khi đọc file tham số: ${e.message}")
                        args
                    }
                } else {
                    println("Không tìm thấy file tham số: ${paramFile.absolutePath}")
                    args
                }
            } else {
                args
            }

            println("Tham số đã xử lý: ${processedArgs.joinToString(" ")}")

            // Xử lý tham số dòng lệnh
            var i = 0
            while (i < processedArgs.size) {
                val arg = processedArgs[i].trim()
                when {
                    arg == "--sourcefile" && i + 1 < processedArgs.size -> {
                        val path = processedArgs[i + 1].trim().replace("\"", "")
                        sourceFile = File(path)
                        println("Đường dẫn nguồn: $path")
                        i += 2
                    }
                    arg == "--strings" && i + 1 < processedArgs.size -> {
                        val strings = processedArgs[i + 1].trim().replace("\"", "")
                        stringsToTranslate = strings.split(",").toSet()
                        println("Danh sách chuỗi: $strings")
                        i += 2
                    }
                    arg == "--languages" && i + 1 < processedArgs.size -> {
                        val langStr = processedArgs[i + 1].trim().replace("\"", "")
                        println("Danh sách ngôn ngữ: $langStr")
                        languages = langStr.split(",").map {
                            val parts = it.split(":")
                            if (parts.size < 2) {
                                // Nếu không có tên ngôn ngữ, sử dụng mã ngôn ngữ làm tên
                                val code = parts[0]
                                code to code
                            } else {
                                // Chuyển đổi định dạng nếu cần
                                var langCode = parts[0]
                                if (langCode.contains("-r")) {
                                    println("Chú ý: Chuyển đổi mã ngôn ngữ ${parts[0]} thành định dạng chuẩn cho API.")
                                    langCode = langCode.replace("-r", "-")
                                }
                                langCode to parts[1]
                            }
                        }
                        i += 2
                    }
                    arg == "--outdir" && i + 1 < processedArgs.size -> {
                        val path = processedArgs[i + 1].trim().replace("\"", "")
                        outputDir = File(path)
                        println("Thư mục đầu ra: $path")
                        i += 2
                    }
                    else -> {
                        println("Bỏ qua tham số không xác định: $arg")
                        i++
                    }
                }
            }

            // Kiểm tra tham số bắt buộc
            if (sourceFile == null || !sourceFile.exists()) {
                println("Lỗi: File nguồn không tồn tại hoặc không được chỉ định: ${sourceFile?.absolutePath}")
                printUsage()
                return
            }

            if (stringsToTranslate.isNullOrEmpty()) {
                println("Lỗi: Không có chuỗi nào được chỉ định để dịch")
                printUsage()
                return
            }

            if (languages.isNullOrEmpty()) {
                println("Lỗi: Không có ngôn ngữ nào được chỉ định")
                printUsage()
                return
            }

            if (outputDir == null) {
                println("Lỗi: Thư mục đầu ra không được chỉ định")
                printUsage()
                return
            }

            // In thông tin 
            println("Bắt đầu quá trình dịch với:")
            println("- Tệp nguồn: ${sourceFile.absolutePath}")
            println("- Các chuỗi cần dịch: ${stringsToTranslate.joinToString(", ")}")
            println("- Ngôn ngữ đích: ${languages.joinToString(", ") { "${it.first}:${it.second}" }}")
            println("- Thư mục đầu ra: ${outputDir.absolutePath}")

            // Tạo và chạy translator
            try {
                val translator = SelectiveStringTranslator(
                    sourceFile,
                    stringsToTranslate,
                    languages,
                    outputDir
                )

                translator.translate()
            } catch (e: Exception) {
                println("Lỗi trong quá trình dịch: ${e.message}")
                e.printStackTrace()
            }
        }

        private fun printUsage() {
            println("""
                Sử dụng: java -jar SelectiveStringTranslator.jar [options]
                Các tùy chọn:
                  --sourcefile <path>           Đường dẫn đến file strings.xml nguồn
                  --strings <name1,name2,...>   Danh sách tên các chuỗi cần dịch
                                                Ví dụ: "welcome_text,login_button,error_message"
                  --languages <lang:name,...>   Danh sách ngôn ngữ cần dịch sang
                                                Ví dụ: "fr:French,es:Spanish,de:German"
                                                Lưu ý: Sử dụng định dạng "xx-YY" cho mã ngôn ngữ với khu vực
                                                (ví dụ: "pt-BR", "zh-CN", không phải "pt-rBR")
                  --outdir <path>               Đường dẫn đến thư mục đầu ra (thường là res/)
            """.trimIndent())
        }
    }
} 