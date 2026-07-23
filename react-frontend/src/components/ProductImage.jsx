import { useEffect, useState } from 'react';
import { urlToTransparentDataUrl } from '../utils/imageBg';

/**
 * Product photo with studio background removed (transparent PNG).
 */
export default function ProductImage({ src, alt, className, onClick, style }) {
  const [url, setUrl] = useState(src);

  useEffect(() => {
    if (!src) {
      setUrl('');
      return;
    }

    let cancelled = false;
    setUrl(src);

    urlToTransparentDataUrl(src)
      .then(dataUrl => {
        if (!cancelled) setUrl(dataUrl);
      })
      .catch(() => {
        if (!cancelled) setUrl(src);
      });

    return () => { cancelled = true; };
  }, [src]);

  return (
    <img
      src={url || src}
      alt={alt}
      className={className}
      onClick={onClick}
      style={style}
      onError={e => {
        e.currentTarget.style.display = 'none';
      }}
    />
  );
}
