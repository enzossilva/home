import { useRef, useState } from 'react';

const CLOUD_NAME = import.meta.env.VITE_CLOUDINARY_CLOUD_NAME;
const UPLOAD_PRESET = import.meta.env.VITE_CLOUDINARY_UPLOAD_PRESET;

export default function ImageUpload({ value, onChange }) {
  const inputRef = useRef(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');

  async function handleFile(e) {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!CLOUD_NAME || !UPLOAD_PRESET) {
      setError('Cloudinary não configurado. Use URL manual ou configure VITE_CLOUDINARY_CLOUD_NAME e VITE_CLOUDINARY_UPLOAD_PRESET no .env');
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      setError('Imagem muito grande. Máximo 5MB.');
      return;
    }

    setUploading(true);
    setError('');
    try {
      const form = new FormData();
      form.append('file', file);
      form.append('upload_preset', UPLOAD_PRESET);
      form.append('folder', 'young-zone');

      const res = await fetch(`https://api.cloudinary.com/v1_1/${CLOUD_NAME}/image/upload`, {
        method: 'POST',
        body: form,
      });

      const data = await res.json();
      if (!res.ok) throw new Error(data.error?.message || 'Erro no upload');
      onChange(data.secure_url);
    } catch (err) {
      setError(err.message);
    } finally {
      setUploading(false);
      inputRef.current.value = '';
    }
  }

  return (
    <div className="image-upload">
      {value && (
        <div className="image-upload-preview">
          <img src={value} alt="Preview" />
          <button type="button" className="image-upload-remove" onClick={() => onChange('')}>✕</button>
        </div>
      )}

      <div className="image-upload-controls">
        <button
          type="button"
          className="btn btn-secondary btn-sm"
          onClick={() => inputRef.current.click()}
          disabled={uploading}
        >
          {uploading ? 'Enviando...' : value ? 'Trocar imagem' : 'Fazer upload'}
        </button>
        <input
          ref={inputRef}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          onChange={handleFile}
          style={{ display: 'none' }}
        />
        <span style={{ fontSize: '0.78rem', color: '#aaa' }}>ou</span>
        <input
          type="url"
          value={value}
          onChange={e => onChange(e.target.value)}
          placeholder="https://... (URL da imagem)"
          style={{ flex: 1, padding: '0.4rem 0.6rem', border: '1px solid #ddd', fontSize: '0.85rem', borderRadius: 4 }}
        />
      </div>

      {error && <p className="error" style={{ marginTop: '0.4rem', fontSize: '0.82rem' }}>{error}</p>}
    </div>
  );
}
