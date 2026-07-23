/** Medidas padrão — camiseta (cm). Aprox. ±0,5 cm. */
export const CAMISETA_MEASUREMENTS = [
  { size: 'PP',  peito: 48, comprimento: 66, manga: 19 },
  { size: 'P',   peito: 51, comprimento: 69, manga: 20 },
  { size: 'M',   peito: 54, comprimento: 72, manga: 21 },
  { size: 'G',   peito: 57, comprimento: 75, manga: 22 },
  { size: 'GG',  peito: 61, comprimento: 78, manga: 23 },
  { size: 'XGG', peito: 65, comprimento: 81, manga: 24 },
];

/**
 * Guia de medidas: só traços (wireframe), no estilo técnico da referência.
 * Se `imageUrl` for passado, usa a imagem custom.
 */
export default function SizeChart({ imageUrl, measurements = CAMISETA_MEASUREMENTS }) {
  if (imageUrl) {
    return (
      <div className="size-chart">
        <img src={imageUrl} alt="Guia de tamanhos" className="size-chart-img" />
      </div>
    );
  }

  const fmt = v => Number(v).toFixed(1).replace(/\.0$/, '');

  return (
    <div className="size-chart">
      <svg
        className="size-chart-svg"
        viewBox="0 0 420 480"
        xmlns="http://www.w3.org/2000/svg"
        role="img"
        aria-label="Guia de medidas da camiseta"
      >
        <defs>
          <marker id="arrow" markerWidth="6" markerHeight="6" refX="3" refY="3" orient="auto">
            <path d="M0,0 L6,3 L0,6 Z" fill="#e63946" />
          </marker>
          <marker id="arrow-start" markerWidth="6" markerHeight="6" refX="3" refY="3" orient="auto-start-reverse">
            <path d="M0,0 L6,3 L0,6 Z" fill="#e63946" />
          </marker>
        </defs>

        {/* —— Contorno camiseta (crew neck, só traço) —— */}
        <g fill="none" stroke="#fff" strokeWidth="1.15" strokeLinejoin="round" strokeLinecap="round">
          {/* Silhueta: ombros, mangas curtas, corpo, bainha */}
          <path d="
            M155 92
            L128 108
            L78 118
            L78 168
            L118 178
            L118 400
            L302 400
            L302 178
            L342 168
            L342 118
            L292 108
            L265 92
            L248 118
            L210 110
            L172 118
            Z
          " />
          {/* Gola redonda (crew) — abertura */}
          <path d="M172 118 Q210 148 248 118" />
          {/* Linha interna da gola */}
          <path d="M180 122 Q210 142 240 122" strokeOpacity="0.5" />
          {/* Bainha */}
          <line x1="118" y1="400" x2="302" y2="400" />
          {/* Costura lateral (leve) */}
          <line x1="118" y1="178" x2="118" y2="400" strokeOpacity="0.3" />
          <line x1="302" y1="178" x2="302" y2="400" strokeOpacity="0.3" />
        </g>

        {/* —— MANGA (seta vermelha diagonal) —— */}
        <line
          x1="128" y1="112"
          x2="82" y2="160"
          stroke="#e63946"
          strokeWidth="1.2"
          markerStart="url(#arrow-start)"
          markerEnd="url(#arrow)"
        />
        <text x="24" y="88" fill="#e63946" fontSize="9" fontFamily="Anton, Impact, Arial Narrow, sans-serif" letterSpacing="1.5">MANGA</text>
        {measurements.map((m, i) => (
          <text
            key={`manga-${m.size}`}
            x="24"
            y={104 + i * 13}
            fill="rgba(255,255,255,0.72)"
            fontSize="8"
            fontFamily="Anton, Impact, Arial Narrow, sans-serif"
            letterSpacing="0.5"
          >
            {m.size} {fmt(m.manga)} cm
          </text>
        ))}

        {/* —— PEITO (seta horizontal) —— */}
        <line
          x1="128" y1="210"
          x2="292" y2="210"
          stroke="#e63946"
          strokeWidth="1.2"
          markerStart="url(#arrow-start)"
          markerEnd="url(#arrow)"
        />
        <text x="188" y="200" fill="#e63946" fontSize="9" fontFamily="Anton, Impact, Arial Narrow, sans-serif" letterSpacing="1.5">PEITO</text>
        {measurements.map((m, i) => (
          <text
            key={`peito-${m.size}`}
            x="168"
            y={228 + i * 13}
            fill="rgba(255,255,255,0.72)"
            fontSize="8"
            fontFamily="Anton, Impact, Arial Narrow, sans-serif"
            letterSpacing="0.5"
          >
            {m.size} {fmt(m.peito)} cm
          </text>
        ))}

        {/* —— COMPRIMENTO (seta vertical) —— */}
        <line
          x1="322" y1="100"
          x2="322" y2="400"
          stroke="#e63946"
          strokeWidth="1.2"
          markerStart="url(#arrow-start)"
          markerEnd="url(#arrow)"
        />
        <text
          x="340"
          y="230"
          fill="#e63946"
          fontSize="9"
          fontFamily="Anton, Impact, Arial Narrow, sans-serif"
          letterSpacing="1.5"
          transform="rotate(90 340 230)"
        >
          COMPRIMENTO
        </text>
        {measurements.map((m, i) => (
          <text
            key={`comp-${m.size}`}
            x="340"
            y={110 + i * 13}
            fill="rgba(255,255,255,0.72)"
            fontSize="8"
            fontFamily="Anton, Impact, Arial Narrow, sans-serif"
            letterSpacing="0.5"
          >
            {m.size} {fmt(m.comprimento)} cm
          </text>
        ))}

        <text
          x="210"
          y="455"
          textAnchor="middle"
          fill="rgba(255,255,255,0.35)"
          fontSize="7.5"
          fontFamily="Anton, Impact, Arial Narrow, sans-serif"
          letterSpacing="0.4"
        >
          Medidas em cm. Podem variar ± 0,5 cm.
        </text>
      </svg>
    </div>
  );
}
