import { useEffect, useState } from 'react';
import { getVideos } from '../api';

function getVideoId(url) {
  const match = url.match(/(?:youtu\.be\/|v=|\/embed\/)([a-zA-Z0-9_-]{11})/);
  return match ? match[1] : null;
}

function getThumbnail(url) {
  const id = getVideoId(url);
  return id ? `https://img.youtube.com/vi/${id}/maxresdefault.jpg` : null;
}

export default function Videos() {
  const [videos, setVideos] = useState([]);
  const [loading, setLoading] = useState(true);

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
        <div className="videos-carousel">
          {videos.map(video => {
            const thumb = getThumbnail(video.youtubeUrl);
            return (
              <a
                key={video.id}
                href={video.youtubeUrl}
                target="_blank"
                rel="noreferrer"
                className="video-card"
              >
                <div className="video-thumb-wrap">
                  {thumb
                    ? <img src={thumb} alt={video.title || 'Video'} />
                    : <div className="video-no-thumb">▶</div>
                  }
                  <div className="video-play-icon">▶</div>
                </div>
                {video.title && <p className="video-title">{video.title}</p>}
              </a>
            );
          })}
        </div>
      )}
    </main>
  );
}
