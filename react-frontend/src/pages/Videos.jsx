import { useEffect, useState } from 'react';
import { getVideos } from '../api';

function getVideoId(url) {
  const match = url.match(/(?:youtu\.be\/|v=|\/embed\/)([a-zA-Z0-9_-]{11})/);
  return match ? match[1] : null;
}

export default function Videos() {
  const [videos, setVideos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [openId, setOpenId] = useState(null);

  useEffect(() => {
    getVideos()
      .then(setVideos)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <main className="videos-page"><p className="loading">Carregando...</p></main>;

  return (
    <main className="videos-page">
      {videos.length === 0 ? (
        <p className="empty" style={{ textAlign: 'center', padding: '4rem' }}>Em breve.</p>
      ) : (
        <div className="videos-list">
          {videos.map(video => {
            const vid = getVideoId(video.youtubeUrl);
            const isOpen = openId === video.id;
            return (
              <article key={video.id} className="video-item">
                <div className="video-embed-wrap">
                  {vid ? (
                    <iframe
                      src={`https://www.youtube.com/embed/${vid}`}
                      title={video.title || 'Video'}
                      allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                      allowFullScreen
                    />
                  ) : (
                    <div className="video-no-embed">URL inválida</div>
                  )}
                </div>

                <div className="video-meta">
                  {video.title && <h2 className="video-meta-title">{video.title}</h2>}
                  {video.description && (
                    <button
                      className={`video-desc-toggle ${isOpen ? 'open' : ''}`}
                      onClick={() => setOpenId(isOpen ? null : video.id)}
                      aria-label="Descrição"
                    >
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <polyline points="6 9 12 15 18 9" />
                      </svg>
                    </button>
                  )}
                </div>

                {video.description && (
                  <div className={`video-desc-body ${isOpen ? 'open' : ''}`}>
                    <p>{video.description}</p>
                  </div>
                )}
              </article>
            );
          })}
        </div>
      )}
    </main>
  );
}
