"""把选定的 25 号图做成 Android 自适应图标 + 传统图标 + 桌面端 PNG。"""
from PIL import Image, ImageDraw
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, '_assets_raw', 'icon_pick25.png')
RES = os.path.join(ROOT, 'androidApp', 'src', 'main', 'res')

src = Image.open(SRC).convert('RGB')
S = src.size[0]
# 原图圆角外是白底，先量出画面真正的背景色（顶部中间），再把外围白角抹掉
bg = src.getpixel((S // 2, int(S * 0.06)))
print('背景色 =', bg)

_flat = Image.new('RGB', (S, S), bg)
_mask = Image.new('L', (S, S), 0)
ImageDraw.Draw(_mask).rounded_rectangle(
    [0, 0, S - 1, S - 1], radius=int(S * 0.225), fill=255)
src = Image.composite(src, _flat, _mask)


def rrect(im, ratio=0.225):
    w, h = im.size
    mask = Image.new('L', (w, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        [0, 0, w - 1, h - 1], radius=int(min(w, h) * ratio), fill=255)
    out = im.convert('RGBA')
    out.putalpha(mask)
    return out


def circle(im):
    w, h = im.size
    mask = Image.new('L', (w, h), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, w - 1, h - 1], fill=255)
    out = im.convert('RGBA')
    out.putalpha(mask)
    return out


# 传统图标（低版本系统用，带圆角/圆形）
LEGACY = {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}
for d, s in LEGACY.items():
    folder = os.path.join(RES, 'mipmap-' + d)
    os.makedirs(folder, exist_ok=True)
    base = src.resize((s, s), Image.LANCZOS)
    rrect(base).save(os.path.join(folder, 'ic_launcher.png'))
    circle(base).save(os.path.join(folder, 'ic_launcher_round.png'))

# 自适应图标三层（108dp 画布，系统只显示中心 72dp，内容缩到 62% 才不会切到碗）
ADAPTIVE = {'mdpi': 108, 'hdpi': 162, 'xhdpi': 216, 'xxhdpi': 324, 'xxxhdpi': 432}
for d, s in ADAPTIVE.items():
    folder = os.path.join(RES, 'mipmap-' + d)
    inner = int(s * 0.62)
    content = src.resize((inner, inner), Image.LANCZOS)
    off = (s - inner) // 2

    fg = Image.new('RGBA', (s, s), (0, 0, 0, 0))
    fg.paste(content.convert('RGBA'), (off, off))
    fg.save(os.path.join(folder, 'ic_launcher_foreground.png'))

    gray = content.convert('L')
    alpha = gray.point(lambda v: 0 if v < 58 else min(255, int((v - 58) * 255 / 150)))
    mono_inner = Image.new('RGBA', (inner, inner), (255, 255, 255, 0))
    mono_inner.putalpha(alpha)
    mono = Image.new('RGBA', (s, s), (0, 0, 0, 0))
    mono.paste(mono_inner, (off, off))
    mono.save(os.path.join(folder, 'ic_launcher_monochrome.png'))

# 桌面端 / 应用内使用的 512 PNG
desk = os.path.join(ROOT, 'shared', 'src', 'commonMain', 'composeResources', 'drawable', 'ic_launcher.png')
os.makedirs(os.path.dirname(desk), exist_ok=True)
rrect(src.resize((512, 512), Image.LANCZOS)).save(desk, optimize=True)

# 圆形与圆角预览，便于核对裁切是否吃到主体
prev_dir = os.path.join(ROOT, '_shots')
os.makedirs(prev_dir, exist_ok=True)
circle(src.resize((512, 512), Image.LANCZOS)).save(os.path.join(prev_dir, 'icon_circle.png'))
rrect(src.resize((512, 512), Image.LANCZOS)).save(os.path.join(prev_dir, 'icon_rounded.png'))

# 模拟启动器：取 108dp 前景层，按系统规则裁中心 72dp 圆形再贴到深色底板
s = 432
inner = int(s * 0.62)
content = src.resize((inner, inner), Image.LANCZOS)
fg = Image.new('RGBA', (s, s), (0, 0, 0, 0))
fg.paste(content.convert('RGBA'), ((s - inner) // 2, (s - inner) // 2))
plate = Image.new('RGBA', (s, s), bg + (255,))
plate.alpha_composite(fg)
vis = int(s * 72 / 108)
left = (s - vis) // 2
plate = plate.crop((left, left, left + vis, left + vis))
circle(plate).save(os.path.join(prev_dir, 'icon_adaptive_preview.png'))
print('生成完成')
