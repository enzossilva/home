/** Medidas padrão — camiseta (cm). Aprox. ±0,5 cm. */
export const CAMISETA_MEASUREMENTS = [
  { size: 'PP',  peito: 48, comprimento: 66, manga: 19 },
  { size: 'P',   peito: 51, comprimento: 69, manga: 20 },
  { size: 'M',   peito: 54, comprimento: 72, manga: 21 },
  { size: 'G',   peito: 57, comprimento: 75, manga: 22 },
  { size: 'GG',  peito: 61, comprimento: 78, manga: 23 },
  { size: 'XGG', peito: 65, comprimento: 81, manga: 24 },
];

const DEFAULT_CHART = '/size-chart-camiseta.png';

/**
 * Guia de medidas: vetor de referência (setas manga / peito / comprimento)
 * + tabela de cm por tamanho.
 */
export default function SizeChart({ imageUrl, measurements = CAMISETA_MEASUREMENTS }) {
  const src = imageUrl || DEFAULT_CHART;
  const fmt = v => Number(v).toFixed(1).replace(/\.0$/, '');

  return (
    <div className="size-chart">
      <img
        src={src}
        alt="Guia de medidas da camiseta: manga, peito e comprimento"
        className="size-chart-img"
      />

      {!imageUrl && (
        <div className="size-chart-legend">
          <table className="size-chart-table">
            <thead>
              <tr>
                <th>Tam.</th>
                <th>Manga</th>
                <th>Peito</th>
                <th>Comp.</th>
              </tr>
            </thead>
            <tbody>
              {measurements.map(m => (
                <tr key={m.size}>
                  <td>{m.size}</td>
                  <td>{fmt(m.manga)} cm</td>
                  <td>{fmt(m.peito)} cm</td>
                  <td>{fmt(m.comprimento)} cm</td>
                </tr>
              ))}
            </tbody>
          </table>
          <p className="size-chart-note">Medidas em cm. Podem variar ± 0,5 cm.</p>
        </div>
      )}
    </div>
  );
}
