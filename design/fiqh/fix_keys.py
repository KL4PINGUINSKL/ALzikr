"""Gives hadiths that matched the wrong HadeethEnc entry a precise search key (found with check_matches.py)."""
import re
KT = r'D:\ALzikr\android\app\src\main\java\com\alzikr\app\Fiqh.kt'
KEYS = {
    'ما منكم من أحد يتوضأ': 'ما منكم من احد يتوضا',
    'جُعلت لي الأرض مسجدًا': 'اعطيت خمسا لم يعطهن احد قبلي',
    'من كان له إمام فقراءة': 'فقراءه الامام له قراءه',
    'كان يكبّر أربعًا تكبيرَه': 'يكبر اربعا تكبيره علي الجنائز',
    'من لم يبيّت الصيام': 'من لم يبيت الصيام قبل الفجر',
    'هل عندكم شيء؟': 'هل عندكم شيء فاني اذن صائم',
    'أتاني الليلة آتٍ من ربي': 'صل في هذا الوادي المبارك',
    'اللهم اغفر له وارحمه، وعافه': 'اغفر له وارحمه وعافه واعف عنه',
    'الذهب بالذهب، والفضة': 'والبر بالبر والشعير بالشعير والتمر بالتمر',
    'ما أسفل من الكعبين': 'ما اسفل من الكعبين من الازار',
    'لا يغتسل أحدكم في الماء الدائم': 'لا يغتسل احدكم في الماء الدائم وهو جنب',
}
s = open(KT, encoding='utf-8').read()
n = 0
def fix(m):
    global n
    text = m.group(1)
    for prefix, key in KEYS.items():
        if text.startswith(prefix):
            n += 1
            return f'h("{text}", "{m.group(2)}", "{m.group(3)}", "{key}")'
    return m.group(0)
s = re.sub(r'\bh\("([^"]*)", "([^"]*)", "([^"]*)"(?:, "[^"]*")?\)', fix, s)
open(KT, 'w', encoding='utf-8', newline='').write(s)
print('keys set', n)
