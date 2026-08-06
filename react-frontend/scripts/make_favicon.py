"""Generate favicons from logo-yz-source.png (black mark on transparent)."""
from PIL import Image
from pathlib import Path

public = Path(__file__).resolve().parent.parent / "public"
src = public / "logo-yz-source.png"


def make_black_mark(im: Image.Image) -> Image.Image:
    im = im.convert("RGBA")
    pixels = im.load()
    w, h = im.size
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    op = out.load()
    for y in range(h):
        for x in range(w):
            r, g, b, a = pixels[x, y]
            brightness = (r + g + b) / 3
            if brightness < 210 and a > 20:
                alpha = min(255, int((210 - brightness) * 1.3))
                op[x, y] = (0, 0, 0, alpha)
    bbox = out.getbbox()
    return out.crop(bbox) if bbox else out


def fit(mark: Image.Image, size: int, pad_ratio: float = 0.04) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    pad = max(0, int(size * pad_ratio))
    box = size - pad * 2
    tw, th = mark.size
    scale = min(box / tw, box / th)
    nw, nh = max(1, int(tw * scale)), max(1, int(th * scale))
    resized = mark.resize((nw, nh), Image.Resampling.LANCZOS)
    canvas.paste(resized, ((size - nw) // 2, (size - nh) // 2), resized)
    return canvas


def main():
    mark = make_black_mark(Image.open(src))
    for name, size in {
        "favicon-16.png": 16,
        "favicon-32.png": 32,
        "favicon-48.png": 48,
        "favicon-256.png": 256,
        "icon-192.png": 192,
    }.items():
        fit(mark, size).save(public / name, optimize=True)
        print("wrote", name)

    fit(mark, 32).save(public / "favicon.png", optimize=True)

    apple = Image.new("RGBA", (180, 180), (255, 255, 255, 255))
    apple.alpha_composite(fit(mark, 180, pad_ratio=0.06))
    apple.convert("RGB").save(public / "apple-touch-icon.png", optimize=True)
    print("wrote apple-touch-icon.png")

    icons = [fit(mark, 16), fit(mark, 32), fit(mark, 48)]
    icons[0].save(
        public / "favicon.ico",
        format="ICO",
        sizes=[(16, 16), (32, 32), (48, 48)],
        append_images=icons[1:],
    )
    print("wrote favicon.ico")


if __name__ == "__main__":
    main()
