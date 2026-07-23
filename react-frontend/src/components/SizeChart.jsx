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
 * Guia de medidas no modelo de referência:
 * contorno fino branco + 3 setas vermelhas (manga / peito / comprimento no centro).
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
  const font = 'Anton, Impact, Arial Narrow, sans-serif';

  return (
    <div className="size-chart">
      <svg
        className="size-chart-svg"
        viewBox="0 0 360 520"
        xmlns="http://www.w3.org/2000/svg"
        role="img"
        aria-label="Guia de medidas da camiseta"
      >
        <defs>
          <marker id="arr" markerWidth="5" markerHeight="5" refX="4.5" refY="2.5" orient="auto">
            <path d="M0,0 L5,2.5 L0,5 Z" fill="#e53935" />
          </marker>
          <marker id="arr-rev" markerWidth="5" markerHeight="5" refX="0.5" refY="2.5" orient="auto-start-reverse">
            <path d="M0,0 L5,2.5 L0,5 Z" fill="#e53935" />
          </marker>
        </defs>

        {/*
          Contorno clássico de camiseta (front flat),
          no estilo da referência: gola redonda, manga curta, bainha reta.
          Coordenadas centrais: CX=180
        */}
        <g fill="none" stroke="#fff" strokeWidth="1.2" strokeLinejoin="round" strokeLinecap="round">
          {/* Corpo + mangas em um path contínuo */}
          <path d="
            M 132 78
            L 108 102
            L 48 118
            L 48 168
            L 98 182
            L 98 430
            L 262 430
            L 262 182
            L 312 168
            L 312 118
            L 252 102
            L 228 78
            L 210 108
            L 180 100
            L 150 108
            Z
          " />
          {/* Abertura da gola (crew) */}
          <path d="M 150 108 Q 180 138 210 108" />
          {/* Bainha */}
          <line x1="98" y1="430" x2="262" y2="430" />
        </g>

        {/* —— MANGA: diagonal no ombro/manga esquerdos —— */}
        <line
          x1="110" y1="100"
          x2="52" y2="160"
          stroke="#e53935"
          strokeWidth="1.35"
          markerStart="url(#arr-rev)"
          markerEnd="url(#arr)"
        />
        <text x="8" y="72" fill="#fff" fontSize="10" fontFamily={font} letterSpacing="1.6">MANGA</text>
        {measurements.map((m, i) => (
          <text
            key={`manga-${m.size}`}
            x="8"
            y={88 + i * 14}
            fill="rgba(255,255,255,0.78)"
            fontSize="9"
            fontFamily={font}
            letterSpacing="0.6"
          >
            {m.size} = {fmt(m.manga)} cm
          </text>
        ))}

        {/* —— PEITO: horizontal de cava a cava —— */}
        <line
          x1="98" y1="200"
          x2="262" y2="200"
          stroke="#e53935"
          strokeWidth="1.35"
          markerStart="url(#arr-rev)"
          markerEnd="url(#arr)"
        />
        <text x="8" y="220" fill="#fff" fontSize="10" fontFamily={font} letterSpacing="1.6">PEITO</text>
        {measurements.map((m, i) => (
          <text
            key={`peito-${m.size}`}
            x="8"
            y={236 + i * 14}
            fill="rgba(255,255,255,0.78)"
            fontSize="9"
            fontFamily={font}
            letterSpacing="0.6"
          >
            {m.size} = {fmt(m.peito)} cm
          </text>
        ))}

        {/* —— COMPRIMENTO: vertical no CENTRO (gola → bainha) —— */}
        <line
          x1="180" y1="108"
          x2="180" y2="430"
          stroke="#e53935"
          strokeWidth="1.35"
          markerStart="url(#arr-rev)"
          markerEnd="url(#arr)"
        />
        <text x="272" y="360" fill="#fff" fontSize="10" fontFamily={font} letterSpacing="1.6">COMPRIMENTO</text>
        {measurements.map((m, i) => (
          <text
            key={`comp-${m.size}`}
            x="272"
            y={376 + i * 14}
            fill="rgba(255,255,255,0.78)"
            fontSize="9"
            fontFamily={font}
            letterSpacing="0.6"
          >
            {m.size} = {fmt(m.comprimento)} cm
          </text>
        ))}

        <text
          x="180"
          y="505"
          textAnchor="middle"
          fill="rgba(255,255,255,0.32)"
          fontSize="8"
          fontFamily={font}
          letterSpacing="0.5"
        >
          Medidas em cm. Podem variar ± 0,5 cm.
        </text>
      </svg>
    </div>
  );
}
