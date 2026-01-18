# Công cụ dịch file strings.xml

Đây là công cụ đơn giản giúp bạn dịch file `strings.xml` của ứng dụng Android sang nhiều ngôn ngữ **hoàn toàn miễn phí**, không cần đăng ký bất kỳ dịch vụ API nào.

## Cách sử dụng

1. Để dịch tất cả chuỗi trong file strings.xml, chỉ cần chạy lệnh:

   ```
   ./gradlew :translator:clean :translator:jar && ./gradlew translateStrings
   ```

2. Công cụ sẽ:
   - Đọc file `app/src/main/res/values/strings.xml`
   - Dịch tất cả các chuỗi sang các ngôn ngữ được cấu hình (mặc định: vi, es, fr, de, ja, ko, zh-CN, ru, pt, it, ar, hi, tr, th)
   - Tạo các file `strings.xml` tương ứng trong các thư mục `values-{lang}`

## Dịch chọn lọc các chuỗi cụ thể

Nếu bạn chỉ muốn dịch một số chuỗi cụ thể thay vì toàn bộ file, bạn có thể sử dụng tính năng dịch chọn lọc:

1. Biên dịch công cụ:

   ```
   ./gradlew :translator:clean :translator:jar
   ```

2. Chạy script dịch chọn lọc:

   ```
   ./translator/selective-translate.sh,login_button
   ```

   Trong đó:

   - Tham số đầu tiên là danh sách các tên chuỗi cần dịch, phân cách bằng dấu phẩy
   - (Tùy chọn) Tham số thứ hai là danh sách ngôn ngữ cần dịch sang, nếu không chỉ định sẽ sử dụng danh sách mặc định

3. Để chỉ định ngôn ngữ cụ thể:

   ```
   ./translator/selective-translate.sh welcome_text,login_button fr:French,es:Spanish
   ```

4. Công cụ sẽ:
   - Chỉ dịch những chuỗi được chỉ định trong danh sách
   - Giữ nguyên các chuỗi khác từ file hiện có (hoặc từ file nguồn nếu chưa có)
   - Cập nhật file `strings.xml` trong các thư mục ngôn ngữ tương ứng

## Ngôn ngữ hỗ trợ mặc định

Công cụ hỗ trợ sẵn các ngôn ngữ và vùng sau:

- en-rUS: English (US)
- en-rGB: English (UK)
- hi: Hindi
- bn: Bengali
- pt-rBR: Portuguese (Brazil)
- ar-rSA: Arabic (Saudi Arabia)
- ar-rTN: Arabic (Tunisia)
- ar-rOM: Arabic (Oman)
- in: Indonesian
- pt-rPT: Portuguese (Portugal)
- es: Spanish
- mr: Marathi
- te: Telugu
- ta: Tamil
- it-rIT: Italian
- ru: Russian
- fr: French
- vi: Vietnamese
- de-rDE: German
- ko: Korean
- ja: Japanese
- zh: Chinese
- tr-rTR: Turkish
- th: Thai
- nl-rNL: Dutch
- da-rDK: Danish
- ga: Irish
- pl: Polish
- zu: Zulu

## Cách hoạt động

- Công cụ sử dụng một phương pháp miễn phí để truy cập dịch vụ Google Translate mà không cần API key
- Phương pháp này phù hợp cho các dự án nhỏ và phát triển cá nhân
- Chuỗi được đánh dấu `translatable="false"` sẽ không được dịch
- Các định dạng đặc biệt như %s, %d, thẻ HTML, và các ký tự đặc biệt được bảo tồn trong quá trình dịch

## Tùy chỉnh ngôn ngữ

Để thêm hoặc bớt ngôn ngữ, chỉnh sửa biến `supportedLanguages` trong task `translateStrings` trong file `app/build.gradle.kts`:

```kotlin
val supportedLanguages = listOf(
    "vi" to "Vietnamese",
    "es" to "Spanish",
    "fr" to "French",
    // Thêm hoặc xóa ngôn ngữ tại đây
)
```

Hoặc chỉnh sửa biến `DEFAULT_LANGUAGES` trong file `selective-translate.sh` nếu bạn đang sử dụng tính năng dịch chọn lọc.

## Xử lý lỗi evave

Công cụ bao gồm cơ chế xử lý lỗi `{{evave_X}}`, một lỗi có thể xảy ra khi dịch các chuỗi chứa dấu nháy kép (`"`). Nếu bạn gặp phải chuỗi dịch có định dạng như `{{evave_0}}`, công cụ sẽ tự động sửa chúng thành dấu nháy kép đúng.

## Lưu ý

- Mã nguồn phải tuân thủ quy tắc "rate limiting" để tránh bị chặn
- Nếu dự án của bạn lớn hơn, hãy cân nhắc sử dụng dịch vụ chính thức
- Không phù hợp cho mục đích thương mại
