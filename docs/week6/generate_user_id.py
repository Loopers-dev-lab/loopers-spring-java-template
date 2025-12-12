import random
from datetime import datetime

# --- 설정값 변경 ---
START_ID = 1
TOTAL_ROWS_TO_GENERATE = 100000  # 1번부터 100개 생성 (마지막 ID는 100)
TOTAL = START_ID + TOTAL_ROWS_TO_GENERATE - 1 # 최종 ID 번호 (100)
BATCH_SIZE = 1_000
OUTPUT_FILE = "user_data.sql"

with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
    # START_ID부터 시작하도록 범위 수정
    for batch_start in range(START_ID, START_ID + TOTAL_ROWS_TO_GENERATE, BATCH_SIZE):
        batch_end = min(batch_start + BATCH_SIZE - 1, START_ID + TOTAL_ROWS_TO_GENERATE - 1)
        values = []

        for i in range(batch_start, batch_end + 1):
            values.append(
                f"user{i}"
            )

        sql = ("")
        sql += "\n".join(values)
        sql += "\n\n"
        f.write(sql)

print(f"Done! Generated {OUTPUT_FILE} starting from ID {START_ID} with {TOTAL_ROWS_TO_GENERATE} rows.")
