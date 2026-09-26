"""Render the simple Senda book mark as iOS/PWA PNG sizes."""
from pathlib import Path
from PIL import Image, ImageDraw

output = Path(__file__).resolve().parents[1] / "dist"
for size in (192, 512):
    scale = 4
    image = Image.new("RGB", (size * scale, size * scale), "#0077b6")
    draw = ImageDraw.Draw(image)
    s = size * scale / 64
    path = [(13, 16), (20, 14), (27, 15), (32, 18), (37, 15), (44, 14), (51, 16), (51, 48), (44, 46), (37, 47), (32, 50), (27, 47), (20, 46), (13, 48), (13, 16)]
    draw.line([(int(x*s), int(y*s)) for x, y in path], fill="white", width=int(3.5*s), joint="curve")
    draw.line([(int(32*s), int(18*s)), (int(32*s), int(49*s))], fill="white", width=int(3*s))
    draw.line([(int(18*s), int(25*s)), (int(23*s), int(24*s)), (int(28*s), int(26*s))], fill="white", width=int(2*s), joint="curve")
    draw.line([(int(36*s), int(26*s)), (int(41*s), int(24*s)), (int(46*s), int(25*s))], fill="white", width=int(2*s), joint="curve")
    image.resize((size, size), Image.Resampling.LANCZOS).save(output / f"icon-{size}.png", optimize=True)
