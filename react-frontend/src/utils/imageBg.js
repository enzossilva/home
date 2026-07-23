/**
 * Flood-fill near-white/gray studio background → transparent.
 * Works from image corners so white print texture on the product is kept.
 */
export function stripStudioBackground(imageData, {
  cornerLumMin = 160,
  colorTolerance = 42,
  fringeLum = 248,
} = {}) {
  const { data, width, height } = imageData;
  const visited = new Uint8Array(width * height);
  const stack = [];
  const tol = colorTolerance * 3;

  const corners = [
    0,
    width - 1,
    (height - 1) * width,
    height * width - 1,
  ];

  for (const start of corners) {
    if (visited[start]) continue;
    const si = start * 4;
    const tr = data[si];
    const tg = data[si + 1];
    const tb = data[si + 2];
    const lum = 0.299 * tr + 0.587 * tg + 0.114 * tb;
    if (lum < cornerLumMin) continue;

    stack.push(start);
    while (stack.length) {
      const i = stack.pop();
      if (i < 0 || i >= width * height || visited[i]) continue;
      visited[i] = 1;

      const p = i * 4;
      const diff =
        Math.abs(data[p] - tr) +
        Math.abs(data[p + 1] - tg) +
        Math.abs(data[p + 2] - tb);
      if (diff > tol) continue;

      data[p + 3] = 0;

      const x = i % width;
      const y = (i / width) | 0;
      if (x > 0) stack.push(i - 1);
      if (x < width - 1) stack.push(i + 1);
      if (y > 0) stack.push(i - width);
      if (y < height - 1) stack.push(i + width);
    }
  }

  // Soft fringe: only near-white pixels still opaque
  for (let i = 0; i < data.length; i += 4) {
    if (data[i + 3] === 0) continue;
    const lum = 0.299 * data[i] + 0.587 * data[i + 1] + 0.114 * data[i + 2];
    if (lum >= fringeLum) data[i + 3] = 0;
  }

  return imageData;
}

export async function fileToTransparentPng(file) {
  const bitmap = await createImageBitmap(file);
  const canvas = document.createElement('canvas');
  canvas.width = bitmap.width;
  canvas.height = bitmap.height;
  const ctx = canvas.getContext('2d', { willReadFrequently: true });
  ctx.drawImage(bitmap, 0, 0);
  bitmap.close();

  const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
  stripStudioBackground(imageData);
  ctx.putImageData(imageData, 0, 0);

  const blob = await new Promise((resolve, reject) => {
    canvas.toBlob(
      b => (b ? resolve(b) : reject(new Error('Falha ao processar imagem'))),
      'image/png'
    );
  });
  return blob;
}

/**
 * Load any image URL (via same-origin proxy when needed) and return a PNG data URL
 * with studio background removed.
 */
export async function urlToTransparentDataUrl(src) {
  if (!src) return src;

  const candidates = [src];
  // Same-origin proxy avoids Cloudinary CORS blocking canvas reads
  if (/^https?:\/\//i.test(src)) {
    candidates.unshift(`/img-proxy?url=${encodeURIComponent(src)}`);
  }

  let lastError;
  for (const url of candidates) {
    try {
      const res = await fetch(url, { mode: 'cors', credentials: 'omit' });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const blob = await res.blob();
      const bitmap = await createImageBitmap(blob);
      const canvas = document.createElement('canvas');
      canvas.width = bitmap.width;
      canvas.height = bitmap.height;
      const ctx = canvas.getContext('2d', { willReadFrequently: true });
      ctx.drawImage(bitmap, 0, 0);
      bitmap.close();
      const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
      stripStudioBackground(imageData);
      ctx.putImageData(imageData, 0, 0);
      return canvas.toDataURL('image/png');
    } catch (err) {
      lastError = err;
    }
  }
  throw lastError || new Error('Não foi possível processar a imagem');
}
