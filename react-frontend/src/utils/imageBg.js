/**
 * Flood-fill studio background → fully transparent (binary alpha).
 * Semi-transparent leftovers look like a faint gray box on black pages.
 */
export function stripStudioBackground(imageData, {
  cornerLumMin = 140,
  colorTolerance = 72,
  fringeLum = 235,
  erodePasses = 2,
} = {}) {
  const { data, width, height } = imageData;
  const visited = new Uint8Array(width * height);
  const bgMask = new Uint8Array(width * height);
  const stack = [];
  const tol = colorTolerance * 3;

  const sampleCorner = (sx, sy) => {
    let r = 0, g = 0, b = 0, n = 0;
    for (let y = sy; y < sy + 6 && y < height; y++) {
      for (let x = sx; x < sx + 6 && x < width; x++) {
        const p = (y * width + x) * 4;
        r += data[p];
        g += data[p + 1];
        b += data[p + 2];
        n++;
      }
    }
    return { r: (r / n) | 0, g: (g / n) | 0, b: (b / n) | 0 };
  };

  const seeds = [
    { i: 0, c: sampleCorner(0, 0) },
    { i: width - 1, c: sampleCorner(width - 6, 0) },
    { i: (height - 1) * width, c: sampleCorner(0, height - 6) },
    { i: height * width - 1, c: sampleCorner(width - 6, height - 6) },
  ];

  for (const { i: start, c } of seeds) {
    const lum = 0.299 * c.r + 0.587 * c.g + 0.114 * c.b;
    if (lum < cornerLumMin) continue;

    stack.length = 0;
    stack.push(start);
    while (stack.length) {
      const i = stack.pop();
      if (i < 0 || i >= width * height || visited[i]) continue;
      visited[i] = 1;

      const p = i * 4;
      const diff =
        Math.abs(data[p] - c.r) +
        Math.abs(data[p + 1] - c.g) +
        Math.abs(data[p + 2] - c.b);
      if (diff > tol) continue;

      bgMask[i] = 1;
      data[p + 3] = 0;

      const x = i % width;
      const y = (i / width) | 0;
      if (x > 0) stack.push(i - 1);
      if (x < width - 1) stack.push(i + 1);
      if (y > 0) stack.push(i - width);
      if (y < height - 1) stack.push(i + width);
    }
  }

  // Kill near-white leftovers anywhere (JPEG noise / soft fringe)
  for (let i = 0; i < width * height; i++) {
    const p = i * 4;
    if (data[p + 3] === 0) continue;
    const lum = 0.299 * data[p] + 0.587 * data[p + 1] + 0.114 * data[p + 2];
    if (lum >= fringeLum) {
      data[p + 3] = 0;
      bgMask[i] = 1;
    }
  }

  // Erode opaque edge touching background (removes gray halo box)
  for (let pass = 0; pass < erodePasses; pass++) {
    const toClear = [];
    for (let i = 0; i < width * height; i++) {
      if (data[i * 4 + 3] === 0) continue;
      const x = i % width;
      const y = (i / width) | 0;
      let bgNeighbors = 0;
      if (x === 0 || bgMask[i - 1]) bgNeighbors++;
      if (x === width - 1 || bgMask[i + 1]) bgNeighbors++;
      if (y === 0 || bgMask[i - width]) bgNeighbors++;
      if (y === height - 1 || bgMask[i + width]) bgNeighbors++;

      const p = i * 4;
      const lum = 0.299 * data[p] + 0.587 * data[p + 1] + 0.114 * data[p + 2];
      // Only erode light edge pixels (halo), keep dark print ink
      if (bgNeighbors >= 1 && lum > 200) toClear.push(i);
      else if (bgNeighbors >= 2 && lum > 160) toClear.push(i);
    }
    for (const i of toClear) {
      data[i * 4 + 3] = 0;
      bgMask[i] = 1;
    }
  }

  // Binary alpha: no semi-transparent haze on black UI
  for (let i = 0; i < data.length; i += 4) {
    if (data[i + 3] === 0) continue;
    if (data[i + 3] < 250) {
      const lum = 0.299 * data[i] + 0.587 * data[i + 1] + 0.114 * data[i + 2];
      if (lum > 150) data[i + 3] = 0;
      else data[i + 3] = 255;
    } else {
      data[i + 3] = 255;
    }
  }

  return imageData;
}

function canvasFromImageData(imageData) {
  const canvas = document.createElement('canvas');
  canvas.width = imageData.width;
  canvas.height = imageData.height;
  const ctx = canvas.getContext('2d');
  ctx.putImageData(imageData, 0, 0);
  return canvas;
}

/** Crop to opaque content so leftover transparent padding is gone */
function cropToOpaque(canvas) {
  const ctx = canvas.getContext('2d', { willReadFrequently: true });
  const { width, height } = canvas;
  const { data } = ctx.getImageData(0, 0, width, height);
  let minX = width, minY = height, maxX = -1, maxY = -1;
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      if (data[(y * width + x) * 4 + 3] > 0) {
        if (x < minX) minX = x;
        if (y < minY) minY = y;
        if (x > maxX) maxX = x;
        if (y > maxY) maxY = y;
      }
    }
  }
  if (maxX < 0) return canvas;
  const pad = 2;
  minX = Math.max(0, minX - pad);
  minY = Math.max(0, minY - pad);
  maxX = Math.min(width - 1, maxX + pad);
  maxY = Math.min(height - 1, maxY + pad);
  const w = maxX - minX + 1;
  const h = maxY - minY + 1;
  const out = document.createElement('canvas');
  out.width = w;
  out.height = h;
  out.getContext('2d').drawImage(canvas, minX, minY, w, h, 0, 0, w, h);
  return out;
}

function canvasToPngBlob(canvas) {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      b => (b ? resolve(b) : reject(new Error('Falha ao processar imagem'))),
      'image/png'
    );
  });
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
  return canvasToPngBlob(cropToOpaque(canvas));
}

/**
 * Load any image URL (via same-origin proxy when needed) and return a PNG data URL
 * with studio background removed.
 */
export async function urlToTransparentDataUrl(src) {
  if (!src) return src;

  const candidates = [src];
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
      const cleaned = canvasFromImageData(imageData);
      return cropToOpaque(cleaned).toDataURL('image/png');
    } catch (err) {
      lastError = err;
    }
  }
  throw lastError || new Error('Não foi possível processar a imagem');
}
