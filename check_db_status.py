import sqlite3
from datetime import datetime

# 데이터베이스 연결
conn = sqlite3.connect('lotto.db')
cursor = conn.cursor()

print("=" * 60)
print("📊 로또 데이터베이스 상태 확인")
print("=" * 60)

# 당첨번호 개수 확인
cursor.execute('SELECT COUNT(*) FROM winning_numbers')
count = cursor.fetchone()[0]
print(f'\n✅ 현재 저장된 당첨번호 개수: {count}개')

if count > 0:
    # 최근 5개 회차 확인
    cursor.execute('''
        SELECT draw_no, num1, num2, num3, num4, num5, num6, bonus, draw_date 
        FROM winning_numbers 
        ORDER BY draw_no DESC 
        LIMIT 5
    ''')
    
    print('\n📋 최근 5개 회차:')
    print('-' * 60)
    for row in cursor.fetchall():
        draw_no = row[0]
        numbers = f"{row[1]}, {row[2]}, {row[3]}, {row[4]}, {row[5]}, {row[6]}"
        bonus = row[7]
        draw_date = row[8] if row[8] else '날짜 없음'
        print(f'  {draw_no}회 ({draw_date}): [{numbers}] + 보너스 {bonus}')
    
    # 가장 오래된 회차 확인
    cursor.execute('''
        SELECT draw_no, draw_date 
        FROM winning_numbers 
        ORDER BY draw_no ASC 
        LIMIT 1
    ''')
    oldest = cursor.fetchone()
    print(f'\n📅 데이터 범위: {oldest[0]}회 ~ {count}회')
else:
    print('\n⚠️ 데이터베이스가 비어있습니다!')

print("=" * 60)

conn.close()
