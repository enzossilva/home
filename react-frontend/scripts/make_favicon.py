"""Generate transparent favicons (white mark only, no black square)."""
from PIL import Image, ImageDraw
from pathlib import Path

public = Path(r"C:\Users\Enzo\Documents\home-push\react-frontend\public")


def draw_mark(size: int) -> Image.Image:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    s = size / 32.0
    sw = max(1, round(1.7 * s))
    sw2 = max(1, round(1.5 * s))
    white = (255, 255, 255, 255)

    def line(x1, y1, x2, y2, w):
        draw.line([(x1, y1), (x2, y2)], fill=white, width=w)

    cx, cy = 16 * s, 28 * s
    for x, y in [
        (16, 10), (12.2, 11.2), (19.8, 11.2), (8.8, 13.5), (23.2, 13.5),
        (6, 17), (26, 17), (4.2, 21), (27.8, 21), (3.5, 25), (28.5, 25),
    ]:
        line(cx, cy, x * s, y * s, sw)

    cx2, cy2 = 16 * s, 8 * s
    for x, y in [(16, 3.5), (13.2, 4.2), (18.8, 4.2), (11.5, 5.5), (20.5, 5.5)]:
        line(cx2, cy2, x * s, y * s, sw2)

    return img


for name, size in {
    "favicon-16.png": 16,
    "favicon-32.png": 32,
    "favicon-48.png": 48,
    "apple-touch-icon.png": 180,
    "icon-192.png": 192,
}.items():
    im = draw_mark(size)
    # Apple touch prefers opaque bg — keep transparent for tab icons;
    # for apple-touch use dark bg so iOS home screen looks fine
    if name == "apple-touch-icon.png":
        bg = Image.new("RGBA", (size, size), (0, 0, 0, 255))
        bg.alpha_composite(im)
        bg.convert("RGB").save(public / name, optimize=True)
    else:
        im.save(public / name, optimize=True)
    print("wrote", name)

draw_mark(32).save(public / "favicon.png", optimize=True)

# ICO with transparency (RGBA)
icons = [draw_mark(16), draw_mark(32), draw_mark(48)]
icons[0].save(
    public / "favicon.ico",
    format="ICO",
    sizes=[(16, 16), (32, 32), (48, 48)],
    append_images=icons[1:],
)
print("wrote favicon.ico (transparent)")
