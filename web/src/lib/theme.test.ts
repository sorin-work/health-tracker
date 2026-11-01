import { afterEach, describe, expect, it } from 'vitest';
import { applyTheme, getTheme, setTheme } from './theme';

describe('theme', () => {
  afterEach(() => {
    delete document.documentElement.dataset.theme;
  });

  it('follows the system by default', () => {
    expect(getTheme()).toBe('system');
  });

  it('remembers a chosen theme and applies it', () => {
    setTheme('dark');

    expect(getTheme()).toBe('dark');
    expect(document.documentElement.dataset.theme).toBe('dark');
  });

  it('going back to system clears the override', () => {
    setTheme('light');
    setTheme('system');

    expect(getTheme()).toBe('system');
    expect(document.documentElement.dataset.theme).toBeUndefined();
  });

  it('ignores unknown stored values', () => {
    localStorage.setItem('health-tracker.theme', 'purple');

    expect(getTheme()).toBe('system');
    applyTheme(getTheme());
    expect(document.documentElement.dataset.theme).toBeUndefined();
  });
});
