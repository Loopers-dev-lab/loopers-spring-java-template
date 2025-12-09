import random
from datetime import datetime

# --- 설정값 변경 ---
START_ID = 1
TOTAL_ROWS_TO_GENERATE = 100000  # 1번부터 100개 생성 (마지막 ID는 100)
TOTAL = START_ID + TOTAL_ROWS_TO_GENERATE - 1 # 최종 ID 번호 (100)
BATCH_SIZE = 1_000
OUTPUT_FILE = "user_data.sql"
NOW = datetime.now().strftime('%Y-%m-%d %H:%M:%S')

with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
    # START_ID부터 시작하도록 범위 수정
    for batch_start in range(START_ID, START_ID + TOTAL_ROWS_TO_GENERATE, BATCH_SIZE):
        batch_end = min(batch_start + BATCH_SIZE - 1, START_ID + TOTAL_ROWS_TO_GENERATE - 1)
        values = []

        for i in range(batch_start, batch_end + 1):
            # --- 랜덤 값 생성 로직 추가 ---
            brand_id = (i % 50) + 1
            name = f"Product {i}"

            # 가격: 1000원에서 50000원 사이의 100원 단위 랜덤 값
            price = random.randrange(1000, 50001, 100)

            # 재고: 0개에서 500개 사이의 랜덤 정수
            stock_quantity = random.randint(0, 500)

            # 좋아요수: 0개에서 10000개 사이의 랜덤 정수
            like_count = random.randint(0, 10000)

            # SQL VALUES 튜플 생성 (id 필드 추가)
            # 가정: 테이블 구조에 id가 첫 번째 칼럼으로 존재한다고 가정합니다.
            values.append(
                f"user{i}"
            )

        sql = ("")
        sql += "\n".join(values)
        sql += "\n\n"
        f.write(sql)

print(f"Done! Generated {OUTPUT_FILE} starting from ID {START_ID} with {TOTAL_ROWS_TO_GENERATE} rows.")
