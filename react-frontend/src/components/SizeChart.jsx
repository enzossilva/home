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
 * Guia de medidas: só traços (wireframe).
 * Camiseta crew-neck — gola baixa, manga um pouco mais longa.
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

        {/* Silhueta: gola mais baixa + mangas mais longas */}
        <g fill="none" stroke="#fff" strokeWidth="1.15" strokeLinejoin="round" strokeLinecap="round">
          <path d="
            M152 78
            L122 96
            L58 112
            L52 210
            L112 222
            L112 400
            L308 400
            L308 222
            L368 210
            L362 112
            L298 96
            L268 78
            L250 128
            L210 118
            L170 128
            Z
          " />
          {/* Gola crew baixa/aberta */}
          <path d="M170 128 Q210 175 250 128" />
          <path d="M178 132 Q210 168 242 132" strokeOpacity="0.45" />
          <line x1="112" y1="400" x2="308" y2="400" />
          <line x1="112" y1="222" x2="112" y2="400" strokeOpacity="0.3" />
          <line x1="308" y1="222" x2="308" y2="400" strokeOpacity="0.3" />
        </g>

        {/* MANGA */}
        <line
          x1="118" y1="100"
          x2="58" y2="200"
          stroke="#e63946"
          strokeWidth="1.2"
          markerStart="url(#arrow-start)"
          markerEnd="url(#arrow)"
        />
        <text x="18" y="88" fill="#e63946" fontSize="9" fontFamily="Anton, Impact, Arial Narrow, sans-serif" letterSpacing="1.5">MANGA</text>
        {measurements.map((m, i) => (
          <text
            key={`manga-${m.size}`}
            x="18"
            y={104 + i * 13}
            fill="rgba(255,255,255,0.72)"
            fontSize="8"
            fontFamily="Anton, Impact, Arial Narrow, sans-serif"
            letterSpacing="0.5"
          >
            {m.size} {fmt(m.manga)} cm
          </text>
        ))}

        {/* PEITO */}
        <line
          x1="122" y1="248"
          x2="298" y2="248"
          stroke="#e63946"
          strokeWidth="1.2"
          markerStart="url(#arrow-start)"
          markerEnd="url(#arrow)"
        />
        <text x="188" y="238" fill="#e63946" fontSize="9" fontFamily="Anton, Impact, Arial Narrow, sans-serif" letterSpacing="1.5">PEITO</text>
        {measurements.map((m, i) => (
          <text
            key={`peito-${m.size}`}
            x="168"
            y={266 + i * 13}
            fill="rgba(255,255,255,0.72)"
            fontSize="8"
            fontFamily="Anton, Impact, Arial Narrow, sans-serif"
            letterSpacing="0.5"
          >
            {m.size} {fmt(m.peito)} cm
          </text>
        ))}

        {/* COMPRIMENTO */}
        <line
          x1="326" y1="88"
          x2="326" y2="400"
          stroke="#e63946"
          strokeWidth="1.2"
          markerStart="url(#arrow-start)"
          markerEnd="url(#arrow)"
        />
        <text
          x="344"
          y="230"
          fill="#e63946"
          fontSize="9"
          fontFamily="Anton, Impact, Arial Narrow, sans-serif"
          letterSpacing="1.5"
          transform="rotate(90 344 230)"
        >
          COMPRIMENTO
        </text>
        {measurements.map((m, i) => (
          <text
            key={`comp-${m.size}`}
            x="344"
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
