import re, io
p = r"_shots\ui.xml"
s = io.open(p, encoding="utf-8").read()
for m in re.finditer(r"<node[^>]*>", s):
    tag = m.group(0)
    t = re.search(r'text="([^"]*)"', tag)
    d = re.search(r'content-desc="([^"]*)"', tag)
    b = re.search(r'bounds="([^"]*)"', tag)
    if not b: continue
    txt = (t.group(1) if t else "") + (d.group(1) if d else "")
    if txt.strip():
        print(repr(txt[:40]), b.group(1))
