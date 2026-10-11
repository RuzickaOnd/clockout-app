#!/usr/bin/env python3
"""Write the demo history the Play screenshots are captured on, as SQL for clockout.db.

    python3 play/seed-demo-history.py app/src/main/res cs /tmp/seed-cs.sql

Weekdays from Mon 3 Aug to Wed 21 Oct 2026 with three missed days: 55 closed days, the
"Through the Park" stage, an eleven-week rhythm, and an October sky with three lit
constellations and a week in progress - captured with the device clock on Thu 22 Oct 18:30.
Questions and answers come from the app's own strings, so reseed after switching language.
"""
import sys, re, datetime, xml.etree.ElementTree as ET
res, lang, out = sys.argv[1:4]
path = f"{res}/values{'-cs' if lang == 'cs' else ''}/strings.xml"
root = ET.parse(path).getroot()
strings = {e.get('name'): (e.text or '') for e in root.findall('string')}
arrays = {e.get('name'): [i.text or '' for i in e.findall('item')] for e in root.findall('string-array')}
unesc = lambda t: t.replace("\\'", "'").replace('\\"', '"')
keys = ['success', 'proud', 'evening', 'let_go', 'enough', 'grateful', 'small_joy', 'tomorrow']
qa = [(unesc(strings['ritual_question_' + k]), [unesc(a) for a in arrays['ritual_answers_' + k]]) for k in keys]
moods = ['CALM', 'SATISFIED', 'CALM', 'TIRED', 'SATISFIED', 'CALM', 'TENSE', 'SATISFIED', 'TIRED', 'CALM']
skip = {datetime.date(2026, 8, 14), datetime.date(2026, 9, 9), datetime.date(2026, 10, 2)}
d, end, days = datetime.date(2026, 8, 3), datetime.date(2026, 10, 21), []
while d <= end:
    if d.weekday() < 5 and d not in skip: days.append(d)
    d += datetime.timedelta(days=1)
q = lambda s: "'" + s.replace("'", "''") + "'"
lines = ["DELETE FROM ritual_logs;", "DELETE FROM practice_days;"]
for i, day in enumerate(days):
    question, answers = qa[i % len(qa)]
    ts = int(datetime.datetime(day.year, day.month, day.day, 18, 10 + i % 40).timestamp() * 1000)
    lines.append(f"INSERT INTO practice_days(date) VALUES ('{day}');")
    lines.append("INSERT INTO ritual_logs(date, question_text, selected_answer, mood_tag, timestamp) VALUES "
                 f"('{day}', {q(question)}, {q(answers[i % len(answers)])}, '{moods[i % len(moods)]}', {ts});")
open(out, 'w', encoding='utf-8').write('\n'.join(lines) + '\n')
print(len(days), 'days', days[0], '..', days[-1])
