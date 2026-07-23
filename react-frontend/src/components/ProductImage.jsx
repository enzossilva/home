import { useEffect, useState } from 'react';

/**
 * Renders a product photo with studio background removed (transparent).
 * Flood-fills from the image corners so white/gray borders disappear.
 */
export default function ProductImage({ src, alt, className, tolerance = 38, onClick, style }) {
  const [url, setUrl] = useState(src);

  useEffect(() => {
    if (!src) return;
    let cancelled = false;
    setUrl(src);

    const img = new Image();
    img.crossOrigin = 'anonymous';

    img.onload = () => {
      try {
        const w = img.naturalWidth;
        const h = img.naturalHeight;
        const canvas = document.createElement('canvas');
        canvas.width = w;
        canvas.height = h;
        const ctx = canvas.getContext('2d', { willReadFrequently: true });
        ctx.drawImage(img, 0, 0);
        const imageData = ctx.getImageData(0, 0, w, h);
        removeBackground(imageData, tolerance);
        ctx.putImageData(imageData, 0, 0);
        if (!cancelled) setUrl(canvas.toDataURL('image/png'));
      } catch {
        if (!cancelled) setUrl(src);
      }
    };

    img.onerror = () => {
      if (!cancelled) setUrl(src);
    };

    img.src = src;
    return () => { cancelled = true; };
  }, [src, tolerance]);

  return (
    <img
      src={url}
      alt={alt}
      className={className}
      onClick={onClick}
      style={style}
      onError={e => { e.target.style.display = 'none'; }}
    />
  );
}

function removeBackground(imageData, tolerance) {
  const { data, width, height } = imageData;
  const visited = new Uint8Array(width * height);
  const stack = [];

  const corners = [
    0,
    width - 1,
    (height - 1) * width,
    (height - 1) * width + (width - 1),
  ];

  for (const start of corners) {
    if (visited[start]) continue;
    const si = start * 4;
    const tr = data[si];
    const tg = data[si + 1];
    const tb = data[si + 2];

    // Only treat light/neutral corner as background
    const lum = 0.299 * tr + 0.587 * tg + 0.114 * tb;
    if (lum < 160) continue;

    stack.push(start);
    while (stack.length) {
      const i = stack.pop();
      if (visited[i]) continue;
      visited[i] = 1;

      const p = i * 4;
      const dr = Math.abs(data[p] - tr);
      const dg = Math.abs(data[p + 1] - tg);
      const db = Math.abs(data[p + 2] - tb);
      if (dr + dg + db > tolerance * 3) continue;

      data[p + 3] = 0;

      const x = i % width;
      const y = (i / width) | 0;
      if (x > 0) stack.push(i - 1);
      if (x < width - 1) stack.push(i + 1);
      if (y > 0) stack.push(i - width);
      if (y < height - 1) stack.push(i + width);
    }
  }

  // Also clear residual near-white pixels (anti-alias fringe)
  for (let i = 0; i < data.length; i += 4) {
    if (data[i + 3] === 0) continue;
    const lum = 0.299 * data[i] + 0.587 * data[i + 1] + 0.114 * data[i + 2];
    if (lum >= 245) data[i + 3] = 0;
  }
}
