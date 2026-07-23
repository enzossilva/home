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
 * Guia de medidas: vetor com setas + textos leves nas mesmas
 * posições da referência (manga canto esq., peito no torso, comprimento canto dir.).
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
    <div className="size-chart size-chart-frame">
      <img
        src={DEFAULT_CHART}
        alt="Guia de medidas da camiseta"
        className="size-chart-img size-chart-img-bg"
      />

      <div className="size-chart-block size-chart-manga">
        <p className="size-chart-label">MANGA</p>
        {measurements.map(m => (
          <p key={`manga-${m.size}`} className="size-chart-value">
            {m.size} = {fmt(m.manga)} cm
          </p>
        ))}
      </div>

      <div className="size-chart-block size-chart-peito">
        <p className="size-chart-label">PEITO</p>
        {measurements.map(m => (
          <p key={`peito-${m.size}`} className="size-chart-value">
            {m.size} = {fmt(m.peito)} cm
          </p>
        ))}
      </div>

      <div className="size-chart-block size-chart-comp">
        <p className="size-chart-label">COMPRIMENTO</p>
        {measurements.map(m => (
          <p key={`comp-${m.size}`} className="size-chart-value">
            {m.size} = {fmt(m.comprimento)} cm
          </p>
        ))}
      </div>
    </div>
  );
}
