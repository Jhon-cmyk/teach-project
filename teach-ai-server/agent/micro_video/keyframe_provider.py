import os
from pathlib import Path

from PIL import Image


def load_keyframe(scene, out_dir, warnings):
    provider = (os.getenv("MICRO_VIDEO_IMAGE_PROVIDER") or "").strip().lower()
    if not provider:
        return None

    if provider != "local":
        warnings.append("AI keyframe provider '%s' is not implemented; local templates were used." % provider)
        return None

    image_dir = Path(os.getenv("MICRO_VIDEO_IMAGE_DIR") or "")
    if not image_dir.exists():
        warnings.append("MICRO_VIDEO_IMAGE_DIR is not configured; local templates were used.")
        return None

    index = int(scene.get("index") or 1)
    candidates = [
        image_dir / ("scene_%02d.png" % index),
        image_dir / ("scene_%02d.jpg" % index),
        image_dir / ("%s.png" % index),
        image_dir / ("%s.jpg" % index),
    ]
    for path in candidates:
        if path.exists():
            try:
                image = Image.open(path).convert("RGB")
                target = Path(out_dir) / ("keyframe_%02d.jpg" % index)
                image.thumbnail((1280, 720))
                canvas = Image.new("RGB", (1280, 720), "#101827")
                x = (1280 - image.width) // 2
                y = (720 - image.height) // 2
                canvas.paste(image, (x, y))
                canvas.save(target, quality=92)
                return canvas
            except Exception as exc:
                warnings.append("Failed to load local keyframe for scene %s: %s" % (index, exc))
                return None
    warnings.append("No local keyframe found for scene %s; local templates were used." % index)
    return None
