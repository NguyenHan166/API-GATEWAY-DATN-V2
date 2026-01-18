#!/bin/bash

# Script chạy SelectiveStringTranslator - dịch chọn lọc các chuỗi string
# Cách sử dụng: ./selective-translate.sh [tên string 1],[tên string 2],... [ngôn ngữ tùy chọn]

# Lấy thư mục hiện tại
SCRIPT_DIR=$(dirname "$0")
PROJECT_DIR=$(cd "$SCRIPT_DIR/.." && pwd)
cd "$PROJECT_DIR" || exit

# Thư mục chứa file strings.xml gốc - mặc định là từ res/values
SOURCE_FILE="$PROJECT_DIR/app/src/main/res/values/strings.xml"
# Thư mục đầu ra - mặc định là thư mục res
OUTPUT_DIR="$PROJECT_DIR/app/src/main/res"

# Danh sách ngôn ngữ mặc định
DEFAULT_LANGUAGES="en-rUS:English (US),en-rGB:English (UK),hi:Hindi,bn:Bengali,pt-rBR:Portuguese (Brazil),ar-rSA:Arabic (Saudi Arabia),ar-rTN:Arabic (Tunisia),ar-rOM:Arabic (Oman),in:Indonesian,pt-rPT:Portuguese (Portugal),es:Spanish,mr:Marathi,te:Telugu,ta:Tamil,it-rIT:Italian,ru:Russian,fr:French,vi:Vietnamese,de-rDE:German,ko:Korean,ja:Japanese,zh:Chinese,tr-rTR:Turkish,th:Thai,nl-rNL:Dutch,da-rDK:Danish,ga:Irish,pl:Polish,zu:Zulu"

# Kiểm tra tham số
if [ $# -lt 1 ]; then
    echo "Thiếu tham số!"
    echo "Cách sử dụng: ./selective-translate.sh [danh sách string] [danh sách ngôn ngữ (tùy chọn)]"
    echo "Ví dụ: ./selective-translate.sh welcome_text,login_button"
    echo "hoặc:  ./selective-translate.sh welcome_text,login_button fr:French,es:Spanish"
    exit 1
fi

# Danh sách string cần dịch
STRINGS_TO_TRANSLATE=$1

# Xử lý tham số ngôn ngữ
if [ $# -ge 2 ]; then
    # Nếu người dùng cung cấp danh sách ngôn ngữ
    LANGUAGES=$2
else
    # Sử dụng danh sách mặc định
    LANGUAGES=$DEFAULT_LANGUAGES
fi

# Kiểm tra file nguồn tồn tại
if [ ! -f "$SOURCE_FILE" ]; then
    echo "Không tìm thấy file nguồn: $SOURCE_FILE"
    exit 1
fi

# Kiểm tra thư mục đầu ra
if [ ! -d "$OUTPUT_DIR" ]; then
    echo "Không tìm thấy thư mục đầu ra: $OUTPUT_DIR"
    exit 1
fi

echo "=== Bắt đầu dịch chọn lọc ==="
echo "File nguồn: $SOURCE_FILE"
echo "Các chuỗi cần dịch: $STRINGS_TO_TRANSLATE"
echo "Ngôn ngữ cần dịch: $LANGUAGES"
echo "Thư mục đầu ra: $OUTPUT_DIR"
echo "==========================="

# Đảm bảo thư mục bin không tồn tại
rm -rf "$PROJECT_DIR/translator/bin"

# Trước tiên hãy đảm bảo rằng dự án đã được biên dịch
echo "Biên dịch dự án..."
./gradlew :translator:clean :translator:deleteBinDirectory :translator:build

# Lưu các tham số vào file tạm thời để tránh lỗi khi truyền tham số phức tạp
PARAM_FILE="$PROJECT_DIR/translator/build/params.txt"
mkdir -p "$(dirname "$PARAM_FILE")"

echo "--sourcefile" > "$PARAM_FILE"
echo "$SOURCE_FILE" >> "$PARAM_FILE"
echo "--strings" >> "$PARAM_FILE"
echo "$STRINGS_TO_TRANSLATE" >> "$PARAM_FILE"
echo "--languages" >> "$PARAM_FILE"
echo "$LANGUAGES" >> "$PARAM_FILE"
echo "--outdir" >> "$PARAM_FILE"
echo "$OUTPUT_DIR" >> "$PARAM_FILE"

# Chạy ứng dụng sử dụng gradle trực tiếp, truyền tham số theo dòng
echo "Chạy SelectiveStringTranslator..."
./gradlew :translator:deleteBinDirectory :translator:runSelectiveTranslator --args="@$PARAM_FILE"

# Kiểm tra kết quả
RESULT=$?
if [ $RESULT -eq 0 ]; then
    echo "Dịch chọn lọc hoàn tất thành công!"
else
    echo "Có lỗi xảy ra khi dịch! (Mã lỗi: $RESULT)"
fi

# Xóa thư mục bin nếu còn tồn tại
rm -rf "$PROJECT_DIR/translator/bin"

# Xóa file tạm
rm -f "$PARAM_FILE" 