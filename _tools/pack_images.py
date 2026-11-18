import os
from PIL import Image

SRC = r"C:\Users\26767\Doubao\chats\2026-11-18\new-chat\what-to-eat\_assets_raw"
DST = r"C:\Users\26767\Doubao\chats\2026-11-18\new-chat\what-to-eat\shared\src\commonMain\composeResources\drawable"
os.makedirs(DST, exist_ok=True)

total_in = 0
total_out = 0
for name in sorted(os.listdir(SRC)):
    if not name.lower().endswith(".png"):
        continue
    src = os.path.join(SRC, name)
    out = os.path.join(DST, os.path.splitext(name)[0] + ".webp")
    with Image.open(src) as im:
        im = im.convert("RGB")
        # 居中裁成正方形，再缩到 640
        w, h = im.size
        side = min(w, h)
        im = im.crop(((w - side) // 2, (h - side) // 2, (w + side) // 2, (h + side) // 2))
        im = im.resize((640, 640), Image.LANCZOS)
        im.save(out, "WEBP", quality=78, method=6)
    total_in += os.path.getsize(src)
    total_out += os.path.getsize(out)
    print(f"{os.path.basename(out):24s} {os.path.getsize(out)//1024:4d} KB")

print(f"\nin {total_in/1024/1024:.1f} MB -> out {total_out/1024:.0f} KB")
