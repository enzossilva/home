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

        {/* —— Contorno da peça (só traço branco) —— */}
        <g fill="none" stroke="#fff" strokeWidth="1.15" strokeLinejoin="round" strokeLinecap="round">
          {/* Ombros + mangas + corpo */}
          <path d="
            M168 78
            L148 98
            L118 88
            L72 128
            L72 158
            L108 172
            L108 400
            L312 400
            L312 172
            L348 158
            L348 128
            L302 88
            L272 98
            L252 78
            L238 102
            L210 94
            L182 102
            Z
          " />
          {/* Gola */}
          <path d="M168 78 L182 102 Q210 118 238 102 L252 78" />
          <path d="M186 104 Q210 122 234 104" />
          {/* Placket / botões (traço leve) */}
          <line x1="210" y1="118" x2="210" y2="168" strokeOpacity="0.55" />
          <circle cx="210" cy="130" r="2.2" strokeOpacity="0.55" />
          <circle cx="210" cy="148" r="2.2" strokeOpacity="0.55" />
          {/* Bainha inferior */}
          <line x1="108" y1="400" x2="312" y2="400" />
          {/* Costura manga esquerda */}
          <line x1="108" y1="172" x2="108" y2="400" strokeOpacity="0.35" />
          <line x1="312" y1="172" x2="312" y2="400" strokeOpacity="0.35" />
        </g>

        {/* —— MANGA (seta vermelha diagonal) —— */}
        <line
          x1="118" y1="96"
          x2="78" y2="148"
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
          x1="118" y1="210"
          x2="302" y2="210"
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
          x1="330" y1="88"
          x2="330" y2="400"
          stroke="#e63946"
          strokeWidth="1.2"
          markerStart="url(#arrow-start)"
          markerEnd="url(#arrow)"
        />
        <text
          x="348"
          y="230"
          fill="#e63946"
          fontSize="9"
          fontFamily="Anton, Impact, Arial Narrow, sans-serif"
          letterSpacing="1.5"
          transform="rotate(90 348 230)"
        >
          COMPRIMENTO
        </text>
        {measurements.map((m, i) => (
          <text
            key={`comp-${m.size}`}
            x="348"
            y={100 + i * 13}
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
