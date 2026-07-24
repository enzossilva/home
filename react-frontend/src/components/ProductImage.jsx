/**
 * Product photo — uses the stored image as-is (same as zoom),
 * without client-side reprocessing that adds a white fringe.
 */
export default function ProductImage({ src, alt, className, onClick, style }) {
  if (!src) return null;

  return (
    <img
      src={src}
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
