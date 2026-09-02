import './Greeting.css';

import { useState } from 'react';
import { Component, UserAgentGenerator, UserAgentInfo, UserAgentParser } from 'library';
import type { AnimationEvent } from 'react';

// Thin harness proving `:library` works as a consumed dependency on the web
// (JS) target -- not a real app experience. Parses a representative UA
// string and generates a UA string from structured data, displaying both.
const SAMPLE_USER_AGENT =
  'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) ' +
  'Chrome/128.0.6613.120 Safari/537.36';

export function Greeting() {
  const parsed = UserAgentParser.getInstance().parse(SAMPLE_USER_AGENT);
  const generated = UserAgentGenerator.getInstance().generate(
    new UserAgentInfo(new Component('Chrome', '128.0'), new Component('Blink', '128.0'), new Component('Windows', '10'), null)
  );

  const [isVisible, setIsVisible] = useState<boolean>(false);
  const [isAnimating, setIsAnimating] = useState<boolean>(false);

  const handleClick = () => {
    if (isVisible) {
      setIsAnimating(true);
    } else {
      setIsVisible(true);
    }
  };

  const handleAnimationEnd = (event: AnimationEvent<HTMLDivElement>) => {
    if (event.animationName === 'fadeOut') {
      setIsVisible(false);
      setIsAnimating(false);
    }
  };

  return (
    <div className="greeting-container">
      <button onClick={handleClick} className="greeting-button">
        Click me!
      </button>

      {isVisible && (
        <div className={isAnimating ? 'greeting-content fade-out' : 'greeting-content'} onAnimationEnd={handleAnimationEnd}>
          <p>UserAgentParser.parse():</p>
          <p>UA: {SAMPLE_USER_AGENT}</p>
          <p>browser: {String(parsed.browser)}</p>
          <p>engine: {String(parsed.engine)}</p>
          <p>os: {String(parsed.os)}</p>
          <p>device: {String(parsed.device)}</p>
          <p>UserAgentGenerator.generate():</p>
          <p>{generated}</p>
        </div>
      )}
    </div>
  );
}
