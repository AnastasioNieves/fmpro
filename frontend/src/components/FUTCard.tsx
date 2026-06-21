import React from 'react';
import { assetUrl } from '../api/client';
import './FUTCard.css';

export interface FUTCardProps {
  name: string;
  position: string;
  photoUrl?: string | null;
  rating: number;
  stats: {
    label1: string; val1: string | number;
    label2: string; val2: string | number;
    label3: string; val3: string | number;
    label4: string; val4: string | number;
    label5: string; val5: string | number;
    label6: string; val6: string | number;
  };
  variant?: 'gold' | 'totw';
}

export function FUTCard({ name, position, photoUrl, rating, stats, variant = 'gold' }: FUTCardProps) {
  const photo = photoUrl ? assetUrl(photoUrl) : null;
  
  return (
    <div className={`fut-card fut-card--${variant}`}>
      <div className="fut-card__bg"></div>
      <div className="fut-card__top">
        <div className="fut-card__info">
          <div className="fut-card__rating">{rating}</div>
          <div className="fut-card__position">{position.substring(0,3).toUpperCase()}</div>
        </div>
        <div className="fut-card__photo-container">
          {photo ? (
            <img src={photo} alt={name} className="fut-card__photo" />
          ) : (
             <div className="fut-card__placeholder"></div>
          )}
        </div>
      </div>
      <div className="fut-card__bottom">
        <div className="fut-card__name">{name}</div>
        <div className="fut-card__divider"></div>
        <div className="fut-card__stats">
          <div className="fut-card__stat-col">
            <div className="fut-card__stat"><span className="val">{stats.val1}</span> <span className="lbl">{stats.label1}</span></div>
            <div className="fut-card__stat"><span className="val">{stats.val2}</span> <span className="lbl">{stats.label2}</span></div>
            <div className="fut-card__stat"><span className="val">{stats.val3}</span> <span className="lbl">{stats.label3}</span></div>
          </div>
          <div className="fut-card__stat-col">
            <div className="fut-card__stat"><span className="val">{stats.val4}</span> <span className="lbl">{stats.label4}</span></div>
            <div className="fut-card__stat"><span className="val">{stats.val5}</span> <span className="lbl">{stats.label5}</span></div>
            <div className="fut-card__stat"><span className="val">{stats.val6}</span> <span className="lbl">{stats.label6}</span></div>
          </div>
        </div>
      </div>
    </div>
  );
}
