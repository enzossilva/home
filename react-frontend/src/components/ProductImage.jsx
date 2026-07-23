import { useEffect, useState } from 'react';
import { urlToTransparentDataUrl } from '../utils/imageBg';

/**
 * Product photo with studio background removed (transparent PNG).
 */
export default function ProductImage({ src, alt, className, onClick, style }) {
  const [url, setUrl] = useState(src);
  const [processing, setProcessing] = useState(Boolean(src));

  useEffect(() => {
    if (!src) {
      setUrl('');
      setProcessing(false);
      return;
    }

    let cancelled = false;
    setUrl(src);
    setProcessing(true);

    urlToTransparentDataUrl(src)
      .then(dataUrl => {
        if (!cancelled) setUrl(dataUrl);
      })
      .catch(() => {
        if (!cancelled) setUrl(src);
      })
      .finally(() => {
        if (!cancelled) setProcessing(false);
      });

    return () => { cancelled = true; };
  }, [src]);

  return (
    <img
      src={url || src}
      alt={alt}
      className={className}
      onClick={onClick}
      style={{
        ...style,
        opacity: processing && url === src ? 0.85 : style?.opacity,
      }}
      onError={e => {
        e.currentTarget.style.display = 'none';
      }}
    />
  );
}
