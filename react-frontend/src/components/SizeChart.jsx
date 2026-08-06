/** Medidas padrão — camiseta (cm). */
export const CAMISETA_MEASUREMENTS = [
  { size: 'M',  largura: 57, comprimento: 64, manga: 20 },
  { size: 'G',  largura: 61, comprimento: 77, manga: 22 },
  { size: 'GG', largura: 62, comprimento: 78, manga: 23 },
];

const DEFAULT_CHART = '/size-chart-camiseta.png';

/**
 * Guia de medidas: vetor com setas + textos leves nas mesmas
 * posições da referência (manga canto esq., largura no torso, comprimento canto dir.).
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
        <p className="size-chart-label">LARGURA</p>
        {measurements.map(m => (
          <p key={`largura-${m.size}`} className="size-chart-value">
            {m.size} = {fmt(m.largura ?? m.peito)} cm
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
