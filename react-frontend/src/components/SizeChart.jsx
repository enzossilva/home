/** Medidas padrão — camiseta (cm, peça estendida). Aprox. ±0,5 cm. */
export const CAMISETA_MEASUREMENTS = [
  { size: 'PP',  peito: 48, comprimento: 66, manga: 19 },
  { size: 'P',   peito: 51, comprimento: 69, manga: 20 },
  { size: 'M',   peito: 54, comprimento: 72, manga: 21 },
  { size: 'G',   peito: 57, comprimento: 75, manga: 22 },
  { size: 'GG',  peito: 61, comprimento: 78, manga: 23 },
  { size: 'XGG', peito: 65, comprimento: 81, manga: 24 },
];

/**
 * Guia visual de medidas para camiseta.
 * Se `imageUrl` for passado, usa a imagem custom; senão, SVG padrão.
 */
export default function SizeChart({ imageUrl, measurements = CAMISETA_MEASUREMENTS }) {
  if (imageUrl) {
    return (
      <div className="size-chart">
        <img src={imageUrl} alt="Guia de tamanhos" className="size-chart-img" />
      </div>
    );
  }

  return (
    <div className="size-chart">
      <div className="size-chart-visual">
        <svg
          className="size-chart-svg"
          viewBox="0 0 320 360"
          xmlns="http://www.w3.org/2000/svg"
          aria-hidden="true"
        >
          {/* Contorno da camiseta */}
          <path
            d="M110 58 L78 78 L52 118 L78 128 L78 300 L242 300 L242 128 L268 118 L242 78 L210 58
               L198 78 L160 70 L122 78 Z"
            fill="none"
            stroke="#fff"
            strokeWidth="1.4"
            strokeLinejoin="round"
          />
          {/* Gola */}
          <path
            d="M122 78 Q160 98 198 78"
            fill="none"
            stroke="#fff"
            strokeWidth="1.2"
          />

          {/* Manga — linha vermelha */}
          <line x1="78" y1="90" x2="58" y2="125" stroke="#e63946" strokeWidth="1.5" />
          <polygon points="58,125 64,118 68,124" fill="#e63946" />
          <text x="28" y="108" fill="#e63946" fontSize="9" fontFamily="sans-serif" letterSpacing="1">MANGA</text>

          {/* Peito — linha vermelha */}
          <line x1="88" y1="155" x2="232" y2="155" stroke="#e63946" strokeWidth="1.5" />
          <polygon points="88,155 96,151 96,159" fill="#e63946" />
          <polygon points="232,155 224,151 224,159" fill="#e63946" />
          <text x="140" y="148" fill="#e63946" fontSize="9" fontFamily="sans-serif" letterSpacing="1">PEITO</text>

          {/* Comprimento — linha vermelha */}
          <line x1="250" y1="78" x2="250" y2="300" stroke="#e63946" strokeWidth="1.5" />
          <polygon points="250,78 246,86 254,86" fill="#e63946" />
          <polygon points="250,300 246,292 254,292" fill="#e63946" />
          <text x="258" y="190" fill="#e63946" fontSize="9" fontFamily="sans-serif" letterSpacing="1"
            transform="rotate(90 258 190)">COMPRIMENTO</text>
        </svg>

        <div className="size-chart-tables">
          <MeasureBlock
            title="MANGA"
            rows={measurements.map(m => ({ size: m.size, value: m.manga }))}
          />
          <MeasureBlock
            title="PEITO"
            rows={measurements.map(m => ({ size: m.size, value: m.peito }))}
          />
          <MeasureBlock
            title="COMPRIMENTO"
            rows={measurements.map(m => ({ size: m.size, value: m.comprimento }))}
          />
        </div>
      </div>
      <p className="size-chart-note">Medidas em cm. Podem variar ± 0,5 cm.</p>
    </div>
  );
}

function MeasureBlock({ title, rows }) {
  return (
    <div className="size-chart-block">
      <p className="size-chart-block-title">{title}</p>
      <ul className="size-chart-list">
        {rows.map(r => (
          <li key={r.size}>
            <span>{r.size}</span>
            <span>{Number(r.value).toFixed(1).replace('.0', '')} cm</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
